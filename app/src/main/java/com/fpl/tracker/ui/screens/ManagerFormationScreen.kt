package com.fpl.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Leaderboard
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import android.util.Log
import com.fpl.tracker.data.repository.FPLRepository
import com.fpl.tracker.ui.components.FootballPitch
import com.fpl.tracker.ui.components.PlayerDetailDialog
import com.fpl.tracker.ui.components.getTeamColor
import com.fpl.tracker.ui.components.getTeamTextColor
import com.fpl.tracker.ui.theme.*

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
    var showTransfersDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val repository = remember { FPLRepository() }
    val formationTitle = teamName.takeIf { it.isNotBlank() } ?: "Team Formation"

    LaunchedEffect(managerId, eventId) {
        viewModel.loadManagerFormation(managerId, eventId)
        val transfersDeferred = async { repository.getManagerTransfers(managerId).getOrNull() }
        gwTransfers = transfersDeferred.await()
            ?.filter { it.event == eventId }
            ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = formationTitle,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = OnSurfaceE5E2E1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = PrimaryA1D494
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface131313
                )
            )
        },
        containerColor = Surface131313
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryA1D494
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
                            color = ErrorFFB4AB
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadManagerFormation(managerId, eventId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryContainer2D5A27,
                                contentColor = PrimaryA1D494
                            )
                        ) {
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

                    val startingXI = playersWithDetails.filter { it.pick.position <= 11 }
                    val bench = playersWithDetails.filter { it.pick.position > 11 }

                    var livePoints = 0
                    startingXI.forEach { pd ->
                        if (pd.isLive && pd.liveStats != null) {
                            var pts = pd.liveStats.stats.totalPoints
                            val currentBonus = pd.liveStats.stats.bonus
                            val provisionalBonusPoints = uiState.provisionalBonus[pd.player.id] ?: 0
                            if (currentBonus == 0 && provisionalBonusPoints > 0) {
                                pts += provisionalBonusPoints
                            }
                            livePoints += pts * pd.pick.multiplier
                        }
                    }

                    val totalGwPoints = managerPicks.entryHistory.points + livePoints
                    val hasLive = startingXI.any { it.isLive }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // ── Dashboard Header ────────────────────────────────
                        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "GAMEWEEK $eventId",
                                        color = PrimaryA1D494,
                                        fontSize = 12.sp,
                                        fontFamily = Manrope,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 3.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formationTitle,
                                        color = OnSurfaceE5E2E1,
                                        fontSize = 36.sp,
                                        fontFamily = SpaceGrotesk,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (hasLive) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                TertiaryContainerA40217,
                                                RoundedCornerShape(50)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(Color.White, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "LIVE",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontFamily = SpaceGrotesk,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Stats Grid ──────────────────────────────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StitchStatCard(
                                label = "GW POINTS",
                                value = "$totalGwPoints",
                                valueColor = PrimaryA1D494,
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.SportsSoccer,
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp),
                                        tint = OnSurfaceE5E2E1.copy(alpha = 0.08f)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StitchStatCard(
                                label = "TOTAL SCORE",
                                value = "${managerPicks.entryHistory.totalPoints + livePoints}",
                                valueColor = SecondaryFFE083,
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Leaderboard,
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp),
                                        tint = OnSurfaceE5E2E1.copy(alpha = 0.08f)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // ── Pill View Toggle ────────────────────────────────
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .background(
                                    SurfaceContainerHighest353535,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(4.dp)
                                .width(200.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isPitchView) PrimaryA1D494 else Color.Transparent
                                    )
                                    .clickable { isPitchView = true }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Pitch",
                                    color = if (isPitchView) OnPrimary0A3909 else Outline8C9387,
                                    fontSize = 14.sp,
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (!isPitchView) PrimaryA1D494 else Color.Transparent
                                    )
                                    .clickable { isPitchView = false }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "List",
                                    color = if (!isPitchView) OnPrimary0A3909 else Outline8C9387,
                                    fontSize = 14.sp,
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // ── Player Click Handler ────────────────────────────
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

                        // ── Pitch or List View ──────────────────────────────
                        if (isPitchView) {
                            FootballPitch(
                                startingXI = startingXI,
                                provisionalBonus = uiState.provisionalBonus,
                                onPlayerClick = handlePlayerClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                        } else {
                            // List View Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "PLAYER / FIXTURE",
                                    color = Outline8C9387,
                                    fontSize = 10.sp,
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = "POINTS",
                                    color = Outline8C9387,
                                    fontSize = 10.sp,
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                startingXI.forEach { playerWithDetails ->
                                    StitchPlayerListRow(
                                        playerDetail = playerWithDetails,
                                        provisionalBonus = uiState.provisionalBonus,
                                        onPlayerClick = handlePlayerClick
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ── Substitutes Bench ───────────────────────────────
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SUBSTITUTES BENCH",
                                color = Outline8C9387,
                                fontSize = 12.sp,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .background(
                                    SurfaceContainerLow1C1B1B,
                                    RoundedCornerShape(16.dp)
                                )
                                .border(
                                    1.dp,
                                    OutlineVariant42493E.copy(alpha = 0.1f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            bench.forEach { benchPlayer ->
                                StitchBenchCard(
                                    playerDetail = benchPlayer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Chips Card ──────────────────────────────────────
                        ManagerChipsCard(
                            usedChips = usedChips,
                            activeChip = managerPicks.activeChip,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── GW Transfers Clickable Summary ──────────────────
                        if (gwTransfers.isNotEmpty() || managerPicks.entryHistory.eventTransfers > 0) {
                            val transferCost = managerPicks.entryHistory.eventTransfersCost
                            val transferCount = managerPicks.entryHistory.eventTransfers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .background(
                                        SurfaceContainerHigh2A2A2A,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable(enabled = gwTransfers.isNotEmpty()) {
                                        showTransfersDialog = true
                                    }
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                PrimaryA1D494.copy(alpha = 0.1f),
                                                RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "⇄",
                                            color = PrimaryA1D494,
                                            fontSize = 22.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "GW Transfers",
                                            color = OnSurfaceE5E2E1,
                                            fontSize = 16.sp,
                                            fontFamily = SpaceGrotesk,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$transferCount used" +
                                                    if (transferCost > 0) ", -${transferCost} pts hit" else ", Free",
                                            color = Outline8C9387,
                                            fontSize = 13.sp,
                                            fontFamily = Manrope
                                        )
                                    }
                                }
                                if (gwTransfers.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                PrimaryContainer2D5A27,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "›",
                                            color = PrimaryA1D494,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }

            // ── Loading overlay ─────────────────────────────────────
            if (isLoadingPlayerData && !showPlayerDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Surface131313.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceContainerHigh2A2A2A
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = PrimaryA1D494)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading player data...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = OnSurfaceE5E2E1
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                selectedPlayer?.player?.webName ?: "",
                                fontSize = 14.sp,
                                color = OnSurfaceVariantC2C9BB
                            )
                        }
                    }
                }
            }

            // ── Player Detail Dialog ────────────────────────────────
            if (showPlayerDialog && selectedPlayer != null) {
                PlayerDetailDialog(
                    player = selectedPlayer!!.player,
                    team = selectedPlayer!!.team,
                    playerDetail = playerDetail,
                    leagueStats = leagueStats,
                    bootstrapData = uiState.bootstrapData,
                    currentEvent = eventId,
                    liveStats = selectedPlayer!!.liveStats,
                    fixture = selectedPlayer!!.fixture,
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

            // ── Transfers Detail Dialog ─────────────────────────────
            if (showTransfersDialog && gwTransfers.isNotEmpty() && uiState.bootstrapData != null) {
                TransfersDetailDialog(
                    eventId = eventId,
                    transfers = gwTransfers,
                    transferCost = uiState.managerPicks?.entryHistory?.eventTransfersCost ?: 0,
                    activeChip = uiState.managerPicks?.activeChip,
                    bootstrapData = uiState.bootstrapData!!,
                    onDismiss = { showTransfersDialog = false }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Stitch Stat Card
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StitchStatCard(
    label: String,
    value: String,
    valueColor: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainerLow1C1B1B)
            .border(
                1.dp,
                OutlineVariant42493E.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-8).dp)
        ) {
            icon()
        }
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = label,
                color = Outline8C9387,
                fontSize = 10.sp,
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 42.sp,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Black
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Stitch Player List Row
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun StitchPlayerListRow(
    playerDetail: PlayerWithDetails,
    provisionalBonus: Map<Int, Int> = emptyMap(),
    onPlayerClick: ((PlayerWithDetails) -> Unit)? = null
) {
    val isCaptain = playerDetail.pick.isCaptain
    val isViceCaptain = playerDetail.pick.isViceCaptain
    val isSub = playerDetail.pick.position > 11

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

    val fixture = playerDetail.fixture
    val statusText: String
    when {
        fixture == null -> statusText = "No fix"
        fixture.finished -> statusText = "FT"
        fixture.started == true -> statusText = "LIVE ${fixture.minutes}'"
        fixture.kickoffTime != null -> {
            statusText = try {
                val instant = java.time.Instant.parse(fixture.kickoffTime)
                val zdt = instant.atZone(java.time.ZoneId.systemDefault())
                val dayAbbrev = zdt.dayOfWeek.getDisplayName(
                    java.time.format.TextStyle.SHORT,
                    java.util.Locale.getDefault()
                ).uppercase()
                val time = "%02d:%02d".format(zdt.hour, zdt.minute)
                "$dayAbbrev $time"
            } catch (_: Exception) {
                fixture.kickoffTime.take(10)
            }
        }
        else -> statusText = "TBC"
    }

    val positionLabel = when (playerDetail.player.elementType) {
        1 -> "GK"
        2 -> "DEF"
        3 -> "MID"
        4 -> "FWD"
        else -> "SUB"
    }

    val accentColor = when {
        isSub -> OutlineVariant42493E
        isCaptain -> SecondaryFFE083
        displayPoints >= 10 -> PrimaryA1D494
        else -> PrimaryContainer2D5A27
    }

    val chipLabel = if (isCaptain) "$positionLabel (C)"
    else if (isViceCaptain) "$positionLabel (V)"
    else if (isSub) "SUB"
    else positionLabel

    val chipBg = when {
        isCaptain -> SecondaryFFE083
        isSub -> OutlineVariant42493E
        else -> SurfaceVariant353535
    }
    val chipFg = when {
        isCaptain -> OnSecondary3C2F00
        isSub -> Surface131313
        else -> OnSurfaceVariantC2C9BB
    }

    val useGradient = displayPoints >= 6 && !isSub
    val pointsCircleBg = if (useGradient) {
        Brush.linearGradient(listOf(PrimaryA1D494, PrimaryContainer2D5A27))
    } else {
        Brush.linearGradient(listOf(SurfaceContainerHighest353535, SurfaceContainerHighest353535))
    }
    val pointsFg = if (useGradient) OnPrimary0A3909 else if (isSub) Outline8C9387 else OnSurfaceE5E2E1

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLow1C1B1B, RoundedCornerShape(12.dp))
            .then(
                if (onPlayerClick != null) Modifier.clickable { onPlayerClick(playerDetail) } else Modifier
            )
            .padding(start = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(72.dp)
                .background(accentColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Team color square
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    getTeamColor(playerDetail.team.id),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = playerDetail.team.shortName.take(3).uppercase(),
                color = getTeamTextColor(playerDetail.team.id),
                fontSize = 10.sp,
                fontFamily = Manrope,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = playerDetail.player.webName,
                    color = OnSurfaceE5E2E1,
                    fontSize = 16.sp,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(chipBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = chipLabel,
                        color = chipFg,
                        fontSize = 10.sp,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                playerDetail.opponentTeam?.let { opp ->
                    val venue = if (isHome) "vs" else "@"
                    Text(
                        text = "${playerDetail.team.shortName} $venue ${opp.shortName}",
                        color = Outline8C9387,
                        fontSize = 12.sp,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (playerDetail.opponentTeam != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (playerDetail.isLive) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(TertiaryFFB3AD, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = statusText,
                    color = if (playerDetail.isLive) TertiaryFFB3AD else Outline8C9387,
                    fontSize = 10.sp,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .background(pointsCircleBg, CircleShape)
                .then(
                    if (!useGradient && !isSub)
                        Modifier.border(1.dp, OutlineVariant42493E, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$displayPoints",
                color = pointsFg,
                fontSize = 18.sp,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Stitch Bench Card
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun StitchBenchCard(
    playerDetail: PlayerWithDetails,
    modifier: Modifier = Modifier
) {
    val points = playerDetail.liveStats?.stats?.totalPoints ?: playerDetail.player.eventPoints

    Column(
        modifier = modifier
            .background(SurfaceContainer20201F, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = playerDetail.team.shortName.take(3).uppercase(),
            color = Outline8C9387,
            fontSize = 8.sp,
            fontFamily = Manrope,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = playerDetail.player.webName,
            color = OnSurfaceE5E2E1,
            fontSize = 10.sp,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        val pillBg = when {
            points >= 6 -> PrimaryContainer2D5A27
            else -> SurfaceContainerHighest353535
        }
        val pillFg = when {
            points >= 6 -> PrimaryA1D494
            else -> OnSurfaceE5E2E1
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(pillBg, RoundedCornerShape(4.dp))
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (points > 0) "$points pts" else "-",
                color = pillFg,
                fontSize = 9.sp,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Transfers Detail Dialog
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun TransfersDetailDialog(
    eventId: Int,
    transfers: List<com.fpl.tracker.data.models.ManagerTransfer>,
    transferCost: Int,
    activeChip: String?,
    bootstrapData: com.fpl.tracker.data.models.BootstrapData,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerLow1C1B1B)
                    .border(
                        1.dp,
                        OutlineVariant42493E.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {}
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceContainerHigh2A2A2A)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⇄", color = SecondaryFFE083, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GW$eventId TRANSFERS",
                            color = OnSurfaceE5E2E1,
                            fontSize = 18.sp,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(SurfaceContainerHighest353535, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = OnSurfaceVariantC2C9BB,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    val isWildcardActive = activeChip == "wildcard"
                    val isFreeHitActive = activeChip == "freehit"
                    val transferModeLabel = when {
                        isFreeHitActive -> "FREE HIT ACTIVE"
                        isWildcardActive -> "ACTIVE WILDCARD"
                        else -> "TRANSFER STATUS"
                    }
                    val transferModeValue = when {
                        isFreeHitActive -> "UNLIMITED"
                        isWildcardActive -> "ACTIVE"
                        else -> "STANDARD"
                    }
                    val costLabel = if (isFreeHitActive) "TRANSFER LIMIT" else "COST"
                    val costValue = when {
                        isFreeHitActive -> "UNLIMITED"
                        transferCost > 0 -> "-$transferCost PTS"
                        else -> "FREE"
                    }
                    val costColor = when {
                        isFreeHitActive -> PrimaryA1D494
                        transferCost > 0 -> TertiaryFFB3AD
                        else -> PrimaryA1D494
                    }

                    // Summary bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceContainer20201F, RoundedCornerShape(8.dp))
                            .drawLeftBorder(SecondaryFFE083, 4.dp)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = transferModeLabel,
                                color = OnSurfaceVariantC2C9BB,
                                fontSize = 10.sp,
                                fontFamily = Manrope,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = transferModeValue,
                                color = OnSurfaceE5E2E1,
                                fontSize = 14.sp,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = costLabel,
                                color = OnSurfaceVariantC2C9BB,
                                fontSize = 10.sp,
                                fontFamily = Manrope,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = costValue,
                                color = costColor,
                                fontSize = 14.sp,
                                fontFamily = SpaceGrotesk,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Transfer rows
                    transfers.forEachIndexed { index, transfer ->
                        val playerIn = bootstrapData.elements.find { it.id == transfer.elementIn }
                        val playerOut = bootstrapData.elements.find { it.id == transfer.elementOut }
                        val teamIn = playerIn?.let { p -> bootstrapData.teams.find { it.id == p.team } }
                        val teamOut = playerOut?.let { p -> bootstrapData.teams.find { it.id == p.team } }

                        val positionLabel = { type: Int ->
                            when (type) {
                                1 -> "GK"
                                2 -> "DEF"
                                3 -> "MID"
                                4 -> "FWD"
                                else -> ""
                            }
                        }

                        Column {
                            // Number divider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "%02d".format(index + 1),
                                    color = OnSurfaceVariantC2C9BB.copy(alpha = 0.4f),
                                    fontSize = 12.sp,
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(OutlineVariant42493E.copy(alpha = 0.2f))
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // OUT player
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color(0xFF0E0E0E),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .drawLeftBorder(TertiaryFFB3AD, 2.dp)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                SurfaceContainerHighest353535,
                                                RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "OUT",
                                            color = TertiaryFFB3AD,
                                            fontSize = 9.sp,
                                            fontFamily = SpaceGrotesk,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = (playerOut?.webName ?: "Unknown").uppercase(),
                                            color = OnSurfaceE5E2E1,
                                            fontSize = 14.sp,
                                            fontFamily = SpaceGrotesk,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${teamOut?.shortName ?: "?"} \u2022 ${positionLabel(playerOut?.elementType ?: 0)}",
                                            color = OnSurfaceVariantC2C9BB,
                                            fontSize = 10.sp,
                                            fontFamily = Manrope
                                        )
                                    }
                                }
                                Text(
                                    text = "£${transfer.elementOutCost / 10.0}m",
                                    color = OnSurfaceVariantC2C9BB,
                                    fontSize = 14.sp,
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Arrow connector
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(SurfaceContainerHigh2A2A2A, CircleShape)
                                        .border(
                                            1.dp,
                                            OutlineVariant42493E.copy(alpha = 0.3f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "↓",
                                        color = SecondaryFFE083,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // IN player
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color(0xFF0E0E0E),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .drawLeftBorder(PrimaryA1D494, 2.dp)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                PrimaryContainer2D5A27.copy(alpha = 0.3f),
                                                RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "IN",
                                            color = PrimaryA1D494,
                                            fontSize = 9.sp,
                                            fontFamily = SpaceGrotesk,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = (playerIn?.webName ?: "Unknown").uppercase(),
                                            color = OnSurfaceE5E2E1,
                                            fontSize = 14.sp,
                                            fontFamily = SpaceGrotesk,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${teamIn?.shortName ?: "?"} \u2022 ${positionLabel(playerIn?.elementType ?: 0)}",
                                            color = OnSurfaceVariantC2C9BB,
                                            fontSize = 10.sp,
                                            fontFamily = Manrope
                                        )
                                    }
                                }
                                Text(
                                    text = "£${transfer.elementInCost / 10.0}m",
                                    color = OnSurfaceVariantC2C9BB,
                                    fontSize = 14.sp,
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.drawLeftBorder(color: Color, width: androidx.compose.ui.unit.Dp): Modifier =
    this.then(
        Modifier.drawWithContent {
            drawContent()
            drawRect(
                color = color,
                topLeft = Offset.Zero,
                size = Size(width.toPx(), size.height)
            )
        }
    )

// ═══════════════════════════════════════════════════════════════════════════════
// Chips Card (preserved logic, Stitch styling)
// ═══════════════════════════════════════════════════════════════════════════════

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
    activeChip: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow1C1B1B),
        shape = RoundedCornerShape(16.dp)
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
                        color = OnSurfaceE5E2E1,
                        fontSize = 16.sp,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (usedChips.isEmpty()) "No chips used yet"
                        else "${usedChips.size} chip${if (usedChips.size == 1) "" else "s"} this season",
                        color = Outline8C9387,
                        fontSize = 12.sp,
                        fontFamily = Manrope
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
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            if (usedChips.isEmpty()) {
                Text(
                    text = "This manager has not activated any chips so far.",
                    color = Outline8C9387,
                    fontSize = 12.sp,
                    fontFamily = Manrope,
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
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp
            )
        }
        Text(
            text = "GW${chip.event}",
            color = Outline8C9387,
            fontSize = 10.sp,
            fontFamily = Manrope,
            fontWeight = FontWeight.Medium
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Legacy composables kept for backward compatibility
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun StatItem(
    label: String,
    value: String,
    highlighted: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlighted) OnSurfaceVariantC2C9BB.copy(alpha = 0.85f)
            else OnSurfaceVariantC2C9BB.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) PrimaryA1D494 else OnSurfaceE5E2E1
        )
    }
}

@Composable
fun PlayerListRow(
    playerDetail: PlayerWithDetails,
    provisionalBonus: Map<Int, Int> = emptyMap(),
    onPlayerClick: ((PlayerWithDetails) -> Unit)? = null
) {
    StitchPlayerListRow(playerDetail, provisionalBonus, onPlayerClick)
}

@Composable
fun BenchPlayerCard(
    playerDetail: PlayerWithDetails,
    position: String,
    onPlayerClick: ((PlayerWithDetails) -> Unit)? = null
) {
    StitchBenchCard(playerDetail)
}

@Composable
fun PlayerCard(playerDetail: PlayerWithDetails) {
    StitchPlayerListRow(playerDetail)
}
