package com.hoangpha.cityguard.data.local
// DataStore lưu phiên qua lần đóng/mở app; bổ sung refresh token và expires_at, xóa đúng phiên khi hết hạn.
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "cityguard_session")

data class StoredSession(val accessToken: String, val refreshToken: String, val userId: String, val expiresAt: Long)
interface SessionStore {
    val session: Flow<StoredSession?>
    suspend fun saveSession(token: String, userId: String, refreshToken: String, expiresIn: Long)
    suspend fun getToken(): String?
    suspend fun getUserId(): String?
    suspend fun clearSession()
    suspend fun clearIfTokenMatches(token: String)
}

class SessionManager(context: Context, private val now: () -> Long = { System.currentTimeMillis() }) : SessionStore {
    // expiresIn là millisecond; lưu refreshToken nhưng thiết kế hiện tại hết hạn sẽ về Login, chưa tự refresh.
    private val context = context.applicationContext
    companion object {
        val TOKEN_KEY = stringPreferencesKey("auth_token")
        val USER_ID_KEY = stringPreferencesKey("user_id")
        private val REFRESH_KEY = stringPreferencesKey("refresh_token")
        private val EXPIRY_KEY = longPreferencesKey("expires_at")
    }

    // đọc một snapshot token/user ID/thời hạn từ cùng giao dịch DataStore. Phiên cũ thiếu expires_at phải Login lại.
    override val session: Flow<StoredSession?> = this.context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val token = prefs[TOKEN_KEY]
            val userId = prefs[USER_ID_KEY]
            val expiry = prefs[EXPIRY_KEY]
            if (token.isNullOrBlank() || userId.isNullOrBlank() || expiry == null) null
            else StoredSession(token, prefs[REFRESH_KEY].orEmpty(), userId, expiry)
        }.distinctUntilChanged()

    // Backend hiện trả expiresIn theo MILLISECOND, không phải giây.
    override suspend fun saveSession(token: String, userId: String, refreshToken: String, expiresIn: Long) {
        require(token.isNotBlank() && userId.isNotBlank() && refreshToken.isNotBlank())
        require(expiresIn in 1..604_800_000L)
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
            prefs[USER_ID_KEY] = userId
            prefs[REFRESH_KEY] = refreshToken
            prefs[EXPIRY_KEY] = now() + expiresIn
        }
    }

    //không gửi access token đã hết hạn lên API; clearIfTokenMatches tránh xóa phiên mới do request cũ.
    override suspend fun getToken(): String? {
        val current = session.first() ?: return null
        if (current.expiresAt <= now()) {
            clearIfTokenMatches(current.accessToken)
            return null
        }
        return current.accessToken
    }
    override suspend fun getUserId(): String? = session.first()?.userId
    override suspend fun clearSession() { context.dataStore.edit { it.clear() } }

    // Response 401 cũ không được xóa phiên vừa đăng nhập mới.
    override suspend fun clearIfTokenMatches(token: String) {
        context.dataStore.edit { if (it[TOKEN_KEY] == token) it.clear() }
    }
}
