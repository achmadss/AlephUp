package dev.achmad.data.auth.google

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dev.achmad.core.Constants
import dev.achmad.data.R
import dev.achmad.data.auth.Auth
import dev.achmad.data.auth.AuthResult
import dev.achmad.data.auth.AuthState
import dev.achmad.data.auth.User
import dev.achmad.data.auth.google.exception.GoogleAuthInvalidEmailException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

/**
 * Google authentication implementation using Firebase Auth and Credential Manager
 */
class GoogleAuth(
    private val context: Context,
    private val allowedEmailDomains: Set<String> = Constants.Auth.Google.ALLOWED_EMAIL_DOMAINS
) : Auth() {
    
    private val credentialManager = CredentialManager.create(context)
    private val clientId = context.getString(R.string.web_client_id)
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    // Keep reference to manually emit state changes
    private var authStateChannel: SendChannel<AuthState>? = null
    
    override val authState: StateFlow<AuthState> = createAuthStateFlow()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = getCurrentAuthState()
        )
    
    /**
     * Sign in with Google using default settings (filter by authorized accounts)
     */
    override suspend fun signIn(options: Map<String, Boolean>): AuthResult {
        return signInWithGoogle(
            filterByAuthorized = options[FILTER_BY_AUTHORIZED] ?: true
        )
    }
    
    /**
     * Sign in with Google with more control over the flow
     * @param filterByAuthorized Whether to show only previously authorized accounts
     * @return GoogleAuthResult indicating the outcome
     */
    private suspend fun signInWithGoogle(
        filterByAuthorized: Boolean
    ): AuthResult {
        return try {
            val credential = getGoogleCredential(filterByAuthorized)
            val firebaseUser = authenticateWithFirebase(credential)

            when {
                firebaseUser == null -> AuthResult.Error(
                    exception = Exception("Firebase authentication failed"),
                    message = "Failed to authenticate with Firebase"
                )
                isEmailAllowed(firebaseUser.email) -> AuthResult.Success(firebaseUser.toUser())
                else -> {
                    signOut()
                    AuthResult.Error(
                        exception = GoogleAuthInvalidEmailException(),
                        message = "Invalid Email"
                    )
                }
            }
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (e: Exception) {
            AuthResult.Error(
                exception = e,
                message = "Google authentication failed: ${e.message}"
            )
        }
    }
    
    override fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser
        return when {
            firebaseUser == null -> null
            else -> firebaseUser.toUser()
        }
    }
    
    override suspend fun signOut(options: Map<String, Boolean>) {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // Log but don't throw - credential manager might not have state to clear
            e.printStackTrace()
        }
        firebaseAuth.signOut()
        
        // Manually emit sign out state since Firebase listener might not trigger
        authStateChannel?.trySend(AuthState.SignedOut)
    }
    
    private suspend fun getGoogleCredential(filterByAuthorized: Boolean): GoogleIdTokenCredential {
        val option = when {
            filterByAuthorized -> {
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(clientId)
                    .build()
            }
            else -> {
                GetSignInWithGoogleOption.Builder(clientId)
                    .build()
            }
        }
        
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
        
        val result = credentialManager.getCredential(context, request)
        val credential = result.credential
        
        return when (credential.type) {
            GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                GoogleIdTokenCredential.createFrom(credential.data)
            }
            else -> throw IllegalStateException("Unexpected credential type: ${credential.type}")
        }
    }
    
    private suspend fun authenticateWithFirebase(googleCredential: GoogleIdTokenCredential): FirebaseUser? {
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        val result = firebaseAuth.signInWithCredential(firebaseCredential).await()
        return result.user
    }
    
    private fun isEmailAllowed(email: String?): Boolean {
        if (email == null) return false
        return allowedEmailDomains.any { domain -> email.endsWith(domain) }
    }
    
    private fun createAuthStateFlow(): Flow<AuthState> = callbackFlow {
        // Store channel reference for manual emissions
        authStateChannel = this
        
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            val state = when {
                firebaseUser == null -> AuthState.SignedOut
                isEmailAllowed(firebaseUser.email) -> AuthState.SignedIn(firebaseUser.toUser())
                else -> {
                    // Sign out user with invalid email automatically
                    auth.signOut()
                    // Manually emit since we just signed out
                    trySend(AuthState.SignedOut)
                    return@AuthStateListener
                }
            }
            trySend(state)
        }
        
        firebaseAuth.addAuthStateListener(authStateListener)
        
        awaitClose {
            authStateChannel = null
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }.distinctUntilChanged()
    
    private fun getCurrentAuthState(): AuthState {
        val firebaseUser = firebaseAuth.currentUser
        return when {
            firebaseUser == null -> AuthState.SignedOut
            isEmailAllowed(firebaseUser.email) -> AuthState.SignedIn(firebaseUser.toUser())
            else -> AuthState.SignedOut
        }
    }

    private fun FirebaseUser.toUser(): User {
        return User(
            name = displayName ?: "Unknown Name",
            email = email ?: "Unknown Email"
        )
    }

    companion object {

        private const val FILTER_BY_AUTHORIZED = "FILTER_BY_AUTHORIZED"

        fun createOptions(
            filterByAuthorized: Boolean = true,
        ): Map<String, Boolean> {
            return mapOf(
                FILTER_BY_AUTHORIZED to filterByAuthorized
            )
        }
    }
}