package com.fpl.tracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
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
import com.fpl.tracker.ui.theme.CelestialPurple
import com.fpl.tracker.ui.theme.FrostedLilac
import com.fpl.tracker.viewmodel.LeagueStandingsViewModel
import com.fpl.tracker.viewmodel.ManagerLiveData
import com.fpl.tracker.viewmodel.UsedChip

// ─── Chip colour palette ─────────────────────────────────────────────────────
private val ChipBenchBoost = Color(0xFF00C853)
private val ChipWildcard   = Color(0xFF2979FF)
private val ChipTripleCap  = Color(0xFFFFD600)
private val ChipFreeHit    = Color(0xFFFF6D00)

fun chipLabel(chip: String?, number: Int): String? {
    val n = number.coerceIn(1, 2)
    return when (chip) {
        "bboost"   -> "BB$n"
        "wildcard" -> "WC$n"
        "3xc"      -> "TC$n"
        "freehit"  -> "FH$n"
        else       -> null
    }
}

fun chipColor(chip: String?): Color = when (chip) {
    "bboost"   -> ChipBenchBoost
    "wildcard" -> ChipWildcard
    "3xc"      -> ChipTripleCap
    "freehit"  -> ChipFreeHit
    else       -> Color.Gray
}

fun chipTextColor(chip: String?): Color = if (chip == "3xc") Color.Black else Color.White

// ─── Screen ──────────────────────────────────────────────────────────────────

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

    var isFavorite by remember(leagueId) {
        mutableStateOf(prefsManager.getFavoriteLeagueId() == leagueId)
    }
    var showFavoriteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(leagueId) {
        // Only load if we haven't already loaded data for this league.
        // This prevents back-navigation from resetting a selected historical GW.
        if (uiState.leagueStandings == null) {
            viewModel.loadLeagueStandings(leagueId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.leagueStandings?.league?.name ?: "League",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Chip history toggle
                    IconButton(onClick = { viewModel.toggleChipHistory() }) {
                        Icon(
                            Icons.Filled.Style,
                            contentDescription = if (uiState.showChipHistory) "Back to Rankings" else "Chip History",
                            tint = if (uiState.showChipHistory) Color(0xFF00FF87) else Color.White
                        )
                    }
                    // Favourite toggle
                    IconButton(onClick = {
                        if (isFavorite) {
                            prefsManager.removeFavoriteLeague()
                            isFavorite = false
                        } else {
                            uiState.leagueStandings?.league?.let { league ->
                                prefsManager.saveFavoriteLeague(leagueId, league.name)
                                isFavorite = true
                            }
                        }
                        showFavoriteDialog = true
                    }) {
                        Icon(
                            if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Favourite",
                            tint = if (isFavorite) Color(0xFFFFD700) else Color.White
                        )
                    }
                    // Refresh
                    IconButton(onClick = {
                        viewModel.loadLeagueStandings(leagueId, uiState.selectedGameweek)
                    }) {
                        Icon(Icons.Filled.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CelestialPurple,
                    titleContentColor = FrostedLilac,
                    navigationIconContentColor = FrostedLilac,
                    actionIconContentColor = FrostedLilac
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF121212))
        ) {
            when {
                uiState.isLoading -> LoadingState()

                uiState.error != null -> ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadLeagueStandings(leagueId) }
                )

                uiState.leagueStandings != null -> {
                    // liveRankings is used for BOTH live re-ranking AND historical corrected standings
                    val isHistorical = uiState.selectedGameweek != null
                    val standings = if (uiState.liveRankings.isNotEmpty())
                        uiState.liveRankings
                    else
                        uiState.leagueStandings!!.standings.results

                    if (uiState.showChipHistory) {
                        ChipHistoryView(
                            navController   = navController,
                            standings       = standings,
                            managerLiveData = uiState.managerLiveData,
                            currentEvent    = uiState.currentEvent,
                            userEntryId     = prefsManager.getManagerId()?.toInt(),
                            leagueId        = leagueId,
                            prefsManager    = prefsManager
                        )
                    } else {
                        NormalRankingsView(
                            standings          = standings,
                            managerLiveData    = uiState.managerLiveData,
                            hasLiveFixtures    = uiState.hasLiveFixtures,
                            leagueName         = uiState.leagueStandings!!.league.name,
                            currentEvent       = uiState.currentEvent,
                            selectedGameweek   = uiState.selectedGameweek,
                            availableGameweeks = uiState.availableGameweeks,
                            userEntryId        = prefsManager.getManagerId()?.toInt(),
                            onGameweekSelected = { gw -> viewModel.selectGameweek(gw) },
                            onClearGameweek    = { viewModel.clearGameweekSelection() },
                            onRowClick         = { standing ->
                                val gwForNav = uiState.selectedGameweek ?: uiState.currentEvent
                                // Persist the league ID so ManagerFormationScreen can fetch
                                // league-wide player stats (Starts/Bench tabs) correctly.
                                prefsManager.saveLeagueId(leagueId)
                                navController.navigate(
                                    Screen.ManagerFormation.createRoute(
                                        standing.entry.toLong(),
                                        gwForNav,
                                        standing.entryName.ifBlank { standing.playerName }
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        if (showFavoriteDialog) {
            AlertDialog(
                onDismissRequest = { showFavoriteDialog = false },
                title = { Text(if (isFavorite) "Added to Favourites" else "Removed from Favourites") },
                text = {
                    Text(
                        if (isFavorite)
                            "This league will load automatically when you open the app."
                        else
                            "Removed from favourites."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showFavoriteDialog = false }) { Text("OK") }
                }
            )
        }
    }
}

// ─── Loading / Error ─────────────────────────────────────────────────────────

@Composable
private fun BoxScope.LoadingState() {
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Color(0xFF00FF87))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Loading standings…", color = Color(0xFF9E9E9E), fontSize = 14.sp)
    }
}

@Composable
private fun BoxScope.ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.align(Alignment.Center).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Warning, null, tint = Color(0xFFFF5555), modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Failed to load standings", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(message, color = Color(0xFF9E9E9E), fontSize = 13.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87))) {
            Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Gameweek selector ────────────────────────────────────────────────────────

@Composable
private fun GameweekSelector(
    currentEvent: Int,
    selectedGameweek: Int?,
    availableGameweeks: List<Int>,
    onGameweekSelected: (Int) -> Unit,
    onClearGameweek: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayGw = selectedGameweek ?: currentEvent
    val isHistorical = selectedGameweek != null && selectedGameweek != currentEvent

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF181818))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Standings after:",
                color = Color(0xFF9E9E9E),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )

            // Dropdown button
            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isHistorical) Color(0xFF00FF87) else Color(0xFF444444)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    if (isHistorical) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFF00FF87))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        "GW $displayGw",
                        fontWeight = if (isHistorical) FontWeight.Bold else FontWeight.Normal,
                        color = if (isHistorical) Color(0xFF00FF87) else Color.White,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .background(Color(0xFF1E1E1E))
                ) {
                    // "Live / Current" option at the top
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = Color(0xFF00FF87),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "GW $currentEvent (Live)",
                                    color = Color(0xFF00FF87),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        },
                        onClick = { onClearGameweek(); expanded = false },
                        modifier = Modifier.background(
                            if (selectedGameweek == null) Color(0xFF2A2A2A) else Color.Transparent
                        )
                    )

                    HorizontalDivider(color = Color(0xFF333333), thickness = 0.5.dp)

                    // All past GWs newest first
                    availableGameweeks.reversed().forEach { gw ->
                        if (gw == currentEvent) return@forEach  // Already shown above
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "GW $gw",
                                    color = if (gw == selectedGameweek) Color(0xFF90CAF9) else Color.White,
                                    fontWeight = if (gw == selectedGameweek) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            onClick = { onGameweekSelected(gw); expanded = false },
                            modifier = Modifier.background(
                                if (gw == selectedGameweek) Color(0xFF1A2744) else Color.Transparent
                            )
                        )
                    }
                }
            }
        }

        // Historical banner shown when a past GW is selected
        if (isHistorical) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A2744))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = Color(0xFF90CAF9),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Historical view — standings as of end of GW $selectedGameweek",
                    color = Color(0xFF90CAF9),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onClearGameweek,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Live", color = Color(0xFF00FF87), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2D2D2D))
    }
}

// ─── Normal rankings view ────────────────────────────────────────────────────

@Composable
private fun NormalRankingsView(
    standings: List<StandingEntry>,
    managerLiveData: Map<Int, ManagerLiveData>,
    hasLiveFixtures: Boolean,
    leagueName: String,
    currentEvent: Int,
    selectedGameweek: Int?,
    availableGameweeks: List<Int>,
    userEntryId: Int?,
    onGameweekSelected: (Int) -> Unit,
    onClearGameweek: () -> Unit,
    onRowClick: (StandingEntry) -> Unit
) {
    // selectedGameweek is only non-null for historical GWs (VM sets it null for current GW)
    val isHistorical = selectedGameweek != null
    val displayGw    = selectedGameweek ?: currentEvent

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // Live banner (only when truly live + not in historical mode)
        if (hasLiveFixtures && !isHistorical) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFFB71C1C), Color(0xFFE53935))))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "LIVE  ·  Rankings updating in real-time",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // League info
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E1E))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(leagueName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (isHistorical)
                        "After GW $displayGw  ·  ${standings.size} managers"
                    else
                        "GW $currentEvent (Live)  ·  ${standings.size} managers",
                    color = if (isHistorical) Color(0xFF90CAF9) else Color(0xFF9E9E9E),
                    fontSize = 12.sp,
                    fontWeight = if (isHistorical) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            HorizontalDivider(color = Color(0xFF2D2D2D))
        }

        // Gameweek selector
        if (availableGameweeks.isNotEmpty()) {
            item {
                GameweekSelector(
                    currentEvent       = currentEvent,
                    selectedGameweek   = selectedGameweek,
                    availableGameweeks = availableGameweeks,
                    onGameweekSelected = onGameweekSelected,
                    onClearGameweek    = onClearGameweek
                )
            }
        }

        // Column headers
        item {
            LeagueTableHeader(
                showLiveColumns = !isHistorical,
                gwLabel = if (isHistorical) "GW$displayGw" else "GW"
            )
        }

        // Rows
        itemsIndexed(standings) { _, standing ->
            StandingRow(
                standing    = standing,
                isUserTeam  = standing.entry == userEntryId,
                liveData    = if (isHistorical) null else managerLiveData[standing.entry],
                isHistorical = isHistorical,
                onClick     = { onRowClick(standing) }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun LeagueTableHeader(showLiveColumns: Boolean = true, gwLabel: String = "GW") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#",       color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(42.dp))
        Text("Team",    color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
        if (showLiveColumns) {
            Text("▶",   color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(34.dp))
            Text("⏳",  color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(34.dp))
        }
        // GW label reflects the selected gameweek when in historical mode
        Text(
            gwLabel,
            color = if (gwLabel == "GW") Color(0xFF9E9E9E) else Color(0xFF90CAF9),
            fontWeight = FontWeight.Bold,
            fontSize = if (gwLabel.length > 2) 10.sp else 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(52.dp)
        )
        Text("Total",   color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(56.dp))
    }
    HorizontalDivider(color = Color(0xFF2D2D2D), thickness = 1.dp)
}

@Composable
private fun StandingRow(
    standing: StandingEntry,
    isUserTeam: Boolean,
    liveData: ManagerLiveData?,
    isHistorical: Boolean = false,
    onClick: () -> Unit
) {
    val rankChange = when {
        standing.rank < standing.lastRank -> RankChange.UP
        standing.rank > standing.lastRank -> RankChange.DOWN
        else -> RankChange.NO_CHANGE
    }
    val rowBg by animateColorAsState(
        targetValue = if (isUserTeam) Color(0xFF0D2744) else Color(0xFF1C1C1C),
        animationSpec = tween(300),
        label = "rowBg"
    )
    val hasLiveDiff = !isHistorical && liveData != null && liveData.livePoints != 0
    // Use calculatedGwPoints (full accurate GW score) when available, otherwise fall back to eventTotal
    val gwDisplayed = if (!isHistorical && liveData != null && liveData.calculatedGwPoints > 0)
        liveData.calculatedGwPoints
    else
        standing.eventTotal
    val activeChip   = if (!isHistorical) liveData?.activeChip else null
    val label        = chipLabel(activeChip, liveData?.chipNumber ?: 1)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBg)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank + arrow
            Row(modifier = Modifier.width(42.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${standing.rank}",
                    color = if (isUserTeam) Color(0xFF64B5F6) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    when (rankChange) { RankChange.UP -> "▲"; RankChange.DOWN -> "▼"; else -> "–" },
                    color = when (rankChange) { RankChange.UP -> Color(0xFF00E676); RankChange.DOWN -> Color(0xFFFF1744); else -> Color(0xFF555555) },
                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }

            // Team + chip badge + sub-line
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        standing.entryName,
                        color = if (isUserTeam) Color(0xFF90CAF9) else Color.White,
                        fontWeight = if (isUserTeam) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (label != null) {
                        Spacer(modifier = Modifier.width(5.dp))
                        ChipBadge(chip = activeChip, label = label)
                    }
                }
                val subText  = if (!isHistorical) liveData?.captainName?.let { "© $it" } ?: standing.playerName else standing.playerName
                val subColor = if (!isHistorical && liveData?.captainName != null) Color(0xFF00E676) else Color(0xFF757575)
                Text(
                    subText,
                    color = subColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (!isHistorical && liveData?.captainName != null) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            // In Play + To Start (live mode only)
            if (!isHistorical) {
                val inPlay  = liveData?.inPlay ?: 0
                val toStart = liveData?.toStart ?: 0
                Text(
                    if (inPlay > 0) "$inPlay" else "–",
                    color = if (inPlay > 0) Color(0xFF00E676) else Color(0xFF555555),
                    fontWeight = if (inPlay > 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.width(34.dp)
                )
                Text(
                    if (toStart > 0) "$toStart" else "–",
                    color = if (toStart > 0) Color(0xFFFFAB40) else Color(0xFF555555),
                    fontWeight = if (toStart > 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.width(34.dp)
                )
            }

            // GW pts
            Column(modifier = Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$gwDisplayed",
                    color = when {
                        hasLiveDiff  -> Color(0xFF00E676)
                        isHistorical -> Color(0xFF90CAF9)
                        else         -> Color.White
                    },
                    fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center
                )
                if (hasLiveDiff && liveData!!.livePoints > 0) {
                    Text("(${standing.eventTotal})", color = Color(0xFF555555), fontSize = 9.sp, textAlign = TextAlign.Center)
                }
            }

            // Total
            Column(modifier = Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${standing.total}",
                    color = when {
                        isUserTeam   -> Color(0xFF90CAF9)
                        hasLiveDiff  -> Color(0xFF00E676)
                        isHistorical -> Color(0xFFE0E0E0)
                        else         -> Color.White
                    },
                    fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center
                )
                if (hasLiveDiff && liveData!!.livePoints > 0) {
                    Text("+${liveData.livePoints}", color = Color(0xFF00E676), fontSize = 9.sp, textAlign = TextAlign.Center)
                }
            }
        }
        HorizontalDivider(color = Color(0xFF282828), thickness = 0.5.dp)
    }
}

// ─── Chip History view ────────────────────────────────────────────────────────

@Composable
private fun ChipHistoryView(
    navController: NavController,
    standings: List<StandingEntry>,
    managerLiveData: Map<Int, ManagerLiveData>,
    currentEvent: Int,
    userEntryId: Int?,
    leagueId: Long = 0L,
    prefsManager: PreferencesManager? = null
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // Header banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1A0A3B), Color(0xFF2D1B6B))))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Style, null, tint = Color(0xFF00FF87), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Chip History", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Tap a chip to view the team for that GW  ·  GW $currentEvent",
                        color = Color(0xFF9E9E9E),
                        fontSize = 11.sp
                    )
                }
            }
            HorizontalDivider(color = Color(0xFF2D2D2D))
        }

        // Colour legend
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181818))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Key:", color = Color(0xFF9E9E9E), fontSize = 10.sp)
                listOf("bboost" to "BB", "wildcard" to "WC", "3xc" to "TC", "freehit" to "FH").forEach { (chip, abbrev) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(chipColor(chip))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(abbrev, color = chipTextColor(chip), fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
                    }
                    val name = when (chip) {
                        "bboost" -> "Bench Boost"; "wildcard" -> "Wildcard"
                        "3xc"    -> "Triple Cap";  "freehit"  -> "Free Hit"; else -> ""
                    }
                    Text(name, color = Color(0xFF9E9E9E), fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            HorizontalDivider(color = Color(0xFF2D2D2D))
        }

        // Column header
        item { ChipHistoryHeader() }

        // Rows
        itemsIndexed(standings) { _, standing ->
            val isUserTeam = standing.entry == userEntryId
            val liveData   = managerLiveData[standing.entry]
            ChipHistoryRow(
                standing   = standing,
                isUserTeam = isUserTeam,
                allChips   = liveData?.allChips ?: emptyList(),
                onChipClick = { managerId, eventId, teamName ->
                    if (leagueId != 0L) prefsManager?.saveLeagueId(leagueId)
                    navController.navigate(
                        Screen.ManagerFormation.createRoute(managerId, eventId, teamName)
                    )
                }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ChipHistoryHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#",   color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(34.dp))
        Text("Team / Chips played this season", color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
    HorizontalDivider(color = Color(0xFF2D2D2D), thickness = 1.dp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipHistoryRow(
    standing: StandingEntry,
    isUserTeam: Boolean,
    allChips: List<UsedChip>,
    onChipClick: (managerId: Long, eventId: Int, teamName: String) -> Unit
) {
    val rowBg by animateColorAsState(
        targetValue = if (isUserTeam) Color(0xFF0D2744) else Color(0xFF1C1C1C),
        animationSpec = tween(300),
        label = "chipRowBg"
    )
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBg)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Rank
            Text(
                "${standing.rank}",
                color = if (isUserTeam) Color(0xFF64B5F6) else Color(0xFF9E9E9E),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(34.dp).padding(top = 2.dp)
            )
            // Team name + manager name + chip badges
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    standing.entryName,
                    color = if (isUserTeam) Color(0xFF90CAF9) else Color.White,
                    fontWeight = if (isUserTeam) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    standing.playerName,
                    color = Color(0xFF757575),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (allChips.isEmpty()) {
                    Text(
                        "No chips played yet",
                        color = Color(0xFF454545),
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement   = Arrangement.spacedBy(5.dp)
                    ) {
                        allChips.forEach { chip ->
                            ChipHistoryBadge(
                                chip     = chip,
                                onClick  = {
                                    onChipClick(
                                        standing.entry.toLong(),
                                        chip.event,
                                        standing.entryName.ifBlank { standing.playerName }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = Color(0xFF282828), thickness = 0.5.dp)
    }
}

/** Badge: coloured label + GW number. Tapping navigates to that team/GW. */
@Composable
private fun ChipHistoryBadge(chip: UsedChip, onClick: () -> Unit) {
    val label     = chipLabel(chip.name, chip.number) ?: chip.name.uppercase()
    val color     = chipColor(chip.name)
    val textColor = chipTextColor(chip.name)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(color)
                .padding(horizontal = 7.dp, vertical = 4.dp)
        ) {
            Text(label, color = textColor, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, letterSpacing = 0.3.sp)
        }
        Text("GW${chip.event}", color = Color(0xFF666666), fontSize = 8.sp, textAlign = TextAlign.Center)
    }
}

// ─── Shared chip badge (in normal standings row) ─────────────────────────────

@Composable
private fun ChipBadge(chip: String?, label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(chipColor(chip))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(label, color = chipTextColor(chip), fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, letterSpacing = 0.3.sp)
    }
}
