package com.ayushi.letstrack

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.lifecycle.lifecycleScope
import com.ayushi.letstrack.data.FirebaseAuthManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authManager = FirebaseAuthManager()

        lifecycleScope.launch {
            try {
                val uid = authManager.signInAnonymously()

                Log.d("FirebaseTest", "Firebase UID: $uid")

                setContent {
                    Text("Firebase connected!\nUID: $uid")
                }

            } catch (e: Exception) {
                Log.e("FirebaseTest", "Firebase authentication failed", e)

                setContent {
                    Text("Firebase authentication failed")
                }
            }
        }
    }
}