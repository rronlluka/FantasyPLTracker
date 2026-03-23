package com.fpl.tracker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fpl.tracker.data.preferences.PreferencesManager

// ── Design tokens ─────────────────────────────────────────────────────────────
private val NavBackground        = Color(0xFF131313)
private val NavSurface           = Color(0xFF1C1B1B)
private val NavPrimary           = Color(0xFFA1D494)
private val NavPrimaryContainer  = Color(0xFF2D5A27)
private val NavOnPrimary         = Color(0xFF0A3909)
private val NavOutlineVariant    = Color(0xFF353535)
private val NavOnSurface         = Color(0xFFE5E2E1)
private val NavOutline           = Color(0xFF8C9387)

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Leagues : BottomNavItem("leagues",  Icons.Filled.EmojiEvents,  "Leagues")
    object Profile : BottomNavItem("profile",  Icons.Filled.Group,        "My Team")
    object Stats   : BottomNavItem("stats",    Icons.Filled.Analytics,    "Stats")
    object Matches : BottomNavItem("matches",  Icons.Filled.SportsScore,  "Matches")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(mainNavController: NavController) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val managerId = prefsManager.getManagerId()

    if (managerId == null) {
        LaunchedEffect(Unit) {
            mainNavController.navigate(com.fpl.tracker.navigation.Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "FPL Tracker",
                        color = NavOnSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = {
                        prefsManager.clearAll()
                        mainNavController.navigate(com.fpl.tracker.navigation.Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.Logout, "Logout", tint = NavOnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavBackground,
                    titleContentColor = NavOnSurface,
                    actionIconContentColor = NavOnSurface
                )
            )
        },
        bottomBar = {
            StitchBottomNav(
                items = listOf(
                    BottomNavItem.Leagues,
                    BottomNavItem.Profile,
                    BottomNavItem.Stats,
                    BottomNavItem.Matches
                ),
                currentRoute = currentRoute,
                onItemClick = { item ->
                    bottomNavController.navigate(item.route) {
                        popUpTo(BottomNavItem.Leagues.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        },
        containerColor = NavBackground
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Leagues.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.Leagues.route) {
                LeaguesListScreen(mainNavController, managerId)
            }
            composable(BottomNavItem.Profile.route) {
                ManagerStatsScreen(mainNavController, managerId)
            }
            composable(BottomNavItem.Stats.route) {
                StatsScreen()
            }
            composable(BottomNavItem.Matches.route) {
                MatchesScreen(mainNavController)
            }
        }
    }
}

// ── Stitch-styled bottom nav ───────────────────────────────────────────────────
@Composable
private fun StitchBottomNav(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                // Semi-transparent background with top rounded corners
                color = NavBackground.copy(alpha = 0.95f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        // Subtle top border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NavOutlineVariant.copy(alpha = 0.3f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                StitchNavItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun StitchNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(200),
        label = "nav_scale"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected)
                    Brush.linearGradient(listOf(NavPrimary, NavPrimaryContainer))
                else
                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = if (isSelected) NavOnPrimary else NavOutline,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = item.label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) NavOnPrimary else NavOutline,
                letterSpacing = 1.sp
            )
        }
    }
}
