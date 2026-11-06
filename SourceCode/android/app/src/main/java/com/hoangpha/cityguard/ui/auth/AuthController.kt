package com.hoangpha.cityguard.ui.auth

import kotlinx.coroutines.flow.StateFlow

/** Actions and state shared by authentication screens. */
interface AuthController {
    val authState: StateFlow<AuthState>

    fun login(username: String, password: String)
    fun register(username: String, email: String, password: String, confirmPass: String)
    fun logout(onComplete: () -> Unit = {})
    fun resetState()
}
