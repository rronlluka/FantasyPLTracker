package com.fpl.tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fpl.tracker.viewmodel.PlayerWithDetails

@Composable
fun FootballPitch(
    startingXI: List<PlayerWithDetails>,
    provisionalBonus: Map<Int, Int> = emptyMap(),
    onPlayerClick: ((PlayerWithDetails) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Group players by position
    val goalkeeper = startingXI.filter { it.player.elementType == 1 }
    val defenders = startingXI.filter { it.player.elementType == 2 }
    val midfielders = startingXI.filter { it.player.elementType == 3 }
    val forwards = startingXI.filter { it.player.elementType == 4 }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(600.dp)
    ) {
        // Draw pitch
        PitchCanvas(
            modifier = Modifier.fillMaxSize()
        )
        
        // Position players
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Forwards
            if (forwards.isNotEmpty()) {
                PlayerRow(players = forwards, provisionalBonus = provisionalBonus, onPlayerClick = onPlayerClick)
            }
            
            // Midfielders
            if (midfielders.isNotEmpty()) {
                PlayerRow(players = midfielders, provisionalBonus = provisionalBonus, onPlayerClick = onPlayerClick)
            }
            
            // Defenders
            if (defenders.isNotEmpty()) {
                PlayerRow(players = defenders, provisionalBonus = provisionalBonus, onPlayerClick = onPlayerClick)
            }
            
            // Goalkeeper
            if (goalkeeper.isNotEmpty()) {
                PlayerRow(players = goalkeeper, provisionalBonus = provisionalBonus, onPlayerClick = onPlayerClick)
            }
        }
    }
}

@Composable
fun PitchCanvas(modifier: Modifier = Modifier) {
    val pitchColor = Color(0xFF1ab745) // Vibrant green from screenshot
    val lineColor = Color.White.copy(alpha = 0.6f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = 3f

        // Draw pitch background
        drawRect(
            color = pitchColor,
            topLeft = Offset.Zero,
            size = Size(width, height)
        )

        // Draw border
        drawRect(
            color = lineColor,
            topLeft = Offset(0f, 0f),
            size = Size(width, height),
            style = Stroke(width = strokeWidth)
        )

        // Draw halfway line
        drawLine(
            color = lineColor,
            start = Offset(0f, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = strokeWidth
        )

        // Draw center circle
        val centerCircleRadius = width * 0.12f
        drawCircle(
            color = lineColor,
            radius = centerCircleRadius,
            center = Offset(width / 2, height / 2),
            style = Stroke(width = strokeWidth)
        )
        
        // Draw center spot
        drawCircle(
            color = lineColor,
            radius = 6f,
            center = Offset(width / 2, height / 2)
        )

        // Top penalty area
        val penaltyBoxWidth = width * 0.6f
        val penaltyBoxHeight = height * 0.15f
        drawRect(
            color = lineColor,
            topLeft = Offset((width - penaltyBoxWidth) / 2, 0f),
            size = Size(penaltyBoxWidth, penaltyBoxHeight),
            style = Stroke(width = strokeWidth)
        )

        // Top goal area
        val goalBoxWidth = width * 0.35f
        val goalBoxHeight = height * 0.08f
        drawRect(
            color = lineColor,
            topLeft = Offset((width - goalBoxWidth) / 2, 0f),
            size = Size(goalBoxWidth, goalBoxHeight),
            style = Stroke(width = strokeWidth)
        )

        // Bottom penalty area
        drawRect(
            color = lineColor,
            topLeft = Offset((width - penaltyBoxWidth) / 2, height - penaltyBoxHeight),
            size = Size(penaltyBoxWidth, penaltyBoxHeight),
            style = Stroke(width = strokeWidth)
        )

        // Bottom goal area
        drawRect(
            color = lineColor,
            topLeft = Offset((width - goalBoxWidth) / 2, height - goalBoxHeight),
            size = Size(goalBoxWidth, goalBoxHeight),
            style = Stroke(width = strokeWidth)
        )

        // Top penalty arc
        val penaltyArcRadius = width * 0.12f
        val penaltySpotY = penaltyBoxHeight * 0.7f
        val arcPath = Path().apply {
            addArc(
                Rect(
                    left = width / 2 - penaltyArcRadius,
                    top = penaltySpotY - penaltyArcRadius,
                    right = width / 2 + penaltyArcRadius,
                    bottom = penaltySpotY + penaltyArcRadius
                ),
                startAngleDegrees = 38f,
                sweepAngleDegrees = 104f
            )
        }
        drawPath(
            path = arcPath,
            color = lineColor,
            style = Stroke(width = strokeWidth)
        )

        // Bottom penalty arc
        val bottomPenaltySpotY = height - penaltyBoxHeight * 0.7f
        val bottomArcPath = Path().apply {
            addArc(
                Rect(
                    left = width / 2 - penaltyArcRadius,
                    top = bottomPenaltySpotY - penaltyArcRadius,
                    right = width / 2 + penaltyArcRadius,
                    bottom = bottomPenaltySpotY + penaltyArcRadius
                ),
                startAngleDegrees = 218f,
                sweepAngleDegrees = 104f
            )
        }
        drawPath(
            path = bottomArcPath,
            color = lineColor,
            style = Stroke(width = strokeWidth)
        )

        // Penalty spots
        drawCircle(
            color = lineColor,
            radius = 4f,
            center = Offset(width / 2, penaltySpotY)
        )
        drawCircle(
            color = lineColor,
            radius = 4f,
            center = Offset(width / 2, bottomPenaltySpotY)
        )
    }
}

@Composable
fun PlayerRow(
    players: List<PlayerWithDetails>,
    provisionalBonus: Map<Int, Int> = emptyMap(),
    onPlayerClick: ((PlayerWithDetails) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        players.forEach { playerDetail ->
            PlayerCardOnPitch(
                playerDetail = playerDetail,
                provisionalBonus = provisionalBonus,
                onPlayerClick = onPlayerClick
            )
        }
    }
}

@Composable
fun PlayerCardOnPitch(
    playerDetail: PlayerWithDetails,
    provisionalBonus: Map<Int, Int> = emptyMap(),
    onPlayerClick: ((PlayerWithDetails) -> Unit)? = null
) {
    val isCaptain = playerDetail.pick.isCaptain
    val isViceCaptain = playerDetail.pick.isViceCaptain
    
    // Use live API data if hasLivePoints
    var points = if (playerDetail.hasLivePoints && playerDetail.liveStats != null) {
        playerDetail.liveStats.stats.totalPoints
    } else {
        playerDetail.player.eventPoints
    }
    
    // Add provisional bonus
    if (playerDetail.hasLivePoints && playerDetail.liveStats != null) {
        val currentBonus = playerDetail.liveStats.stats.bonus
        val provisionalBonusPoints = provisionalBonus[playerDetail.player.id] ?: 0
        if (currentBonus == 0 && provisionalBonusPoints > 0) {
            points += provisionalBonusPoints
        }
    }
    
    val displayPoints = points * playerDetail.pick.multiplier
    
    val infiniteTransition = rememberInfiniteTransition(label = "live")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(62.dp)
    ) {
        // Captain/Vice-Captain badge OR Live indicator
        if (isCaptain || isViceCaptain) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(
                        color = Color(0xFF6B4EE6), // Purple from screenshot 16
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCaptain) "C" else "V",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        } else if (playerDetail.isLive) {
            Box(
                modifier = Modifier
                    .height(16.dp)
                    .background(
                        color = Color(0xFFB44B3E).copy(alpha = alpha),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }

        // White card matching screenshot 16
        Card(
            modifier = Modifier
                .width(62.dp)
                .clickable { onPlayerClick?.invoke(playerDetail) },
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top: Team Background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = getTeamColor(playerDetail.team.id))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = playerDetail.team.shortName.take(3).uppercase(),
                        color = getTeamTextColor(playerDetail.team.id),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Middle: Player Name
                Text(
                    text = playerDetail.player.webName,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                )

                // Bottom: Points Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .padding(bottom = 6.dp)
                        .background(
                            color = when {
                                displayPoints >= 10 -> Color(0xFFC8E6C9) // Green pill for high points
                                displayPoints >= 6 -> Color(0xFFBBDEFB)  // Blue pill for avg
                                displayPoints <= 2 -> Color(0xFFFFCDD2)  // Pink pill for blank
                                else -> Color(0xFFF5F5F5)                // Neutral pill
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$displayPoints pts",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
        
        // Minor spacing at bottom
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// Simple team color mapping (you can enhance this with more teams)
@Composable
fun getTeamColor(teamId: Int): Color {
    return when (teamId) {
        1 -> Color(0xFFEF0107) // Arsenal - Red
        2 -> Color(0xFF95BFE5) // Aston Villa - Claret Blue
        3 -> Color(0xFFE62333) // Bournemouth - Red
        4 -> Color(0xFFE30613) // Brentford - Red
        5 -> Color(0xFF0057B8) // Brighton - Blue
        6 -> Color(0xFF034694) // Chelsea - Blue
        7 -> Color(0xFF1B458F) // Crystal Palace - Blue
        8 -> Color(0xFF003399) // Everton - Blue
        9 -> Color(0xFF000000) // Fulham - Black (was white)
        10 -> Color(0xFF0057B8) // Ipswich - Blue
        11 -> Color(0xFF003090) // Leicester - Blue
        12 -> Color(0xFFC8102E) // Liverpool - Red
        13 -> Color(0xFF6CABDD) // Man City - Sky Blue
        14 -> Color(0xFFDA291C) // Man United - Red
        15 -> Color(0xFF241F20) // Newcastle - Black
        16 -> Color(0xFFE53233) // Nottm Forest - Red
        17 -> Color(0xFFD71920) // Southampton - Red
        18 -> Color(0xFF132257) // Tottenham - Navy
        19 -> Color(0xFF7A263A) // West Ham - Claret
        20 -> Color(0xFFFDB913) // Wolves - Gold
        else -> MaterialTheme.colorScheme.primary // Default FPL Purple
    }
}

// Get contrasting text color for team badges
fun getTeamTextColor(teamId: Int): Color {
    return when (teamId) {
        9 -> Color.White // Fulham (black background)
        15 -> Color.White // Newcastle (black background)
        20 -> Color.Black // Wolves (gold background)
        13 -> Color.Black // Man City (light blue background)
        else -> Color.White // Most teams use white text
    }
}

