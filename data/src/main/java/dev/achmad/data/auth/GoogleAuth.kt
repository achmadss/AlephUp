package dev.achmad.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dev.achmad.data.R
import kotlinx.coroutines.tasks.await

class GoogleAuth(
    private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)
    private val clientId = context.getString(R.string.web_client_id)

    suspend fun signInWithGoogle(
        filterByAuthorized: Boolean = true,
        onValidEmail: suspend () -> Unit = {},
        onInvalidEmail: suspend (String?) -> Unit = {},
    ): FirebaseUser? {
        val option = if (filterByAuthorized) {
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(clientId)
                .build()
        } else {
            GetSignInWithGoogleOption.Builder(clientId).build()
        }

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val credential = credentialManager.getCredential(context, request).credential
            when(credential.type) {
                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCred = GoogleAuthProvider.getCredential(googleCred.idToken, null)
                    val result = FirebaseAuth.getInstance()
                        .signInWithCredential(firebaseCred)
                        .await()
                    val user = result.user
                    if (user != null) {
                        if (user.email?.endsWith("@aleph-labs.com") == true) {
                            onValidEmail()
                            user
                        } else {
                            onInvalidEmail(user.email)
                            null
                        }
                    } else null
                }
                else -> null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
        FirebaseAuth.getInstance().signOut()
    }
}
