package com.fpl.tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpl.tracker.ui.theme.*
import com.fpl.tracker.viewmodel.PlayerWithDetails
import com.fpl.tracker.ui.theme.SpaceGrotesk
import com.fpl.tracker.ui.theme.Manrope

@Composable
fun FootballPitch(
    startingXI: List<PlayerWithDetails>,
    provisionalBonus: Map<Int, Int> = emptyMap(),
    onPlayerClick: ((PlayerWithDetails) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val goalkeeper = startingXI.filter { it.player.elementType == 1 }
    val defenders = startingXI.filter { it.player.elementType == 2 }
    val midfielders = startingXI.filter { it.player.elementType == 3 }
    val forwards = startingXI.filter { it.player.elementType == 4 }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(24.dp))
            .border(3.dp, SurfaceContainerHighest353535, RoundedCornerShape(24.dp))
    ) {
        // Radial gradient pitch background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            PrimaryContainer2D5A27,
                            Surface131313
                        ),
                        radius = 800f
                    )
                )
        )

        // Grid lines overlay
        PitchGridOverlay(modifier = Modifier.fillMaxSize())

        // Players positioned in formation
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (goalkeeper.isNotEmpty()) {
                PlayerRow(
                    players = goalkeeper,
                    provisionalBonus = provisionalBonus,
                    onPlayerClick = onPlayerClick
                )
            }
            if (defenders.isNotEmpty()) {
                PlayerRow(
                    players = defenders,
                    provisionalBonus = provisionalBonus,
                    onPlayerClick = onPlayerClick
                )
            }
            if (midfielders.isNotEmpty()) {
                PlayerRow(
                    players = midfielders,
                    provisionalBonus = provisionalBonus,
                    onPlayerClick = onPlayerClick
                )
            }
            if (forwards.isNotEmpty()) {
                PlayerRow(
                    players = forwards,
                    provisionalBonus = provisionalBonus,
                    onPlayerClick = onPlayerClick
                )
            }
        }
    }
}

@Composable
private fun PitchGridOverlay(modifier: Modifier = Modifier) {
    val lineColor = PrimaryA1D494.copy(alpha = 0.08f)
    val gridSize = 40f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Vertical lines
        var x = 0f
        while (x <= w) {
            drawLine(lineColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
            x += gridSize
        }
        // Horizontal lines
        var y = 0f
        while (y <= h) {
            drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            y += gridSize
        }
    }
}

@Composable
fun PlayerRow(
    players: List<PlayerWithDetails>,
    provisionalBonus: Map<Int, Int> = emptyMap(),
    onPlayerClick: ((PlayerWithDetails) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCompact = players.size >= 5

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isCompact) 2.dp else 4.dp),
        horizontalArrangement = if (isCompact) Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally) else Arrangement.SpaceEvenly
    ) {
        players.forEach { playerDetail ->
            PlayerCardOnPitch(
                playerDetail = playerDetail,
                provisionalBonus = provisionalBonus,
                onPlayerClick = onPlayerClick,
                compact = isCompact,
                modifier = if (isCompact) Modifier.weight(1f) else Modifier
            )
        }
    }
}

@Composable
fun PlayerCardOnPitch(
    playerDetail: PlayerWithDetails,
    provisionalBonus: Map<Int, Int> = emptyMap(),
    onPlayerClick: ((PlayerWithDetails) -> Unit)? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isCaptain = playerDetail.pick.isCaptain
    val isViceCaptain = playerDetail.pick.isViceCaptain

    var points = if (playerDetail.hasLivePoints && playerDetail.liveStats != null) {
        playerDetail.liveStats.stats.totalPoints
    } else {
        playerDetail.player.eventPoints
    }

    if (playerDetail.hasLivePoints && playerDetail.liveStats != null) {
        val currentBonus = playerDetail.liveStats.stats.bonus
        val provisionalBonusPoints = provisionalBonus[playerDetail.player.id] ?: 0
        if (currentBonus == 0 && provisionalBonusPoints > 0) {
            points += provisionalBonusPoints
        }
    }

    val displayPoints = points * playerDetail.pick.multiplier

    val cardBorder = when {
        isCaptain -> SecondaryFFE083.copy(alpha = 0.4f)
        displayPoints >= 8 -> PrimaryA1D494.copy(alpha = 0.3f)
        else -> OutlineVariant42493E.copy(alpha = 0.3f)
    }

    val pointsPillBg = when {
        displayPoints >= 8 -> PrimaryContainer2D5A27
        displayPoints >= 4 -> SurfaceContainer20201F
        else -> SurfaceContainer20201F
    }
    val pointsPillFg = when {
        displayPoints >= 8 -> PrimaryA1D494
        else -> OnSurfaceE5E2E1
    }

    val cardWidth = if (compact) Modifier else Modifier.width(72.dp)
    val innerPadding = if (compact) 4.dp else 6.dp
    val teamFontSize = if (compact) 8.sp else 9.sp
    val nameFontSize = if (compact) 10.sp else 11.sp
    val ptsFontSize = if (compact) 9.sp else 10.sp
    val cornerRadius = if (compact) 6.dp else 8.dp
    val badgeSize = if (compact) 16.dp else 18.dp
    val badgeFont = if (compact) 7.sp else 8.sp

    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = cardWidth
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(SurfaceContainerHighest353535.copy(alpha = 0.9f))
                    .border(1.dp, cardBorder, RoundedCornerShape(cornerRadius))
                    .clickable { onPlayerClick?.invoke(playerDetail) }
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = playerDetail.team.shortName.take(3).uppercase(),
                        color = Outline8C9387,
                        fontSize = teamFontSize,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = playerDetail.player.webName,
                        color = OnSurfaceE5E2E1,
                        fontSize = nameFontSize,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(if (compact) 3.dp else 4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(pointsPillBg, RoundedCornerShape(4.dp))
                            .padding(vertical = if (compact) 2.dp else 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$displayPoints pts",
                            color = pointsPillFg,
                            fontSize = ptsFontSize,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        if (isCaptain || isViceCaptain) {
            Box(
                modifier = Modifier
                    .offset(x = 4.dp, y = (-6).dp)
                    .size(badgeSize)
                    .background(
                        if (isCaptain) SecondaryFFE083 else OnSurfaceVariantC2C9BB,
                        RoundedCornerShape(3.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCaptain) "C" else "V",
                    color = if (isCaptain) OnSecondary3C2F00 else Surface131313,
                    fontSize = badgeFont,
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
fun getTeamColor(teamId: Int): Color {
    return when (teamId) {
        1 -> Color(0xFFEF0107)   // Arsenal
        2 -> Color(0xFF95BFE5)   // Aston Villa
        3 -> Color(0xFFE62333)   // Bournemouth
        4 -> Color(0xFFE30613)   // Brentford
        5 -> Color(0xFF0057B8)   // Brighton
        6 -> Color(0xFF034694)   // Chelsea
        7 -> Color(0xFF1B458F)   // Crystal Palace
        8 -> Color(0xFF003399)   // Everton
        9 -> Color(0xFF000000)   // Fulham
        10 -> Color(0xFF0057B8)  // Ipswich
        11 -> Color(0xFF003090)  // Leicester
        12 -> Color(0xFFC8102E)  // Liverpool
        13 -> Color(0xFF6CABDD)  // Man City
        14 -> Color(0xFFDA291C)  // Man United
        15 -> Color(0xFF241F20)  // Newcastle
        16 -> Color(0xFFE53233)  // Nottm Forest
        17 -> Color(0xFFD71920)  // Southampton
        18 -> Color(0xFF132257)  // Tottenham
        19 -> Color(0xFF7A263A)  // West Ham
        20 -> Color(0xFFFDB913)  // Wolves
        else -> MaterialTheme.colorScheme.primary
    }
}

fun getTeamTextColor(teamId: Int): Color {
    return when (teamId) {
        9 -> Color.White
        15 -> Color.White
        20 -> Color.Black
        13 -> Color.Black
        else -> Color.White
    }
}
