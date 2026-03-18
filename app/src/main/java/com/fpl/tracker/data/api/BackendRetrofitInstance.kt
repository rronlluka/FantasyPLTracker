package com.fpl.tracker.data.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object BackendRetrofitInstance {

    /**
     * ⚠️  CHANGE THIS to your backend's address.
     *
     * While developing on the same machine as the Android emulator:
     *   "http://10.0.2.2:3000/api/"   ← emulator → host machine
     *
     * While testing on a real physical phone on the same Wi-Fi:
     *   "http://192.168.X.X:3000/api/" ← your computer's local IP
     *
     * In production (deployed server):
     *   "https://your-domain.com/api/"
     */
    const val BACKEND_URL = "http://127.0.0.1:3000/api/"

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC   // Use BODY for full request/response logging
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: BackendApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BACKEND_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(BackendApiService::class.java)
    }
}
