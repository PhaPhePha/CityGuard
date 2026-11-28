
package com.hoangpha.cityguard.data.api

import com.hoangpha.cityguard.BuildConfig
import com.hoangpha.cityguard.data.local.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private val BASE_URL = BuildConfig.BASE_URL
    private val refreshLock = Any()

    private fun createLoggingInterceptor() =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
        }

    private fun createAuthApi(): AuthApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(createLoggingInterceptor())
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    private fun retrofit(sessionManager: SessionManager): Retrofit {

        val refreshApi = createAuthApi()

        val authenticator = object : Authenticator {

            override fun authenticate(
                route: Route?,
                response: Response
            ): Request? {

                var responseCount = 1
                var previousResponse = response.priorResponse

                while (previousResponse != null) {
                    responseCount++
                    previousResponse = previousResponse.priorResponse
                }

                if (responseCount >= 2) return null

                return synchronized(refreshLock) {

                    val failedToken = response.request
                        .header("Authorization")
                        ?.removePrefix("Bearer ")

                    val currentToken = runBlocking {
                        sessionManager.getToken()
                    }

                    if (currentToken != null &&
                        currentToken != failedToken
                    ) {
                        return@synchronized response.request
                            .newBuilder()
                            .header("Authorization", "Bearer $currentToken")
                            .build()
                    }

                    val refreshToken = runBlocking {
                        sessionManager.getRefreshToken()
                    } ?: return@synchronized null

                    try {
                        val refreshResponse = runBlocking {
                            refreshApi.refresh(
                                RefreshTokenRequest(refreshToken)
                            )
                        }

                        if (!refreshResponse.isSuccessful) {
                            if (refreshResponse.code() == 400 ||
                                refreshResponse.code() == 401 ||
                                refreshResponse.code() == 403
                            ) {
                                runBlocking {
                                    sessionManager.clearSession()
                                }
                            }

                            return@synchronized null
                        }

                        val body = refreshResponse.body()?.data
                            ?: return@synchronized null

                        if (body.accessToken.isBlank() ||
                            body.refreshToken.isBlank()
                        ) {
                            return@synchronized null
                        }

                        runBlocking {
                            sessionManager.updateTokens(
                                accessToken = body.accessToken,
                                refreshToken = body.refreshToken
                            )
                        }

                        response.request.newBuilder()
                            .header(
                                "Authorization",
                                "Bearer ${body.accessToken}"
                            )
                            .build()

                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(createLoggingInterceptor())

            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val path = originalRequest.url.encodedPath

                val isAuthRequest =
                    path.contains("/auth/login") ||
                            path.contains("/auth/register") ||
                            path.contains("/auth/refresh") ||
                            path.contains("/auth/logout")

                val token = runBlocking {
                    sessionManager.getToken()
                }

                val request = originalRequest.newBuilder()

                if (token != null && !isAuthRequest) {
                    request.header("Authorization", "Bearer $token")
                }

                val userId = runBlocking {
                    sessionManager.getUserId()
                }

                if (userId != null) {
                    request.header("X-User-Id", userId)
                }

                chain.proceed(request.build())
            }

            .authenticator(authenticator)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun auth(sessionManager: SessionManager): AuthApi {
        return createAuthApi()
    }

    fun reports(sessionManager: SessionManager): ReportApi {
        return retrofit(sessionManager).create(ReportApi::class.java)
    }
}
