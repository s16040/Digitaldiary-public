package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import com.example.app.geofence.GeofenceHelper
import com.example.app.ui.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                val startDestination = if (auth.currentUser == null) "auth" else "list"

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("auth") {
                        AuthScreen(navController, auth)
                    }
                    composable("list") {
                        EntryListScreen(navController, auth)
                    }
                    composable("form") {
                        EntryFormScreen(
                            navController,
                            auth.currentUser?.uid ?: ""
                        )
                    }
                    composable("map") {
                        MapScreen(navController)
                    }
                }
            }
        }
    }
}
