package com.nitor.multimodalcoreweekdemo

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.microsoft.graph.concurrency.ICallback
import com.microsoft.graph.core.ClientException
import com.microsoft.identity.client.IAccount
import com.nitor.multimodalcoreweekdemo.intents.IntentAction
import com.nitor.multimodalcoreweekdemo.intents.IntentParser
import com.nitor.multimodalcoreweekdemo.models.Project
import com.nitor.multimodalcoreweekdemo.services.PulaattoriClient
import com.speechly.client.slu.Segment
import com.speechly.client.speech.Client
import com.speechly.ui.SpeechlyButton
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.InputStream
import java.util.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var microsoftAccount: MicrosoftAccount? = null

    private val speechlyClient: Client = Client.fromActivity(
        activity = this,
        appId = UUID.fromString("0f669f15-88dc-4429-b7fb-2b049302d0ad")
    )

    private val intentParser: IntentParser = IntentParser()

    private var speechlyButton: SpeechlyButton? = null
    private var textView: TextView? = null
    private var resultView: TextView? = null

    private var signInButton: Button? = null
    private var signOutButton: Button? = null
    private var currentUserTextView: TextView? = null
    private var currentUserImageView: ImageView? = null

    private lateinit var webView: WebView

    val projects:MutableLiveData<List<Project>> = MutableLiveData()

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeUi()

        val url: String = "https://hours.dev.nitor.zone/rest/projects"
        var cookie : String? = CookieManager.getInstance().getCookie(url)
        getCookieFromURL(url);
        /*
        if (cookie==null && microsoftAccount == null) {
            //User has not logged in with MSAL and cookie used for API is null => Log in to tuntipulaattori to extract cookie
            //TODO: Investigate how to do this better. Now 2 login screens are needed...
            getCookieFromURL(url);
        }*/

        microsoftAccount =  MicrosoftAccount(applicationContext,
            object: MicrosoftAccount.MicrosoftAccountCallback {
                override fun onAccountLoaded(account: IAccount?) {
                    updateUI(account)
                }
            }
        )

        GlobalScope.launch(Dispatchers.Default) {
            speechlyClient.onSegmentChange { segment: Segment ->
                val transcript: String = segment.words.values.joinToString(" ", transform = { it.value })
                GlobalScope.launch(Dispatchers.Main) {
                    textView?.text = transcript
                    intentParser.updateLatestSegment(segment)
                }
            }
        }

    }

    @ExperimentalCoroutinesApi
    @SuppressLint("ClickableViewAccessibility")
    private fun initializeUi() {
        speechlyButton = findViewById(R.id.speechly)
        textView = findViewById(R.id.textView)
        resultView = findViewById(R.id.resultView)
        textView?.visibility = View.INVISIBLE

        signInButton = findViewById(R.id.signIn)
        signOutButton = findViewById(R.id.clearCache)
        currentUserTextView = findViewById(R.id.currentUser)
        currentUserImageView = findViewById(R.id.avatar)
        webView = findViewById(R.id.webView)
        webView?.visibility = INVISIBLE

        // Start speechly context
        speechlyButton?.setOnTouchListener(View.OnTouchListener { _, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    textView?.visibility = View.VISIBLE
                    textView?.text = ""
                    resultView?.text = "..."
                    speechlyClient.startContext()
                }
                MotionEvent.ACTION_UP -> {
                    speechlyClient.stopContext()
                    iterateThroughProjects() //TODO: Remove. Simply a test that api-calls now work
                    GlobalScope.launch(Dispatchers.Default) {
                        delay(500)
                        //TODO how to make sure that the last segment has arrived?
                        val action: IntentAction? = intentParser.resolveAction()
                        intentParser.reset()
                        val result = action?.process() ?: "Intent not recognized"
                        withContext(Dispatchers.Main) {
                            textView?.visibility = View.INVISIBLE
                            resultView?.text = result
                        }
                    }
                }
            }
            true
        })

        // Sign in user
        signInButton?.setOnClickListener(View.OnClickListener {
            if (microsoftAccount == null) {
                return@OnClickListener
            }
            microsoftAccount!!.signIn(this@MainActivity)
        })

        // Sign out user
        signOutButton?.setOnClickListener(View.OnClickListener {
            if (microsoftAccount == null) {
                return@OnClickListener
            }
            microsoftAccount!!.signOut()
        })
    }

    private fun updateUI(account: IAccount?) {
        if (account != null) {
            signInButton?.isEnabled = false
            signOutButton?.isEnabled = true
            currentUserTextView?.text = account.username
            microsoftAccount!!.loadProfilePicture(object: ICallback<InputStream> {
                override fun success(photoInputStream: InputStream) {
                    val bitmap: Bitmap = BitmapFactory.decodeStream(photoInputStream)
                    runOnUiThread { currentUserImageView?.setImageBitmap(bitmap) }
                }

                override fun failure(exception: ClientException?) {
                    Log.e(TAG, "Profile picture fetch failed", exception)
                }
            })
            resultView?.text = "..."
        } else {
            signInButton?.isEnabled = true
            signOutButton?.isEnabled = false
            currentUserTextView?.text = "Please login..."
            currentUserImageView?.setImageResource(0)
            resultView?.text = "..."
        }
    }

    private fun getCookieFromURL(url: String) {
        Log.d(TAG, "Opening WEBVIEW to $url to get authetnication cookie")
        webView?.visibility = VISIBLE
        webView.settings.setJavaScriptEnabled(true)
        webView.clearCache(true)
        webView.settings.userAgentString = "multimodal-core"
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                //TODO: What if cookie fetching fails? Maybe do some error handling
                val cookie: String = CookieManager.getInstance().getCookie(url)
                Log.d(TAG, "Cookie extraction successfull:> $cookie ")
                webView.destroy()
                webView?.visibility = INVISIBLE
                updateProjects(cookie)
            }
        }
        webView.loadUrl(url)
    }

    private fun updateProjects(cookie: String) {
        val authName = microsoftAccount!!.getFullName()!!
        PulaattoriClient.create(cookie, authName).getProjects()
            .enqueue( object : Callback<List<Project>> {
                override fun onResponse(call: Call<List<Project>>?, response: Response<List<Project>>?) {
                    if(response?.body() != null)
                        projects.value = response.body()
                }
                override fun onFailure(call: Call<List<Project>>?, t: Throwable?) {
                    Log.e(TAG, "Epic fail when getting projects", t)
                }
            })
    }

    private fun iterateThroughProjects(){
        //Just a simple method to prove that projects now contains the values of a persons projects
        println("Available projects:>")
        projects.value?.forEach { p -> println(p) }
    }
}


