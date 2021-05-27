package com.nitor.multimodalcoreweekdemo.services

import com.nitor.multimodalcoreweekdemo.models.HourEntry
import com.nitor.multimodalcoreweekdemo.models.Project
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT


interface PulaattoriClient {

    @GET("rest/projects")
    fun getProjects(): Call<List<Project>>

    @PUT("rest/hours")
    fun uploadHours(@Body entries: List<HourEntry>): Call<ResponseBody>

    companion object {

        private var BASE_URL = "https://hours.dev.nitor.zone/"

        fun create(cookieString: String, x_auth_name: String) : PulaattoriClient {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            val builder = OkHttpClient.Builder().addInterceptor(logging)
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