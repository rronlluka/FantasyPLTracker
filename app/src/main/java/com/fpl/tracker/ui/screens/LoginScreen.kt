package com.fpl.tracker.ui.screens

import com.fpl.tracker.BuildConfig
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fpl.tracker.data.api.BackendDiagnostics
import com.fpl.tracker.data.api.BackendRetrofitInstance
import com.fpl.tracker.data.preferences.PreferencesManager
import com.fpl.tracker.data.repository.FPLRepository
import com.fpl.tracker.navigation.Screen
import kotlinx.coroutines.launch

// Brand colours (mirroring theme values for use in custom drawing)
private val BrandGreen = Color(0xFFA1D494)
private val BrandGreenDark = Color(0xFF2D5A27)
private val BrandYellow = Color(0xFFFFE083)
private val DarkBg = Color(0xFF131313)
private val CardBg = Color(0xFF1C1B1B)
private val CardHighBg = Color(0xFF2A2A2A)

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

    // Auto-navigate if already logged in
    LaunchedEffect(Unit) {
        val savedManagerId = prefsManager.getManagerId()
        if (savedManagerId != null) {
            navController.navigate(Screen.MainApp.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    // Subtle pulse animation for the glow blob
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Background radial glow (top-left accent)
        Box(
            modifier = Modifier
                .size(320.dp)
                .offset(x = (-60).dp, y = 40.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                BrandGreen.copy(alpha = glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = statusBarPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── HERO SECTION ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 36.dp, bottom = 8.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column {
                    // Badge pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(BrandGreenDark.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(BrandGreen)
                        )
                        Text(
                            text = "LIVE · 2024/25",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreen,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // App wordmark
                    Text(
                        text = "FPL",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 52.sp,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "LIVE\nTRACKER",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandGreen,
                        lineHeight = 52.sp,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Real-time insights for your\nFantasy Premier League squad.",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.55f),
                        lineHeight = 24.sp
                    )
                }
            }

            // ── STAT CHIPS ROW ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChip(label = "Live Rank", modifier = Modifier.weight(1f))
                StatChip(label = "Best XI", modifier = Modifier.weight(1f))
                StatChip(label = "Transfers", modifier = Modifier.weight(1f))
            }

            // ── SIGN-IN CARD ──────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        text = "Sign in",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Enter your FPL Manager ID to get started",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    // Manager ID field
                    OutlinedTextField(
                        value = managerIdText,
                        onValueChange = {
                            managerIdText = it
                            showError = false
                        },
                        label = { Text("Manager ID") },
                        placeholder = { Text("e.g. 685991") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = BrandGreen,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
                            cursorColor = BrandGreen,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    // How-to help toggle
                    var showHelp by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = { showHelp = !showHelp },
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (showHelp) "Hide instructions ▲" else "How do I find my Manager ID? ▼",
                            fontSize = 12.sp,
                            color = BrandGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (showHelp) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = CardHighBg
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "Finding your Manager ID",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BrandGreen,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                listOf(
                                    "Go to fantasy.premierleague.com",
                                    "Open the Points or Transfers page",
                                    "Look at the URL in your browser",
                                    "The entry number in the URL is your ID"
                                ).forEachIndexed { index, step ->
                                    Text(
                                        "${index + 1}. $step",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.75f),
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = DarkBg.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "fantasy.premierleague.com/entry/",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                        Text(
                                            "685991",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandYellow,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                        Text(
                                            "/event/12",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                                Text(
                                    "↑ 685991 is your Manager ID",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }

                    // Error
                    if (showError) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // CTA button
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
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGreen,
                            contentColor = Color(0xFF0A3909)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "GET STARTED",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    Text(
                        text = "No account needed · free to use",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )

                    // ── DEBUG SECTION (release builds excluded) ────────────
                    if (BuildConfig.DEBUG) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Debug · Backend",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BrandGreen.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = backendUrlText,
                            onValueChange = { backendUrlText = it },
                            label = { Text("Backend URL") },
                            placeholder = { Text("http://127.0.0.1:3000/api/") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandGreen.copy(alpha = 0.6f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = BrandGreen.copy(alpha = 0.6f),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.3f),
                                cursorColor = BrandGreen
                            )
                        )
                        Spacer(Modifier.height(8.dp))
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
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BrandGreenDark,
                                    contentColor = BrandGreen
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) { Text("Save URL", fontSize = 12.sp) }
                            OutlinedButton(
                                onClick = {
                                    val defaultUrl = BackendRetrofitInstance.getDefaultBaseUrl()
                                    backendUrlText = defaultUrl
                                    prefsManager.saveBackendUrl(defaultUrl)
                                    BackendRetrofitInstance.updateBaseUrl(defaultUrl)
                                    backendStatusMessage = "Reset to default URL"
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color.White.copy(alpha = 0.15f)
                                )
                            ) { Text("Reset", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f)) }
                        }
                        OutlinedButton(
                            onClick = {
                                isCheckingBackend = true
                                scope.launch {
                                    val result = repository.getBackendHealth()
                                    backendStatusMessage = result.getOrNull()?.let {
                                        "Backend healthy · uptime ${it.uptimeSeconds ?: 0}s"
                                    } ?: "Health check failed: ${result.exceptionOrNull()?.message}"
                                    isCheckingBackend = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCheckingBackend,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, Color.White.copy(alpha = 0.12f)
                            )
                        ) {
                            Text(
                                if (isCheckingBackend) "Checking…" else "Check Backend Health",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.55f)
                            )
                        }
                        Text(
                            text = "URL: ${backendDiagnostics.baseUrl.ifBlank { BackendRetrofitInstance.getBaseUrl() }}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        backendStatusMessage?.let {
                            Text(text = it, fontSize = 11.sp, color = BrandGreen.copy(alpha = 0.8f))
                        }
                        backendDiagnostics.lastHealthStatus?.let {
                            Text(text = "Health: $it", fontSize = 11.sp, color = Color.White.copy(alpha = 0.45f))
                        }
                        backendDiagnostics.lastError?.let {
                            Text(text = "Error: $it", fontSize = 11.sp, color = Color(0xFFFF6B6B))
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun StatChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CardBg
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(BrandGreen)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
