package com.fpl.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.util.Log
import com.fpl.tracker.data.api.RetrofitInstance
import com.fpl.tracker.data.repository.FPLRepository
import com.fpl.tracker.ui.components.FootballPitch
import com.fpl.tracker.ui.components.PlayerDetailDialog
import com.fpl.tracker.viewmodel.ManagerFormationViewModel
import com.fpl.tracker.viewmodel.PlayerWithDetails
import com.fpl.tracker.viewmodel.LeagueStandingsViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.fpl.tracker.data.preferences.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerFormationScreen(
    navController: NavController,
    managerId: Long,
    eventId: Int,
    viewModel: ManagerFormationViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val uiState by viewModel.uiState.collectAsState()
    val leagueViewModel: LeagueStandingsViewModel = viewModel()
    
    var selectedPlayer by remember { mutableStateOf<PlayerWithDetails?>(null) }
    var showPlayerDialog by remember { mutableStateOf(false) }
    var playerDetail by remember { mutableStateOf<com.fpl.tracker.data.models.PlayerDetailResponse?>(null) }
    var leagueStats by remember { mutableStateOf<com.fpl.tracker.data.models.LeaguePlayerStats?>(null) }
    var isLoadingPlayerData by remember { mutableStateOf(false) }
    var isPitchView by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val repository = remember { FPLRepository(RetrofitInstance.api) }
    
    LaunchedEffect(managerId, eventId) {
        viewModel.loadManagerFormation(managerId, eventId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Formation - GW$eventId") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isPitchView) "Pitch" else "List",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = isPitchView,
                            onCheckedChange = { isPitchView = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00FF87),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
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
                        Button(onClick = { viewModel.loadManagerFormation(managerId, eventId) }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.managerPicks != null -> {
                    val managerPicks = uiState.managerPicks!!
                    val playersWithDetails = uiState.playersWithDetails
                    val bootstrap = uiState.bootstrapData!!
                    
                    // Split players into starting XI and bench
                    val startingXI = playersWithDetails.filter { it.pick.position <= 11 }
                    val bench = playersWithDetails.filter { it.pick.position > 11 }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Summary Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF37003C)
                            )
                        ) {
                            // Calculate live points - sum up starting XI with live/just finished games
                            var livePoints = 0
                            startingXI.forEach { playerDetail ->
                                // Count points if hasLivePoints (live or just finished within 3 hours)
                                if (playerDetail.hasLivePoints && playerDetail.liveStats != null) {
                                    var pts = playerDetail.liveStats.stats.totalPoints
                                    
                                    // Add provisional bonus if player doesn't have bonus yet
                                    // (works for both truly live AND just finished games)
                                    val currentBonus = playerDetail.liveStats.stats.bonus
                                    val provisionalBonusPoints = uiState.provisionalBonus[playerDetail.player.id] ?: 0
                                    if (currentBonus == 0 && provisionalBonusPoints > 0) {
                                        pts += provisionalBonusPoints
                                    }
                                    
                                    livePoints += pts * playerDetail.pick.multiplier
                                }
                            }
                            
                            val totalGwPoints = managerPicks.entryHistory.points + livePoints
                            
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    StatItem(
                                        label = "GW Points",
                                        value = "$totalGwPoints",
                                        highlighted = true
                                    )
                                    StatItem(
                                        label = "Total Points",
                                        value = "${managerPicks.entryHistory.totalPoints + livePoints}"
                                    )
                                    StatItem(
                                        label = "GW Rank",
                                        value = managerPicks.entryHistory.rank?.let { 
                                            "#${String.format("%,d", it)}" 
                                        } ?: "N/A"
                                    )
                                }
                                
                                if (livePoints > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "🔴 Live: +$livePoints pts from ${startingXI.count { it.isLive }} players",
                                        color = Color(0xFF00FF87),
                                        fontSize = 12.sp,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                        
                        // Additional info
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Text(
                                    "Transfers: ${managerPicks.entryHistory.eventTransfers}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Cost: ${managerPicks.entryHistory.eventTransfersCost}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Bench: ${managerPicks.entryHistory.pointsOnBench}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            managerPicks.activeChip?.let {
                                Text(
                                    "Active Chip: $it",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // Starting XI - Pitch View or List View
                        Text(
                            text = "Starting XI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        // Shared click handler for player clicks
                        val handlePlayerClick: (PlayerWithDetails) -> Unit = { playerWithDetails ->
                            selectedPlayer = playerWithDetails
                            isLoadingPlayerData = true
                            scope.launch {
                                try {
                                    Log.d("PlayerDialog", "Loading data for player: ${playerWithDetails.player.webName}")
                                    val detailResult = repository.getPlayerDetail(playerWithDetails.player.id)
                                    val detail = detailResult.getOrNull()
                                    Log.d("PlayerDialog", "Player detail loaded: ${detail != null}")
                                    var stats: com.fpl.tracker.data.models.LeaguePlayerStats? = null
                                    val savedLeagueId = prefsManager.getLeagueId()
                                    if (savedLeagueId != null && uiState.bootstrapData != null) {
                                        Log.d("PlayerDialog", "Loading league stats for league: $savedLeagueId")
                                        val leagueResult = repository.getLeagueStandings(savedLeagueId)
                                        val leagueStandings = leagueResult.getOrNull()
                                        if (leagueStandings != null) {
                                            Log.d("PlayerDialog", "League standings loaded, calculating stats...")
                                            stats = leagueViewModel.calculateLeaguePlayerStats(
                                                playerId = playerWithDetails.player.id,
                                                leagueStandings = leagueStandings,
                                                currentEvent = eventId,
                                                bootstrapData = uiState.bootstrapData!!
                                            )
                                            Log.d("PlayerDialog", "Stats calculated - Starts: ${stats.startsCount}, Bench: ${stats.benchCount}, Captain: ${stats.captainCount}")
                                        } else {
                                            Log.d("PlayerDialog", "Failed to load league standings")
                                        }
                                    } else {
                                        Log.d("PlayerDialog", "No saved league ID or bootstrap data not loaded")
                                    }
                                    playerDetail = detail
                                    leagueStats = stats
                                    isLoadingPlayerData = false
                                    showPlayerDialog = true
                                } catch (e: Exception) {
                                    Log.e("PlayerDialog", "Error loading player data: ${e.message}")
                                    isLoadingPlayerData = false
                                }
                            }
                        }

                        if (isPitchView) {
                            FootballPitch(
                                startingXI = startingXI,
                                provisionalBonus = uiState.provisionalBonus,
                                onPlayerClick = handlePlayerClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // List View
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                startingXI.forEach { playerWithDetails ->
                                    PlayerListRow(
                                        playerDetail = playerWithDetails,
                                        provisionalBonus = uiState.provisionalBonus,
                                        onPlayerClick = handlePlayerClick
                                    )
                                }
                            }
                        }

                        // Bench Section
                        Text(
                            text = "Substitutes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                bench.forEachIndexed { index, benchPlayer ->
                                    BenchPlayerCard(
                                        playerDetail = benchPlayer,
                                        position = if (index == 0) "GKP" else "${index}. ${
                                            when(benchPlayer.player.elementType) {
                                                2 -> "DEF"
                                                3 -> "MID"
                                                4 -> "FWD"
                                                else -> "SUB"
                                            }
                                        }",
                                        onPlayerClick = handlePlayerClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Show loading overlay when loading player data
            if (isLoadingPlayerData) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF37003C)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading player data...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                selectedPlayer?.player?.webName ?: "",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
            
            // Show player detail dialog only after data is loaded
            if (showPlayerDialog && selectedPlayer != null && !isLoadingPlayerData) {
                PlayerDetailDialog(
                    player = selectedPlayer!!.player,
                    team = selectedPlayer!!.team,
                    playerDetail = playerDetail,
                    leagueStats = leagueStats,
                    bootstrapData = uiState.bootstrapData,
                    currentEvent = eventId,
                    liveStats = selectedPlayer!!.liveStats,
                    onDismiss = {
                        showPlayerDialog = false
                        selectedPlayer = null
                        playerDetail = null
                        leagueStats = null
                    }
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    highlighted: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlighted) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) Color(0xFF00FF87) else Color.White
        )
    }
}

@Composable
fun BenchPlayerCard(
    playerDetail: PlayerWithDetails,
    position: String,
    onPlayerClick: ((PlayerWithDetails) -> Unit)? = null
) {
    val points = playerDetail.liveStats?.stats?.totalPoints ?: playerDetail.player.eventPoints
    val goals = playerDetail.liveStats?.stats?.goalsScored ?: 0
    val assists = playerDetail.liveStats?.stats?.assists ?: 0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(75.dp)
    ) {
        Text(
            text = position,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        Card(
            modifier = Modifier
                .width(75.dp)
                .height(100.dp)
                .then(
                    if (onPlayerClick != null) {
                        Modifier.clickable { onPlayerClick(playerDetail) }
                    } else Modifier
                ),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Team badge/color
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = com.fpl.tracker.ui.components.getTeamColor(playerDetail.team.id),
                            shape = CircleShape
                        )
                )
                
                // Player name
                Text(
                    text = playerDetail.player.webName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                
                // Points
                Text(
                    text = "$points pts",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Goals and Assists
                if (goals > 0 || assists > 0) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (goals > 0) {
                            Text(
                                text = "⚽$goals",
                                fontSize = 9.sp
                            )
                        }
                        if (goals > 0 && assists > 0) {
                            Text(text = " ", fontSize = 9.sp)
                        }
                        if (assists > 0) {
                            Text(
                                text = "🅰️$assists",
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                
                // Opponent team
                playerDetail.opponentTeam?.let { opponent ->
                    val isHome = playerDetail.fixture?.teamH == playerDetail.team.id
                    Text(
                        text = "${if (isHome) "vs" else "@"} ${opponent.shortName}",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerCard(playerDetail: PlayerWithDetails) {
    val points = playerDetail.liveStats?.stats?.totalPoints ?: playerDetail.player.eventPoints
    val isCaptain = playerDetail.pick.isCaptain
    val isViceCaptain = playerDetail.pick.isViceCaptain
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCaptain) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF37003C), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "C",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (isViceCaptain) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color(0xFF888888), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "V",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Column {
                        Text(
                            text = playerDetail.player.webName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = playerDetail.team.shortName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Points
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (points > 5) Color(0xFF00FF87).copy(alpha = 0.3f) 
                               else if (points < 2) Color(0xFFFF4458).copy(alpha = 0.3f)
                               else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${points * playerDetail.pick.multiplier}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PlayerListRow(
    playerDetail: PlayerWithDetails,
    provisionalBonus: Map<Int, Int> = emptyMap(),
    onPlayerClick: ((PlayerWithDetails) -> Unit)? = null
) {
    val isCaptain = playerDetail.pick.isCaptain
    val isViceCaptain = playerDetail.pick.isViceCaptain

    var points = if (playerDetail.hasLivePoints && playerDetail.liveStats != null) {
        playerDetail.liveStats.stats.totalPoints
    } else {
        playerDetail.player.eventPoints
    }
    if (playerDetail.hasLivePoints && playerDetail.liveStats != null) {
        val currentBonus = playerDetail.liveStats.stats.bonus
        val provisionalBonusPoints = provisionalBonus[playerDetail.player.id] ?: 0
        if (currentBonus == 0 && provisionalBonusPoints > 0) {
            points += provisionalBonusPoints
        }
    }
    val displayPoints = points * playerDetail.pick.multiplier

    val isHome = playerDetail.fixture?.teamH == playerDetail.team.id
    val difficulty = playerDetail.fixture?.let {
        if (isHome) it.teamHDifficulty else it.teamADifficulty
    } ?: 3

    // Difficulty colours matched to FPL official palette
    val diffColor = when (difficulty) {
        1 -> Color(0xFF257D5A)  // dark green
        2 -> Color(0xFF00FF87)  // bright green
        3 -> Color(0xFFE7E7E7)  // light grey/white
        4 -> Color(0xFFFF4455)  // red/pink
        5 -> Color(0xFF80072D)  // dark maroon
        else -> Color.Gray
    }

    // Status label
    val fixture = playerDetail.fixture
    val statusText: String
    val statusColor: Color
    when {
        fixture == null -> {
            statusText = "No fix"
            statusColor = Color.Gray
        }
        fixture.finished -> {
            statusText = "Done"
            statusColor = Color(0xFF9E9E9E)
        }
        fixture.started == true -> {
            statusText = "${fixture.minutes}'"
            statusColor = Color(0xFF00E676)
        }
        fixture.kickoffTime != null -> {
            // Parse kickoff time to show e.g. "Sat 15:00"
            val kTime = try {
                val instant = java.time.Instant.parse(fixture.kickoffTime)
                val zdt = instant.atZone(java.time.ZoneId.systemDefault())
                val dayAbbrev = zdt.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
                val time = "%02d:%02d".format(zdt.hour, zdt.minute)
                "$dayAbbrev $time"
            } catch (e: Exception) {
                fixture.kickoffTime.take(10)
            }
            statusText = kTime
            statusColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
        else -> {
            statusText = "TBC"
            statusColor = Color.Gray
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onPlayerClick != null) Modifier.clickable { onPlayerClick(playerDetail) } else Modifier),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Player name + C/V badge ──────────────────────────────────
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = playerDetail.player.webName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isCaptain || isViceCaptain) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .background(
                                if (isCaptain) Color(0xFF37003C) else Color(0xFF888888),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCaptain) "C" else "V",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (playerDetail.isLive) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "🔴", fontSize = 10.sp)
                }
            }

            // ── Opponent chip: "ARS H" with diff colour ──────────────────
            playerDetail.opponentTeam?.let { opponent ->
                val oppShort = opponent.shortName.take(3).uppercase()
                val venueLabel = if (isHome) "H" else "A"
                val textColor = if (difficulty == 3) Color(0xFF333333) else Color.White
                Box(
                    modifier = Modifier
                        .background(diffColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$oppShort $venueLabel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // ── Status ───────────────────────────────────────────────────
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = statusColor,
                fontWeight = if (fixture?.started == true && fixture.finished.not()) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.width(56.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(8.dp))

            // ── Points badge ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when {
                            displayPoints >= 10 -> Color(0xFF00FF87).copy(alpha = 0.22f)
                            displayPoints >= 6  -> Color(0xFF4CAF50).copy(alpha = 0.18f)
                            displayPoints <= 2  -> Color(0xFFFF4458).copy(alpha = 0.18f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$displayPoints",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

