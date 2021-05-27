package com.nitor.multimodalcoreweekdemo.services

import com.nitor.multimodalcoreweekdemo.models.HourEntry
import com.nitor.multimodalcoreweekdemo.models.Project
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


interface PulaattoriClient {

    @GET("rest/projects")
    fun getProjects(): Call<List<Project>>

    @POST("PUT/hours")
    fun uploadHours(@Body entries: List<HourEntry>): Response<ResponseBody>

    companion object {

        private var BASE_URL = "https://hours.dev.nitor.zone/"

        fun create(cookieString: String, x_auth_name: String) : PulaattoriClient {
            val builder = OkHttpClient.Builder()
            // We add the interceptor to OkHttpClient so we can add necessary authentication headers (the cookie and x-auth-name)
            builder.interceptors().add(AuthenticationInterceptor(cookieString, x_auth_name))
            val client = builder.build()

            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .client(client)
                .build()
            return retrofit.create(PulaattoriClient::class.java)
        }
    }

}