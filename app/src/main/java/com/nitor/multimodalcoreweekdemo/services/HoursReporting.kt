package com.nitor.multimodalcoreweekdemo.services

import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.MutableLiveData
import com.nitor.multimodalcoreweekdemo.MicrosoftAccount
import com.nitor.multimodalcoreweekdemo.models.HourEntry
import com.nitor.multimodalcoreweekdemo.models.Project
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDate
import java.util.*

private const val TAG = "HoursReporting"

class HoursReporting (
    private val microsoftAccount: MicrosoftAccount,
    url: String = "https://hours.dev.nitor.zone/rest/projects",
    val webView: WebView //TODO haxor to get the cookie through UI
) {

    var authCookie: String? = CookieManager.getInstance().getCookie(url)

    val projects: MutableLiveData<List<Project>> = MutableLiveData()

    init {
        if (authCookie == null) getCookieFromURL(url) {
            fetchAvailableProjects()
        } else {
            fetchAvailableProjects()
        }


        /*
        if (cookie==null && microsoftAccount == null) {
            //User has not logged in with MSAL and cookie used for API is null => Log in to tuntipulaattori to extract cookie
            //TODO: Investigate how to do this better. Now 2 login screens are needed...
            getCookieFromURL(url);
        }
        */
    }

    fun sendHours(projectName: String, hours: Number): Boolean {
        // TODO here should be the mapping of projectAlias,hour tuple to real request
        val authName = microsoftAccount.getFullName()!!

        val today = LocalDate.now()
        val description = "Core Week PoC"
        val projectId = resolveProjectId(projectName)
        val hourEntry = HourEntry(
            hours = hours.toDouble(),
            date = today.toString(),
            projectId = projectId,
            description = description
        )

        PulaattoriClient.create(authCookie!!, authName).uploadHours(listOf(hourEntry))
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>?, response: Response<ResponseBody>?) {
                    Log.d(TAG, "Successfully reported hours")
                }
                override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
                    Log.e(TAG, "Failed to report hours", t)
                }
            })
        return true
    }

    private fun resolveProjectId(projectName: String): String {
        // customerName: Nitor
        // caseName: Nitor Creations Core
        // projectName: "Core Projects"
        // projectId: f1cd281606b08570123b5875fa523c44
//        projects.value?.sortedBy {
//            (it.projectName.toUpperCase()
//        }
        return "f1cd281606b08570123b5875fa523c44"
    }

    private fun fetchAvailableProjects() {
        val authName = microsoftAccount.getFullName()!!
        PulaattoriClient.create(authCookie!!, authName).getProjects()
            .enqueue( object : Callback<List<Project>> {
                override fun onResponse(call: Call<List<Project>>?, response: Response<List<Project>>?) {
                    projects.value = response?.body() ?: emptyList()
                    Log.d(TAG, "Succesfully fetched available project listing, size of ${projects.value!!.size}")
                }
                override fun onFailure(call: Call<List<Project>>?, t: Throwable?) {
                    Log.e(TAG, "Epic fail when getting projects", t)
                }
            })
    }

    private fun getCookieFromURL(url: String, callback: () -> Unit) {
        Log.d(TAG, "Opening WEBVIEW to $url to get authentication cookie")
        webView?.visibility = View.VISIBLE
        webView.settings.setJavaScriptEnabled(true)
        webView.clearCache(true)
        webView.settings.userAgentString = "multimodal-core"
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                //TODO: What if cookie fetching fails? Maybe do some error handling
                authCookie = CookieManager.getInstance().getCookie(url)
                Log.d(TAG, "Cookie extraction successfull:> $authCookie ")
                webView.destroy()
                webView?.visibility = View.INVISIBLE
                callback()
            }
        }
        webView.loadUrl(url)
    }
}