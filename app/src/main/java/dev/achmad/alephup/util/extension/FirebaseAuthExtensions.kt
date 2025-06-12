package dev.achmad.alephup.util.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun FirebaseAuth.getUserAsFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun rememberFirebaseUser(): FirebaseUser? {
    val user = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().getUserAsFlow().collect {
            user.value = it
        }
    }

    return user.value
}
