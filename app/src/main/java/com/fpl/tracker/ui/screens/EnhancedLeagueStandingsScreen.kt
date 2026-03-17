package com.fpl.tracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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

// ─── Chip colour palette ────────────────────────────────────────────────────
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
        viewModel.loadLeagueStandings(leagueId)
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
                    // ── Chip history toggle ──────────────────────────────
                    IconButton(onClick = { viewModel.toggleChipHistory() }) {
                        Icon(
                            Icons.Filled.Style,
                            contentDescription = if (uiState.showChipHistory) "Back to Rankings" else "Chip History",
                            tint = if (uiState.showChipHistory) Color(0xFF00FF87) else Color.White
                        )
                    }
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
                    IconButton(onClick = { viewModel.loadLeagueStandings(leagueId) }) {
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
                    val standings = if (uiState.liveRankings.isNotEmpty())
                        uiState.liveRankings
                    else
                        uiState.leagueStandings!!.standings.results

                    if (uiState.showChipHistory) {
                        // ── CHIP HISTORY VIEW ────────────────────────────
                        ChipHistoryView(
                            standings = standings,
                            managerLiveData = uiState.managerLiveData,
                            currentEvent = uiState.currentEvent,
                            userEntryId = prefsManager.getManagerId()?.toInt()
                        )
                    } else {
                        // ── NORMAL RANKINGS VIEW ─────────────────────────
                        NormalRankingsView(
                            standings = standings,
                            managerLiveData = uiState.managerLiveData,
                            hasLiveFixtures = uiState.hasLiveFixtures,
                            leagueName = uiState.leagueStandings!!.league.name,
                            currentEvent = uiState.currentEvent,
                            userEntryId = prefsManager.getManagerId()?.toInt(),
                            onRowClick = { standing ->
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

// ─── Loading / Error helpers ─────────────────────────────────────────────────

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

// ─── Normal rankings view ────────────────────────────────────────────────────

@Composable
private fun NormalRankingsView(
    standings: List<StandingEntry>,
    managerLiveData: Map<Int, ManagerLiveData>,
    hasLiveFixtures: Boolean,
    leagueName: String,
    currentEvent: Int,
    userEntryId: Int?,
    onRowClick: (StandingEntry) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // Live banner
        if (hasLiveFixtures) {
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
                    "GW $currentEvent  ·  ${standings.size} managers",
                    color = Color(0xFF9E9E9E),
                    fontSize = 12.sp
                )
            }
            HorizontalDivider(color = Color(0xFF2D2D2D))
        }

        // Column headers
        item { LeagueTableHeader() }

        // Rows
        itemsIndexed(standings) { _, standing ->
            StandingRow(
                standing = standing,
                isUserTeam = standing.entry == userEntryId,
                liveData = managerLiveData[standing.entry],
                onClick = { onRowClick(standing) }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun LeagueTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#",     color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(42.dp))
        Text("Team",  color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text("▶",     color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(34.dp))
        Text("⏳",    color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(34.dp))
        Text("GW",    color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(52.dp))
        Text("Total", color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(56.dp))
    }
    HorizontalDivider(color = Color(0xFF2D2D2D), thickness = 1.dp)
}

@Composable
private fun StandingRow(
    standing: StandingEntry,
    isUserTeam: Boolean,
    liveData: ManagerLiveData?,
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

    val hasLiveDiff = liveData != null && liveData.livePoints != 0
    val gwDisplayed  = standing.eventTotal + (liveData?.livePoints ?: 0)
    val activeChip   = liveData?.activeChip
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

            // Team + chip badge + captain
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
                val captainText  = liveData?.captainName?.let { "© $it" } ?: standing.playerName
                val captainColor = if (liveData?.captainName != null) Color(0xFF00E676) else Color(0xFF757575)
                Text(
                    captainText,
                    color = captainColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (liveData?.captainName != null) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            // In Play
            val inPlay = liveData?.inPlay ?: 0
            Text(
                if (inPlay > 0) "$inPlay" else "–",
                color = if (inPlay > 0) Color(0xFF00E676) else Color(0xFF555555),
                fontWeight = if (inPlay > 0) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.width(34.dp)
            )

            // To Start
            val toStart = liveData?.toStart ?: 0
            Text(
                if (toStart > 0) "$toStart" else "–",
                color = if (toStart > 0) Color(0xFFFFAB40) else Color(0xFF555555),
                fontWeight = if (toStart > 0) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.width(34.dp)
            )

            // GW pts
            Column(modifier = Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$gwDisplayed",
                    color = if (hasLiveDiff) Color(0xFF00E676) else Color.White,
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
    standings: List<StandingEntry>,
    managerLiveData: Map<Int, ManagerLiveData>,
    currentEvent: Int,
    userEntryId: Int?
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // Header banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(Color(0xFF1A0A3B), Color(0xFF2D1B6B)))
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Style,
                    contentDescription = null,
                    tint = Color(0xFF00FF87),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "Chip History",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "All chips played this season  ·  GW $currentEvent",
                        color = Color(0xFF9E9E9E),
                        fontSize = 11.sp
                    )
                }
            }
            HorizontalDivider(color = Color(0xFF2D2D2D))
        }

        // Legend row
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
                        Text(
                            abbrev,
                            color = chipTextColor(chip),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp
                        )
                    }
                    val fullName = when (chip) {
                        "bboost"   -> "Bench Boost"
                        "wildcard" -> "Wildcard"
                        "3xc"      -> "Triple Cap"
                        "freehit"  -> "Free Hit"
                        else       -> ""
                    }
                    Text(fullName, color = Color(0xFF9E9E9E), fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            HorizontalDivider(color = Color(0xFF2D2D2D))
        }

        // Column headers
        item { ChipHistoryHeader() }

        // Manager rows
        itemsIndexed(standings) { _, standing ->
            val isUserTeam = standing.entry == userEntryId
            val liveData   = managerLiveData[standing.entry]
            ChipHistoryRow(
                standing   = standing,
                isUserTeam = isUserTeam,
                allChips   = liveData?.allChips ?: emptyList()
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
        Text("#",    color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(34.dp))
        Text("Team / Chips played this season", color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
    }
    HorizontalDivider(color = Color(0xFF2D2D2D), thickness = 1.dp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipHistoryRow(
    standing: StandingEntry,
    isUserTeam: Boolean,
    allChips: List<UsedChip>
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
                modifier = Modifier
                    .width(34.dp)
                    .padding(top = 2.dp)
            )

            // Team name + manager name + chips all in one column
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
                    // FlowRow wraps badges onto new lines if they don't fit
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        allChips.forEach { chip ->
                            ChipHistoryBadge(chip = chip)
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = Color(0xFF282828), thickness = 0.5.dp)
    }
}

/** A badge showing chip label + GW number it was played in */
@Composable
private fun ChipHistoryBadge(chip: UsedChip) {
    val label = chipLabel(chip.name, chip.number) ?: chip.name.uppercase()
    val color = chipColor(chip.name)
    val textColor = chipTextColor(chip.name)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(color)
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                label,
                color = textColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp,
                letterSpacing = 0.3.sp
            )
        }
        Text(
            "GW${chip.event}",
            color = Color(0xFF666666),
            fontSize = 8.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Shared chip badge ────────────────────────────────────────────────────────

@Composable
private fun ChipBadge(chip: String?, label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(chipColor(chip))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            label,
            color = chipTextColor(chip),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 9.sp,
            letterSpacing = 0.3.sp
        )
    }
}
