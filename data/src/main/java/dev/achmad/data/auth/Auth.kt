package dev.achmad.data.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Authentication abstract class that defines common authentication operations
 */
abstract class Auth {
    /**
     * StateFlow that emits the current authentication state
     */
    abstract val authState: StateFlow<AuthState>
    
    /**
     * Sign in with this authentication method
     * @return AuthResult indicating the outcome
     */
    abstract suspend fun signIn(
        options: Map<String, Boolean> = mapOf()
    ): AuthResult
    
    /**
     * Sign out from this authentication method
     */
    abstract suspend fun signOut(
        options: Map<String, Boolean> = mapOf()
    )
    
    /**
     * Get current signed-in user
     * @return Current user if signed in, null otherwise
     */
    abstract fun getCurrentUser(): User?
    
    /**
     * Check if user is currently signed in
     * @return true if signed in, false otherwise
     */
    fun isSignedIn(): Boolean = getCurrentUser() != null
}