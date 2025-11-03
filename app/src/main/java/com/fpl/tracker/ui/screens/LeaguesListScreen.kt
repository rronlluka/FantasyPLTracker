package com.fpl.tracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fpl.tracker.data.preferences.PreferencesManager
import com.fpl.tracker.navigation.Screen
import com.fpl.tracker.viewmodel.ManagerStatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaguesListScreen(
    navController: NavController,
    managerId: Long,
    viewModel: ManagerStatsViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val uiState by viewModel.uiState.collectAsState()
    val favoriteLeagueId = prefsManager.getFavoriteLeagueId()
    
    LaunchedEffect(managerId) {
        viewModel.loadManagerData(managerId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF37003C))
                }
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = Color(0xFFFF5555)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadManagerData(managerId) }) {
                        Text("Retry")
                    }
                }
            }
            uiState.managerData != null -> {
                val managerData = uiState.managerData!!
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "My Leagues",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF37003C)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    items(managerData.leagues.classic) { league ->
                        val isFavorite = league.id.toLong() == favoriteLeagueId
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate(
                                        Screen.LeagueStandings.createRoute(league.id.toLong())
                                    )
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isFavorite) Color(0xFFFFE0B2) else Color(0xFF37003C)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = league.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFavorite) Color.Black else Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    league.entryRank?.let {
                                        Text(
                                            text = "Your Rank: #$it",
                                            fontSize = 14.sp,
                                            color = if (isFavorite) Color(0xFF37003C) else Color(0xFF00FF87)
                                        )
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isFavorite) {
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = "Favorite",
                                            tint = Color(0xFFFFD700),
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "View",
                                        tint = if (isFavorite) Color(0xFF37003C) else Color(0xFF00FF87)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

