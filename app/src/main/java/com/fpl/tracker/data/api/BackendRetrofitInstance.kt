package com.fpl.tracker.data.api

import com.fpl.tracker.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object BackendRetrofitInstance {
    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var currentBaseUrl: String = sanitizeBaseUrl(BuildConfig.DEFAULT_BACKEND_URL)

    @Volatile
    private var currentApi: BackendApiService? = null

    @Volatile
    private var currentApiUrl: String? = null

    fun getDefaultBaseUrl(): String = sanitizeBaseUrl(BuildConfig.DEFAULT_BACKEND_URL)

    fun getBaseUrl(): String = currentBaseUrl

    @Synchronized
    fun updateBaseUrl(url: String?) {
        currentBaseUrl = sanitizeBaseUrl(url ?: BuildConfig.DEFAULT_BACKEND_URL)
        BackendDiagnostics.updateBaseUrl(currentBaseUrl)
        if (currentApiUrl != currentBaseUrl) {
            currentApi = null
        }
    }

    val api: BackendApiService
        get() = synchronized(this) {
            if (currentApi == null || currentApiUrl != currentBaseUrl) {
                currentApiUrl = currentBaseUrl
                currentApi = Retrofit.Builder()
                    .baseUrl(currentBaseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
                    .create(BackendApiService::class.java)
            }
            currentApi!!
        }

    private fun sanitizeBaseUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.endsWith("/")) {
            normalized += "/"
        }
        if (!normalized.endsWith("api/")) {
            normalized = normalized.removeSuffix("/") + "/api/"
        }
        return normalized
    }
}
