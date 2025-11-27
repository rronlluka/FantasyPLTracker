package com.fpl.tracker.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fpl.tracker.data.preferences.PreferencesManager

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Leagues : BottomNavItem("leagues", Icons.Filled.EmojiEvents, "Leagues")
    object Profile : BottomNavItem("profile", Icons.Filled.Person, "Profile")
    object Matches : BottomNavItem("matches", Icons.Filled.SportsScore, "Matches")
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
        // Should not happen, but redirect to login if no manager ID
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
                title = { Text("FPL Tracker") },
                actions = {
                    IconButton(onClick = {
                        prefsManager.clearAll()
                        mainNavController.navigate(com.fpl.tracker.navigation.Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.Logout, "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                val items = listOf(
                    BottomNavItem.Leagues,
                    BottomNavItem.Profile,
                    BottomNavItem.Matches
                )
                
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            bottomNavController.navigate(item.route) {
                                popUpTo(BottomNavItem.Leagues.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
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
            composable(BottomNavItem.Matches.route) {
                MatchesScreen(mainNavController)
            }
        }
    }
}

