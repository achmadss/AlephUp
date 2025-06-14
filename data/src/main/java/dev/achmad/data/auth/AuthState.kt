package dev.achmad.data.auth

sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(val user: User) : AuthState
}