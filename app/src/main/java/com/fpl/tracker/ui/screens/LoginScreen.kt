package com.fpl.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fpl.tracker.data.preferences.PreferencesManager
import com.fpl.tracker.navigation.Screen

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    
    var managerIdText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Check if already logged in and auto-navigate
    LaunchedEffect(Unit) {
        val savedManagerId = prefsManager.getManagerId()
        if (savedManagerId != null) {
            navController.navigate(Screen.MainApp.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF6B35),
                        Color(0xFFF7931E)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
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
                    fontSize = 48.sp
                )
                Text(
                    text = "FPL GAMEWEEK",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF37003C)
                )
            }
        }

        // Main Title
        Text(
            text = "THE COMPLETE\nFPL DASHBOARD",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 38.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Track your points, your rivals, your rank\nand much more LIVE!",
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        // Login Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Sign In",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF37003C),
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
                        focusedBorderColor = Color(0xFF37003C),
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                
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
                        containerColor = Color(0xFF37003C)
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
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

