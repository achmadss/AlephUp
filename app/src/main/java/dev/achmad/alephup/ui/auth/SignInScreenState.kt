package dev.achmad.alephup.ui.auth

sealed interface SignInScreenState {
    data object Idle: SignInScreenState
    data object Loading: SignInScreenState
}