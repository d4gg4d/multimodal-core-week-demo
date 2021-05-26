package com.nitor.multimodalcoreweekdemo

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.microsoft.graph.concurrency.ICallback
import com.microsoft.graph.core.ClientException
import com.microsoft.identity.client.IAccount
import com.nitor.multimodalcoreweekdemo.intents.IntentAction
import com.nitor.multimodalcoreweekdemo.intents.IntentParser
import com.speechly.client.slu.Segment
import com.speechly.client.speech.Client
import com.speechly.ui.SpeechlyButton
import kotlinx.coroutines.*
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

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeUi()

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

    private fun showMessage(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }
}