package dev.achmad.alephup.ui.auth

import android.content.Context
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.achmad.alephup.util.toast
import dev.achmad.core.util.extension.inject
import dev.achmad.data.auth.Auth
import dev.achmad.data.auth.AuthResult
import dev.achmad.data.auth.google.exception.GoogleAuthInvalidEmailException
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInScreenModel(
    private val auth: Auth = inject()
) : StateScreenModel<SignInScreenState>(SignInScreenState.Idle) {

    fun signIn(context: Context) = screenModelScope.launch {
        mutableState.update { SignInScreenState.Loading }
        when(val result = auth.signIn()) {
            is AuthResult.Error -> {
                when(result.exception) {
                    is GoogleAuthInvalidEmailException -> context.toast("Only Aleph Email is allowed")
                    else -> context.toast(result.message ?: "Unknown Error")
                }
            }
            else -> Unit
        }
        mutableState.update { SignInScreenState.Idle }
    }

}
