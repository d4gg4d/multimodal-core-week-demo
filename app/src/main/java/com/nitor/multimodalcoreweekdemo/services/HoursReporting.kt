package com.nitor.multimodalcoreweekdemo.services

import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.MutableLiveData
import com.nitor.multimodalcoreweekdemo.MicrosoftAccount
import com.nitor.multimodalcoreweekdemo.models.Project
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val TAG = "HoursReporting"

class HoursReportingService(
    private val microsoftAccount: MicrosoftAccount,
    private val url: String = "https://hours.dev.nitor.zone/rest/projects",
    val webView: WebView //TODO haxor to get the cookie through UI
) {

    var authCookie: String? = CookieManager.getInstance().getCookie(url)

    val projects: MutableLiveData<List<Project>> = MutableLiveData()

    init {
        if (authCookie == null) getCookieFromURL(url) {
            fetchAvailableProjects(authCookie!!)
        }
        /*
        if (cookie==null && microsoftAccount == null) {
            //User has not logged in with MSAL and cookie used for API is null => Log in to tuntipulaattori to extract cookie
            //TODO: Investigate how to do this better. Now 2 login screens are needed...
            getCookieFromURL(url);
        }
        */
    }

    private fun fetchAvailableProjects(cookie: String) {
        val authName = microsoftAccount.getFullName()!!
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

    private fun iterateThroughProjects() {
        println("Available projects:>")
        projects.value?.forEach { p -> println(p) }
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