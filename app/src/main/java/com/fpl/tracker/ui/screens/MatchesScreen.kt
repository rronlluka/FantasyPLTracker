package com.fpl.tracker.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fpl.tracker.data.models.Fixture
import com.fpl.tracker.data.models.LiveElement
import com.fpl.tracker.data.models.Player
import com.fpl.tracker.data.models.Team
import com.fpl.tracker.viewmodel.MatchesViewModel

// FPL brand colours
private val FplGreen = Color(0xFF00FF87)
private val FplLiveRed = Color(0xFFE90052)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchesScreen(
    navController: NavController,
    viewModel: MatchesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFixture by remember { mutableStateOf<Fixture?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentGameweekFixtures()
    }

    val dropdownExpanded = remember { mutableStateOf(false) }
    val events = uiState.events
    val selectedEvent = events.firstOrNull { it.id == uiState.currentEvent }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded.value,
                    onExpandedChange = { dropdownExpanded.value = !dropdownExpanded.value },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = selectedEvent?.name ?: "Gameweek ${uiState.currentEvent}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gameweek") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(dropdownExpanded.value)
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded.value,
                        onDismissRequest = { dropdownExpanded.value = false }
                    ) {
                        events.forEach { event ->
                            DropdownMenuItem(
                                text = { Text(event.name) },
                                onClick = {
                                    dropdownExpanded.value = false
                                    viewModel.selectGameweek(event.id)
                                }
                            )
                        }
                    }
                }
                IconButton(onClick = { viewModel.refreshFixtures() }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    uiState.error != null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⚠️ Failed to load fixtures",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = uiState.error ?: "",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refreshFixtures() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Retry", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                    uiState.fixtures.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.fixtures) { fixture ->
                                FixtureCard(
                                    fixture = fixture,
                                    homeTeam = uiState.teams.find { it.id == fixture.teamH },
                                    awayTeam = uiState.teams.find { it.id == fixture.teamA },
                                    liveElements = uiState.liveElements,
                                    onClick = { selectedFixture = fixture }
                                )
                            }
                        }
                    }
                    else -> {
                        Text(
                            "No fixtures found",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    selectedFixture?.let { fixture ->
        FixtureDetailDialog(
            fixture = fixture,
            homeTeam = uiState.teams.find { it.id == fixture.teamH },
            awayTeam = uiState.teams.find { it.id == fixture.teamA },
            players = uiState.players,
            liveElements = uiState.liveElements,
            onDismiss = { selectedFixture = null }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Fixture Card
// ─────────────────────────────────────────────────────────────

@Composable
fun FixtureCard(
    fixture: Fixture,
    homeTeam: Team?,
    awayTeam: Team?,
    liveElements: List<LiveElement>,
    onClick: () -> Unit
) {
    val isLive = fixture.started == true && !fixture.finished && !fixture.finishedProvisional
    val isFinished = fixture.finished || fixture.finishedProvisional

    // For live games show live score from fixture (API updates scores directly)
    val homeScore = fixture.teamHScore
    val awayScore = fixture.teamAScore

    val cardBg = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLive) 6.dp else 2.dp)
    ) {
        // Live top banner
        if (isLive) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FplLiveRed)
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveDot(tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "LIVE · ${fixture.minutes}'",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home team
            TeamBlock(
                name = homeTeam?.name ?: "Home",
                shortName = homeTeam?.shortName ?: "?",
                score = homeScore,
                showScore = isLive || isFinished,
                align = Alignment.Start,
                modifier = Modifier.weight(1.4f)
            )

            // Centre: score divider or kickoff
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    isLive -> {
                        Text(
                            text = "${homeScore ?: 0}  –  ${awayScore ?: 0}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    isFinished -> {
                        Text(
                            text = "${homeScore ?: 0}  –  ${awayScore ?: 0}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "FT",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Text(
                            text = "vs",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        fixture.kickoffTime?.let { kt ->
                            val time = kt.substringAfter("T").take(5)
                            Text(
                                text = time,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Away team
            TeamBlock(
                name = awayTeam?.name ?: "Away",
                shortName = awayTeam?.shortName ?: "?",
                score = awayScore,
                showScore = isLive || isFinished,
                align = Alignment.End,
                modifier = Modifier.weight(1.4f)
            )
        }

        // Quick goals summary for live/finished games
        if ((isLive || isFinished) && fixture.stats != null) {
            val goals = fixture.stats.find { it.identifier == "goals_scored" }
            if (goals != null && (goals.h.isNotEmpty() || goals.a.isNotEmpty())) {
                HorizontalDivider(color = Color(0xFFEEEEEE))
                // We show scorer names inline
                GoalsSummaryRow(goals.h, goals.a, fixture.stats, emptyList())
            }
        }
    }
}

@Composable
private fun TeamBlock(
    name: String,
    shortName: String,
    score: Int?,
    showScore: Boolean,
    align: Alignment.Horizontal,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = align
    ) {
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = shortName,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (showScore && score != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = score.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun GoalsSummaryRow(
    homeGoals: List<com.fpl.tracker.data.models.FixtureStatValue>,
    awayGoals: List<com.fpl.tracker.data.models.FixtureStatValue>,
    @Suppress("UNUSED_PARAMETER") stats: List<com.fpl.tracker.data.models.FixtureStat>?,
    @Suppress("UNUSED_PARAMETER") players: List<Player>
) {
    // Simple row showing ⚽ count per side
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val homeText = if (homeGoals.isNotEmpty()) homeGoals.joinToString(", ") { "⚽ ×${it.value}" } else ""
        val awayText = if (awayGoals.isNotEmpty()) awayGoals.joinToString(", ") { "⚽ ×${it.value}" } else ""

        Text(homeText, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f))
        Text(awayText, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

// ─────────────────────────────────────────────────────────────
// Live pulsing dot
// ─────────────────────────────────────────────────────────────

@Composable
fun LiveDot(tint: Color = FplLiveRed) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_alpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = alpha))
    )
}

// ─────────────────────────────────────────────────────────────
// Fixture Detail Dialog – full match data
// ─────────────────────────────────────────────────────────────

@Composable
fun FixtureDetailDialog(
    fixture: Fixture,
    homeTeam: Team?,
    awayTeam: Team?,
    players: List<Player>,
    liveElements: List<LiveElement>,
    onDismiss: () -> Unit
) {
    val isLive = fixture.started == true && !fixture.finished && !fixture.finishedProvisional
    val isFinished = fixture.finished || fixture.finishedProvisional

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (isLive) {
                        Row(
                            Modifier.align(Alignment.CenterStart),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LiveDot(tint = FplGreen)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "LIVE · ${fixture.minutes}'",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FplGreen,
                                letterSpacing = 1.sp
                            )
                        }
                    } else {
                        Text(
                            text = if (isFinished) "Full Time" else "Match Details",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                }

                // Score section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                .background(
                    if (isLive)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
                        .padding(vertical = 20.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home team
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                homeTeam?.name ?: "Home",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.surface,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                            Text(
                                homeTeam?.shortName ?: "",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        // Score
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isLive || isFinished) {
                                Text(
                                    "${fixture.teamHScore ?: 0}  –  ${fixture.teamAScore ?: 0}",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isLive) FplLiveRed else MaterialTheme.colorScheme.surface
                                )
                            } else {
                                Text(
                                    "vs",
                                    fontSize = 28.sp,
                                    color = Color.Gray
                                )
                                fixture.kickoffTime?.let { kt ->
                                    val time = kt.substringAfter("T").take(5)
                                    val date = kt.substringBefore("T")
                                    Text(time, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.surface)
                                    Text(date, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }

                        // Away team
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                awayTeam?.name ?: "Away",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.surface,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                            Text(
                                awayTeam?.shortName ?: "",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Scrollable body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ── Match info ──────────────────────────────────────
                    SectionHeader("Match Info")
                    InfoCard {
                        InfoRow("Status", when {
                            isLive -> "🔴 Live (${fixture.minutes}')"
                            isFinished -> "✅ Finished"
                            else -> "⏳ Not started"
                        })
                        fixture.kickoffTime?.let { kt ->
                            InfoRow("Kickoff", kt.substringBefore("Z").replace("T", " "))
                        }
                        InfoRow("Home difficulty", "★".repeat(fixture.teamHDifficulty) + "☆".repeat(5 - fixture.teamHDifficulty))
                        InfoRow("Away difficulty", "★".repeat(fixture.teamADifficulty) + "☆".repeat(5 - fixture.teamADifficulty))
                    }

                    // ── Live player stats (only during live games) ──────
                    if (isLive && liveElements.isNotEmpty()) {
                        SectionHeader("🔴 Live Player Stats")
                        LivePlayerStatsSection(
                            fixture = fixture,
                            homeTeam = homeTeam,
                            awayTeam = awayTeam,
                            players = players,
                            liveElements = liveElements
                        )
                    }

                    // ── Fixture event stats ─────────────────────────────
                    val relevantStats = fixture.stats?.filter { stat ->
                        (stat.h.isNotEmpty() || stat.a.isNotEmpty()) &&
                                stat.identifier != "bps" // BPS is internal, skip unless wanted
                    } ?: emptyList()

                    if (relevantStats.isNotEmpty()) {
                        SectionHeader(if (isLive) "Match Events" else "Match Statistics")
                        relevantStats.forEach { stat ->
                            StatCard(
                                stat = stat,
                                homeTeamName = homeTeam?.shortName ?: "H",
                                awayTeamName = awayTeam?.shortName ?: "A",
                                players = players
                            )
                        }
                    } else if (isLive) {
                        InfoCard {
                            Text(
                                "Stats update as the game progresses…",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else if (isFinished) {
                        InfoCard {
                            Text(
                                "No detailed stats available for this match",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // ── Bonus points section ────────────────────────────
                    val bpsStat = fixture.stats?.find { it.identifier == "bps" }
                    val bonusStat = fixture.stats?.find { it.identifier == "bonus" }
                    if (bpsStat != null || bonusStat != null) {
                        SectionHeader("⭐ Bonus & BPS")
                        if (bonusStat != null && (bonusStat.h.isNotEmpty() || bonusStat.a.isNotEmpty())) {
                            StatCard(
                                stat = bonusStat,
                                homeTeamName = homeTeam?.shortName ?: "H",
                                awayTeamName = awayTeam?.shortName ?: "A",
                                players = players,
                                label = "Bonus Points"
                            )
                        }
                        if (bpsStat != null && (bpsStat.h.isNotEmpty() || bpsStat.a.isNotEmpty())) {
                            BpsCard(
                                stat = bpsStat,
                                homeTeamName = homeTeam?.shortName ?: "H",
                                awayTeamName = awayTeam?.shortName ?: "A",
                                players = players
                            )
                        }
                    }
                }

                // Bottom close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F8F8))
                        .padding(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Live player stats section
// ─────────────────────────────────────────────────────────────

@Composable
private fun LivePlayerStatsSection(
    fixture: Fixture,
    homeTeam: Team?,
    awayTeam: Team?,
    players: List<Player>,
    liveElements: List<LiveElement>
) {
    // Find which player IDs are in this fixture's teams
    val homePlayerIds = players.filter { it.team == fixture.teamH }.map { it.id }.toSet()
    val awayPlayerIds = players.filter { it.team == fixture.teamA }.map { it.id }.toSet()

    val homeLive = liveElements.filter { it.id in homePlayerIds && hasActivity(it) }
        .sortedByDescending { it.stats.totalPoints }
    val awayLive = liveElements.filter { it.id in awayPlayerIds && hasActivity(it) }
        .sortedByDescending { it.stats.totalPoints }

    if (homeLive.isEmpty() && awayLive.isEmpty()) {
        InfoCard {
            Text(
                "No live player data yet",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        return
    }

    // Header row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            homeTeam?.shortName ?: "Home",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.weight(1f)
        )
        Text(
            awayTeam?.shortName ?: "Away",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.surface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }

    val maxRows = maxOf(homeLive.size, awayLive.size)
    for (i in 0 until maxRows) {
        val homeEl = homeLive.getOrNull(i)
        val awayEl = awayLive.getOrNull(i)
        val homePlayer = homeEl?.let { el -> players.find { it.id == el.id } }
        val awayPlayer = awayEl?.let { el -> players.find { it.id == el.id } }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home player
            Column(modifier = Modifier.weight(1f)) {
                if (homePlayer != null && homeEl != null) {
                    Text(homePlayer.webName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        buildLiveStatsSummary(homeEl),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Away player
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                if (awayPlayer != null && awayEl != null) {
                    Text(
                        awayPlayer.webName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End
                    )
                    Text(
                        buildLiveStatsSummary(awayEl),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun hasActivity(el: LiveElement): Boolean {
    val s = el.stats
    return s.minutes > 0 || s.goalsScored > 0 || s.assists > 0 || s.saves > 0 ||
            s.bonus > 0 || s.yellowCards > 0 || s.redCards > 0 || s.cleanSheets > 0
}

private fun buildLiveStatsSummary(el: LiveElement): String {
    val s = el.stats
    val parts = mutableListOf<String>()
    if (s.goalsScored > 0) parts += "⚽ ${s.goalsScored}"
    if (s.assists > 0) parts += "🅰️ ${s.assists}"
    if (s.saves > 0) parts += "🧤 ${s.saves}"
    if (s.cleanSheets > 0) parts += "🛡️ CS"
    if (s.yellowCards > 0) parts += "🟨"
    if (s.redCards > 0) parts += "🟥"
    if (s.bonus > 0) parts += "⭐ ${s.bonus}"
    if (s.minutes > 0) parts += "${s.minutes}'"
    return if (parts.isEmpty()) "—" else parts.joinToString("  ")
}

// ─────────────────────────────────────────────────────────────
// Stat card (goals, assists, cards, etc.)
// ─────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    stat: com.fpl.tracker.data.models.FixtureStat,
    homeTeamName: String,
    awayTeamName: String,
    players: List<Player>,
    label: String? = null
) {
    val statLabel = label ?: when (stat.identifier) {
        "goals_scored" -> "⚽ Goals"
        "assists" -> "🅰️ Assists"
        "own_goals" -> "Own Goals"
        "penalties_saved" -> "Penalties Saved"
        "penalties_missed" -> "Penalties Missed"
        "yellow_cards" -> "🟨 Yellow Cards"
        "red_cards" -> "🟥 Red Cards"
        "saves" -> "🧤 Saves"
        "bonus" -> "⭐ Bonus Points"
        else -> stat.identifier.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Stat type label + team headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    statLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.surface
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Home players
                Column(modifier = Modifier.weight(1f)) {
                    Text(homeTeamName, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    if (stat.h.isEmpty()) {
                        Text("—", fontSize = 12.sp, color = Color.LightGray)
                    } else {
                        stat.h.forEach { sv ->
                            val p = players.find { it.id == sv.element }
                            val name = p?.webName ?: "Player ${sv.element}"
                            val valStr = if (sv.value > 1) " ×${sv.value}" else ""
                            Text("$name$valStr", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Away players
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(awayTeamName, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                    Spacer(Modifier.height(4.dp))
                    if (stat.a.isEmpty()) {
                        Text("—", fontSize = 12.sp, color = Color.LightGray, textAlign = TextAlign.End)
                    } else {
                        stat.a.forEach { sv ->
                            val p = players.find { it.id == sv.element }
                            val name = p?.webName ?: "Player ${sv.element}"
                            val valStr = if (sv.value > 1) " ×${sv.value}" else ""
                            Text("$name$valStr", fontSize = 12.sp, color = Color.DarkGray, textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BpsCard(
    stat: com.fpl.tracker.data.models.FixtureStat,
    homeTeamName: String,
    awayTeamName: String,
    players: List<Player>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("BPS (Bonus Point System)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.surface)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(homeTeamName, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    val top = stat.h.sortedByDescending { it.value }.take(5)
                    if (top.isEmpty()) {
                        Text("—", fontSize = 12.sp, color = Color.LightGray)
                    } else {
                        top.forEach { sv ->
                            val name = players.find { it.id == sv.element }?.webName ?: "Player ${sv.element}"
                            Text("$name  ${sv.value}", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(awayTeamName, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                    Spacer(Modifier.height(4.dp))
                    val top = stat.a.sortedByDescending { it.value }.take(5)
                    if (top.isEmpty()) {
                        Text("—", fontSize = 12.sp, color = Color.LightGray, textAlign = TextAlign.End)
                    } else {
                        top.forEach { sv ->
                            val name = players.find { it.id == sv.element }?.webName ?: "Player ${sv.element}"
                            Text("$name  ${sv.value}", fontSize = 12.sp, color = Color.DarkGray, textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Reusable helpers
// ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), content = content)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF222222))
    }
}
