package com.hoangpha.cityguard.data.api

import com.hoangpha.cityguard.BuildConfig
import com.hoangpha.cityguard.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // Sử dụng BASE_URL từ BuildConfig để không bị hard-code
    private val BASE_URL = BuildConfig.BASE_URL

    private fun retrofit(sessionManager: SessionManager): Retrofit {
        // Công cụ in log ra màn hình Console để dễ debug
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                // Tự động lấy token từ SessionManager gắn vào Header
                val token = runBlocking { sessionManager.getToken() }
                val request = chain.request().newBuilder()
                if (token != null) {
                    request.addHeader("Authorization", "Bearer $token")
                }
                val userId = runBlocking { sessionManager.getUserId() }
                if (userId != null) request.addHeader("X-User-Id", userId)
                chain.proceed(request.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun auth(sessionManager: SessionManager): AuthApi = retrofit(sessionManager).create(AuthApi::class.java)
    fun reports(sessionManager: SessionManager): ReportApi = retrofit(sessionManager).create(ReportApi::class.java)
}
