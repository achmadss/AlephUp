package dev.achmad.alephup.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.achmad.alephup.util.toast
import dev.achmad.core.util.extension.inject
import dev.achmad.data.auth.Auth
import dev.achmad.data.auth.AuthResult
import dev.achmad.data.auth.google.GoogleAuth
import dev.achmad.data.auth.google.exception.GoogleAuthInvalidEmailException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInScreenViewModel(
    private val auth: Auth = inject()
): ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    fun signIn(
        context: Context
    ) = viewModelScope.launch {
        _loading.update { true }
        val authorizedResult = auth.signIn(
            GoogleAuth.createOptions(filterByAuthorized = true)
        )
        if (authorizedResult !is AuthResult.Error) return@launch
        if (authorizedResult.exception is GoogleAuthInvalidEmailException) {
            context.toast("Only Aleph Email is allowed")
        } else {
            context.toast(authorizedResult.message ?: "Unknown Error")
            return@launch
        }
        val result = auth.signIn(
            GoogleAuth.createOptions(filterByAuthorized = false)
        )
        if (result !is AuthResult.Error) return@launch
        if (result.exception is GoogleAuthInvalidEmailException) {
            context.toast("Only Aleph Email is allowed")
        } else {
            context.toast(authorizedResult.message ?: "Unknown Error")
        }
        _loading.update { false }
    }

}