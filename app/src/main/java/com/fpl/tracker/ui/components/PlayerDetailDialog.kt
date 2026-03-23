package com.fpl.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fpl.tracker.data.models.*
import com.fpl.tracker.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDetailDialog(
    player: Player,
    team: Team,
    playerDetail: PlayerDetailResponse?,
    leagueStats: LeaguePlayerStats?,
    bootstrapData: BootstrapData?,
    currentEvent: Int,
    liveStats: LiveElement? = null,
    fixture: Fixture? = null,
    isLoadingLeagueStats: Boolean = false,
    leagueStatsError: String? = null,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Summary",
        if (leagueStats != null) "Starts (${leagueStats.startsCount})" else "Starts",
        if (leagueStats != null) "Bench (${leagueStats.benchCount})" else "Bench"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceContainerLow1C1B1B
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceContainerHigh2A2A2A)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        getTeamColor(team.id),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = team.shortName,
                                    color = getTeamTextColor(team.id),
                                    fontSize = 10.sp,
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = player.webName,
                                    color = OnSurfaceE5E2E1,
                                    fontSize = 20.sp,
                                    fontFamily = SpaceGrotesk,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${team.shortName} · ${getPositionName(player.elementType)}",
                                    color = OnSurfaceVariantC2C9BB,
                                    fontSize = 12.sp,
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Selected league · GW$currentEvent",
                                    color = Outline8C9387,
                                    fontSize = 11.sp,
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(SurfaceContainerHighest353535, androidx.compose.foundation.shape.CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                "Close",
                                tint = OnSurfaceVariantC2C9BB,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = SurfaceContainer20201F,
                    contentColor = PrimaryA1D494,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = PrimaryA1D494
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    title,
                                    fontFamily = Manrope,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (selectedTabIndex == index) PrimaryA1D494 else Outline8C9387
                                )
                            }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTabIndex) {
                        0 -> SummaryTab(
                            player,
                            team,
                            playerDetail,
                            leagueStats,
                            bootstrapData,
                            currentEvent,
                            liveStats,
                            fixture
                        )
                        1 -> StartsTab(leagueStats, isLoadingLeagueStats, leagueStatsError)
                        2 -> BenchTab(leagueStats, isLoadingLeagueStats, leagueStatsError)
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryContainer2D5A27,
                        contentColor = PrimaryA1D494
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "CLOSE",
                        fontSize = 14.sp,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryTab(
    player: Player,
    team: Team,
    playerDetail: PlayerDetailResponse?,
    leagueStats: LeaguePlayerStats?,
    bootstrapData: BootstrapData?,
    currentEvent: Int,
    liveStats: LiveElement? = null,
    fixture: Fixture? = null,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PrimaryContainer2D5A27.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    "League stats on this dialog are for the selected league and GW$currentEvent only.",
                    modifier = Modifier.padding(14.dp),
                    color = PrimaryA1D494,
                    fontSize = 11.sp,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        item {
            liveStats?.explain?.let { explains ->
                android.util.Log.d("PlayerDialog", "=== EXPLAIN STATS FOR ${player.webName} ===")
                explains.forEach { explain ->
                    android.util.Log.d("PlayerDialog", "Fixture ${explain.fixture}:")
                    explain.stats.forEach { stat ->
                        android.util.Log.d("PlayerDialog", "  - identifier: ${stat.identifier}, value: ${stat.value}, points: ${stat.points}")
                    }
                }
            }
        }

        item {
            liveStats?.explain?.let { explains ->
                explains.forEach { explain ->
                    explain.stats.forEach { stat ->
                        if (stat.identifier.contains("def", ignoreCase = true) ||
                            stat.identifier == "defensive_contributions" ||
                            stat.identifier == "defensive" ||
                            stat.identifier == "def_contributions") {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PrimaryContainer2D5A27.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "DEFENSIVE CONTRIBUTIONS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = SpaceGrotesk,
                                        color = PrimaryA1D494,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    val threshold = if (player.elementType == 2) 10 else 12
                                    val positionName = when(player.elementType) {
                                        2 -> "Defender"
                                        3 -> "Midfielder"
                                        4 -> "Forward"
                                        else -> "Player"
                                    }

                                    Text(
                                        "$positionName: ${stat.value} defensive actions",
                                        fontSize = 12.sp,
                                        fontFamily = Manrope,
                                        color = OnSurfaceE5E2E1
                                    )
                                    Text(
                                        "Threshold: $threshold actions for +2 pts",
                                        fontSize = 11.sp,
                                        fontFamily = Manrope,
                                        color = Outline8C9387
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    PointRow("Defensive bonus:", "+${stat.points}", true)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            playerDetail?.history?.lastOrNull()?.let { latestMatch ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer20201F),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "LATEST MATCH",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = SpaceGrotesk,
                            color = OnSurfaceVariantC2C9BB,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (latestMatch.yellowCards > 0) {
                            PointRow("Yellow card:", "-1")
                        }
                        if (latestMatch.redCards > 0) {
                            PointRow("Red card:", "-3")
                        }
                        if (latestMatch.saves > 0) {
                            PointRow("${latestMatch.saves} saves:", "${latestMatch.saves / 3}")
                        }
                        if (latestMatch.goalsScored > 0) {
                            val pointsPerGoal = when(player.elementType) {
                                4 -> 4
                                3 -> 5
                                else -> 6
                            }
                            PointRow("${latestMatch.goalsScored} goals:", "${latestMatch.goalsScored * pointsPerGoal}")
                        }
                        if (latestMatch.assists > 0) {
                            PointRow("${latestMatch.assists} assists:", "${latestMatch.assists * 3}")
                        }
                        if (latestMatch.cleanSheets > 0) {
                            val cleanSheetPoints = when(player.elementType) {
                                1 -> 4
                                2 -> 4
                                3 -> 1
                                else -> 0
                            }
                            PointRow("Clean sheet:", "$cleanSheetPoints")
                        }
                        if (latestMatch.goalsConceded > 0) {
                            val gcPoints = when(player.elementType) {
                                1, 2 -> if (latestMatch.goalsConceded >= 2) -latestMatch.goalsConceded / 2 else 0
                                else -> 0
                            }
                            if (gcPoints < 0) {
                                PointRow("Goals conceded (${latestMatch.goalsConceded}):", "$gcPoints")
                            }
                        }
                        val displayMinutes = if (fixture != null && fixture.started == true && !fixture.finished) {
                            fixture.minutes
                        } else {
                            latestMatch.minutes
                        }
                        PointRow("Played $displayMinutes min:", if (displayMinutes >= 60) "2" else "1")
                        PointRow("Bonus (${latestMatch.bps} bps):", "${latestMatch.bonus}")

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = OutlineVariant42493E.copy(alpha = 0.3f)
                        )
                        PointRow("Total Points:", "${latestMatch.totalPoints}", true)
                    }
                }
            }
        }

        item {
            leagueStats?.let { stats ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainer20201F),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "OWNERSHIP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = SpaceGrotesk,
                            color = OnSurfaceVariantC2C9BB,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        StatRow("Starts league:", "${String.format("%.1f", stats.startsPercentage)}%")
                        StatRow("Owned league:", "${String.format("%.1f", stats.ownedPercentage)}%")
                        StatRow("Owned overall:", "${player.selectedByPercent}%")
                        StatRow("Captain count:", "${stats.captainCount}")
                        StatRow("Price:", "£${player.nowCost / 10.0}M")
                    }
                }
            }
        }

        item {
            playerDetail?.history?.let { history ->
                if (history.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainer20201F),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "PREVIOUS FIXTURES",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = SpaceGrotesk,
                                color = OnSurfaceVariantC2C9BB,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GW", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = Manrope, color = Outline8C9387, modifier = Modifier.width(40.dp))
                                Text("Opp", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = Manrope, color = Outline8C9387, modifier = Modifier.width(70.dp))
                                Text("Min", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = Manrope, color = Outline8C9387, modifier = Modifier.width(40.dp))
                                Text("Pts", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = Manrope, color = Outline8C9387, modifier = Modifier.width(40.dp))
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = OutlineVariant42493E.copy(alpha = 0.3f))

                            history.takeLast(5).forEach { match ->
                                val opponentTeam = bootstrapData?.teams?.find { it.id == match.opponentTeam }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${match.round}", fontSize = 12.sp, fontFamily = SpaceGrotesk, color = OnSurfaceE5E2E1, modifier = Modifier.width(40.dp))
                                    Text(
                                        "${opponentTeam?.shortName ?: "OPP"} ${if (match.wasHome) "(H)" else "(A)"}",
                                        fontSize = 12.sp,
                                        fontFamily = Manrope,
                                        color = OnSurfaceVariantC2C9BB,
                                        modifier = Modifier.width(70.dp)
                                    )
                                    Text("${match.minutes}", fontSize = 12.sp, fontFamily = SpaceGrotesk, color = OnSurfaceVariantC2C9BB, modifier = Modifier.width(40.dp))
                                    Text("${match.totalPoints}", fontSize = 12.sp, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, color = PrimaryA1D494, modifier = Modifier.width(40.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            playerDetail?.fixtures?.let { fixtures ->
                val upcoming = fixtures.filter { !it.finished }.take(5)
                if (upcoming.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainer20201F),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "UPCOMING FIXTURES",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = SpaceGrotesk,
                                color = OnSurfaceVariantC2C9BB,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GW", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = Manrope, color = Outline8C9387, modifier = Modifier.width(40.dp))
                                Text("Opp", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = Manrope, color = Outline8C9387, modifier = Modifier.width(70.dp))
                                Text("Diff", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = Manrope, color = Outline8C9387, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = OutlineVariant42493E.copy(alpha = 0.3f))

                            upcoming.forEach { upcomingFixture ->
                                val opponentTeamId = if (upcomingFixture.isHome) upcomingFixture.teamA else upcomingFixture.teamH
                                val opponentTeam = bootstrapData?.teams?.find { it.id == opponentTeamId }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${upcomingFixture.event}", fontSize = 12.sp, fontFamily = SpaceGrotesk, color = OnSurfaceE5E2E1, modifier = Modifier.width(40.dp))
                                    Text(
                                        "${opponentTeam?.shortName ?: "TBD"} ${if (upcomingFixture.isHome) "(H)" else "(A)"}",
                                        fontSize = 12.sp,
                                        fontFamily = Manrope,
                                        color = OnSurfaceVariantC2C9BB,
                                        modifier = Modifier.width(70.dp)
                                    )
                                    Box(
                                        modifier = Modifier.width(50.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    when(upcomingFixture.difficulty) {
                                                        1, 2 -> PrimaryContainer2D5A27
                                                        3 -> Color(0xFF5C4A00)
                                                        else -> TertiaryContainerA40217
                                                    },
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                "${upcomingFixture.difficulty}",
                                                color = when(upcomingFixture.difficulty) {
                                                    1, 2 -> PrimaryA1D494
                                                    3 -> SecondaryFFE083
                                                    else -> TertiaryFFB3AD
                                                },
                                                fontSize = 11.sp,
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
        }
    }
}

@Composable
fun StartsTab(
    leagueStats: LeaguePlayerStats?,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    if (leagueStats == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isLoading) {
                    CircularProgressIndicator(color = PrimaryA1D494)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Checking league teams…", color = Outline8C9387, fontSize = 13.sp, fontFamily = Manrope)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("This checks up to 50 teams", color = Outline8C9387, fontSize = 11.sp, fontFamily = Manrope)
                } else if (errorMessage != null) {
                    Text("League stats unavailable", color = Outline8C9387, fontSize = 14.sp, fontFamily = Manrope, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(errorMessage, color = Outline8C9387, fontSize = 12.sp, fontFamily = Manrope, textAlign = TextAlign.Center)
                } else {
                    Text("No league data available", color = Outline8C9387, fontSize = 14.sp, fontFamily = Manrope, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Open a league to see starts info", color = Outline8C9387, fontSize = 12.sp, fontFamily = Manrope)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "TEAMS WHO STARTED THIS PLAYER",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = SpaceGrotesk,
                color = PrimaryA1D494,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        leagueStats.let { stats ->
            if (stats.startedBy.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainer20201F),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "No teams in this league started this player",
                            modifier = Modifier.padding(16.dp),
                            color = Outline8C9387,
                            fontFamily = Manrope
                        )
                    }
                }
            } else {
                val captainedTeams = stats.captainedBy
                val startedOnly = stats.startedBy.filter { !captainedTeams.contains(it) }

                if (captainedTeams.isNotEmpty()) {
                    item {
                        Text(
                            "Captained By (${captainedTeams.size})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            fontFamily = SpaceGrotesk,
                            color = SecondaryFFE083
                        )
                    }

                    items(captainedTeams) { manager ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF3C2F00).copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⭐", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                ManagerRankText(manager = manager, fontSize = 13.sp, color = OnSurfaceE5E2E1, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                if (startedOnly.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Started By (${startedOnly.size})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            fontFamily = SpaceGrotesk,
                            color = PrimaryA1D494
                        )
                    }

                    items(startedOnly) { manager ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PrimaryContainer2D5A27.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✓", fontSize = 15.sp, color = PrimaryA1D494, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                ManagerRankText(manager = manager, fontSize = 13.sp, color = OnSurfaceE5E2E1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BenchTab(
    leagueStats: LeaguePlayerStats?,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    if (leagueStats == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isLoading) {
                    CircularProgressIndicator(color = PrimaryA1D494)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Checking league teams…", color = Outline8C9387, fontSize = 13.sp, fontFamily = Manrope)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("This checks up to 50 teams", color = Outline8C9387, fontSize = 11.sp, fontFamily = Manrope)
                } else if (errorMessage != null) {
                    Text("League stats unavailable", color = Outline8C9387, fontSize = 14.sp, fontFamily = Manrope, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(errorMessage, color = Outline8C9387, fontSize = 12.sp, fontFamily = Manrope, textAlign = TextAlign.Center)
                } else {
                    Text("No league data available", color = Outline8C9387, fontSize = 14.sp, fontFamily = Manrope, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Open a league to see bench info", color = Outline8C9387, fontSize = 12.sp, fontFamily = Manrope)
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "TEAMS WHO BENCHED THIS PLAYER",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                fontFamily = SpaceGrotesk,
                color = TertiaryFFB3AD,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        leagueStats.let { stats ->
            if (stats.benchCount == 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainer20201F),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "No teams in this league benched this player",
                            modifier = Modifier.padding(16.dp),
                            color = Outline8C9387,
                            fontFamily = Manrope
                        )
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TertiaryContainerA40217.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Benched Count: ${stats.benchCount}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                fontFamily = SpaceGrotesk,
                                color = TertiaryFFB3AD
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "These managers own this player but have them on the bench",
                                fontSize = 12.sp,
                                fontFamily = Manrope,
                                color = Outline8C9387
                            )
                        }
                    }
                }

                items(stats.benchedBy) { manager ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainer20201F),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🪑", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            ManagerRankText(manager = manager, fontSize = 13.sp, color = OnSurfaceE5E2E1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PointRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontFamily = Manrope,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) OnSurfaceE5E2E1 else OnSurfaceVariantC2C9BB
        )
        Text(
            value,
            fontSize = 13.sp,
            fontFamily = SpaceGrotesk,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = if (bold) PrimaryA1D494 else OnSurfaceE5E2E1
        )
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, fontFamily = Manrope, color = Outline8C9387)
        Text(value, fontSize = 13.sp, fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, color = OnSurfaceE5E2E1)
    }
}

@Composable
fun ManagerRankText(
    manager: LeagueManagerRef,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text = "#${manager.rank} ${manager.entryName}",
        fontSize = fontSize,
        fontFamily = Manrope,
        color = color,
        fontWeight = fontWeight
    )
}

fun getPositionName(elementType: Int): String {
    return when(elementType) {
        1 -> "Goalkeeper"
        2 -> "Defender"
        3 -> "Midfielder"
        4 -> "Forward"
        else -> "Player"
    }
}
