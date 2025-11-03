package com.fpl.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.fpl.tracker.data.preferences.PreferencesManager
import com.fpl.tracker.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialScreen(navController: NavController) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    
    var managerIdText by remember { mutableStateOf("") }
    var leagueIdText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Check if IDs are saved and auto-navigate
    LaunchedEffect(Unit) {
        val savedManagerId = prefsManager.getManagerId()
        val savedLeagueId = prefsManager.getLeagueId()
        
        if (savedManagerId != null) {
            navController.navigate(Screen.ManagerStats.createRoute(savedManagerId)) {
                popUpTo(Screen.Initial.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FPL Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to FPL Tracker",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = managerIdText,
                onValueChange = { 
                    managerIdText = it
                    showError = false
                },
                label = { Text("Manager ID") },
                placeholder = { Text("Enter your Manager ID") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = leagueIdText,
                onValueChange = { 
                    leagueIdText = it
                    showError = false
                },
                label = { Text("League ID (Optional)") },
                placeholder = { Text("Enter League ID") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                singleLine = true
            )

            if (showError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    val managerId = managerIdText.toLongOrNull()
                    val leagueId = leagueIdText.toLongOrNull()

                    when {
                        managerId == null -> {
                            showError = true
                            errorMessage = "Please enter a valid Manager ID"
                        }
                        else -> {
                            // Save IDs
                            prefsManager.saveManagerId(managerId)
                            if (leagueId != null) {
                                prefsManager.saveLeagueId(leagueId)
                            }
                            
                            // Navigate to manager stats
                            navController.navigate(Screen.ManagerStats.createRoute(managerId)) {
                                popUpTo(Screen.Initial.route) { inclusive = true }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Continue")
            }

            if (leagueIdText.isNotEmpty() && leagueIdText.toLongOrNull() != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        val leagueId = leagueIdText.toLongOrNull()
                        if (leagueId != null) {
                            prefsManager.saveLeagueId(leagueId)
                            navController.navigate(Screen.LeagueStandings.createRoute(leagueId)) {
                                popUpTo(Screen.Initial.route) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Go to League Only")
                }
            }
        }
    }
}

