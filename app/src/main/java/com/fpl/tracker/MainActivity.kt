package com.fpl.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.fpl.tracker.navigation.NavGraph
import com.fpl.tracker.navigation.Screen
import com.fpl.tracker.data.api.BackendRetrofitInstance
import com.fpl.tracker.data.preferences.PreferencesManager
import com.fpl.tracker.ui.theme.FantasyLiveTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PreferencesManager(this)
        BackendRetrofitInstance.updateBaseUrl(
            prefs.getBackendUrl() ?: BackendRetrofitInstance.getDefaultBaseUrl()
        )
        enableEdgeToEdge()
        setContent {
            FantasyLiveTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        startDestination = Screen.Login.route
                    )
                }
            }
        }
    }
}
