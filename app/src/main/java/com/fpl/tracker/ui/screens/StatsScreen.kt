package com.fpl.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val StatsBackground = Color(0xFF131313)
private val StatsPrimary    = Color(0xFFA1D494)
private val StatsOutline    = Color(0xFF8C9387)

@Composable
fun StatsScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StatsBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "📊",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "STATS",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = StatsPrimary,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Coming soon",
                fontSize = 13.sp,
                color = StatsOutline
            )
        }
    }
}
