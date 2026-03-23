package com.fpl.tracker.ui.screens

import android.util.Log
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
import androidx.compose.ui.text.font.FontStyle
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
import com.fpl.tracker.data.models.FixtureStat
import com.fpl.tracker.data.models.FixtureStatValue
import com.fpl.tracker.data.models.LiveElement
import com.fpl.tracker.data.models.Player
import com.fpl.tracker.data.models.Team
import com.fpl.tracker.viewmodel.MatchesViewModel

// ─── Stitch design-system tokens ────────────────────────────────────────────
private val StitchBackground        = Color(0xFF131313)
private val StitchSurface           = Color(0xFF1C1B1B)
private val StitchSurfaceContainer  = Color(0xFF20201F)
private val StitchSurfaceHigh       = Color(0xFF2A2A2A)
private val StitchSurfaceHighest    = Color(0xFF353535)
private val StitchPrimary           = Color(0xFFA1D494)
private val StitchPrimaryContainer  = Color(0xFF2D5A27)
private val StitchOnPrimary         = Color(0xFF0A3909)
private val StitchSecondary         = Color(0xFFFFE083)
private val StitchTertiary          = Color(0xFFFFB3AD)
private val StitchTertiaryContainer = Color(0xFFA40217)
private val StitchOnSurface         = Color(0xFFE5E2E1)
private val StitchOnSurfaceVariant  = Color(0xFFC2C9BB)
private val StitchOutline           = Color(0xFF8C9387)
private val StitchOutlineVariant    = Color(0xFF42493E)

// Legacy aliases still used by FixtureCard live-dot
private val FplGreen   = StitchPrimary
private val FplLiveRed = StitchTertiaryContainer

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
    val liveCount = uiState.fixtures.count {
        it.started == true && !it.finished && !it.finishedProvisional
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBackground)
    ) {
        // ── Matchday Central Header ───────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = "MATCHDAY CENTRAL",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = StitchPrimary,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // GW selector tap area
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded.value,
                    onExpandedChange = { dropdownExpanded.value = !dropdownExpanded.value },
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .menuAnchor()
                            .clickable { dropdownExpanded.value = true }
                    ) {
                        Text(
                            text = selectedEvent?.name ?: "GW ${uiState.currentEvent}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StitchOnSurface,
                            letterSpacing = (-1).sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            tint = StitchSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded.value,
                        onDismissRequest = { dropdownExpanded.value = false },
                        containerColor = StitchSurfaceHigh
                    ) {
                        events.forEach { event ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        event.name,
                                        color = StitchOnSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                onClick = {
                                    dropdownExpanded.value = false
                                    viewModel.selectGameweek(event.id)
                                }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (liveCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(StitchSurface)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LiveDot(tint = StitchTertiary)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "$liveCount Live",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchOnSurface,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = { viewModel.refreshFixtures() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = StitchOutline
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.3f))
        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = StitchPrimary
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
                            color = StitchPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "",
                            fontSize = 13.sp,
                            color = StitchOnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.refreshFixtures() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StitchPrimary
                            )
                        ) {
                            Text("Retry", color = StitchOnPrimary)
                        }
                    }
                }
                uiState.fixtures.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp, end = 16.dp, bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.fixtures) { fixture ->
                            FixtureCard(
                                fixture = fixture,
                                homeTeam = uiState.teams.find { it.id == fixture.teamH },
                                awayTeam = uiState.teams.find { it.id == fixture.teamA },
                                liveElements = uiState.liveElements,
                                players = uiState.players,
                                onClick = { selectedFixture = fixture }
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        "No fixtures found",
                        modifier = Modifier.align(Alignment.Center),
                        color = StitchOnSurfaceVariant
                    )
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
    players: List<Player> = emptyList(),
    onClick: () -> Unit
) {
    val isLive = fixture.started == true && !fixture.finished && !fixture.finishedProvisional
    val isFinished = fixture.finished || fixture.finishedProvisional
    val isHalfTime = isLive && fixture.minutes == 45

    val homeScore = fixture.teamHScore
    val awayScore = fixture.teamAScore

    // Goal scorers for footer
    val goalsStat = fixture.stats?.find { it.identifier == "goals_scored" }
    val ownGoalsStat = fixture.stats?.find { it.identifier == "own_goals" }
    val homeGoalEntries = if (isLive || isFinished) buildGoalEntries(
        scoringEntries = goalsStat?.h.orEmpty(),
        ownGoalEntries = ownGoalsStat?.a.orEmpty(),
        scoringTeamShort = homeTeam?.shortName ?: "H",
        ownGoalTeamShort = awayTeam?.shortName ?: "A",
        players = players
    ) else emptyList()
    val awayGoalEntries = if (isLive || isFinished) buildGoalEntries(
        scoringEntries = goalsStat?.a.orEmpty(),
        ownGoalEntries = ownGoalsStat?.h.orEmpty(),
        scoringTeamShort = awayTeam?.shortName ?: "A",
        ownGoalTeamShort = homeTeam?.shortName ?: "H",
        players = players
    ) else emptyList()
    val hasGoals = homeGoalEntries.isNotEmpty() || awayGoalEntries.isNotEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StitchSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Top gradient accent bar (stadium-gradient style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = when {
                                isLive     -> listOf(StitchTertiary, StitchTertiaryContainer)
                                isFinished -> listOf(StitchPrimary, StitchPrimaryContainer)
                                else       -> listOf(StitchOutlineVariant, StitchOutlineVariant)
                            }
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 3.dp) // account for accent bar
            ) {
                // ── Status row ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when {
                            isHalfTime -> {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = StitchSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "HALF TIME",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchSecondary,
                                    letterSpacing = 2.sp
                                )
                            }
                            isLive -> {
                                LiveDot(tint = StitchTertiary)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "LIVE - ${fixture.minutes}'",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchTertiary,
                                    letterSpacing = 2.sp
                                )
                            }
                            isFinished -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StitchOutlineVariant)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "FINAL RESULT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchOnSurfaceVariant.copy(alpha = 0.6f),
                                    letterSpacing = 2.sp
                                )
                            }
                            else -> {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = StitchSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                fixture.kickoffTime?.let { kt ->
                                    val time = kt.substringAfter("T").take(5)
                                    Text(
                                        text = "TODAY $time",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StitchSecondary,
                                        letterSpacing = 2.sp
                                    )
                                } ?: Text(
                                    text = "UPCOMING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchSecondary,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }

                // ── Teams + Score Row ───────────────────────────────────
                if (isLive || isFinished) {
                    // Compact layout with score in center and names inline
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 0.dp)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(5f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StitchSurfaceHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = homeTeam?.shortName?.take(3) ?: "H",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchOnSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = homeTeam?.shortName ?: "Home",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Score
                        Row(
                            modifier = Modifier.weight(3f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${homeScore ?: 0}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLive) StitchPrimary else StitchOnSurface,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = "  -  ",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOutlineVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "${awayScore ?: 0}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLive) StitchPrimary else StitchOnSurface,
                                letterSpacing = (-1).sp
                            )
                        }
                        // Away
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.weight(5f)
                        ) {
                            Text(
                                text = awayTeam?.shortName ?: "Away",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StitchSurfaceHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = awayTeam?.shortName?.take(3) ?: "A",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchOnSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Upcoming: big circular crests, VS in center
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 0.dp)
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(StitchSurfaceHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = homeTeam?.shortName?.take(3) ?: "H",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchOnSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = homeTeam?.shortName ?: "Home",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOnSurface
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "VS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOutlineVariant,
                                letterSpacing = 4.sp
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(StitchSurfaceHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = awayTeam?.shortName?.take(3) ?: "A",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = StitchOnSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = awayTeam?.shortName ?: "Away",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOnSurface
                            )
                        }
                    }
                }

                // ── Goal scorers footer ─────────────────────────────────
                if (hasGoals) {
                    HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Home scorers
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            homeGoalEntries.forEach { entry ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚽", fontSize = 11.sp)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = buildString {
                                            append(entry.playerName)
                                            if (entry.count > 1) append(" ×${entry.count}")
                                            if (entry.isOwnGoal) append(" (og)")
                                        },
                                        fontSize = 11.sp,
                                        color = StitchOnSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        // Away scorers (right aligned)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            awayGoalEntries.forEach { entry ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = buildString {
                                            append(entry.playerName)
                                            if (entry.count > 1) append(" ×${entry.count}")
                                            if (entry.isOwnGoal) append(" (og)")
                                        },
                                        fontSize = 11.sp,
                                        color = StitchOnSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("⚽", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
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
    val isLive     = fixture.started == true && !fixture.finished && !fixture.finishedProvisional
    val isFinished = fixture.finished || fixture.finishedProvisional

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.95f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StitchBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Top App Bar  ─────────────────────────────────────────
                // "PITCH-SIDE GALLERY" with back/close + share
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131313))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Back/close circle button
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StitchSurfaceHighest)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = StitchPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // Title
                    Text(
                        text = "PITCH-SIDE GALLERY",
                        modifier = Modifier.align(Alignment.Center),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchPrimary,
                        letterSpacing = 1.sp
                    )
                    // LIVE badge or share icon placeholder
                    if (isLive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clip(RoundedCornerShape(50))
                                .background(StitchTertiaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                LiveDot(tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "${fixture.minutes}'",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                // ── Scoreboard Hero ──────────────────────────────────────
                // Card with blurred blob, huge score, team badges
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StitchSurface)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    // Status row (Finished / Live dot)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Status pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isLive     -> StitchTertiaryContainer
                                            isFinished -> StitchSecondary
                                            else       -> StitchOutline
                                        }
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isLive     -> "LIVE"
                                    isFinished -> "FINISHED"
                                    else       -> "UPCOMING"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isLive     -> StitchTertiary
                                    isFinished -> StitchSecondary
                                    else       -> StitchOutline
                                },
                                letterSpacing = 3.sp
                            )
                        }

                        // Teams + Score row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Home team
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Team badge placeholder
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(StitchSurfaceContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        homeTeam?.shortName?.take(3) ?: "H",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StitchOnSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = homeTeam?.shortName?.uppercase() ?: "HOME",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontStyle = FontStyle.Italic,
                                    color = StitchOnSurface,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(StitchSurfaceHighest)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "Diff: ${fixture.teamHDifficulty}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = fdrColor(fixture.teamHDifficulty),
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            // Score
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                if (isLive || isFinished) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${fixture.teamHScore ?: 0}",
                                            fontSize = 64.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = StitchOnSurface,
                                            letterSpacing = (-2).sp
                                        )
                                        Text(
                                            ":",
                                            fontSize = 40.sp,
                                            color = StitchOutlineVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        Text(
                                            "${fixture.teamAScore ?: 0}",
                                            fontSize = 64.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = StitchOnSurface,
                                            letterSpacing = (-2).sp
                                        )
                                    }
                                } else {
                                    Text(
                                        "VS",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StitchOutline
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = fixture.kickoffTime
                                        ?.substringBefore("Z")
                                        ?.replace("T", " • ")
                                        ?: "",
                                    fontSize = 10.sp,
                                    color = StitchOutline,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Away team
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(StitchSurfaceContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        awayTeam?.shortName?.take(3) ?: "A",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = StitchOnSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = awayTeam?.shortName?.uppercase() ?: "AWAY",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontStyle = FontStyle.Italic,
                                    color = StitchOnSurface,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(StitchSurfaceHighest)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "Diff: ${fixture.teamADifficulty}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = fdrColor(fixture.teamADifficulty),
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.4f))

                // ── Scrollable body ──────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── Live player stats ────────────────────────────────
                    if (isLive && liveElements.isNotEmpty()) {
                        MatchReportBentoCard(
                            icon = "⚡",
                            iconColor = StitchTertiary,
                            title = "Live Player Stats"
                        ) {
                            LivePlayerStatsSection(
                                fixture = fixture,
                                homeTeam = homeTeam,
                                awayTeam = awayTeam,
                                players = players,
                                liveElements = liveElements
                            )
                        }
                    }

                    // ── Bento grid of stat cards ─────────────────────────
                    val allStats = fixture.stats ?: emptyList()

                    // Goals
                    val goalsStat = allStats.find { it.identifier == "goals_scored" }
                    val ownGoalsStat = allStats.find { it.identifier == "own_goals" }
                    val homeGoalEntries = buildGoalEntries(
                        scoringEntries = goalsStat?.h.orEmpty(),
                        ownGoalEntries = ownGoalsStat?.a.orEmpty(),
                        scoringTeamShort = homeTeam?.shortName ?: "H",
                        ownGoalTeamShort = awayTeam?.shortName ?: "A",
                        players = players
                    )
                    val awayGoalEntries = buildGoalEntries(
                        scoringEntries = goalsStat?.a.orEmpty(),
                        ownGoalEntries = ownGoalsStat?.h.orEmpty(),
                        scoringTeamShort = awayTeam?.shortName ?: "A",
                        ownGoalTeamShort = homeTeam?.shortName ?: "H",
                        players = players
                    )
                    if (homeGoalEntries.isNotEmpty() || awayGoalEntries.isNotEmpty()) {
                        MatchReportBentoCard(
                            icon = "⚽",
                            iconColor = StitchPrimary,
                            title = "Goals",
                            count = (homeGoalEntries.sumOf { it.count } + awayGoalEntries.sumOf { it.count }).toString()
                        ) {
                            GoalEventPlayerList(
                                homeEntries = homeGoalEntries,
                                awayEntries = awayGoalEntries,
                                homeShort = homeTeam?.shortName ?: "H",
                                awayShort = awayTeam?.shortName ?: "A"
                            )
                        }
                    }

                    // Assists
                    val assistsStat = allStats.find { it.identifier == "assists" }
                    if (assistsStat != null && (assistsStat.h.isNotEmpty() || assistsStat.a.isNotEmpty())) {
                        MatchReportBentoCard(
                            icon = "🔗",
                            iconColor = Color(0xFF9DD090),
                            title = "Assists"
                        ) {
                            MatchStatPlayerList(
                                homePlayers = assistsStat.h,
                                awayPlayers = assistsStat.a,
                                allPlayers = players,
                                homeShort = homeTeam?.shortName ?: "H",
                                awayShort = awayTeam?.shortName ?: "A"
                            )
                        }
                    }

                    // Saves
                    val savesStat = allStats.find { it.identifier == "saves" }
                    if (savesStat != null && (savesStat.h.isNotEmpty() || savesStat.a.isNotEmpty())) {
                        MatchReportBentoCard(
                            icon = "🧤",
                            iconColor = StitchSecondary,
                            title = "Saves"
                        ) {
                            MatchStatPlayerList(
                                homePlayers = savesStat.h,
                                awayPlayers = savesStat.a,
                                allPlayers = players,
                                homeShort = homeTeam?.shortName ?: "H",
                                awayShort = awayTeam?.shortName ?: "A",
                                showCount = true
                            )
                        }
                    }

                    // Yellow cards
                    val yellowStat = allStats.find { it.identifier == "yellow_cards" }
                    if (yellowStat != null && (yellowStat.h.isNotEmpty() || yellowStat.a.isNotEmpty())) {
                        MatchReportBentoCard(
                            icon = "🟨",
                            iconColor = StitchSecondary,
                            title = "Yellow Cards"
                        ) {
                            MatchStatPlayerList(
                                homePlayers = yellowStat.h,
                                awayPlayers = yellowStat.a,
                                allPlayers = players,
                                homeShort = homeTeam?.shortName ?: "H",
                                awayShort = awayTeam?.shortName ?: "A"
                            )
                        }
                    }

                    // Red cards
                    val redStat = allStats.find { it.identifier == "red_cards" }
                    if (redStat != null && (redStat.h.isNotEmpty() || redStat.a.isNotEmpty())) {
                        MatchReportBentoCard(
                            icon = "🟥",
                            iconColor = StitchTertiaryContainer,
                            title = "Red Cards"
                        ) {
                            MatchStatPlayerList(
                                homePlayers = redStat.h,
                                awayPlayers = redStat.a,
                                allPlayers = players,
                                homeShort = homeTeam?.shortName ?: "H",
                                awayShort = awayTeam?.shortName ?: "A"
                            )
                        }
                    }

                    // Bonus Points
                    val bonusStat = allStats.find { it.identifier == "bonus" }
                    if (bonusStat != null && (bonusStat.h.isNotEmpty() || bonusStat.a.isNotEmpty())) {
                        MatchReportBentoCard(
                            icon = "🏅",
                            iconColor = StitchPrimary,
                            title = "Bonus Points"
                        ) {
                            MatchStatPlayerList(
                                homePlayers = bonusStat.h,
                                awayPlayers = bonusStat.a,
                                allPlayers = players,
                                homeShort = homeTeam?.shortName ?: "H",
                                awayShort = awayTeam?.shortName ?: "A",
                                showCount = true
                            )
                        }
                    }

                    // Defensive Contributions
                    val defensiveContributionsStat = allStats.firstOrNull {
                        it.identifier.contains("def", ignoreCase = true) ||
                            it.identifier == "defensive_contributions" ||
                            it.identifier == "defensive" ||
                            it.identifier == "def_contributions"
                    }

                    val defensiveContributionEntries = buildDefensiveContributionEntries(
                        fixtureId = fixture.id,
                        fixtureStat = defensiveContributionsStat,
                        players = players,
                        liveElements = liveElements,
                        homeTeamId = fixture.teamH,
                        awayTeamId = fixture.teamA
                    )
                    defensiveContributionEntries.away.forEach {

                        Log.d("rronirroni", "$defensiveContributionsStat")
                    }
                    if (defensiveContributionEntries.home.isNotEmpty() || defensiveContributionEntries.away.isNotEmpty()) {
                        DefensiveContributionCard(
                            homeEntries = defensiveContributionEntries.home,
                            awayEntries = defensiveContributionEntries.away,
                            homeTeamName = homeTeam?.name ?: "Home",
                            awayTeamName = awayTeam?.name ?: "Away"
                        )
                    }

                    // BPS Table
                    val bpsStat = allStats.find { it.identifier == "bps" }
                    if (bpsStat != null && (bpsStat.h.isNotEmpty() || bpsStat.a.isNotEmpty())) {
                        BpsTableCard(
                            stat = bpsStat,
                            homeTeamShort = homeTeam?.shortName ?: "H",
                            awayTeamShort = awayTeam?.shortName ?: "A",
                            players = players
                        )
                    }

                    // No stats fallback
                    if (allStats.isEmpty() && !isLive) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(StitchSurfaceHigh)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFinished) "No detailed stats available" else "Stats appear once the match starts",
                                fontSize = 13.sp,
                                color = StitchOutline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── Bento card wrapper  ───────────────────────────────────────────────────────
@Composable
private fun MatchReportBentoCard(
    icon: String,
    iconColor: Color,
    title: String,
    count: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StitchSurfaceContainer)
            .padding(16.dp)
    ) {
        // Card header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    title.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    color = iconColor,
                    letterSpacing = 1.sp
                )
            }
            if (count != null) {
                Text(
                    count,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = iconColor,
                    letterSpacing = (-1).sp
                )
            }
        }
        HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 10.dp))
        content()
    }
}

// ── Two-column player list inside a bento card ────────────────────────────────
@Composable
private fun MatchStatPlayerList(
    homePlayers: List<FixtureStatValue>,
    awayPlayers: List<FixtureStatValue>,
    allPlayers: List<Player>,
    homeShort: String,
    awayShort: String,
    showMinute: Boolean = false,
    showCount: Boolean = false
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Home column
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (homePlayers.isEmpty()) {
                Text("—", fontSize = 12.sp, color = StitchOutlineVariant)
            } else {
                homePlayers.forEach { sv ->
                    val name = allPlayers.find { it.id == sv.element }?.webName ?: "P${sv.element}"
                    Column {
                        Text(
                            name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface
                        )
                        Text(
                            buildString {
                                append(homeShort)
                                if (showCount && sv.value > 1) append(" ×${sv.value}")
                            },
                            fontSize = 10.sp,
                            color = StitchOutline,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        // Away column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (awayPlayers.isEmpty()) {
                Text("—", fontSize = 12.sp, color = StitchOutlineVariant, textAlign = TextAlign.End)
            } else {
                awayPlayers.forEach { sv ->
                    val name = allPlayers.find { it.id == sv.element }?.webName ?: "P${sv.element}"
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface,
                            textAlign = TextAlign.End
                        )
                        Text(
                            buildString {
                                append(awayShort)
                                if (showCount && sv.value > 1) append(" ×${sv.value}")
                            },
                            fontSize = 10.sp,
                            color = StitchOutline,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

private data class GoalEventEntry(
    val playerName: String,
    val teamShort: String,
    val count: Int,
    val isOwnGoal: Boolean
)

private data class DefensiveContributionEntry(
    val playerName: String,
    val value: Int
)

private data class DefensiveContributionEntries(
    val home: List<DefensiveContributionEntry>,
    val away: List<DefensiveContributionEntry>
)

private fun buildGoalEntries(
    scoringEntries: List<FixtureStatValue>,
    ownGoalEntries: List<FixtureStatValue>,
    scoringTeamShort: String,
    ownGoalTeamShort: String,
    players: List<Player>
): List<GoalEventEntry> {
    val scored = scoringEntries.map { entry ->
        GoalEventEntry(
            playerName = players.find { it.id == entry.element }?.webName ?: "P${entry.element}",
            teamShort = scoringTeamShort,
            count = entry.value,
            isOwnGoal = false
        )
    }
    val ownGoals = ownGoalEntries.map { entry ->
        GoalEventEntry(
            playerName = players.find { it.id == entry.element }?.webName ?: "P${entry.element}",
            teamShort = ownGoalTeamShort,
            count = entry.value,
            isOwnGoal = true
        )
    }
    return scored + ownGoals
}

private fun  buildDefensiveContributionEntries(
    fixtureId: Int,
    fixtureStat: FixtureStat?,
    players: List<Player>,
    liveElements: List<LiveElement>,
    homeTeamId: Int,
    awayTeamId: Int
): DefensiveContributionEntries {
    val playersById = players.associateBy { it.id }
    val fromLive = liveElements.mapNotNull { liveElement ->
        val player = playersById[liveElement.id] ?: return@mapNotNull null
        val defensiveValue = liveElement.explain
            ?.firstOrNull { it.fixture == fixtureId }
            ?.stats
            ?.firstOrNull {
                it.identifier.contains("def", ignoreCase = true) ||
                    it.identifier == "defensive_contributions" ||
                    it.identifier == "defensive" ||
                    it.identifier == "def_contributions"
            }
            ?.value
            ?: return@mapNotNull null

        when (player.team) {
            homeTeamId -> player.team to DefensiveContributionEntry(player.webName, defensiveValue)
            awayTeamId -> player.team to DefensiveContributionEntry(player.webName, defensiveValue)
            else -> null
        }
    }

    val liveEntriesHome = fromLive.filter { it.first == homeTeamId }.map { it.second }
    val liveEntriesAway = fromLive.filter { it.first == awayTeamId }.map { it.second }

    fun mapFixtureEntries(entries: List<FixtureStatValue>): List<DefensiveContributionEntry> =
        entries.map { entry ->
            DefensiveContributionEntry(
                playerName = playersById[entry.element]?.webName ?: "P${entry.element}",
                value = entry.value
            )
        }

    val fixtureEntriesHome = mapFixtureEntries(fixtureStat?.h.orEmpty())
    val fixtureEntriesAway = mapFixtureEntries(fixtureStat?.a.orEmpty())

    // Merge live and fixture entries, preferring live if a player exists in both
    val combinedHome = (liveEntriesHome + fixtureEntriesHome).distinctBy { it.playerName }
        .filter { it.value > 0 }.sortedByDescending { it.value }
    val combinedAway = (liveEntriesAway + fixtureEntriesAway).distinctBy { it.playerName }
        .filter { it.value > 0 }.sortedByDescending { it.value }

    return DefensiveContributionEntries(
        home = combinedHome,
        away = combinedAway
    )
}

@Composable
private fun GoalEventPlayerList(
    homeEntries: List<GoalEventEntry>,
    awayEntries: List<GoalEventEntry>,
    homeShort: String,
    awayShort: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        GoalEventColumn(
            modifier = Modifier.weight(1f),
            entries = homeEntries,
            fallbackLabel = homeShort,
            horizontalAlignment = Alignment.Start,
            textAlign = TextAlign.Start
        )
        Spacer(Modifier.width(8.dp))
        GoalEventColumn(
            modifier = Modifier.weight(1f),
            entries = awayEntries,
            fallbackLabel = awayShort,
            horizontalAlignment = Alignment.End,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun GoalEventColumn(
    entries: List<GoalEventEntry>,
    fallbackLabel: String,
    horizontalAlignment: Alignment.Horizontal,
    textAlign: TextAlign,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = horizontalAlignment
    ) {
        if (entries.isEmpty()) {
            Text("—", fontSize = 12.sp, color = StitchOutlineVariant, textAlign = textAlign)
        } else {
            entries.forEach { entry ->
                Column(horizontalAlignment = horizontalAlignment) {
                    Text(
                        entry.playerName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface,
                        textAlign = textAlign
                    )
                    Text(
                        buildString {
                            append(entry.teamShort.ifBlank { fallbackLabel })
                            if (entry.isOwnGoal) append(" • Own Goal")
                            if (entry.count > 1) append(" ×${entry.count}")
                        },
                        fontSize = 10.sp,
                        color = if (entry.isOwnGoal) StitchTertiary else StitchOutline,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = textAlign
                    )
                }
            }
        }
    }
}

// ── BPS Table card ────────────────────────────────────────────────────────────
@Composable
private fun BpsTableCard(
    stat: FixtureStat,
    homeTeamShort: String,
    awayTeamShort: String,
    players: List<Player>
) {
    val allEntries = (stat.h.map { it to homeTeamShort } + stat.a.map { it to awayTeamShort })
        .sortedByDescending { it.first.value }
        .take(8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StitchSurfaceContainer)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "BONUS POINT SYSTEM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    color = StitchPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    "BPS Breakdown & Advanced Metrics",
                    fontSize = 9.sp,
                    color = StitchOutline,
                    letterSpacing = 0.5.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(StitchPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📊", fontSize = 16.sp)
            }
        }
        HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 10.dp))
        // Table header
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            Text("PLAYER", modifier = Modifier.weight(1.8f), fontSize = 9.sp, color = StitchOutline, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Text("TEAM",   modifier = Modifier.weight(0.8f), fontSize = 9.sp, color = StitchOutline, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            Text("BPS",    modifier = Modifier.weight(0.8f), fontSize = 9.sp, color = StitchOutline, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
        HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 4.dp))
        // Table rows
        allEntries.forEachIndexed { idx, (sv, team) ->
            val name = players.find { it.id == sv.element }?.webName ?: "P${sv.element}"
            val rowBg = if (idx % 2 == 0) Color.Transparent else StitchSurfaceHigh.copy(alpha = 0.4f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(rowBg)
                    .padding(vertical = 5.dp)
            ) {
                Text(
                    name,
                    modifier = Modifier.weight(1.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface
                )
                Text(
                    team,
                    modifier = Modifier.weight(0.8f),
                    fontSize = 11.sp,
                    color = StitchOutline,
                    textAlign = TextAlign.End
                )
                Text(
                    sv.value.toString(),
                    modifier = Modifier.weight(0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    color = StitchSecondary,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun DefensiveContributionCard(
    homeEntries: List<DefensiveContributionEntry>,
    awayEntries: List<DefensiveContributionEntry>,
    homeTeamName: String,
    awayTeamName: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StitchSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StitchSurfaceContainer)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "🛡 Defensive Contribution",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    color = StitchOnSurface
                )
                Text(
                    text = "Total recoveries & clearances",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchOutline,
                    letterSpacing = 1.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            DefensiveContributionColumn(
                modifier = Modifier.weight(1f),
                teamName = homeTeamName,
                accentColor = StitchPrimary,
                entries = homeEntries
            )
            DefensiveContributionColumn(
                modifier = Modifier.weight(1f),
                teamName = awayTeamName,
                accentColor = StitchTertiary,
                entries = awayEntries
            )
        }
    }
}

@Composable
private fun DefensiveContributionColumn(
    teamName: String,
    accentColor: Color,
    entries: List<DefensiveContributionEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(StitchSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = teamName.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            letterSpacing = 1.5.sp
        )
        if (entries.isEmpty()) {
            Text("—", fontSize = 12.sp, color = StitchOutlineVariant)
        } else {
            entries.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = entry.playerName,
                        fontSize = 18.sp,
                        color = StitchOnSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = entry.value.toString().padStart(2, '0'),
                        fontSize = 28.sp,
                        color = StitchOnSurfaceVariant.copy(alpha = 0.35f),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                HorizontalDivider(color = StitchOutlineVariant.copy(alpha = 0.2f))
            }
        }
    }
}

// Small coloured difficulty dots (FDR 1–5)
@Composable
private fun DifficultyDots(fdr: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (i in 1..5) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (i <= fdr) fdrColor(fdr) else StitchOutlineVariant)
            )
        }
    }
}

private fun fdrColor(fdr: Int): Color = when (fdr) {
    1, 2 -> Color(0xFF47D185)
    3    -> Color(0xFFFFE083)
    4    -> Color(0xFFFFB3AD)
    else -> Color(0xFFA40217)
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

    // Column headers
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = (homeTeam?.shortName ?: "Home").uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = StitchOutline,
            letterSpacing = 2.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = (awayTeam?.shortName ?: "Away").uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = StitchOutline,
            letterSpacing = 2.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }

    val maxRows = maxOf(homeLive.size, awayLive.size)
    for (i in 0 until maxRows) {
        val homeEl     = homeLive.getOrNull(i)
        val awayEl     = awayLive.getOrNull(i)
        val homePlayer = homeEl?.let { el -> players.find { it.id == el.id } }
        val awayPlayer = awayEl?.let { el -> players.find { it.id == el.id } }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home player
            Column(modifier = Modifier.weight(1f)) {
                if (homePlayer != null && homeEl != null) {
                    Text(
                        homePlayer.webName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchOnSurface
                    )
                    Text(
                        buildLiveStatsSummary(homeEl),
                        fontSize = 11.sp,
                        color = StitchOutline,
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
                        color = StitchOnSurface,
                        textAlign = TextAlign.End
                    )
                    Text(
                        buildLiveStatsSummary(awayEl),
                        fontSize = 11.sp,
                        color = StitchOutline,
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
    stat: FixtureStat,
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

    // accent colour for the left border based on stat type
    val accentColor = when {
        stat.identifier.contains("goal")   -> StitchPrimary
        stat.identifier.contains("assist") -> StitchSecondary
        stat.identifier.contains("card")   -> StitchTertiary
        stat.identifier.contains("bonus")  -> StitchSecondary
        else                               -> StitchOutlineVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StitchSurfaceHigh)
    ) {
        // Left accent stripe
        Box(
            modifier = Modifier
                .width(3.dp)
                .matchParentSize()
                .background(accentColor)
        )
        Column(modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)) {
            Text(
                statLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOutline,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Home
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        homeTeamName.uppercase(),
                        fontSize = 10.sp,
                        color = StitchOutline,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    if (stat.h.isEmpty()) {
                        Text("—", fontSize = 12.sp, color = StitchOutlineVariant)
                    } else {
                        stat.h.forEach { sv ->
                            val name = players.find { it.id == sv.element }?.webName ?: "Player ${sv.element}"
                            val valStr = if (sv.value > 1) " ×${sv.value}" else ""
                            Text(
                                "$name$valStr",
                                fontSize = 12.sp,
                                color = StitchOnSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                // Away
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        awayTeamName.uppercase(),
                        fontSize = 10.sp,
                        color = StitchOutline,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.height(4.dp))
                    if (stat.a.isEmpty()) {
                        Text("—", fontSize = 12.sp, color = StitchOutlineVariant, textAlign = TextAlign.End)
                    } else {
                        stat.a.forEach { sv ->
                            val name = players.find { it.id == sv.element }?.webName ?: "Player ${sv.element}"
                            val valStr = if (sv.value > 1) " ×${sv.value}" else ""
                            Text(
                                "$name$valStr",
                                fontSize = 12.sp,
                                color = StitchOnSurface,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BpsCard(
    stat: FixtureStat,
    homeTeamName: String,
    awayTeamName: String,
    players: List<Player>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StitchSurfaceHigh)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .matchParentSize()
                .background(StitchSecondary)
        )
        Column(modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)) {
            Text(
                "BPS (BONUS POINT SYSTEM)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOutline,
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        homeTeamName.uppercase(),
                        fontSize = 10.sp,
                        color = StitchOutline,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    val top = stat.h.sortedByDescending { it.value }.take(5)
                    if (top.isEmpty()) {
                        Text("—", fontSize = 12.sp, color = StitchOutlineVariant)
                    } else {
                        top.forEach { sv ->
                            val name = players.find { it.id == sv.element }?.webName ?: "Player ${sv.element}"
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Text(name, fontSize = 12.sp, color = StitchOnSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(sv.value.toString(), fontSize = 12.sp, color = StitchSecondary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        awayTeamName.uppercase(),
                        fontSize = 10.sp,
                        color = StitchOutline,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.height(4.dp))
                    val top = stat.a.sortedByDescending { it.value }.take(5)
                    if (top.isEmpty()) {
                        Text("—", fontSize = 12.sp, color = StitchOutlineVariant, textAlign = TextAlign.End)
                    } else {
                        top.forEach { sv ->
                            val name = players.find { it.id == sv.element }?.webName ?: "Player ${sv.element}"
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(sv.value.toString(), fontSize = 12.sp, color = StitchSecondary, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Text(name, fontSize = 12.sp, color = StitchOnSurface, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
                            }
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

// ─── Legacy aliases (kept for backward compat) ────────────────────────────────
@Composable
private fun SectionHeader(title: String) = StitchSectionHeader(title)

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) = StitchInfoCard(content)

// ─── Stitch-styled helpers ────────────────────────────────────────────────────

@Composable
private fun StitchSectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(StitchPrimary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = StitchOutline,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun StitchInfoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StitchSurfaceHigh)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun StitchInfoRow(
    label: String,
    value: String,
    valueColor: Color = StitchOnSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = StitchOutline,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = valueColor
        )
    }
}

// Needed by the external InfoRow call in MatchesScreen
@Composable
fun InfoRow(label: String, value: String, valueColor: Color = StitchOnSurface) =
    StitchInfoRow(label, value, valueColor)
