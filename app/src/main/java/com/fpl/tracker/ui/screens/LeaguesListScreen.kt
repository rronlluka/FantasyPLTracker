package com.fpl.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.fpl.tracker.data.models.LeagueInfo
import com.fpl.tracker.data.models.ManagerData
import com.fpl.tracker.data.models.ManagerHistory
import com.fpl.tracker.data.preferences.PreferencesManager
import com.fpl.tracker.navigation.Screen
import com.fpl.tracker.viewmodel.ManagerStatsViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

// ── Brand palette ─────────────────────────────────────────────────────────────
private val BrandGreen       = Color(0xFFA1D494)
private val BrandGreenDark   = Color(0xFF2D5A27)
private val BrandYellow      = Color(0xFFFFE083)
private val DarkBg           = Color(0xFF131313)
private val CardBg           = Color(0xFF1C1B1B)
private val CardMidBg        = Color(0xFF20201F)
private val OnSurface        = Color(0xFFE5E2E1)

private fun formatRank(rank: Int): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).format(rank)

private fun formatRankDeltaShort(n: Int): String {
    val v = abs(n)
    return when {
        v >= 1_000_000 -> "${v / 1_000_000}M"
        v >= 1_000     -> "${v / 1_000}K"
        else           -> "$v"
    }
}

/** Positive delta = improved (rank number went down). */
private fun overallRankDelta(managerData: ManagerData, history: ManagerHistory?): Int? {
    val prevGw = managerData.currentEvent - 1
    if (prevGw < 1) return null
    val prevEntry = history?.current?.find { it.event == prevGw } ?: return null
    return prevEntry.overallRank - managerData.summaryOverallRank
}

@Composable
fun LeaguesListScreen(
    navController: NavController,
    managerId: Long,
    viewModel: ManagerStatsViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val uiState by viewModel.uiState.collectAsState()
    var favoriteLeagueId by remember { mutableStateOf(prefsManager.getFavoriteLeagueId()) }

    LaunchedEffect(managerId) {
        viewModel.loadManagerData(managerId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandGreen)
                }
            }

            uiState.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("⚠", fontSize = 36.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = Color(0xFFFF6B6B),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadManagerData(managerId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGreen,
                            contentColor = Color(0xFF0A3909)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Retry", fontWeight = FontWeight.Bold) }
                }
            }

            uiState.managerData != null -> {
                val managerData    = uiState.managerData!!
                val history        = uiState.managerHistory
                val classicLeagues = managerData.leagues.classic
                val favoriteLeague = classicLeagues.firstOrNull { it.id.toLong() == favoriteLeagueId }

                val sortedLeagues = buildList {
                    if (favoriteLeague != null) add(favoriteLeague)
                    addAll(
                        classicLeagues
                            .filter { it.id.toLong() != favoriteLeagueId }
                            .sortedBy { it.entryRank ?: Int.MAX_VALUE }
                    )
                }

                val rankDelta = remember(
                    managerData.summaryOverallRank,
                    managerData.currentEvent,
                    history?.current
                ) { overallRankDelta(managerData, history) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {

                    // ── MY LEAGUES + team name header ────────────────────────
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(top = 12.dp, bottom = 20.dp)
                        ) {
                            Text(
                                text          = "MY LEAGUES",
                                fontSize      = 11.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = BrandGreen,
                                letterSpacing = 2.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text       = managerData.name,
                                fontSize   = 28.sp,
                                fontWeight = FontWeight.Black,
                                color      = Color.White,
                                maxLines   = 1,
                                overflow   = TextOverflow.Ellipsis
                            )
                            Text(
                                text     = "${managerData.playerFirstName} ${managerData.playerLastName}",
                                fontSize = 14.sp,
                                color    = Color.White.copy(alpha = 0.45f)
                            )
                        }
                    }

                    // ── HERO: Favourite league (if set) OR Overall rank ──────
                    item {
                        if (favoriteLeague != null) {
                            // ── Favourite league hero card ───────────────────
                            FavoriteLeagueHeroCard(
                                league  = favoriteLeague,
                                onClick = {
                                    navController.navigate(
                                        Screen.LeagueStandings.createRoute(favoriteLeague.id.toLong())
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            // ── Overall rank compact strip ───────────────────
                            OverallRankStrip(
                                overallRank   = managerData.summaryOverallRank,
                                overallPoints = managerData.summaryOverallPoints,
                                rankDelta     = rankDelta,
                                modifier      = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            )
                        } else {
                            // ── Overall rank full hero card ──────────────────
                            OverallRankHeroCard(
                                overallRank   = managerData.summaryOverallRank,
                                overallPoints = managerData.summaryOverallPoints,
                                rankDelta     = rankDelta,
                                currentGw     = managerData.currentEvent,
                                modifier      = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // ── "All Leagues" section header ─────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text       = "All Leagues",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                            Text(
                                text     = "${classicLeagues.size} leagues",
                                fontSize = 13.sp,
                                color    = Color.White.copy(alpha = 0.35f)
                            )
                        }
                    }

                    // ── League rows ──────────────────────────────────────────
                    itemsIndexed(sortedLeagues) { _, league ->
                        val isFavorite = league.id.toLong() == favoriteLeagueId
                        LeagueRow(
                            league     = league,
                            isFavorite = isFavorite,
                            onFavoriteToggle = {
                                if (isFavorite) {
                                    prefsManager.removeFavoriteLeague()
                                    favoriteLeagueId = null
                                } else {
                                    prefsManager.saveFavoriteLeague(league.id.toLong(), league.name)
                                    favoriteLeagueId = league.id.toLong()
                                }
                            },
                            onClick = {
                                navController.navigate(
                                    Screen.LeagueStandings.createRoute(league.id.toLong())
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Favourite league hero card ────────────────────────────────────────────────
@Composable
private fun FavoriteLeagueHeroCard(
    league: LeagueInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2D5A27), Color(0xFF1A3A14))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column {
            // ⭐ badge
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier              = Modifier.padding(bottom = 10.dp)
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint     = BrandYellow,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text          = "FAVOURITE LEAGUE",
                    fontSize      = 10.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = BrandYellow.copy(alpha = 0.9f),
                    letterSpacing = 1.5.sp
                )
            }

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = league.name,
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.Black,
                        color      = Color.White,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                        lineHeight = 28.sp
                    )
                    league.entryRank?.let { rank ->
                        Spacer(Modifier.height(10.dp))
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text     = "Your rank",
                                fontSize = 13.sp,
                                color    = Color.White.copy(alpha = 0.55f)
                            )
                            // Rank chip
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandGreen.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text       = "#${formatRank(rank)}",
                                    fontSize   = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = BrandGreen,
                                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            // Rank change vs last GW
                            league.entryLastRank?.let { lastRank ->
                                val diff = lastRank - rank
                                if (diff != 0) {
                                    val isUp = diff > 0
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isUp) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                            contentDescription = null,
                                            tint     = if (isUp) BrandGreen else Color(0xFFFF6B6B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text       = formatRankDeltaShort(abs(diff)),
                                            fontSize   = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = if (isUp) BrandGreen else Color(0xFFFF6B6B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Arrow circle button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(BrandGreen.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open league",
                        tint     = BrandGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Overall rank compact strip (below favourite hero) ────────────────────────
@Composable
private fun OverallRankStrip(
    overallRank: Int,
    overallPoints: Int,
    rankDelta: Int?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CardMidBg, RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text          = "OVERALL RANK",
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.5.sp
                )
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text       = "#${formatRank(overallRank)}",
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.Black,
                        color      = BrandGreen
                    )
                    rankDelta?.let { delta ->
                        if (delta != 0) {
                            val isUp = delta > 0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isUp) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint     = if (isUp) BrandGreen else Color(0xFFFF6B6B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text       = formatRankDeltaShort(abs(delta)),
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = if (isUp) BrandGreen else Color(0xFFFF6B6B)
                                )
                            }
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text          = "TOTAL POINTS",
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.5.sp
                )
                Text(
                    text       = "${overallPoints} pts",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Black,
                    color      = Color.White
                )
            }
        }
    }
}

// ── Overall rank full hero (no favourite set) ─────────────────────────────────
@Composable
private fun OverallRankHeroCard(
    overallRank: Int,
    overallPoints: Int,
    rankDelta: Int?,
    currentGw: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CardMidBg, RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top
        ) {
            Column {
                Surface(
                    color = BrandGreenDark,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text          = "GLOBAL ARENA",
                        modifier      = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = BrandGreen.copy(alpha = 0.9f),
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text          = "OVERALL",
                    fontSize      = 20.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = OnSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text     = "Global Ranking · GW $currentGw",
                    fontSize = 13.sp,
                    color    = Color.White.copy(alpha = 0.45f)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text     = "⭐  Star a league to pin it here",
                    fontSize = 11.sp,
                    color    = Color.White.copy(alpha = 0.3f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text          = formatRank(overallRank),
                    fontSize      = 42.sp,
                    fontWeight    = FontWeight.Black,
                    color         = BrandYellow,
                    letterSpacing = (-1).sp,
                    maxLines      = 1,
                    overflow      = TextOverflow.Clip
                )
                rankDelta?.let { delta ->
                    if (delta != 0) {
                        val isUp = delta > 0
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier              = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isUp) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint     = if (isUp) BrandGreen else Color(0xFFFF6B6B),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text       = buildString {
                                    append(if (isUp) "UP " else "DOWN ")
                                    append(formatRankDeltaShort(delta))
                                },
                                fontSize      = 11.sp,
                                fontWeight    = FontWeight.Bold,
                                color         = if (isUp) BrandGreen else Color(0xFFFF6B6B),
                                letterSpacing = 0.5.sp
                            )
                        }
                    } else {
                        Text(
                            text     = "No change",
                            fontSize = 11.sp,
                            color    = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = "$overallPoints pts",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White.copy(alpha = 0.55f)
                )
            }
        }
    }
}

// ── Individual league row ─────────────────────────────────────────────────────
@Composable
private fun LeagueRow(
    league: LeagueInfo,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rank = league.entryRank

    Box(
        modifier = modifier
            .background(CardBg, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        color = if (isFavorite) BrandGreenDark.copy(alpha = 0.6f)
                                else Color.White.copy(alpha = 0.07f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = rank?.let { "#${it}" } ?: "–",
                    fontSize   = if ((rank ?: 0) >= 10) 13.sp else 15.sp,
                    fontWeight = FontWeight.Black,
                    color      = if (isFavorite) BrandYellow else BrandGreen,
                    textAlign  = TextAlign.Center,
                    maxLines   = 1
                )
            }

            Spacer(Modifier.width(14.dp))

            // League name + rank change
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = league.name,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                // Rank change detail line
                val lastRank = league.entryLastRank
                if (rank != null && lastRank != null) {
                    val diff = lastRank - rank
                    val (changeText, changeColor) = when {
                        diff > 0 -> "▼ ${formatRankDeltaShort(diff)} this GW" to Color(0xFFFF6B6B)
                        diff < 0 -> "▲ ${formatRankDeltaShort(abs(diff))} this GW" to BrandGreen
                        else     -> "No change" to Color.White.copy(alpha = 0.35f)
                    }
                    Text(
                        text       = changeText,
                        fontSize   = 12.sp,
                        color      = changeColor,
                        fontWeight = if (diff != 0) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }

            // Star toggle
            IconButton(
                onClick  = onFavoriteToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = if (isFavorite) "Unpin favourite" else "Set as favourite",
                    tint     = if (isFavorite) BrandYellow else Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Arrow
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open league",
                tint     = Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
