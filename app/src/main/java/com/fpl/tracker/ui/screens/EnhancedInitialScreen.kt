package com.fpl.tracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedInitialScreen(navController: NavController) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    
    var teamIdText by remember { mutableStateOf("") }
    var leagueIdText by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Check for favorite league and auto-navigate
    LaunchedEffect(Unit) {
        val favoriteLeagueId = prefsManager.getFavoriteLeagueId()
        if (favoriteLeagueId != null) {
            navController.navigate(Screen.LeagueStandings.createRoute(favoriteLeagueId)) {
                popUpTo(Screen.Initial.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF6B35), // Orange
                        Color(0xFFF7931E)  // Light Orange
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // App Logo/Title
        Card(
            modifier = Modifier.padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚽",
                    fontSize = 40.sp
                )
                Text(
                    text = "FPL GAMEWEEK",
                    fontSize = 16.sp,
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
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Text(
            text = "Track your points, your rivals, your rank\nand much more LIVE!",
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Team ID Input Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Your Team Id",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF37003C),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = teamIdText,
                        onValueChange = { 
                            teamIdText = it
                            showError = false
                        },
                        placeholder = { Text("Enter Team ID") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF37003C),
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Button(
                        onClick = {
                            val managerId = teamIdText.toLongOrNull()
                            if (managerId != null) {
                                prefsManager.saveManagerId(managerId)
                                navController.navigate(Screen.ManagerStats.createRoute(managerId)) {
                                    popUpTo(Screen.Initial.route) { inclusive = true }
                                }
                            } else {
                                showError = true
                                errorMessage = "Please enter a valid Team ID"
                            }
                        },
                        modifier = Modifier.height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF37003C)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("LET'S GO")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, "Go")
                    }
                }
            }
        }

        // OR Divider
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(
                modifier = Modifier.weight(1f),
                color = Color.White,
                thickness = 2.dp
            )
            Text(
                text = "  OR  ",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Divider(
                modifier = Modifier.weight(1f),
                color = Color.White,
                thickness = 2.dp
            )
        }

        // League ID Input Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Or Your League Id",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF37003C),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = leagueIdText,
                        onValueChange = { 
                            leagueIdText = it
                            showError = false
                        },
                        placeholder = { Text("LEAGUE ID") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF37003C),
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Button(
                        onClick = {
                            val leagueId = leagueIdText.toLongOrNull()
                            if (leagueId != null) {
                                prefsManager.saveLeagueId(leagueId)
                                navController.navigate(Screen.LeagueStandings.createRoute(leagueId)) {
                                    popUpTo(Screen.Initial.route) { inclusive = true }
                                }
                            } else {
                                showError = true
                                errorMessage = "Please enter a valid League ID"
                            }
                        },
                        modifier = Modifier.height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF37003C)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SEARCH")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Search, "Search")
                    }
                }
            }
        }

        if (showError) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF5555)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Info Text
        Text(
            text = "Find your Team ID at fantasy.premierleague.com",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

