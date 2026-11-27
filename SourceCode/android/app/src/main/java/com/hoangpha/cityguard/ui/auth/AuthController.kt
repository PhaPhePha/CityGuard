package com.hoangpha.cityguard.ui.auth

import kotlinx.coroutines.flow.StateFlow

/** Actions and state shared by authentication screens. */
interface AuthController {
    val authState: StateFlow<AuthState>

    fun login(username: String, password: String)
    fun register(username: String, email: String, password: String, confirmPass: String)
    fun logout(onComplete: () -> Unit = {})
    // TV2: form chủ động hủy request khi Back; default giữ tương thích controller test/caller cũ.
    fun cancelPending() {}
    fun resetState()
}
