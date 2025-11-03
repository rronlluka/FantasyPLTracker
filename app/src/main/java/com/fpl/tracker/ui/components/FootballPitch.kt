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
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val pitchColor = Color(0xFF00B050)
        val lineColor = Color.White
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
    val basePoints = playerDetail.liveStats?.stats?.totalPoints ?: playerDetail.player.eventPoints
    val isCaptain = playerDetail.pick.isCaptain
    val isViceCaptain = playerDetail.pick.isViceCaptain
    
    // Add provisional bonus for live players
    var points = basePoints
    if (playerDetail.isLive) {
        val currentBonus = playerDetail.liveStats?.stats?.bonus ?: 0
        val provisionalBonusPoints = provisionalBonus[playerDetail.player.id] ?: 0
        if (currentBonus == 0 && provisionalBonusPoints > 0) {
            points += provisionalBonusPoints
        }
    }
    
    val displayPoints = points * playerDetail.pick.multiplier
    
    // Get stats from live data
    val goals = playerDetail.liveStats?.stats?.goalsScored ?: 0
    val assists = playerDetail.liveStats?.stats?.assists ?: 0
    
    // Pulsing animation for live players
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
        modifier = Modifier.width(68.dp)
    ) {
        // Captain/Vice-Captain badge OR Live indicator
        if (isCaptain || isViceCaptain) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = if (isCaptain) Color(0xFF37003C) else Color(0xFF666666),
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
                    .height(18.dp)
                    .background(
                        color = Color(0xFFFF0000).copy(alpha = alpha),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Spacer(modifier = Modifier.height(22.dp))
        }

        // Player card with border for live players
        Card(
            modifier = Modifier
                .width(68.dp)
                .height(92.dp)
                .clickable { onPlayerClick?.invoke(playerDetail) }
                .then(
                    if (playerDetail.isLive) {
                        Modifier.border(
                            width = 2.dp,
                            color = Color(0xFFFF0000).copy(alpha = alpha),
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else Modifier
                ),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Jersey/Shirt representation
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            color = getTeamColor(playerDetail.team.id),
                            shape = RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = playerDetail.team.shortName.take(3).uppercase(),
                        color = getTeamTextColor(playerDetail.team.id),
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // Player name
                Text(
                    text = playerDetail.player.webName,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                // Points with goals/assists
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = when {
                                    displayPoints >= 10 -> Color(0xFF00FF87).copy(alpha = 0.3f)
                                    displayPoints >= 6 -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                                    displayPoints <= 2 -> Color(0xFFFF5555).copy(alpha = 0.3f)
                                    else -> Color(0xFFF0F0F0)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$displayPoints pts",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    
                    // Goals and Assists row
                    if (goals > 0 || assists > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (goals > 0) {
                                Text(
                                    text = "⚽$goals",
                                    fontSize = 8.sp,
                                    color = Color.Black
                                )
                            }
                            if (goals > 0 && assists > 0) {
                                Text(
                                    text = " ",
                                    fontSize = 8.sp
                                )
                            }
                            if (assists > 0) {
                                Text(
                                    text = "🅰️$assists",
                                    fontSize = 8.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                    
                    // Opponent team
                    playerDetail.opponentTeam?.let { opponent ->
                        val isHome = playerDetail.fixture?.teamH == playerDetail.team.id
                        Text(
                            text = "${if (isHome) "vs" else "@"} ${opponent.shortName}",
                            fontSize = 7.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// Simple team color mapping (you can enhance this with more teams)
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
        else -> Color(0xFF37003C) // Default FPL Purple
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

