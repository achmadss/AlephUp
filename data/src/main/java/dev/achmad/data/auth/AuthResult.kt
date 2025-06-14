package dev.achmad.data.auth

/**
 * Generic authentication result that can be extended for specific auth methods
 */
sealed interface AuthResult {
    data class Success(val user: User) : AuthResult
    data class Error(val exception: Exception, val message: String? = null) : AuthResult
    data object Cancelled : AuthResult
}