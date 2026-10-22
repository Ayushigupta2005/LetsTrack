package com.ayushi.letstrack.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager {

    private val auth = FirebaseAuth.getInstance()

    suspend fun signInAnonymously(): String {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            return currentUser.uid
        }

        val result = auth.signInAnonymously().await()

        return result.user?.uid
            ?: throw Exception("Authentication failed")
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}