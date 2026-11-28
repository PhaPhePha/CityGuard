package com.hoangpha.cityguard.data.api

//Gắn JWT tập trung, bỏ header X-User-Id cũ; chỉ response 401 của API bảo vệ mới làm hết phiên.

import com.hoangpha.cityguard.BuildConfig
import com.hoangpha.cityguard.data.local.SessionStore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    //API bảo vệ dùng Bearer JWT CityGuard, không gửi X-User-Id/Firebase ID Token
    private fun retrofit(sessionManager: SessionStore, retryConnection: Boolean = true): Retrofit {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            // Auth POST không tự gửi lại khi kết nối lỗi; UI để user quyết định sau timeout.
            .retryOnConnectionFailure(retryConnection)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val original = chain.request()
                // Login/Register/Logout không gắn JWT cũ, tránh phiên cũ cản việc đăng nhập lại.
                val isAuthRequest = original.url.encodedPath.startsWith("/api/v1/auth/")
                val token = if (isAuthRequest) null else runBlocking { sessionManager.getToken() }
                val request = original.newBuilder().apply {
                    if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
                }.build()
                val response = chain.proceed(request)
                // 401 Login chỉ là sai thông tin; 401 từ API bảo vệ mới báo phiên không còn hợp lệ.
                if (!isAuthRequest && response.code == 401 && token != null) {
                    runBlocking { sessionManager.clearIfTokenMatches(token) }
                }
                response
            }.build()
        return Retrofit.Builder().baseUrl(BuildConfig.BASE_URL).client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
    }
    fun auth(session: SessionStore): AuthApi = retrofit(session, retryConnection = false).create(AuthApi::class.java)
    fun reports(session: SessionStore): ReportApi = retrofit(session).create(ReportApi::class.java)
    fun profile(session: SessionStore): ProfileApi = retrofit(session).create(ProfileApi::class.java)
}
