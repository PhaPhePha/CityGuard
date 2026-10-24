package com.hoangpha.cityguard.data.api

import com.hoangpha.cityguard.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // 10.0.2.2 là địa chỉ IP để máy ảo Android gọi vào localhost của máy tính
    private const val BASE_URL = "http://10.0.2.2:8080/"

    fun create(sessionManager: SessionManager): AuthApi {
        // Công cụ in log ra màn hình Console để dễ debug
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
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
                chain.proceed(request.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}