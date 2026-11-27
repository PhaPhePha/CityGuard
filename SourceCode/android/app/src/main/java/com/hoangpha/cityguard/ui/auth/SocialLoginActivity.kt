package com.hoangpha.cityguard.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.hoangpha.cityguard.R
import com.hoangpha.cityguard.data.api.FirebaseProvider
import com.hoangpha.cityguard.ui.theme.CityGuardAppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Private activity: provider credentials never reach the backend or SessionManager.
 * Only a fresh Firebase ID token is returned to the login screen, in memory.
 */
class SocialLoginActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PROVIDER = "provider"
        const val EXTRA_ID_TOKEN = "firebase_id_token"
        const val EXTRA_ERROR = "social_login_error"
        private const val STATE_EXCHANGING = "exchanging_firebase_credential"
    }

    private var finishingLogin = false
    private var facebookManager: LoginManager? = null
    private var callbackManager: CallbackManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState?.getBoolean(STATE_EXCHANGING) == true) {
            finishWithError("Quá trình đăng nhập bị gián đoạn. Vui lòng thử lại.")
            return
        }
        setContent {
            CityGuardAppTheme {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
        val provider = runCatching {
            FirebaseProvider.valueOf(intent.getStringExtra(EXTRA_PROVIDER).orEmpty())
        }.getOrNull()
        when (provider) {
            FirebaseProvider.GOOGLE -> lifecycleScope.launch { googleLogin() }
            FirebaseProvider.FACEBOOK -> facebookLogin(savedInstanceState)
            null -> finishWithError("Phương thức đăng nhập không hợp lệ.")
        }
    }

    private suspend fun googleLogin() {
        try {
            val option = GetGoogleIdOption.Builder()
                .setServerClientId(getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()
            val result = CredentialManager.create(this).getCredential(
                context = this,
                request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            )
            val credential = result.credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                finishWithError("Không nhận được tài khoản Google hợp lệ.")
                return
            }
            val googleToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            exchangeWithFirebase(GoogleAuthProvider.getCredential(googleToken, null))
        } catch (_: GetCredentialCancellationException) {
            finishCancelled()
        } catch (_: NoCredentialException) {
            finishWithError("Chưa tìm thấy tài khoản Google. Hãy thêm tài khoản Google trên thiết bị rồi thử lại.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            finishWithError(firebaseMessage(e))
        }
    }

    @Suppress("DEPRECATION")
    private fun facebookLogin(savedInstanceState: Bundle?) {
        if (getString(R.string.facebook_app_id).isBlank() ||
            getString(R.string.facebook_client_token).isBlank()) {
            finishWithError("Đăng nhập Facebook chưa được cấu hình cho bản ứng dụng này.")
            return
        }
        try {
            // Initialize only when requested; no analytics/advertising collection is needed for login.
            FacebookSdk.sdkInitialize(applicationContext)
            FacebookSdk.fullyInitialize()
            val manager = LoginManager.getInstance()
            facebookManager = manager
            val callbacks = CallbackManager.Factory.create()
            callbackManager = callbacks
            manager.registerCallback(callbacks, object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    lifecycleScope.launch {
                        exchangeWithFirebase(FacebookAuthProvider.getCredential(result.accessToken.token))
                    }
                }
                override fun onCancel() = finishCancelled()
                override fun onError(error: FacebookException) =
                    finishWithError("Không đăng nhập được Facebook. Vui lòng thử lại hoặc kiểm tra quyền truy cập ứng dụng.")
            })
            val launcher = registerForActivityResult(manager.createLogInActivityResultContract(callbacks)) { }
            if (savedInstanceState == null) {
                manager.logOut()
                // App Meta hiện chưa cho phép scope email (Invalid Scopes trong màn OAuth).
                // Xác thực bằng Facebook UID; TV5 phải xử lý trường email có thể thiếu.
                // Chỉ xin thêm email sau khi nhóm cấu hình quyền đó trên Meta và kiểm thử lại.
                launcher.launch(listOf("public_profile"))
            }
        } catch (_: Exception) {
            finishWithError("Không mở được đăng nhập Facebook. Vui lòng kiểm tra cấu hình ứng dụng.")
        }
    }

    private suspend fun exchangeWithFirebase(credential: AuthCredential) {
        //  Google ID token/Facebook access token chỉ đổi credential tại Firebase.
        // getIdToken(true) bên dưới mới là token gửi TV5; Firebase UID không thay thế users.id.
        if (finishingLogin) return
        finishingLogin = true
        var auth: FirebaseAuth? = null
        try {
            auth = FirebaseAuth.getInstance()
            auth.signOut()
            val currentAuth = auth
            val task = currentAuth.signInWithCredential(credential)
            // Firebase Tasks continue after coroutine cancellation. Clean up even if Back/rotation
            // destroys this activity before the network result arrives.
            task.addOnSuccessListener {
                if (isFinishing || isDestroyed) currentAuth.signOut()
            }
            val user = task.await().user
                ?: error("Missing Firebase user")
            val firebaseIdToken = user.getIdToken(true).await().token
                ?.takeIf { it.isNotBlank() } ?: error("Missing Firebase ID token")
            setResult(RESULT_OK, Intent()
                .putExtra(EXTRA_PROVIDER, intent.getStringExtra(EXTRA_PROVIDER))
                .putExtra(EXTRA_ID_TOKEN, firebaseIdToken))
            finish()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            finishWithError(firebaseMessage(e))
        } finally {
            // CityGuard JWT is the only app session. Discard temporary Firebase/social sessions.
            auth?.signOut()
            facebookManager?.logOut()
        }
    }

    private fun firebaseMessage(error: Exception): String = when (error) {
        is FirebaseAuthUserCollisionException ->
            "Email này đã dùng phương thức đăng nhập khác. Hãy đăng nhập bằng phương thức cũ trước khi liên kết tài khoản."
        is FirebaseNetworkException -> "Không kết nối được dịch vụ đăng nhập. Kiểm tra mạng rồi thử lại."
        is FirebaseAuthException -> when (error.errorCode) {
            "ERROR_USER_DISABLED" -> "Tài khoản đăng nhập đã bị vô hiệu hóa."
            "ERROR_INVALID_CREDENTIAL", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" ->
                "Mã xác thực không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại."
            else -> "Không xác thực được tài khoản. Vui lòng thử lại."
        }
        else -> "Không hoàn tất được đăng nhập. Vui lòng thử lại."
    }

    private fun finishWithError(message: String) {
        setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_ERROR, message))
        finish()
    }

    private fun finishCancelled() {
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onDestroy() {
        callbackManager?.let { facebookManager?.unregisterCallback(it) }
        if (isFinishing) facebookManager?.logOut()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_EXCHANGING, finishingLogin)
        super.onSaveInstanceState(outState)
    }
}
