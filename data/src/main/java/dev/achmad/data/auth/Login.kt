package dev.achmad.data.auth

import android.util.Patterns
import dev.achmad.core.network.NetworkHelper

sealed interface LoginResult {
    data class HttpError(val message: String? = null): LoginResult
    data object InvalidEmail: LoginResult
    data object InvalidPassword: LoginResult
    data class Success(
        val token: String,
        val name: String,
    ): LoginResult
}

class Login(
    private val networkHelper: NetworkHelper
) {
    suspend fun await(
        email: String,
        password: String,
    ): LoginResult {
        if (!isValidEmail(email)) return LoginResult.InvalidEmail
        if (password.isEmpty()) return LoginResult.InvalidPassword
        // TODO logic
        return LoginResult.Success(
            token = "token",
            name = "placeholder",
        )
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

}