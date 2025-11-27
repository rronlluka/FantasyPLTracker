package com.fpl.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fpl.tracker.data.models.Fixture
import com.fpl.tracker.viewmodel.MatchesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    navController: NavController,
    viewModel: MatchesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFixture by remember { mutableStateOf<Fixture?>(null) }
    var showFixtureDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadCurrentGameweekFixtures()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Gameweek ${uiState.currentEvent} Fixtures",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            uiState.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = Color(0xFFFF5555)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadCurrentGameweekFixtures() }) {
                        Text("Retry")
                    }
                }
            }
            uiState.fixtures.isNotEmpty() -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.fixtures) { fixture ->
                        FixtureCard(
                            fixture = fixture,
                            homeTeam = uiState.teams.find { it.id == fixture.teamH },
                            awayTeam = uiState.teams.find { it.id == fixture.teamA },
                            onClick = {
                                selectedFixture = fixture
                                showFixtureDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Fixture Detail Dialog
    if (showFixtureDialog && selectedFixture != null) {
        FixtureDetailDialog(
            fixture = selectedFixture!!,
            homeTeam = uiState.teams.find { it.id == selectedFixture!!.teamH },
            awayTeam = uiState.teams.find { it.id == selectedFixture!!.teamA },
            players = uiState.players,
            onDismiss = {
                showFixtureDialog = false
                selectedFixture = null
            }
        )
    }
}

@Composable
fun FixtureCard(
    fixture: Fixture,
    homeTeam: com.fpl.tracker.data.models.Team?,
    awayTeam: com.fpl.tracker.data.models.Team?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                fixture.started == true && fixture.finished == false -> Color(0xFFFFEBEE)
                fixture.finished -> Color.White
                else -> Color(0xFFF5F5F5)
            }
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
            // Home Team
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = homeTeam?.shortName ?: "TBD",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (fixture.finished) {
                    Text(
                        text = "${fixture.teamHScore ?: 0}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF37003C)
                    )
                }
            }
            
            // Status/Score
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                when {
                    fixture.started == true && fixture.finished == false -> {
                        Text(
                            "🔴 LIVE",
                            color = Color(0xFFFF5555),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            "${fixture.minutes}'",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                    fixture.finished -> {
                        Text(
                            "FT",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    else -> {
                        Text(
                            "vs",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        fixture.kickoffTime?.let {
                            Text(
                                it.substringAfter("T").substringBefore(":00"),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
            
            // Away Team
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = awayTeam?.shortName ?: "TBD",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (fixture.finished) {
                    Text(
                        text = "${fixture.teamAScore ?: 0}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF37003C)
                    )
                }
            }
        }
    }
}

@Composable
fun FixtureDetailDialog(
    fixture: Fixture,
    homeTeam: com.fpl.tracker.data.models.Team?,
    awayTeam: com.fpl.tracker.data.models.Team?,
    players: List<com.fpl.tracker.data.models.Player>,
    onDismiss: () -> Unit
) {
    // Debug logging
    android.util.Log.d("MatchesScreen", "=== FIXTURE DETAIL ===")
    android.util.Log.d("MatchesScreen", "Fixture ID: ${fixture.id}")
    android.util.Log.d("MatchesScreen", "Stats null? ${fixture.stats == null}")
    android.util.Log.d("MatchesScreen", "Stats size: ${fixture.stats?.size ?: 0}")
    fixture.stats?.forEach { stat ->
        android.util.Log.d("MatchesScreen", "Stat: ${stat.identifier}, h: ${stat.h.size}, a: ${stat.a.size}")
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Match Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF37003C)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Teams
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            homeTeam?.name ?: "Home",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        if (fixture.finished) {
                            Text(
                                "${fixture.teamHScore}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF37003C)
                            )
                        }
                    }
                    
                    Text("vs", fontSize = 20.sp, color = Color.Gray)
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            awayTeam?.name ?: "Away",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        if (fixture.finished) {
                            Text(
                                "${fixture.teamAScore}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF37003C)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Match Info
                InfoRow("Status", when {
                    fixture.started == true && fixture.finished == false -> "🔴 LIVE (${fixture.minutes}')"
                    fixture.finished -> "Finished"
                    else -> "Not Started"
                })
                
                fixture.kickoffTime?.let {
                    InfoRow("Kickoff", it.substringBefore("Z").replace("T", " "))
                }
                
                InfoRow("Home Difficulty", "${fixture.teamHDifficulty}/5")
                InfoRow("Away Difficulty", "${fixture.teamADifficulty}/5")
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Match Statistics Section
                if (fixture.stats != null && fixture.stats.isNotEmpty()) {
                    Text(
                        "Match Statistics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(fixture.stats) { stat ->
                            if (stat.h.isNotEmpty() || stat.a.isNotEmpty()) {
                                val statName = when(stat.identifier) {
                                    "goals_scored" -> "⚽ Goals"
                                    "assists" -> "🅰️ Assists"
                                    "own_goals" -> "Own Goals"
                                    "penalties_saved" -> "Penalties Saved"
                                    "penalties_missed" -> "Penalties Missed"
                                    "yellow_cards" -> "🟨 Yellow Cards"
                                    "red_cards" -> "🟥 Red Cards"
                                    "saves" -> "🧤 Saves"
                                    "bonus" -> "⭐ Bonus Points"
                                    "bps" -> "BPS"
                                    else -> stat.identifier.replace("_", " ").replaceFirstChar { it.uppercase() }
                                }
                                
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF5F5F5)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            statName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Home Team Stats
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.Start
                                            ) {
                                                if (stat.h.isEmpty()) {
                                                    Text(
                                                        "-",
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                } else {
                                                    stat.h.forEach { value ->
                                                        val player = players.find { it.id == value.element }
                                                        Text(
                                                            "${player?.webName ?: "Unknown"} (${value.value})",
                                                            fontSize = 12.sp,
                                                            color = Color.Black
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            // Away Team Stats
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                if (stat.a.isEmpty()) {
                                                    Text(
                                                        "-",
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                } else {
                                                    stat.a.forEach { value ->
                                                        val player = players.find { it.id == value.element }
                                                        Text(
                                                            "${player?.webName ?: "Unknown"} (${value.value})",
                                                            fontSize = 12.sp,
                                                            color = Color.Black
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                } else if (fixture.finished) {
                    Text(
                        "No detailed stats available for this match",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

