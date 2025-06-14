package dev.achmad.alephup.ui.auth

import android.content.Context
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.achmad.alephup.util.toast
import dev.achmad.core.util.extension.inject
import dev.achmad.data.auth.Auth
import dev.achmad.data.auth.AuthResult
import dev.achmad.data.auth.google.GoogleAuth
import dev.achmad.data.auth.google.exception.GoogleAuthInvalidEmailException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInScreenModel(
    private val auth: Auth = inject()
) : StateScreenModel<SignInScreenState>(SignInScreenState.Idle) {

    fun signIn(context: Context) = screenModelScope.launch {
        mutableState.update { SignInScreenState.Loading }
        val result = trySignIn(context, filterByAuthorized = true)
            ?: trySignIn(context, filterByAuthorized = false)
        if (result is AuthResult.Error) {
            context.toast(result.message ?: "Unknown Error")
        }
        mutableState.update { SignInScreenState.Idle }
    }

    private suspend fun trySignIn(context: Context, filterByAuthorized: Boolean): AuthResult? {
        return when(val result = auth.signIn(GoogleAuth.createOptions(filterByAuthorized))) {
            is AuthResult.Cancelled -> null
            is AuthResult.Error -> {
                if (result.exception is GoogleAuthInvalidEmailException) {
                    context.toast("Only Aleph Email is allowed")
                    return null
                }
                result
            }
            is AuthResult.Success -> result
        }
    }
}
