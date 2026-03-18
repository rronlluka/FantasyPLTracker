package com.fpl.tracker.ui.screens

import com.fpl.tracker.BuildConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fpl.tracker.data.api.BackendDiagnostics
import com.fpl.tracker.data.api.BackendRetrofitInstance
import com.fpl.tracker.data.preferences.PreferencesManager
import com.fpl.tracker.data.repository.FPLRepository
import com.fpl.tracker.navigation.Screen
import com.fpl.tracker.ui.theme.AuroraTeal
import com.fpl.tracker.ui.theme.CelestialPurple
import com.fpl.tracker.ui.theme.DeepSpace
import com.fpl.tracker.ui.theme.FrostedLilac
import com.fpl.tracker.ui.theme.NightSky
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val repository = remember { FPLRepository() }
    val scope = rememberCoroutineScope()
    val backendDiagnostics by BackendDiagnostics.state.collectAsState()
    
    var managerIdText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var backendUrlText by remember {
        mutableStateOf(prefsManager.getBackendUrl() ?: BackendRetrofitInstance.getBaseUrl())
    }
    var backendStatusMessage by remember { mutableStateOf<String?>(null) }
    var isCheckingBackend by remember { mutableStateOf(false) }

    // Check if already logged in and auto-navigate
    LaunchedEffect(Unit) {
        val savedManagerId = prefsManager.getManagerId()
        if (savedManagerId != null) {
            navController.navigate(Screen.MainApp.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        NightSky,
                        DeepSpace
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(top = statusBarPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo/Title
        Card(
            modifier = Modifier.padding(bottom = 24.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
        Text(
            text = "⚽",
            fontSize = 48.sp,
            color = FrostedLilac
        )
        Text(
            text = "FPL LIVE TRACKER",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = FrostedLilac
        )
            }
        }

        // Main Title
        Text(
            text = "Live manager insights",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = FrostedLilac,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

//        Text(
//            text = "A darker, high-contrast home for your Fantasy Premier League data—sign in and stay live with every transfer, rank and hit.",
//            fontSize = 16.sp,
//            color = FrostedLilac.copy(alpha = 0.85f),
//            textAlign = TextAlign.Center,
//            modifier = Modifier.padding(bottom = 32.dp)
//        )

        // Login Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Sign In with your Manager ID",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = managerIdText,
                    onValueChange = { 
                        managerIdText = it
                        showError = false
                    },
                    label = { Text("Manager ID") },
                    placeholder = { Text("Enter your FPL Manager ID") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                )
                
                // Help section
                var showHelp by remember { mutableStateOf(false) }
                
                TextButton(
                    onClick = { showHelp = !showHelp },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = if (showHelp) "Hide instructions ▲" else "How to find your Manager ID? ▼",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (showHelp) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                "How to find your Manager ID",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            listOf(
                                "Go to fantasy.premierleague.com",
                                "Open the Points or Transfers page",
                                "Inspect the URL shown in your browser",
                                "The entry number in that URL is your ID"
                            ).forEachIndexed { index, step ->
                                Text(
                                    "${index + 1}. $step",
                                    fontSize = 12.sp,
                                    color = FrostedLilac.copy(alpha = 0.8f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        "fantasy.premierleague.com/entry/",
                                        fontSize = 11.sp,
                                        color = FrostedLilac.copy(alpha = 0.7f),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                    Text(
                                        "685991",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                    Text(
                                        "/event/12",
                                        fontSize = 11.sp,
                                        color = FrostedLilac.copy(alpha = 0.7f),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                "↑ That number (685991) is your Manager ID",
                                fontSize = 11.sp,
                                color = FrostedLilac.copy(alpha = 0.8f),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
                
                if (showError) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFF5555),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = {
                        val managerId = managerIdText.toLongOrNull()
                        if (managerId != null) {
                            prefsManager.saveManagerId(managerId)
                            navController.navigate(Screen.MainApp.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } else {
                            showError = true
                            errorMessage = "Please enter a valid Manager ID"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuroraTeal,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("LET'S GO", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, "Go")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
        Text(
            text = "Find your ID at fantasy.premierleague.com",
            fontSize = 12.sp,
            color = FrostedLilac.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        )

                if (BuildConfig.DEBUG) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Debug Backend",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = backendUrlText,
                                onValueChange = { backendUrlText = it },
                                label = { Text("Backend URL") },
                                placeholder = { Text("http://127.0.0.1:3000/api/") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        prefsManager.saveBackendUrl(backendUrlText)
                                        BackendRetrofitInstance.updateBaseUrl(backendUrlText)
                                        backendStatusMessage = "Saved backend URL"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Save URL")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val defaultUrl = BackendRetrofitInstance.getDefaultBaseUrl()
                                        backendUrlText = defaultUrl
                                        prefsManager.saveBackendUrl(defaultUrl)
                                        BackendRetrofitInstance.updateBaseUrl(defaultUrl)
                                        backendStatusMessage = "Reset to default URL"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reset")
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    isCheckingBackend = true
                                    scope.launch {
                                        val result = repository.getBackendHealth()
                                        backendStatusMessage = result.getOrNull()?.let {
                                            "Backend healthy • uptime ${it.uptimeSeconds ?: 0}s"
                                        } ?: "Health check failed: ${result.exceptionOrNull()?.message}"
                                        isCheckingBackend = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isCheckingBackend
                            ) {
                                Text(if (isCheckingBackend) "Checking…" else "Check Backend Health")
                            }
                            Text(
                                text = "Current URL: ${backendDiagnostics.baseUrl.ifBlank { BackendRetrofitInstance.getBaseUrl() }}",
                                fontSize = 12.sp,
                                color = FrostedLilac.copy(alpha = 0.85f)
                            )
                            backendStatusMessage?.let {
                                Text(
                                    text = it,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            backendDiagnostics.lastHealthStatus?.let {
                                Text(
                                    text = "Health: $it",
                                    fontSize = 12.sp,
                                    color = FrostedLilac.copy(alpha = 0.8f)
                                )
                            }
                            backendDiagnostics.lastError?.let {
                                Text(
                                    text = "Last backend error: $it",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFF5555)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
