package com.fpl.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fpl.tracker.data.models.RankChange
import com.fpl.tracker.data.models.StandingEntry
import com.fpl.tracker.data.preferences.PreferencesManager
import com.fpl.tracker.navigation.Screen
import com.fpl.tracker.viewmodel.LeagueStandingsViewModel
import com.fpl.tracker.viewmodel.ManagerLiveData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedLeagueStandingsScreen(
    navController: NavController,
    leagueId: Long,
    viewModel: LeagueStandingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val uiState by viewModel.uiState.collectAsState()
    
    var showFavoriteDialog by remember { mutableStateOf(false) }
    var showChangeLeagueDialog by remember { mutableStateOf(false) }
    val isFavorite = remember(leagueId) { 
        prefsManager.getFavoriteLeagueId() == leagueId 
    }
    
    LaunchedEffect(leagueId) {
        viewModel.loadLeagueStandings(leagueId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = uiState.leagueStandings?.league?.name ?: "League",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Favorite button
                    IconButton(onClick = {
                        if (isFavorite) {
                            prefsManager.removeFavoriteLeague()
                            showFavoriteDialog = true
                        } else {
                            uiState.leagueStandings?.league?.let { league ->
                                prefsManager.saveFavoriteLeague(leagueId, league.name)
                                showFavoriteDialog = true
                            }
                        }
                    }) {
                        Icon(
                            if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            "Favorite",
                            tint = if (isFavorite) Color(0xFFFFD700) else Color.White
                        )
                    }
                    
                    // Change league button
                    IconButton(onClick = { showChangeLeagueDialog = true }) {
                        Icon(Icons.Filled.Refresh, "Change League")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF37003C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1E1E1E))
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF00FF87)
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
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadLeagueStandings(leagueId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00FF87)
                            )
                        ) {
                            Text("Retry", color = Color.Black)
                        }
                    }
                }
                uiState.leagueStandings != null -> {
                    // Use live rankings if available, otherwise use original
                    val standings = if (uiState.liveRankings.isNotEmpty()) {
                        uiState.liveRankings
                    } else {
                        uiState.leagueStandings!!.standings.results
                    }
                    
                    val hasLiveData = uiState.managerLiveData.isNotEmpty()
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Live indicator banner
                        if (hasLiveData) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFF5722))
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "🔴 LIVE - Rankings updating in real-time",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        
                        // Header Row
                        item {
                            LeagueTableHeader()
                        }
                        
                        // Standing Rows
                        itemsIndexed(standings) { index, standing ->
                            val isUserTeam = standing.entry == prefsManager.getManagerId()?.toInt()
                            val liveData = uiState.managerLiveData[standing.entry]
                            EnhancedStandingRow(
                                standing = standing,
                                isUserTeam = isUserTeam,
                                playersInPlay = liveData?.inPlay ?: 0,
                                playersToStart = liveData?.toStart ?: 0,
                                liveData = liveData,
                                onClick = {
                                    navController.navigate(
                                        Screen.ManagerFormation.createRoute(
                                            standing.entry.toLong(),
                                            uiState.currentEvent
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Favorite Dialog
        if (showFavoriteDialog) {
            AlertDialog(
                onDismissRequest = { showFavoriteDialog = false },
                title = { 
                    Text(
                        if (isFavorite) "Removed from Favorites" else "Added to Favorites"
                    ) 
                },
                text = { 
                    Text(
                        if (isFavorite) {
                            "This league has been removed from favorites"
                        } else {
                            "This league will load automatically when you open the app"
                        }
                    ) 
                },
                confirmButton = {
                    Button(onClick = { showFavoriteDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
        
        // Change League Dialog
        if (showChangeLeagueDialog) {
            var newLeagueId by remember { mutableStateOf("") }
            
            AlertDialog(
                onDismissRequest = { showChangeLeagueDialog = false },
                title = { Text("Change League") },
                text = {
                    OutlinedTextField(
                        value = newLeagueId,
                        onValueChange = { newLeagueId = it },
                        label = { Text("League ID") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            newLeagueId.toLongOrNull()?.let { id ->
                                showChangeLeagueDialog = false
                                navController.navigate(Screen.LeagueStandings.createRoute(id)) {
                                    popUpTo(Screen.LeagueStandings.route) { inclusive = true }
                                }
                            }
                        }
                    ) {
                        Text("Go")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangeLeagueDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun LeagueTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2D2D))
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "#",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.width(35.dp)
        )
        Text(
            "Captain",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.weight(1.5f)
        )
        Text(
            "In Play",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(50.dp)
        )
        Text(
            "To Start",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(50.dp)
        )
        Text(
            "GW Pts",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(60.dp)
        )
        Text(
            "Total",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(60.dp)
        )
    }
}

@Composable
fun EnhancedStandingRow(
    standing: StandingEntry,
    isUserTeam: Boolean,
    playersInPlay: Int,
    playersToStart: Int,
    liveData: ManagerLiveData?,
    onClick: () -> Unit
) {
    // Calculate rank change
    val rankChange = when {
        standing.rank < standing.lastRank -> RankChange.UP
        standing.rank > standing.lastRank -> RankChange.DOWN
        else -> RankChange.NO_CHANGE
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isUserTeam) Color(0xFF1565C0) else Color(0xFF1E1E1E)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank with arrow
        Row(
            modifier = Modifier.width(35.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${standing.rank}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            when (rankChange) {
                RankChange.UP -> Text(
                    text = "▲",
                    color = Color(0xFF00FF87),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                RankChange.DOWN -> Text(
                    text = "▼",
                    color = Color(0xFFFF5555),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                RankChange.NO_CHANGE -> Text(
                    text = "—",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        
        // Captain (Team name + Manager)
        Column(
            modifier = Modifier.weight(1.5f)
        ) {
            Text(
                text = standing.entryName,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = standing.playerName,
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // In Play
        Text(
            text = "$playersInPlay",
            color = if (playersInPlay > 0) Color(0xFF00FF87) else Color.White,
            fontWeight = if (playersInPlay > 0) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(50.dp)
        )
        
        // To Start
        Text(
            text = "$playersToStart",
            color = if (playersToStart > 0) Color(0xFFFFAA00) else Color.White,
            fontWeight = if (playersToStart > 0) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(50.dp)
        )
        
        // GW Pts (with live points if applicable)
        val hasLivePoints = playersInPlay > 0 && liveData != null
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasLivePoints) {
                // Show live total
                Text(
                    text = "${standing.eventTotal + (liveData?.livePoints ?: 0)}",
                    color = Color(0xFF00FF87),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                // Show base points in smaller text
                Text(
                    text = "(${standing.eventTotal})",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "${standing.eventTotal}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Total (with live points added)
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (hasLivePoints) {
                // Show live total (already updated in standings)
                Text(
                    text = "${standing.total}",
                    color = Color(0xFF00FF87),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                // Show added live points
                if (liveData.livePoints > 0) {
                    Text(
                        text = "+${liveData.livePoints}",
                        color = Color(0xFF00FF87),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "${standing.total}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    
    Divider(color = Color(0xFF2D2D2D), thickness = 1.dp)
}

