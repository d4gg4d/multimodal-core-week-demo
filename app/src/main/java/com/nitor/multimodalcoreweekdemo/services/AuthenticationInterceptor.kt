package com.nitor.multimodalcoreweekdemo.services

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor used to intercept the actual request and give required x-auth-name and Cookie and the vital User-agent (hours wasted...)
 */

class AuthenticationInterceptor(cookieString: String, x_auth_name: String) : Interceptor {

    var x_auth_name: String= x_auth_name;

    var cookieString: String = cookieString;

    override fun intercept(chain: Interceptor.Chain): Response {
        cookieString = "__Host-auth" + cookieString.substringAfter("__Host-auth").substringBefore(';')//TODO: This is also a bit of a hack. Why does the getCookie return so much crap...
        val newRequest = chain.request().newBuilder()
            .addHeader("x-auth-name", x_auth_name)
            .addHeader("Cookie", cookieString)
            .addHeader("User-Agent", "multimodal-core")
            .build()
        return chain.proceed(newRequest)
    }
}

