
package com.hoangpha.cityguard.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "cityguard_session")

class SessionManager(private val context: Context) {

    companion object {
        val TOKEN_KEY = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
        val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    // Lưu Access Token, Refresh Token và User ID
    suspend fun saveSession(
        token: String,
        userId: String,
        refreshToken: String? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_ID_KEY] = userId

            if (refreshToken != null) {
                prefs[REFRESH_TOKEN_KEY] = refreshToken
            } else {
                prefs.remove(REFRESH_TOKEN_KEY)
            }
        }
    }

    suspend fun getToken(): String? =
        context.dataStore.data.map { it[TOKEN_KEY] }.first()

    suspend fun getRefreshToken(): String? =
        context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }.first()

    suspend fun getUserId(): String? =
        context.dataStore.data.map { it[USER_ID_KEY] }.first()

    // Cập nhật cặp token sau khi refresh thành công
    suspend fun updateTokens(
        accessToken: String,
        refreshToken: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = accessToken
            prefs[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
