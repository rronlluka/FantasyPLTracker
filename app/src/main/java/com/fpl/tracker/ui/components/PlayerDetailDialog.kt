package com.fpl.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fpl.tracker.data.models.*

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
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Summary", "Starts (${leagueStats?.startsCount ?: 0})", "Bench (${leagueStats?.benchCount ?: 0})")
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        getTeamColor(team.id),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = team.shortName,
                                    color = getTeamTextColor(team.id),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = player.webName,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${team.shortName} • ${getPositionName(player.elementType)}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                "Close",
                                tint = Color.White
                            )
                        }
                    }
                }
                
                // Tabs
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                ) 
                            }
                        )
                    }
                }

                // Content based on selected tab
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTabIndex) {
                        0 -> SummaryTab(player, team, playerDetail, leagueStats, bootstrapData, currentEvent, liveStats)
                        1 -> StartsTab(leagueStats)
                        2 -> BenchTab(leagueStats)
                    }
                }

                // OK Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("OK", fontSize = 16.sp)
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
    liveStats: LiveElement? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Debug: Log all explain identifiers to find defensive contributions
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
        
        // Defensive Contributions (if available in live stats)
        item {
            liveStats?.explain?.let { explains ->
                explains.forEach { explain ->
                    explain.stats.forEach { stat ->
                        // Try multiple possible identifiers for defensive contributions
                        if (stat.identifier.contains("def", ignoreCase = true) || 
                            stat.identifier == "defensive_contributions" ||
                            stat.identifier == "defensive" ||
                            stat.identifier == "def_contributions") {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFE8F5E9)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        "Defensive Contributions",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.Black
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
                                        color = Color.Black
                                    )
                                    Text(
                                        "Threshold: $threshold actions for +2 pts",
                                        fontSize = 11.sp,
                                        color = Color.Gray
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
        
        // Latest Match Summary
        item {
            playerDetail?.history?.lastOrNull()?.let { latestMatch ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Latest Match",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
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
                                4 -> 4 // FWD
                                3 -> 5 // MID
                                else -> 6 // DEF/GK
                            }
                            PointRow("${latestMatch.goalsScored} goals:", "${latestMatch.goalsScored * pointsPerGoal}")
                        }
                        if (latestMatch.assists > 0) {
                            PointRow("${latestMatch.assists} assists:", "${latestMatch.assists * 3}")
                        }
                        if (latestMatch.cleanSheets > 0) {
                            val cleanSheetPoints = when(player.elementType) {
                                1 -> 4 // GK
                                2 -> 4 // DEF
                                3 -> 1 // MID
                                else -> 0 // FWD
                            }
                            PointRow("Clean sheet:", "$cleanSheetPoints")
                        }
                        if (latestMatch.goalsConceded > 0) {
                            val gcPoints = when(player.elementType) {
                                1, 2 -> if (latestMatch.goalsConceded >= 2) -latestMatch.goalsConceded / 2 else 0 // GK/DEF
                                else -> 0
                            }
                            if (gcPoints < 0) {
                                PointRow("Goals conceded (${latestMatch.goalsConceded}):", "$gcPoints")
                            }
                        }
                        PointRow("Played ${latestMatch.minutes} min:", if (latestMatch.minutes >= 60) "2" else "1")
                        PointRow("Bonus (${latestMatch.bps} bps):", "${latestMatch.bonus}")
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        PointRow("Total Points:", "${latestMatch.totalPoints}", true)
                    }
                }
            }
        }

        // Ownership Stats
        item {
            leagueStats?.let { stats ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Ownership",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        StatRow("Starts league:", "${String.format("%.1f", stats.startsPercentage)}%")
                        StatRow("Owned league:", "${String.format("%.1f", stats.ownedPercentage)}%")
                        StatRow("Owned overall:", "${player.selectedByPercent}%")
                        StatRow("Captain count:", "${stats.captainCount}")
                        StatRow("Price:", "£${player.nowCost / 10.0}M")
                    }
                }
            }
        }

        // Previous Fixtures
        item {
            playerDetail?.history?.let { history ->
                if (history.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Previous Fixtures",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("GW", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.width(40.dp))
                                Text("Opp", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.width(70.dp))
                                Text("Min", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.width(40.dp))
                                Text("Pts", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.width(40.dp))
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            history.takeLast(5).forEach { match ->
                                val opponentTeam = bootstrapData?.teams?.find { it.id == match.opponentTeam }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${match.round}", fontSize = 12.sp, color = Color.Black, modifier = Modifier.width(40.dp))
                                    Text(
                                        "${opponentTeam?.shortName ?: "OPP"} ${if (match.wasHome) "(H)" else "(A)"}", 
                                        fontSize = 12.sp,
                                        color = Color.Black,
                                        modifier = Modifier.width(70.dp)
                                    )
                                    Text("${match.minutes}", fontSize = 12.sp, color = Color.Black, modifier = Modifier.width(40.dp))
                                    Text("${match.totalPoints}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.width(40.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Upcoming Fixtures
        item {
            playerDetail?.fixtures?.let { fixtures ->
                val upcoming = fixtures.filter { !it.finished }.take(5)
                if (upcoming.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF5F5F5)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Upcoming Fixtures",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Table Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("GW", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.width(40.dp))
                                Text("Opp", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.width(70.dp))
                                Text("Diff", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            upcoming.forEach { fixture ->
                                val opponentTeamId = if (fixture.isHome) fixture.teamA else fixture.teamH
                                val opponentTeam = bootstrapData?.teams?.find { it.id == opponentTeamId }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${fixture.event}", fontSize = 12.sp, color = Color.Black, modifier = Modifier.width(40.dp))
                                    Text(
                                        "${opponentTeam?.shortName ?: "TBD"} ${if (fixture.isHome) "(H)" else "(A)"}", 
                                        fontSize = 12.sp,
                                        color = Color.Black,
                                        modifier = Modifier.width(70.dp)
                                    )
                                    Box(
                                        modifier = Modifier.width(50.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    when(fixture.difficulty) {
                                                        1, 2 -> Color(0xFF4CAF50)
                                                        3 -> Color(0xFFFFC107)
                                                        else -> Color(0xFFFF5555)
                                                    },
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                "${fixture.difficulty}",
                                                color = Color.White,
                                                fontSize = 11.sp,
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
fun StartsTab(leagueStats: LeaguePlayerStats?) {
    if (leagueStats == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading league data...", color = Color.Gray)
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
                "Teams Who Started This Player",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        leagueStats.let { stats ->
            if (stats.startedBy.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Text(
                            "No teams in this league started this player",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray
                        )
                    }
                }
            } else {
                // Show captains first
                val captainedTeams = stats.captainedBy
                val startedOnly = stats.startedBy.filter { !captainedTeams.contains(it) }
                
                if (captainedTeams.isNotEmpty()) {
                    item {
                        Text(
                            "Captained By (${captainedTeams.size})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFFFF9800)
                        )
                    }
                    
                    items(captainedTeams) { teamName ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFFFE0B2)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⭐", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(teamName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
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
                            fontSize = 14.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    
                    items(startedOnly) { teamName ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E9)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✓", fontSize = 16.sp, color = Color(0xFF4CAF50))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(teamName, fontSize = 14.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BenchTab(leagueStats: LeaguePlayerStats?) {
    if (leagueStats == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading league data...", color = Color.Gray)
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
                "Teams Who Benched This Player",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF37003C)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        leagueStats.let { stats ->
            if (stats.benchCount == 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Text(
                            "No teams in this league benched this player",
                            modifier = Modifier.padding(16.dp),
                            color = Color.Gray
                        )
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Benched Count: ${stats.benchCount}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "These managers own this player but have them on the bench",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                // Show benched teams
                items(stats.benchedBy) { teamName ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF9C4)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🪑", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(teamName, fontSize = 14.sp, color = Color.Black)
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
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = Color.Black
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
        Text(label, fontSize = 13.sp, color = Color(0xFF666666))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
    }
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

