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

// ─── Chip colour palette ────────────────────────────────────────────────────
private val ChipBenchBoost  = Color(0xFF00C853)   // vivid green
private val ChipWildcard    = Color(0xFF2979FF)   // bright blue
private val ChipTripleCap   = Color(0xFFFFD600)   // gold
private val ChipFreeHit     = Color(0xFFFF6D00)   // orange

/** Returns the display label for a chip (e.g. "BB1", "WC2", "TC1", "FH2") */
fun chipLabel(activeChip: String?, chipNumber: Int): String? {
    val num = chipNumber.coerceIn(1, 2)
    return when (activeChip) {
        "bboost"   -> "BB$num"
        "wildcard" -> "WC$num"
        "3xc"      -> "TC$num"
        "freehit"  -> "FH$num"
        else       -> null
    }
}

/** Returns the badge colour for a given chip string */
fun chipColor(activeChip: String?): Color = when (activeChip) {
    "bboost"   -> ChipBenchBoost
    "wildcard" -> ChipWildcard
    "3xc"      -> ChipTripleCap
    "freehit"  -> ChipFreeHit
    else       -> Color.Gray
}

// ─── Screen ─────────────────────────────────────────────────────────────────

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
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00FF87))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Loading standings…", color = Color(0xFF9E9E9E), fontSize = 14.sp)
                    }
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF5555),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Failed to load standings",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = uiState.error ?: "",
                            color = Color(0xFF9E9E9E),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.loadLeagueStandings(leagueId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF87))
                        ) {
                            Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                uiState.leagueStandings != null -> {
                    val standings = if (uiState.liveRankings.isNotEmpty())
                        uiState.liveRankings
                    else
                        uiState.leagueStandings!!.standings.results

                    LazyColumn(modifier = Modifier.fillMaxSize()) {

                        // ── Live banner ──────────────────────────────────────
                        if (uiState.hasLiveFixtures) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFFB71C1C), Color(0xFFE53935))
                                            )
                                        )
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

                        // ── League info card ─────────────────────────────────
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E1E1E))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = uiState.leagueStandings!!.league.name,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "GW ${uiState.currentEvent}  ·  ${standings.size} managers",
                                        color = Color(0xFF9E9E9E),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFF2D2D2D))
                        }

                        // ── Column headers ───────────────────────────────────
                        item {
                            LeagueTableHeader2()
                        }

                        // ── Rows ─────────────────────────────────────────────
                        itemsIndexed(standings) { _, standing ->
                            val isUserTeam = standing.entry == prefsManager.getManagerId()?.toInt()
                            val liveData = uiState.managerLiveData[standing.entry]
                            StandingRow2(
                                standing = standing,
                                isUserTeam = isUserTeam,
                                liveData = liveData,
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

                        // Bottom padding
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }

        // ── Favourite dialog ─────────────────────────────────────────────────
        if (showFavoriteDialog) {
            AlertDialog(
                onDismissRequest = { showFavoriteDialog = false },
                title = {
                    Text(if (isFavorite) "Added to Favourites" else "Removed from Favourites")
                },
                text = {
                    Text(
                        if (isFavorite)
                            "This league will load automatically when you open the app."
                        else
                            "This league has been removed from favourites."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showFavoriteDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

// ─── Header row ──────────────────────────────────────────────────────────────

@Composable
fun LeagueTableHeader2() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank column
        Text(
            "#",
            color = Color(0xFF9E9E9E),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.width(42.dp)
        )
        // Team column
        Text(
            "Team",
            color = Color(0xFF9E9E9E),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        // In Play
        Text(
            "▶",
            color = Color(0xFF9E9E9E),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(34.dp)
        )
        // To Start
        Text(
            "⏳",
            color = Color(0xFF9E9E9E),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(34.dp)
        )
        // GW pts
        Text(
            "GW",
            color = Color(0xFF9E9E9E),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(52.dp)
        )
        // Total
        Text(
            "Total",
            color = Color(0xFF9E9E9E),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp)
        )
    }
    HorizontalDivider(color = Color(0xFF2D2D2D), thickness = 1.dp)
}

// ─── Standing row ─────────────────────────────────────────────────────────────

@Composable
fun StandingRow2(
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
        targetValue = when {
            isUserTeam -> Color(0xFF0D2744)
            else       -> Color(0xFF1C1C1C)
        },
        animationSpec = tween(300),
        label = "rowBg"
    )

    val hasLiveDiff = liveData != null && liveData.livePoints != 0
    val gwDisplayed  = standing.eventTotal + (liveData?.livePoints ?: 0)

    val activeChip = liveData?.activeChip
    val chipLabel  = chipLabel(activeChip, liveData?.chipNumber ?: 1)
    val chipColor  = chipColor(activeChip)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBg)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Rank + arrow ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.width(42.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${standing.rank}",
                    color = if (isUserTeam) Color(0xFF64B5F6) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = when (rankChange) {
                        RankChange.UP        -> "▲"
                        RankChange.DOWN      -> "▼"
                        RankChange.NO_CHANGE -> "–"
                    },
                    color = when (rankChange) {
                        RankChange.UP        -> Color(0xFF00E676)
                        RankChange.DOWN      -> Color(0xFFFF1744)
                        RankChange.NO_CHANGE -> Color(0xFF555555)
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // ── Team name + captain + chip badge ─────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = standing.entryName,
                        color = if (isUserTeam) Color(0xFF90CAF9) else Color.White,
                        fontWeight = if (isUserTeam) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    // Chip badge – shown next to the team name
                    if (chipLabel != null) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(chipColor)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = chipLabel,
                                color = if (activeChip == "3xc") Color.Black else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
                // Captain or manager name sub-line
                val captainText = liveData?.captainName?.let { "© $it" } ?: standing.playerName
                val captainColor = if (liveData?.captainName != null) Color(0xFF00E676) else Color(0xFF757575)
                Text(
                    text = captainText,
                    color = captainColor,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (liveData?.captainName != null) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            // ── In Play ─────────────────────────────────────────────────────
            val inPlay = liveData?.inPlay ?: 0
            Text(
                text = if (inPlay > 0) "$inPlay" else "–",
                color = if (inPlay > 0) Color(0xFF00E676) else Color(0xFF555555),
                fontWeight = if (inPlay > 0) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(34.dp)
            )

            // ── To Start ────────────────────────────────────────────────────
            val toStart = liveData?.toStart ?: 0
            Text(
                text = if (toStart > 0) "$toStart" else "–",
                color = if (toStart > 0) Color(0xFFFFAB40) else Color(0xFF555555),
                fontWeight = if (toStart > 0) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(34.dp)
            )

            // ── GW Points ───────────────────────────────────────────────────
            Column(
                modifier = Modifier.width(52.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$gwDisplayed",
                    color = if (hasLiveDiff) Color(0xFF00E676) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                // Show original API pts underneath if we overrode it
                if (hasLiveDiff && liveData!!.livePoints > 0) {
                    Text(
                        text = "(${standing.eventTotal})",
                        color = Color(0xFF555555),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Total Points ────────────────────────────────────────────────
            Column(
                modifier = Modifier.width(56.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${standing.total}",
                    color = if (isUserTeam) Color(0xFF90CAF9)
                            else if (hasLiveDiff) Color(0xFF00E676)
                            else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                if (hasLiveDiff && liveData!!.livePoints > 0) {
                    Text(
                        text = "+${liveData.livePoints}",
                        color = Color(0xFF00E676),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        HorizontalDivider(color = Color(0xFF282828), thickness = 0.5.dp)
    }
}
