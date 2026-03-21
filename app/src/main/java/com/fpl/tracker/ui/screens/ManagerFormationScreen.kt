package com.fpl.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.util.Log
import com.fpl.tracker.data.repository.FPLRepository
import com.fpl.tracker.ui.components.FootballPitch
import com.fpl.tracker.ui.components.PlayerDetailDialog
import com.fpl.tracker.ui.theme.AuroraTeal
import com.fpl.tracker.ui.theme.CelestialPurple
import com.fpl.tracker.ui.theme.EmberRed
import com.fpl.tracker.ui.theme.FrostedLilac
import com.fpl.tracker.ui.theme.SolarGold
import com.fpl.tracker.viewmodel.ManagerFormationViewModel
import com.fpl.tracker.viewmodel.PlayerWithDetails
import com.fpl.tracker.viewmodel.UsedChip
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.fpl.tracker.data.preferences.PreferencesManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerFormationScreen(
    navController: NavController,
    managerId: Long,
    eventId: Int,
    teamName: String,
    viewModel: ManagerFormationViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedPlayer by remember { mutableStateOf<PlayerWithDetails?>(null) }
    var showPlayerDialog by remember { mutableStateOf(false) }
    var playerDetail by remember { mutableStateOf<com.fpl.tracker.data.models.PlayerDetailResponse?>(null) }
    var leagueStats by remember { mutableStateOf<com.fpl.tracker.data.models.LeaguePlayerStats?>(null) }
    var leagueStatsError by remember { mutableStateOf<String?>(null) }
    var gwTransfers by remember { mutableStateOf<List<com.fpl.tracker.data.models.ManagerTransfer>>(emptyList()) }
    var isLoadingPlayerData by remember { mutableStateOf(false) }
    var playerRequestId by remember { mutableIntStateOf(0) }
    var isPitchView by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val repository = remember { FPLRepository() }
    val formationTitle = teamName.takeIf { it.isNotBlank() } ?: "Team Formation"
    
    LaunchedEffect(managerId, eventId) {
        viewModel.loadManagerFormation(managerId, eventId)
        // Fetch transfers once for this screen.
        val transfersDeferred = async { repository.getManagerTransfers(managerId).getOrNull() }

        gwTransfers = transfersDeferred.await()
            ?.filter { it.event == eventId }
            ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = formationTitle,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = FrostedLilac
                        )
                        Text(
                            text = "GW$eventId",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FrostedLilac.copy(alpha = 0.8f)
                        )
                    }
                },
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
                        Button(onClick = { viewModel.loadManagerFormation(managerId, eventId) }) {
                            Text("Retry")
                        }
                    }
                }
                uiState.managerPicks != null -> {
                    val managerPicks = uiState.managerPicks!!
                    val playersWithDetails = uiState.playersWithDetails
                    val bootstrap = uiState.bootstrapData!!
                    val usedChips = remember(uiState.managerHistory?.chips) {
                        buildUsedChips(uiState.managerHistory?.chips)
                    }
                    
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
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            // Calculate live points - ONLY for truly live games (not yet finished by FPL)
                            // For historical GWs all fixtures are finished, so livePoints stays 0
                            // and entryHistory.points is already the correct final total.
                            var livePoints = 0
                            startingXI.forEach { playerDetail ->
                                // Only count if the game is truly in-progress (isLive), not for
                                // finished games (including historical GW lookups)
                                if (playerDetail.isLive && playerDetail.liveStats != null) {
                                    var pts = playerDetail.liveStats.stats.totalPoints

                                    // Add provisional bonus if player doesn't have bonus yet
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
                                        color = MaterialTheme.colorScheme.primary,
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
                        }

                        ManagerChipsCard(
                            usedChips = usedChips,
                            activeChip = managerPicks.activeChip
                        )

                        // Starting XI - Pitch View or List View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Starting XI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isPitchView) "Pitch" else "List",
                                    color = FrostedLilac,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = isPitchView,
                                    onCheckedChange = { isPitchView = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = FrostedLilac,
                                        checkedTrackColor = AuroraTeal.copy(alpha = 0.65f),
                                        uncheckedThumbColor = FrostedLilac.copy(alpha = 0.7f),
                                        uncheckedTrackColor = FrostedLilac.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }

                        // Shared click handler for player clicks
                        val handlePlayerClick: (PlayerWithDetails) -> Unit = { playerWithDetails ->
                            val requestId = playerRequestId + 1
                            playerRequestId = requestId
                            selectedPlayer = playerWithDetails
                            playerDetail = null
                            leagueStats = null
                            leagueStatsError = null
                            showPlayerDialog = true
                            isLoadingPlayerData = true
                            scope.launch {
                                try {
                                    val detailDeferred = async {
                                        repository.getPlayerDetail(playerWithDetails.player.id).getOrNull()
                                    }
                                    val statsDeferred = async {
                                        val savedLeagueId = prefsManager.getLeagueId()
                                        if (savedLeagueId != null) {
                                            repository.getLeaguePlayerStats(
                                                leagueId = savedLeagueId,
                                                gameweek = eventId,
                                                playerId = playerWithDetails.player.id
                                            )
                                        } else {
                                            Result.failure(Exception("No league selected for league-scoped stats"))
                                        }
                                    }

                                    val detail = detailDeferred.await()
                                    val statsResult = statsDeferred.await()

                                    if (playerRequestId == requestId &&
                                        selectedPlayer?.player?.id == playerWithDetails.player.id
                                    ) {
                                        playerDetail = detail
                                        leagueStats = statsResult.getOrNull()
                                        leagueStatsError = statsResult.exceptionOrNull()?.message
                                    }
                                } catch (e: Exception) {
                                    Log.e("PlayerDialog", "Error loading player data: ${e.message}")
                                    leagueStatsError = e.message
                                } finally {
                                    if (playerRequestId == requestId &&
                                        selectedPlayer?.player?.id == playerWithDetails.player.id
                                    ) {
                                        isLoadingPlayerData = false
                                    }
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

                        // GW Transfers Section
                        if (gwTransfers.isNotEmpty()) {
                            val transferCost = managerPicks.entryHistory.eventTransfersCost
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1A0A2E)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Header row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "GW Transfers",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = FrostedLilac
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        CelestialPurple.copy(alpha = 0.7f),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "${gwTransfers.size} transfer${if (gwTransfers.size > 1) "s" else ""}",
                                                    fontSize = 11.sp,
                                                    color = FrostedLilac,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (transferCost > 0) EmberRed else AuroraTeal,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = if (transferCost > 0) "-${transferCost}pts" else "Free",
                                                    fontSize = 11.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    gwTransfers.forEachIndexed { index, transfer ->
                                        val playerIn = bootstrap.elements.find { it.id == transfer.elementIn }
                                        val playerOut = bootstrap.elements.find { it.id == transfer.elementOut }
                                        val teamIn = playerIn?.let { p -> bootstrap.teams.find { it.id == p.team } }
                                        val teamOut = playerOut?.let { p -> bootstrap.teams.find { it.id == p.team } }

                                        if (index > 0) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                color = FrostedLilac.copy(alpha = 0.12f)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // OUT player
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(
                                                            color = if (teamOut != null) com.fpl.tracker.ui.components.getTeamColor(teamOut.id) else EmberRed.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(10.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = teamOut?.shortName ?: "?",
                                                        color = if (teamOut != null) com.fpl.tracker.ui.components.getTeamTextColor(teamOut.id) else Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(5.dp))
                                                Text(
                                                    text = playerOut?.webName ?: "Unknown",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = EmberRed,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "£${transfer.elementOutCost / 10.0}m",
                                                    fontSize = 10.sp,
                                                    color = FrostedLilac.copy(alpha = 0.55f)
                                                )
                                            }

                                            // Arrow
                                            Column(
                                                modifier = Modifier.padding(horizontal = 10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "→",
                                                    color = SolarGold,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // IN player
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(
                                                            color = if (teamIn != null) com.fpl.tracker.ui.components.getTeamColor(teamIn.id) else AuroraTeal.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(10.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = teamIn?.shortName ?: "?",
                                                        color = if (teamIn != null) com.fpl.tracker.ui.components.getTeamTextColor(teamIn.id) else Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(5.dp))
                                                Text(
                                                    text = playerIn?.webName ?: "Unknown",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AuroraTeal,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "£${transfer.elementInCost / 10.0}m",
                                                    fontSize = 10.sp,
                                                    color = FrostedLilac.copy(alpha = 0.55f)
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
            
            // Only block the full screen if the dialog has not opened yet.
            if (isLoadingPlayerData && !showPlayerDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Show player detail dialog only after data is loaded
            if (showPlayerDialog && selectedPlayer != null) {
                PlayerDetailDialog(
                    player = selectedPlayer!!.player,
                    team = selectedPlayer!!.team,
                    playerDetail = playerDetail,
                    leagueStats = leagueStats,
                    bootstrapData = uiState.bootstrapData,
                    currentEvent = eventId,
                    liveStats = selectedPlayer!!.liveStats,
                    isLoadingLeagueStats = isLoadingPlayerData,
                    leagueStatsError = leagueStatsError,
                    onDismiss = {
                        showPlayerDialog = false
                        selectedPlayer = null
                        playerDetail = null
                        leagueStats = null
                        leagueStatsError = null
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
            color = if (highlighted)
                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            else
                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun buildUsedChips(chips: List<com.fpl.tracker.data.models.ChipUsage>?): List<UsedChip> {
    if (chips.isNullOrEmpty()) return emptyList()
    val countByName = mutableMapOf<String, Int>()
    return chips.sortedBy { it.event }.map { chip ->
        val number = (countByName[chip.name] ?: 0) + 1
        countByName[chip.name] = number
        UsedChip(
            name = chip.name,
            event = chip.event,
            number = number
        )
    }
}

@Composable
private fun ManagerChipsCard(
    usedChips: List<UsedChip>,
    activeChip: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161626)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chips Played",
                        color = FrostedLilac,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (usedChips.isEmpty()) {
                            "No chips used yet"
                        } else {
                            "${usedChips.size} chip${if (usedChips.size == 1) "" else "s"} this season"
                        },
                        color = FrostedLilac.copy(alpha = 0.68f),
                        fontSize = 12.sp
                    )
                }
                activeChip?.let { chip ->
                    val label = chipLabel(chip, 1) ?: chip.uppercase()
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipColor(chip))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Active $label",
                            color = chipTextColor(chip),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            if (usedChips.isEmpty()) {
                Text(
                    text = "This manager has not activated any chips so far.",
                    color = Color(0xFF8C8CA1),
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    usedChips.forEach { chip ->
                        ManagerChipHistoryBadge(chip = chip)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerChipHistoryBadge(chip: UsedChip) {
    val label = chipLabel(chip.name, chip.number) ?: chip.name.uppercase()
    val color = chipColor(chip.name)
    val textColor = chipTextColor(chip.name)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp
            )
        }
        Text(
            text = "GW${chip.event}",
            color = Color(0xFF8C8CA1),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
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
                                .background(Color(0xFF00FF87), CircleShape),
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
                                .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
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
                        color = when {
                            points > 5 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            points < 2 -> EmberRed.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
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

    // Difficulty colours from the requested palette
    val diffColor = when (difficulty) {
        1 -> Color(0xFF257D5A)  // dark green
        2 -> Color(0xFF00FF87)  // bright green
        3 -> Color(0xFFE7E7E7)  // light grey/white
        4 -> Color(0xFFFF4455)  // red/pink
        5 -> Color(0xFF80072D)  // dark maroon
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Status label
    val fixture = playerDetail.fixture
    val statusText: String
    val statusColor: Color
    when {
        fixture == null -> {
            statusText = "No fix"
            statusColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        }
        fixture.finished -> {
            statusText = "Done"
            statusColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        }
        fixture.started == true -> {
            statusText = "${fixture.minutes}'"
            statusColor = AuroraTeal
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
            statusColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        }
        else -> {
            statusText = "TBC"
            statusColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
                                if (isCaptain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                val textColor = if (difficulty == 3) Color(0xFF111111) else Color.White
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
                fontWeight = if (fixture?.started == true && fixture.finished.not() && fixture.finishedProvisional.not()) FontWeight.Bold else FontWeight.Normal,
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
                            displayPoints >= 10 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            displayPoints >= 6  -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            displayPoints <= 2  -> EmberRed.copy(alpha = 0.2f)
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
