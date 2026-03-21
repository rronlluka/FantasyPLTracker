package com.fpl.tracker.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.fpl.tracker.ui.theme.SpaceGrotesk
import com.fpl.tracker.ui.theme.Manrope
import com.fpl.tracker.viewmodel.LeagueStandingsViewModel
import com.fpl.tracker.viewmodel.ManagerLiveData
import com.fpl.tracker.viewmodel.UsedChip
import java.text.NumberFormat
import java.util.Locale

// ─── Stitch colour palette ───────────────────────────────────────────────────
private val PitchGreen        = Color(0xFFA1D494)
private val PitchGreenDark    = Color(0xFF2D5A27)
private val GoldSecondary     = Color(0xFFFFE083)
private val SurfaceBg         = Color(0xFF131313)
private val SurfaceContLow    = Color(0xFF1C1B1B)
private val SurfaceCont       = Color(0xFF20201F)
private val SurfaceContHigh   = Color(0xFF2A2A2A)
private val OnSurface         = Color(0xFFE5E2E1)
private val OnSurfaceVariant  = Color(0xFFC2C9BB)
private val OutlineVar        = Color(0xFF42493E)
private val TertiaryRed       = Color(0xFFA40217)

private val ChipBenchBoost = Color(0xFFFFE083)
private val ChipWildcard   = Color(0xFF2D5A27)
private val ChipTripleCap  = Color(0xFF353535)
private val ChipFreeHit    = Color(0xFFA40217)

private val ChipBBText = Color(0xFF3C2F00)
private val ChipWCText = Color(0xFFA1D494)
private val ChipTCText = Color.White
private val ChipFHText = Color(0xFFFFDAD7)

fun chipLabel(chip: String?, number: Int): String? {
    val n = number.coerceIn(1, 2)
    return when (chip) {
        "bboost"   -> if (n > 1) "BB$n" else "BB"
        "wildcard" -> "WC$n"
        "3xc"      -> if (n > 1) "TC$n" else "TC"
        "freehit"  -> if (n > 1) "FH$n" else "FH"
        else       -> null
    }
}

fun chipShortLabel(chip: String?): String = when (chip) {
    "bboost"   -> "BB"
    "wildcard" -> "WC"
    "3xc"      -> "TC"
    "freehit"  -> "FH"
    else       -> "?"
}

fun chipColor(chip: String?): Color = when (chip) {
    "bboost"   -> ChipBenchBoost
    "wildcard" -> ChipWildcard
    "3xc"      -> ChipTripleCap
    "freehit"  -> ChipFreeHit
    else       -> Color.Gray
}

fun chipTextColor(chip: String?): Color = when (chip) {
    "bboost"   -> ChipBBText
    "wildcard" -> ChipWCText
    "3xc"      -> ChipTCText
    "freehit"  -> ChipFHText
    else       -> Color.White
}

private fun formatTotal(n: Int): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).format(n)

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
        if (uiState.leagueStandings == null) {
            viewModel.loadLeagueStandings(leagueId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PITCH-SIDE GALLERY",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp,
                            color = PitchGreen,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PitchGreen)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleChipHistory() }) {
                        Icon(
                            Icons.Filled.Style,
                            contentDescription = if (uiState.showChipHistory) "Back to Rankings" else "Chip History",
                            tint = if (uiState.showChipHistory) PitchGreen else OnSurfaceVariant
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
                            tint = if (isFavorite) GoldSecondary else OnSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        viewModel.loadLeagueStandings(leagueId, uiState.selectedGameweek)
                    }) {
                        Icon(Icons.Filled.Refresh, "Refresh", tint = OnSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceBg,
                    titleContentColor = OnSurface,
                    navigationIconContentColor = PitchGreen,
                    actionIconContentColor = OnSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SurfaceBg)
        ) {
            when {
                uiState.isLoading -> LoadingState()

                uiState.error != null -> ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.loadLeagueStandings(leagueId) }
                )

                uiState.leagueStandings != null -> {
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
                            prefsManager    = prefsManager,
                            leagueName      = uiState.leagueStandings!!.league.name
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
        CircularProgressIndicator(color = PitchGreen)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Loading standings…",
            color = OnSurfaceVariant,
            fontSize = 14.sp,
            fontFamily = Manrope
        )
    }
}

@Composable
private fun BoxScope.ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Warning, null, tint = Color(0xFFFFB4AB), modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Failed to load standings",
            color = OnSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            fontFamily = SpaceGrotesk
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(message, color = OnSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center, fontFamily = Manrope)
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PitchGreen),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Retry", color = Color(0xFF0A3909), fontWeight = FontWeight.Bold, fontFamily = Manrope)
        }
    }
}

// ─── Live Status Bar ─────────────────────────────────────────────────────────

@Composable
private fun LiveStatusBar(currentEvent: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContLow)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(TertiaryRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "LIVE MATCHDAY STATUS",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                color = Color(0xFFFFB3AD)
            )
        }
        Text(
            "GW $currentEvent \u2022 ACTIVE",
            fontFamily = Manrope,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
            color = OnSurfaceVariant
        )
    }
}

// ─── Gameweek selector ───────────────────────────────────────────────────────

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
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Standings after:",
                color = OnSurfaceVariant,
                fontSize = 10.sp,
                fontFamily = Manrope,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )

            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SurfaceContHigh)
                        .border(
                            width = 1.dp,
                            color = if (isHistorical) PitchGreen.copy(alpha = 0.5f) else OutlineVar.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { expanded = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "GW $displayGw",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        color = if (isHistorical) PitchGreen else OnSurface,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.ExpandMore,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = OnSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .background(SurfaceCont)
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Bolt,
                                    contentDescription = null,
                                    tint = PitchGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "GW $currentEvent (Live)",
                                    color = PitchGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    fontFamily = SpaceGrotesk
                                )
                            }
                        },
                        onClick = { onClearGameweek(); expanded = false },
                        modifier = Modifier.background(
                            if (selectedGameweek == null) SurfaceContHigh else Color.Transparent
                        )
                    )

                    HorizontalDivider(color = OutlineVar.copy(alpha = 0.3f), thickness = 0.5.dp)

                    availableGameweeks.reversed().forEach { gw ->
                        if (gw == currentEvent) return@forEach
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "GW $gw",
                                    color = if (gw == selectedGameweek) PitchGreen else OnSurface,
                                    fontWeight = if (gw == selectedGameweek) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    fontFamily = SpaceGrotesk
                                )
                            },
                            onClick = { onGameweekSelected(gw); expanded = false },
                            modifier = Modifier.background(
                                if (gw == selectedGameweek) PitchGreenDark.copy(alpha = 0.3f) else Color.Transparent
                            )
                        )
                    }
                }
            }
        }

        if (isHistorical) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PitchGreenDark.copy(alpha = 0.25f))
                    .padding(horizontal = 16.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = PitchGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Historical view \u2014 standings as of end of GW $selectedGameweek",
                    color = PitchGreen,
                    fontSize = 11.sp,
                    fontFamily = Manrope,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onClearGameweek,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Live", color = GoldSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = SpaceGrotesk)
                }
            }
        }
    }
}

// ─── Normal Rankings View ────────────────────────────────────────────────────

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
    val isHistorical = selectedGameweek != null
    val displayGw    = selectedGameweek ?: currentEvent

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // Live status bar
        if (hasLiveFixtures && !isHistorical) {
            item { LiveStatusBar(currentEvent) }
        }

        // League header ("LEAGUE STANDINGS" + league name)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 4.dp)
            ) {
                Text(
                    "LEAGUE STANDINGS",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    letterSpacing = (-1).sp,
                    color = OnSurface,
                    lineHeight = 34.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceCont)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        leagueName,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = GoldSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "\u2022 ${standings.size} managers",
                        fontFamily = Manrope,
                        fontSize = 12.sp,
                        color = OnSurfaceVariant
                    )
                }
            }
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

        // Column header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            LeagueTableHeader(
                showLiveColumns = !isHistorical,
                gwLabel = if (isHistorical) "GW$displayGw" else "GW PTS"
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

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun LeagueTableHeader(showLiveColumns: Boolean = true, gwLabel: String = "GW PTS") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "RK",
            color = GoldSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            fontFamily = SpaceGrotesk,
            letterSpacing = 1.5.sp,
            modifier = Modifier.width(40.dp)
        )
        Text(
            "TEAM & MANAGER",
            color = GoldSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            fontFamily = SpaceGrotesk,
            letterSpacing = 1.5.sp,
            modifier = Modifier.weight(1f)
        )
        if (showLiveColumns) {
            Text(
                "\u25B6",
                color = GoldSecondary.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(28.dp)
            )
            Text(
                "\u23F3",
                color = GoldSecondary.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(28.dp)
            )
        }
        Text(
            gwLabel,
            color = GoldSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            fontFamily = SpaceGrotesk,
            letterSpacing = 1.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(56.dp)
        )
        Text(
            "TOTAL",
            color = GoldSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            fontFamily = SpaceGrotesk,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(64.dp)
        )
    }
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
    val hasLiveDiff = !isHistorical && liveData != null && liveData.livePoints != 0
    val gwDisplayed = if (!isHistorical && liveData != null && liveData.calculatedGwPoints > 0)
        liveData.calculatedGwPoints
    else
        standing.eventTotal
    val activeChip = if (!isHistorical) liveData?.activeChip else null
    val label      = chipLabel(activeChip, liveData?.chipNumber ?: 1)

    val rowBg = if (isUserTeam) PitchGreenDark else SurfaceContLow

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(rowBg)
                .then(
                    if (isUserTeam)
                        Modifier.drawBehind {
                            drawRect(
                                color = PitchGreen,
                                topLeft = Offset.Zero,
                                size = Size(4.dp.toPx(), size.height)
                            )
                        }
                    else Modifier
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                "%02d".format(standing.rank),
                color = if (isUserTeam) PitchGreen else OnSurfaceVariant.copy(alpha = 0.4f),
                fontWeight = FontWeight.Black,
                fontSize = if (isUserTeam) 22.sp else 18.sp,
                fontFamily = SpaceGrotesk,
                modifier = Modifier.width(40.dp)
            )

            // Team + chip badge + manager + captain (each on own line)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        standing.entryName.uppercase(),
                        color = OnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = SpaceGrotesk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (label != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StitchChipBadge(chip = activeChip, label = chipShortLabel(activeChip))
                    }
                }
                Text(
                    standing.playerName.uppercase(),
                    color = if (isUserTeam) PitchGreen.copy(alpha = 0.8f) else OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isHistorical && liveData?.captainName != null) {
                    Text(
                        "\u00A9 ${liveData.captainName}",
                        color = PitchGreen,
                        fontSize = 10.sp,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // In Play + To Start (live mode only)
            if (!isHistorical) {
                val inPlay  = liveData?.inPlay ?: 0
                val toStart = liveData?.toStart ?: 0
                Text(
                    if (inPlay > 0) "$inPlay" else "\u2013",
                    color = if (inPlay > 0) PitchGreen else OnSurfaceVariant.copy(alpha = 0.3f),
                    fontWeight = if (inPlay > 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    fontFamily = Manrope,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(28.dp)
                )
                Text(
                    if (toStart > 0) "$toStart" else "\u2013",
                    color = if (toStart > 0) GoldSecondary else OnSurfaceVariant.copy(alpha = 0.3f),
                    fontWeight = if (toStart > 0) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    fontFamily = Manrope,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(28.dp)
                )
            }

            // GW pts
            Column(modifier = Modifier.width(56.dp), horizontalAlignment = Alignment.End) {
                Text(
                    "$gwDisplayed",
                    color = when {
                        isUserTeam   -> PitchGreen
                        hasLiveDiff  -> PitchGreen
                        isHistorical -> OnSurfaceVariant
                        else         -> OnSurface
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isUserTeam) 20.sp else 16.sp,
                    fontFamily = SpaceGrotesk,
                    textAlign = TextAlign.End
                )
                if (hasLiveDiff && liveData!!.livePoints > 0) {
                    Text(
                        "+${liveData.livePoints} LIVE",
                        color = PitchGreen,
                        fontSize = 9.sp,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Total
            Text(
                formatTotal(standing.total),
                color = if (isUserTeam) OnSurface else OnSurface,
                fontWeight = if (isUserTeam) FontWeight.Black else FontWeight.Bold,
                fontSize = if (isUserTeam) 22.sp else 18.sp,
                fontFamily = SpaceGrotesk,
                textAlign = TextAlign.End,
                modifier = Modifier.width(64.dp)
            )
        }
    }
}

// ─── Stitch Chip Badge (inline in standings row) ─────────────────────────────

@Composable
private fun StitchChipBadge(chip: String?, label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .background(chipColor(chip))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            label,
            color = chipTextColor(chip),
            fontWeight = FontWeight.Black,
            fontSize = 9.sp,
            fontFamily = SpaceGrotesk,
            letterSpacing = 0.3.sp
        )
    }
}

// ─── Chip History View ───────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipHistoryView(
    navController: NavController,
    standings: List<StandingEntry>,
    managerLiveData: Map<Int, ManagerLiveData>,
    currentEvent: Int,
    userEntryId: Int?,
    leagueId: Long = 0L,
    prefsManager: PreferencesManager? = null,
    leagueName: String = ""
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // Header section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 8.dp)
            ) {
                Text(
                    leagueName.uppercase(),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = PitchGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "LEAGUE CHIPS",
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    letterSpacing = (-1).sp,
                    color = OnSurface,
                    lineHeight = 34.sp
                )
            }
        }

        // Season progress
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceContLow)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "SEASON PROGRESS",
                        fontFamily = Manrope,
                        fontSize = 9.sp,
                        letterSpacing = 1.5.sp,
                        color = OnSurfaceVariant
                    )
                    Text(
                        "GW $currentEvent / 38",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GoldSecondary
                    )
                }
            }
        }

        // Chip legend (2x2 grid)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipLegendCard("WC", "Wildcard", "wildcard", Modifier.weight(1f))
                ChipLegendCard("FH", "Free Hit", "freehit", Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChipLegendCard("BB", "Bench Boost", "bboost", Modifier.weight(1f))
                ChipLegendCard("TC", "Triple Cpt", "3xc", Modifier.weight(1f))
            }
        }

        // Manager rows
        itemsIndexed(standings) { _, standing ->
            val isUserTeam = standing.entry == userEntryId
            val liveData   = managerLiveData[standing.entry]
            ChipHistoryRow(
                standing   = standing,
                isUserTeam = isUserTeam,
                allChips   = liveData?.allChips ?: emptyList(),
                totalPoints = standing.total,
                onChipClick = { managerId, eventId, teamName ->
                    if (leagueId != 0L) prefsManager?.saveLeagueId(leagueId)
                    navController.navigate(
                        Screen.ManagerFormation.createRoute(managerId, eventId, teamName)
                    )
                }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun ChipLegendCard(abbrev: String, name: String, chipType: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCont)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(chipColor(chipType)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                abbrev,
                color = chipTextColor(chipType),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = SpaceGrotesk
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            name.uppercase(),
            fontFamily = Manrope,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipHistoryRow(
    standing: StandingEntry,
    isUserTeam: Boolean,
    allChips: List<UsedChip>,
    totalPoints: Int,
    onChipClick: (managerId: Long, eventId: Int, teamName: String) -> Unit
) {
    val rowBg = if (isUserTeam) PitchGreenDark.copy(alpha = 0.2f) else SurfaceContLow

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(rowBg)
                .then(
                    if (isUserTeam)
                        Modifier.drawBehind {
                            drawRect(
                                color = PitchGreen,
                                topLeft = Offset.Zero,
                                size = Size(4.dp.toPx(), size.height)
                            )
                        }
                    else Modifier
                )
                .border(
                    width = 1.dp,
                    color = if (isUserTeam) Color.Transparent else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            // Top: Rank + Team + Points
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "%02d".format(standing.rank),
                    color = if (isUserTeam) PitchGreen.copy(alpha = 0.5f) else OnSurfaceVariant.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    fontFamily = SpaceGrotesk,
                    modifier = Modifier.width(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        standing.entryName.uppercase(),
                        color = OnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = SpaceGrotesk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    Row {
                        Text(
                            standing.playerName,
                            color = OnSurfaceVariant,
                            fontSize = 11.sp,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            " | ",
                            color = OnSurfaceVariant.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontFamily = Manrope
                        )
                        Text(
                            "${formatTotal(totalPoints)} pts",
                            color = if (isUserTeam) PitchGreen else OnSurface,
                            fontSize = 11.sp,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Only chips actually played (chronological by GW)
            if (allChips.isEmpty()) {
                Text(
                    "No chips played yet",
                    color = OnSurfaceVariant.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    fontFamily = Manrope,
                    fontStyle = FontStyle.Italic
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allChips.sortedBy { it.event }.forEach { chip ->
                        ChipGridItemUsed(
                            chip = chip,
                            onClick = {
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
}

@Composable
private fun ChipGridItemUsed(
    chip: UsedChip,
    onClick: () -> Unit
) {
    val lbl = chipLabel(chip.name, chip.number) ?: chip.name.uppercase()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(chipColor(chip.name))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                lbl,
                color = chipTextColor(chip.name),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = SpaceGrotesk
            )
        }
        Text(
            "GW ${chip.event}",
            color = OnSurfaceVariant,
            fontSize = 9.sp,
            fontFamily = Manrope,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
