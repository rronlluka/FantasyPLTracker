package com.fpl.tracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fpl.tracker.data.preferences.PreferencesManager
import com.fpl.tracker.navigation.Screen
import com.fpl.tracker.viewmodel.ManagerStatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerStatsScreen(
    navController: NavController,
    managerId: Long,
    viewModel: ManagerStatsViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val uiState by viewModel.uiState.collectAsState()
    
    var showLeagueDropdown by remember { mutableStateOf(false) }
    
    LaunchedEffect(managerId) {
        viewModel.loadManagerData(managerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manager Stats") },
                navigationIcon = {
                    IconButton(onClick = { 
                        prefsManager.clearAll()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadManagerData(managerId) }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.managerData != null -> {
                    val managerData = uiState.managerData!!
                    val managerHistory = uiState.managerHistory
                    val bootstrapData = uiState.bootstrapData
                    val currentEvent = bootstrapData?.events?.find { it.isCurrent }?.id ?: 1

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Manager Info Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "${managerData.playerFirstName} ${managerData.playerLastName}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = managerData.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Region: ${managerData.playerRegionName}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Stats Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Overall Stats",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    StatsRow("Overall Points", managerData.summaryOverallPoints.toString())
                                    StatsRow("Overall Rank", "#${String.format("%,d", managerData.summaryOverallRank)}")
                                    StatsRow("Gameweek Points", managerData.summaryEventPoints.toString())
                                    managerData.summaryEventRank?.let {
                                        StatsRow("Gameweek Rank", "#${String.format("%,d", it)}")
                                    }
                                }
                            }
                        }

                        // View Team Button
                        item {
                            Button(
                                onClick = {
                                    navController.navigate(
                                        Screen.ManagerFormation.createRoute(managerId, currentEvent)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("View Current Team")
                            }
                        }

                        // Leagues Dropdown
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showLeagueDropdown = !showLeagueDropdown }
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Leagues (${managerData.leagues.classic.size})",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Expand"
                                        )
                                    }
                                    
                                    if (showLeagueDropdown) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        managerData.leagues.classic.forEach { league ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable {
                                                    prefsManager.saveLeagueId(league.id.toLong())
                                                    // Navigate to enhanced league standings
                                                    navController.navigate(
                                                        Screen.LeagueStandings.createRoute(league.id.toLong())
                                                    )
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFF37003C)
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
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    league.entryRank?.let {
                                                        Text(
                                                            text = "Your Rank: #$it",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = Color(0xFF00FF87)
                                                        )
                                                    }
                                                }
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = "View League",
                                                    tint = Color(0xFF00FF87)
                                                )
                                            }
                                        }
                                        }
                                    }
                                }
                            }
                        }

                        // Gameweek History
                        if (managerHistory != null && managerHistory.current.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Recent Gameweeks",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        managerHistory.current.takeLast(5).reversed().forEach { gw ->
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("GW ${gw.event}")
                                                    Text("${gw.points} pts")
                                                    gw.rank?.let {
                                                        Text("Rank: #${String.format("%,d", it)}")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Check League Button
                        item {
                            val savedLeagueId = prefsManager.getLeagueId()
                            if (savedLeagueId != null) {
                                OutlinedButton(
                                    onClick = {
                                        navController.navigate(
                                            Screen.LeagueStandings.createRoute(savedLeagueId)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View Saved League")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

