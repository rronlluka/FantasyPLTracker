package com.fpl.tracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fpl.tracker.data.models.StandingEntry
import com.fpl.tracker.navigation.Screen
import com.fpl.tracker.ui.theme.CelestialPurple
import com.fpl.tracker.ui.theme.FrostedLilac
import com.fpl.tracker.viewmodel.LeagueStandingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueStandingsScreen(
    navController: NavController,
    leagueId: Long,
    viewModel: LeagueStandingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(leagueId) {
        viewModel.loadLeagueStandings(leagueId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("League Standings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CelestialPurple,
                    titleContentColor = FrostedLilac
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
                        Button(onClick = { viewModel.loadLeagueStandings(leagueId) }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.leagueStandings != null -> {
                    val leagueStandings = uiState.leagueStandings!!
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // League Info Header
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = leagueStandings.league.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Total Entries: ${leagueStandings.standings.results.size}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Column Headers
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Rank",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                    Text(
                                        "Manager",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(2f)
                                    )
                                    Text(
                                        "GW",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(0.7f)
                                    )
                                    Text(
                                        "Total",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }
                            }
                        }

                        // Standings List
                        items(leagueStandings.standings.results) { standing ->
                            StandingCard(
                                standing = standing,
                                onClick = {
                                    navController.navigate(
                                        Screen.ManagerFormation.createRoute(
                                            standing.entry.toLong(),
                                            uiState.currentEvent,
                                            standing.entryName.ifBlank { standing.playerName }
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StandingCard(standing: StandingEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = "${standing.rank}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.8f)
            )
            
            // Manager Info
            Column(
                modifier = Modifier.weight(2f)
            ) {
                Text(
                    text = standing.entryName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = standing.playerName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Gameweek Points
            Text(
                text = "${standing.eventTotal}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(0.7f)
            )
            
            // Total Points
            Text(
                text = "${standing.total}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(0.8f)
            )
        }
    }
}
