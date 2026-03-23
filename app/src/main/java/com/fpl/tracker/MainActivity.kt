package com.fpl.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fpl.tracker.sharedui.app.FantasyApp
import com.fpl.tracker.sharedui.app.PreferencesManager
import com.fpl.tracker.sharedui.app.SharedAppContainer
import com.fpl.tracker.sharedui.theme.FantasySharedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferencesManager(this)
        // Ensure we use the local backend for development if nothing is set
        SharedAppContainer.initialize {
            prefs.getBackendUrl() ?: "http://192.168.0.10:3000/api/"
        }
        enableEdgeToEdge()
        setContent {
            FantasySharedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FantasyApp()
                }
            }
        }
    }
}
