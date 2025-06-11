package dev.achmad.alephup.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.achmad.core.util.extension.inject
import dev.achmad.data.auth.Login
import dev.achmad.data.auth.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginScreenViewModel(
    private val login: Login = inject()
): ViewModel() {

    private val mutableState = MutableStateFlow(LoginScreenState())
    val state = mutableState.asStateFlow()

    fun login(
        onHttpError: () -> Unit,
    ) = viewModelScope.launch {
        val result = login.await(
            email = state.value.email,
            password = state.value.password,
        )
        when(result) {
            is LoginResult.HttpError -> onHttpError()
            is LoginResult.InvalidEmail -> {
                mutableState.update {
                    it.copy(
                        emailError = "Invalid Email"
                    )
                }
            }
            is LoginResult.Success -> TODO()
        }
    }

}