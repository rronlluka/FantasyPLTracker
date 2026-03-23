package com.fpl.tracker.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fpl.tracker.data.models.ManagerTransfer
import com.fpl.tracker.data.preferences.PreferencesManager
import com.fpl.tracker.navigation.Screen
import com.fpl.tracker.ui.theme.*
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
    var showAllGameweeks by remember { mutableStateOf(false) }

    LaunchedEffect(managerId) {
        viewModel.loadManagerData(managerId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBackground)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    color = StitchPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = StitchTertiary, modifier = Modifier.size(48.dp))
                    Text(
                        "Failed to load profile",
                        color = StitchOnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(uiState.error ?: "", color = StitchOutline, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Button(
                        onClick = { viewModel.loadManagerData(managerId) },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPrimary)
                    ) { Text("Retry", color = StitchOnPrimary) }
                }
            }

            uiState.managerData != null -> {
                val managerData = uiState.managerData!!
                val managerHistory = uiState.managerHistory
                val bootstrapData = uiState.bootstrapData
                val players = bootstrapData?.elements ?: emptyList()
                val teams = bootstrapData?.teams ?: emptyList()
                val currentEvent = bootstrapData?.events?.find { it.isCurrent }?.id ?: managerData.currentEvent
                val teamName = managerData.name.takeIf { it.isNotBlank() }
                    ?: "${managerData.playerFirstName} ${managerData.playerLastName}"

                // Squad value & bank from latest history entry
                val currentHistory = managerHistory?.current ?: emptyList()
                val latestGw = currentHistory.lastOrNull()
                val prevGw = if (currentHistory.size > 1) currentHistory[currentHistory.size - 2] else null
                
                val totalValueM = (latestGw?.value ?: 0) / 10f
                val bankM = (latestGw?.bank ?: 0) / 10f
                val squadValueM = totalValueM - bankM
                
                val prevTotalValueM = (prevGw?.value ?: 0) / 10f
                val valueChangeM = totalValueM - prevTotalValueM

                // Highest rank
                val bestGwEntry = currentHistory.minByOrNull { it.overallRank }
                val highestRank = bestGwEntry?.overallRank
                val highestRankGw = bestGwEntry?.event
                val highestPointsEntry = currentHistory.maxByOrNull { it.points }
                val highestPoints = highestPointsEntry?.points
                val highestPointsGw = highestPointsEntry?.event

                val teamValueLabel = "£${"%.1f".format(squadValueM)}m"
                val bankLabel = "£${"%.1f".format(bankM)}m"
                val bestRankLabel = highestRank?.let { "#${formatRankString(it)}" } ?: "—"
                val bestRankSubtitle = highestRankGw?.let { "GW $it" } ?: "UNUSED"
                val mostPointsLabel = highestPoints?.toString() ?: "—"
                val mostPointsSubtitle = highestPointsGw?.let { "GW $it" } ?: "UNUSED"

                // Transfers grouped by GW (most recent first)
                val allTransferGroups = uiState.transfers
                    .sortedByDescending { it.event }
                    .groupBy { it.event }
                    .entries
                    .sortedByDescending { it.key }
                val transfers = allTransferGroups.take(5)

                // GW history
                val allGwHistory = currentHistory.reversed()
                val gwHistory = if (showAllGameweeks) allGwHistory else allGwHistory.take(10)

                // Chips
                val chips = managerHistory?.chips ?: emptyList()
                val chipDefs = orderedChipDefinitions(chips)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {

                    // ── 1. Hero Header ────────────────────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StitchBackground)
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        ) {
                            // Region chip
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Flag,
                                    contentDescription = null,
                                    tint = StitchPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${managerData.playerRegionName} • Region".uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = StitchOutline
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            // Manager real name
                            Text(
                                text = "${managerData.playerFirstName} ${managerData.playerLastName}".uppercase(),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp,
                                color = StitchOnSurface,
                                lineHeight = 38.sp
                            )
                            // Team name + ID
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = teamName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = StitchPrimary
                                )
                                Text(
                                    " / ",
                                    fontSize = 14.sp,
                                    color = StitchOutline
                                )
                                Text(
                                    text = "ID: ${managerData.id}",
                                    fontSize = 12.sp,
                                    color = StitchOutline
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // 4 stat pills
                            val gwPoints = if (uiState.livePoints > 0) uiState.totalLivePoints else managerData.summaryEventPoints
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileStatPill(
                                    modifier = Modifier.weight(1f),
                                    label = "OVERALL PTS",
                                    value = "%,d".format(managerData.summaryOverallPoints),
                                    accentColor = StitchPrimary
                                )
                                ProfileStatPill(
                                    modifier = Modifier.weight(1f),
                                    label = "OVERALL RANK",
                                    value = formatRank(managerData.summaryOverallRank),
                                    accentColor = StitchSecondary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ProfileStatPill(
                                    modifier = Modifier.weight(1f),
                                    label = if (uiState.hasLiveFixtures) "LIVE PTS" else "GW PTS",
                                    value = gwPoints.toString(),
                                    accentColor = StitchPrimary,
                                    isLive = uiState.hasLiveFixtures
                                )
                                ProfileStatPill(
                                    modifier = Modifier.weight(1f),
                                    label = "GW RANK",
                                    value = managerData.summaryEventRank?.let { formatRank(it) } ?: "—",
                                    accentColor = StitchTertiary
                                )
                            }

                            // Live in-play / to-start sub-cards
                            if (uiState.hasLiveFixtures) {
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ProfileStatPill(modifier = Modifier.weight(1f), label = "In Play", value = uiState.inPlay.toString(), accentColor = StitchPrimary)
                                    ProfileStatPill(modifier = Modifier.weight(1f), label = "To Start", value = uiState.toStart.toString(), accentColor = StitchSecondary)
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ProfileStatPill(
                                        modifier = Modifier.weight(1f),
                                        label = "Team Value",
                                        value = teamValueLabel,
                                        accentColor = StitchPrimary
                                    )
                                    ProfileStatPill(
                                        modifier = Modifier.weight(1f),
                                        label = "In Bank",
                                        value = bankLabel,
                                        accentColor = StitchSecondary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ProfileStatPill(
                                        modifier = Modifier.weight(1f),
                                        label = "Best Rank",
                                        value = bestRankLabel,
                                        accentColor = StitchTertiary,
                                        subtitle = bestRankSubtitle,
                                        onClick = highestRankGw?.let {
                                            {
                                                navController.navigate(
                                                    Screen.ManagerFormation.createRoute(managerId, it, teamName)
                                                )
                                            }
                                        }
                                    )
                                    ProfileStatPill(
                                        modifier = Modifier.weight(1f),
                                        label = "Most Points",
                                        value = mostPointsLabel,
                                        accentColor = StitchPrimary,
                                        subtitle = mostPointsSubtitle,
                                        onClick = highestPointsGw?.let {
                                            {
                                                navController.navigate(
                                                    Screen.ManagerFormation.createRoute(managerId, it, teamName)
                                                )
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            // View Team Button
                            Button(
                                onClick = {
                                    navController.navigate(
                                        Screen.ManagerFormation.createRoute(managerId, currentEvent, teamName)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StitchPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Group, contentDescription = null, tint = StitchOnPrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("View Current Team", color = StitchOnPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // ── 2. Chips ─────────────────────────────────────────────
                    item {
                        ProfileSectionHeader("Chips", "/ Played & Unused")
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val rows = chipDefs.chunked(2)
                            rows.forEach { rowChips ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowChips.forEach { chipDef ->
                                        val usage = resolveChipUsage(chipDef, chips)

                                        ChipCard(
                                            modifier = Modifier.weight(1f),
                                            label = chipDef.label,
                                            gwLabel = usage?.let { "GW ${it.event}" } ?: "UNUSED",
                                            icon = chipDef.icon,
                                            iconTint = chipDef.color,
                                            dimmed = usage == null,
                                            isLocked = usage == null
                                        )
                                    }
                                    if (rowChips.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // ── 3. Transfers ─────────────────────────────────────────
                    if (transfers.isNotEmpty()) {
                        item {
                            ProfileSectionHeader("Transfers Activity", null)
                        }
                        
                        // Flatten and limit to last 10 transfers
                        val flattenedTransfers = transfers
                            .flatMap { entry -> 
                                entry.value.mapIndexed { index, transfer -> Triple(entry.key, transfer, index) } 
                            }
                            .take(10)

                        items(
                            items = flattenedTransfers,
                            key = { triple -> "${triple.first}_${triple.second.elementIn}_${triple.second.elementOut}" }
                        ) { triple: Triple<Int, com.fpl.tracker.data.models.ManagerTransfer, Int> ->
                            val (gw, transfer, index) = triple
                            val gwCost = managerHistory?.current?.find { it.event == gw }?.eventTransfersCost ?: 0
                            val transfersThisGw = transfers.firstOrNull { it.key == gw }?.value?.size ?: 0
                            val paidTransfersCount = (gwCost / 4).coerceAtMost(transfersThisGw)
                            val freeTransfersCount = (transfersThisGw - paidTransfersCount).coerceAtLeast(0)
                            val isFree = index < freeTransfersCount
                            
                            val playerOut = players.find { it.id == transfer.elementOut }
                            val playerIn  = players.find { it.id == transfer.elementIn }
                            val teamOut   = teams.find { it.id == playerOut?.team }
                            val teamIn    = teams.find { it.id == playerIn?.team }
                            
                            TransferActivityCard(
                                transfer = transfer,
                                gw = gw,
                                isFree = isFree,
                                playerIn = playerIn,
                                playerOut = playerOut,
                                teamIn = teamIn,
                                teamOut = teamOut
                            )
                        }
                        item {
                            Button(
                                onClick = {
                                    navController.navigate(
                                        Screen.TransferHistory.createRoute(managerId, teamName)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, StitchOutlineVariant.copy(alpha = 0.1f)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StitchSurfaceHighest.copy(alpha = 0.3f),
                                    contentColor = StitchOutline
                                )
                            ) {
                                Text(
                                    "VIEW FULL TRANSFER HISTORY", 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }


                    // ── 4. GW History ─────────────────────────────────────────
                    if (gwHistory.isNotEmpty()) {
                        item { ProfileSectionHeader("GW History", null) }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StitchSurfaceContainer)
                            ) {
                                // Header row
                                GwHistoryHeaderRow()
                                HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.3f))
                                gwHistory.forEach { gw ->
                                    GwHistoryRow(
                                        event = gw.event,
                                        points = gw.points,
                                        rank = gw.rank,
                                        totalPoints = gw.totalPoints,
                                        transfers = gw.eventTransfers,
                                        transferCost = gw.eventTransfersCost,
                                        teamName = teamName,
                                        onClick = {
                                            navController.navigate(
                                                Screen.ManagerFormation.createRoute(managerId, gw.event, teamName)
                                            )
                                        }
                                    )
                                    HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.15f))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showAllGameweeks = !showAllGameweeks }) {
                                    Text(
                                        if (showAllGameweeks) "SHOW LESS" else "SHOW ALL",
                                        color = StitchPrimary,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    // ── 5. Past Seasons ───────────────────────────────────────
                    val pastSeasons = managerHistory?.past
                    if (!pastSeasons.isNullOrEmpty()) {
                        item { ProfileSectionHeader("Previous Seasons", null) }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StitchSurfaceContainer)
                            ) {
                                pastSeasons.reversed().forEach { season ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(season.seasonName, color = StitchOnSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("%,d pts".format(season.totalPoints), color = StitchPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("#%,d".format(season.rank), color = StitchOutline, fontSize = 13.sp)
                                    }
                                    HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.15f))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }

                    // ── 6. Leagues ────────────────────────────────────────────
                    item {
                        ProfileSectionHeader("Leagues", "/ ${managerData.leagues.classic.size} Joined")
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(StitchSurfaceContainer)
                                .clickable { showLeagueDropdown = !showLeagueDropdown }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Classic Leagues",
                                    color = StitchOnSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Icon(
                                    if (showLeagueDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle",
                                    tint = StitchPrimary
                                )
                            }
                            if (showLeagueDropdown) {
                                Spacer(Modifier.height(12.dp))
                                managerData.leagues.classic.forEach { league ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(StitchSurfaceHigh)
                                            .clickable {
                                                prefsManager.saveLeagueId(league.id.toLong())
                                                navController.navigate(Screen.LeagueStandings.createRoute(league.id.toLong()))
                                            }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                league.name,
                                                color = StitchOnSurface,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            league.entryRank?.let {
                                                Text(
                                                    "Rank: #%,d".format(it),
                                                    color = StitchPrimary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = StitchPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferHistoryScreen(
    navController: NavController,
    managerId: Long,
    teamName: String,
    viewModel: ManagerStatsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(managerId) {
        viewModel.loadManagerData(managerId)
    }

    Scaffold(
        containerColor = StitchBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Transfer History",
                            color = StitchOnSurface,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            teamName,
                            color = StitchOutline,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = StitchPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StitchBackground,
                    titleContentColor = StitchOnSurface
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = StitchPrimary
                    )
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Text(
                        text = uiState.error ?: "",
                        color = StitchTertiary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            else -> {
                val historyData = uiState.managerHistory
                val transfersByGw = uiState.transfers
                    .groupBy { transfer: ManagerTransfer -> transfer.event }
                    .toSortedMap(compareByDescending { it })

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        ProfileSectionHeader("All Transfer History", "/ Every Gameweek")
                    }

                    if (transfersByGw.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(StitchSurfaceContainer)
                                    .padding(20.dp)
                            ) {
                                Text("No transfers recorded", color = StitchOutline)
                            }
                        }
                    } else {
                        items(
                            items = transfersByGw.entries.toList(),
                            key = { entry: Map.Entry<Int, List<ManagerTransfer>> -> entry.key }
                        ) { entry: Map.Entry<Int, List<ManagerTransfer>> ->
                            val gw = entry.key
                            val gwTransfers = entry.value
                            val gwCost = historyData?.current?.find { currentGw -> currentGw.event == gw }?.eventTransfersCost ?: 0
                            val paidTransfersCount = (gwCost / 4).coerceAtMost(gwTransfers.size)
                            val freeTransfersCount = (gwTransfers.size - paidTransfersCount).coerceAtLeast(0)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "GW $gw",
                                        color = StitchOnSurface,
                                        fontWeight = FontWeight.Black,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = if (gwCost > 0) "TOTAL -${gwCost} PTS" else "TOTAL FREE",
                                        color = if (gwCost > 0) StitchTertiary else StitchPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                                    color = StitchOutlineVariant.copy(alpha = 0.25f)
                                )

                                gwTransfers.forEachIndexed { index, transfer ->
                                    val isFree = index < freeTransfersCount
                                    val playerOut = uiState.bootstrapData?.elements?.find { it.id == transfer.elementOut }
                                    val playerIn = uiState.bootstrapData?.elements?.find { it.id == transfer.elementIn }
                                    val teamOut = uiState.bootstrapData?.teams?.find { it.id == playerOut?.team }
                                    val teamIn = uiState.bootstrapData?.teams?.find { it.id == playerIn?.team }

                                    TransferActivityCard(
                                        transfer = transfer,
                                        gw = gw,
                                        isFree = isFree,
                                        playerIn = playerIn,
                                        playerOut = playerOut,
                                        teamIn = teamIn,
                                        teamOut = teamOut
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

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun formatRank(rank: Int): String {
    return when {
        rank >= 1_000_000 -> "%.1fM".format(rank / 1_000_000f)
        rank >= 1_000     -> "%,.0fK".format(rank / 1_000f)
        else              -> "%,d".format(rank)
    }
}

private data class ChipDefinition(
    val identifier: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val instance: Int = 0 // for multiple uses like WC1/WC2
)

private fun allChipDefinitions(): List<ChipDefinition> = listOf(
    ChipDefinition("wildcard", "WC1", Icons.Default.Style, Color(0xFF60A5FA), 1),
    ChipDefinition("freehit", "FH1", Icons.Default.Bolt, Color(0xFFFB923C), 1),
    ChipDefinition("bboost", "BB1", Icons.Default.Groups, Color(0xFF4ADE80), 1),
    ChipDefinition("3xc", "TC1", Icons.Default.MilitaryTech, Color(0xFFFACC15), 1),
    ChipDefinition("wildcard", "WC2", Icons.Default.Style, Color(0xFF60A5FA), 2),
    ChipDefinition("freehit", "FH2", Icons.Default.Bolt, Color(0xFFFB923C), 2),
    ChipDefinition("bboost", "BB2", Icons.Default.Groups, Color(0xFF4ADE80), 2),
    ChipDefinition("3xc", "TC2", Icons.Default.MilitaryTech, Color(0xFFFACC15), 2),
)

private fun resolveChipUsage(
    chipDefinition: ChipDefinition,
    chips: List<com.fpl.tracker.data.models.ChipUsage>
): com.fpl.tracker.data.models.ChipUsage? {
    val usages = chips
        .filter { it.name == chipDefinition.identifier }
        .sortedBy { it.event }
    return if (chipDefinition.instance > 0) usages.getOrNull(chipDefinition.instance - 1) else usages.firstOrNull()
}

private fun orderedChipDefinitions(
    chips: List<com.fpl.tracker.data.models.ChipUsage>
): List<ChipDefinition> =
    allChipDefinitions().sortedWith(
        compareBy<ChipDefinition>(
            { resolveChipUsage(it, chips) == null },
            { resolveChipUsage(it, chips)?.event ?: Int.MAX_VALUE }
        ).thenBy { it.label }
    )

private fun formatRankString(rank: Int): String {
    return "%,d".format(rank)
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun ProfileSectionHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title.uppercase(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = StitchOnSurface,
            letterSpacing = (-0.5).sp,
            fontStyle = FontStyle.Italic
        )
        if (subtitle != null) {
            Text(
                " $subtitle",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = StitchPrimary.copy(alpha = 0.5f),
                letterSpacing = (-0.5).sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun ProfileStatPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color,
    subtitle: String? = null,
    isLive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(StitchSurfaceContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .height(if (subtitle != null) 100.dp else 84.dp)
    ) {
        // Neon Accent bar
        Box(
            modifier = Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(accentColor)
        )
        
        Column(
            modifier = Modifier
                .padding(start = 20.dp, end = 12.dp)
                .align(Alignment.CenterStart)
        ) {
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = StitchOutline.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    letterSpacing = (-1).sp
                )
                if (isLive) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOutline.copy(alpha = 0.5f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}


@Composable
private fun ChipCard(
    modifier: Modifier = Modifier,
    label: String,
    gwLabel: String,
    icon: ImageVector,
    iconTint: Color,
    dimmed: Boolean = false,
    isLocked: Boolean = false
) {
    val isUnused = gwLabel == "UNUSED" || isLocked
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isUnused) Color.Transparent else StitchSurfaceContainer)
            .then(
                if (isUnused) {
                    Modifier.drawBehind {
                        val stroke = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        drawRoundRect(
                            color = StitchOutline.copy(alpha = 0.3f),
                            style = stroke,
                            cornerRadius = CornerRadius(24.dp.toPx())
                        )
                    }
                } else {
                    Modifier.border(1.dp, StitchOutlineVariant.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                }
            )
            .height(150.dp)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isUnused) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(StitchSurfaceContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock, 
                        contentDescription = null, 
                        tint = StitchOutline.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    label.uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = StitchOutline.copy(alpha = 0.5f)
                )
                Text(
                    "UNUSED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOutline.copy(alpha = 0.3f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    label.uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = StitchOnSurface
                )
                Text(
                    gwLabel.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconTint,
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}



@Composable
private fun TransferActivityCard(
    modifier: Modifier = Modifier,
    transfer: ManagerTransfer,
    gw: Int,
    isFree: Boolean,
    playerIn: com.fpl.tracker.data.models.Player?,
    playerOut: com.fpl.tracker.data.models.Player?,
    teamIn: com.fpl.tracker.data.models.Team?,
    teamOut: com.fpl.tracker.data.models.Team?
) {
    val accentColor = if (isFree) StitchPrimary else StitchTertiary
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(StitchSurfaceContainer)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(8.dp)
                .fillMaxHeight()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp))
                .background(accentColor)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 20.dp, top = 20.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = StitchTertiary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = playerIn?.webName ?: "Unknown",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "IN",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = StitchOutline
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = playerOut?.webName ?: "Unknown",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Normal,
                            color = StitchOutline
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "OUT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = StitchOutline
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isFree) "FREE" else "-4 PTS",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isFree) StitchOnSurface else StitchTertiary,
                    fontStyle = FontStyle.Italic
                )
                Text(
                    text = "GW $gw",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOutline,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
private fun TransferGwGroup(
    gw: Int,
    gwTransfers: List<ManagerTransfer>,
    gwCost: Int,
    players: List<com.fpl.tracker.data.models.Player>,
    teams: List<com.fpl.tracker.data.models.Team>,
    activeChip: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(StitchSurfaceContainer)
            .border(1.dp, StitchOutlineVariant.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        if (activeChip != null) {
            Box(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(StitchPrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$activeChip ACTIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = StitchPrimary,
                    letterSpacing = 1.sp
                )
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. GW Badge
            Column(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(StitchSurfaceHigh),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("$gw", fontSize = 22.sp, fontWeight = FontWeight.Black, color = StitchOnSurface)
                Text("GW", fontSize = 10.sp, color = StitchOutline, fontWeight = FontWeight.Bold)
            }

            // 2. Main Content
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                gwTransfers.forEach { transfer ->
                    val playerOut = players.find { it.id == transfer.elementOut }
                    val playerIn  = players.find { it.id == transfer.elementIn }
                    val teamOut   = teams.find { it.id == playerOut?.team }
                    val teamIn    = teams.find { it.id == playerIn?.team }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        TransferPlayerSubRow(
                            modifier = Modifier.weight(1f),
                            dotColor = StitchTertiary,
                            team = teamOut?.shortName ?: "—",
                            name = playerOut?.webName ?: "Unknown",
                            price = "£${"%.1f".format(transfer.elementOutCost / 10f)}m"
                        )
                        TransferPlayerSubRow(
                            modifier = Modifier.weight(1f),
                            dotColor = StitchPrimary,
                            team = teamIn?.shortName ?: "—",
                            name = playerIn?.webName ?: "Unknown",
                            price = "£${"%.1f".format(transfer.elementInCost / 10f)}m"
                        )
                    }
                }

                Column {
                    Text("COST", fontSize = 9.sp, color = StitchOutline, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (gwCost > 0) "-${gwCost} pts" else "0 pts",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (gwCost > 0) StitchTertiary else StitchOnSurface,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            Icon(
                Icons.Default.KeyboardArrowRight, 
                contentDescription = null, 
                tint = StitchOutlineVariant, 
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Composable
private fun TransferPlayerSubRow(
    modifier: Modifier = Modifier,
    dotColor: Color,
    team: String,
    name: String,
    price: String
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Column {
            Text(team, fontSize = 9.sp, color = StitchOutline, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(price, fontSize = 11.sp, color = StitchOutline, fontStyle = FontStyle.Italic)
        }
    }
}


@Composable
private fun StatsDetailCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accentColor: Color,
    subtitle: String? = null,
    icon: ImageVector? = null,
    highlighted: Boolean = false,
    showProgressBar: Boolean = false,
    progress: Float = 0f
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (highlighted) accentColor.copy(alpha = 0.12f)
                else StitchSurfaceContainer
            )
            .border(1.dp, if (highlighted) accentColor.copy(alpha = 0.2f) else StitchOutlineVariant.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Column {
            Text(label, fontSize = 12.sp, letterSpacing = 2.sp, color = accentColor.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                }
                
                Text(
                    value, 
                    fontSize = 42.sp, 
                    fontWeight = FontWeight.Black, 
                    color = StitchOnSurface, 
                    lineHeight = 44.sp,
                    letterSpacing = (-1).sp
                )
                
                if (label == "SQUAD VALUE") {
                    Text("m", fontSize = 18.sp, color = StitchOutline, modifier = Modifier.padding(start = 4.dp, top = 16.dp))
                }
            }
            
            if (showProgressBar) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(StitchOutlineVariant.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }
            }
            
            if (subtitle != null) {
                Spacer(Modifier.height(12.dp))
                Text(subtitle.uppercase(), fontSize = 10.sp, color = StitchOutline, letterSpacing = 1.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
            }
        }
    }
}

@Composable
private fun GwHistoryHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text("GW", fontSize = 10.sp, color = StitchOutline, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
        Text("PTS", fontSize = 10.sp, color = StitchOutline, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("RANK", fontSize = 10.sp, color = StitchOutline, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("TOTAL", fontSize = 10.sp, color = StitchOutline, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
private fun GwHistoryRow(
    event: Int,
    points: Int,
    rank: Int?,
    totalPoints: Int,
    transfers: Int,
    transferCost: Int,
    teamName: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(StitchSurfaceHigh)
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("$event", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
        }
        Text(
            "$points pts",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = StitchPrimary,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start
        )
        Text(
            rank?.let { formatRank(it) } ?: "—",
            fontSize = 12.sp,
            color = StitchOnSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            Text(
                "%,d".format(totalPoints),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurface
            )
            if (transferCost != 0) {
                Text("${transferCost}pt hit", fontSize = 9.sp, color = StitchTertiary)
            } else if (transfers > 0) {
                Text("$transfers xfer", fontSize = 9.sp, color = StitchOutline)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Open GW $event team",
                tint = StitchPrimary
            )
            Text(
                teamName.take(10).uppercase(),
                fontSize = 8.sp,
                color = StitchOutline,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun StatsRow(
    label: String,
    value: String,
    valueColor: Color? = null,
    valueFontWeight: FontWeight = FontWeight.SemiBold
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = valueFontWeight,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}
