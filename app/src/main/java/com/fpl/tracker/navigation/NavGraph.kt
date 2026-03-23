package com.fpl.tracker.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fpl.tracker.ui.screens.*

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object MainApp : Screen("main_app")
    object Initial : Screen("initial")
    object ManagerStats : Screen("manager_stats/{managerId}") {
        fun createRoute(managerId: Long) = "manager_stats/$managerId"
    }
    object LeagueStandings : Screen("league_standings/{leagueId}") {
        fun createRoute(leagueId: Long) = "league_standings/$leagueId"
    }
    object ManagerFormation : Screen("manager_formation/{managerId}/{eventId}/{teamName}") {
        fun createRoute(managerId: Long, eventId: Int, teamName: String) =
            "manager_formation/$managerId/$eventId/${Uri.encode(teamName)}"
    }
    object TransferHistory : Screen("transfer_history/{managerId}/{teamName}") {
        fun createRoute(managerId: Long, teamName: String) =
            "transfer_history/$managerId/${Uri.encode(teamName)}"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        
        composable(Screen.MainApp.route) {
            MainAppScreen(navController)
        }
        
        composable(Screen.Initial.route) {
            EnhancedInitialScreen(navController)
        }
        
        composable(
            route = Screen.ManagerStats.route,
            arguments = listOf(navArgument("managerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val managerId = backStackEntry.arguments?.getLong("managerId") ?: 0L
            ManagerStatsScreen(navController, managerId)
        }
        
        composable(
            route = Screen.LeagueStandings.route,
            arguments = listOf(navArgument("leagueId") { type = NavType.LongType })
        ) { backStackEntry ->
            val leagueId = backStackEntry.arguments?.getLong("leagueId") ?: 0L
            EnhancedLeagueStandingsScreen(navController, leagueId)
        }
        
        composable(
            route = Screen.ManagerFormation.route,
            arguments = listOf(
                navArgument("managerId") { type = NavType.LongType },
                navArgument("eventId") { type = NavType.IntType },
                navArgument("teamName") {
                    type = NavType.StringType
                    defaultValue = "Team Formation"
                }
            )
        ) { backStackEntry ->
            val managerId = backStackEntry.arguments?.getLong("managerId") ?: 0L
            val eventId = backStackEntry.arguments?.getInt("eventId") ?: 1
            val rawTeamName = backStackEntry.arguments?.getString("teamName") ?: "Team Formation"
            val decodedTeamName = Uri.decode(rawTeamName).takeIf { it.isNotBlank() } ?: "Team Formation"
            ManagerFormationScreen(navController, managerId, eventId, decodedTeamName)
        }

        composable(
            route = Screen.TransferHistory.route,
            arguments = listOf(
                navArgument("managerId") { type = NavType.LongType },
                navArgument("teamName") {
                    type = NavType.StringType
                    defaultValue = "Transfer History"
                }
            )
        ) { backStackEntry ->
            val managerId = backStackEntry.arguments?.getLong("managerId") ?: 0L
            val rawTeamName = backStackEntry.arguments?.getString("teamName") ?: "Transfer History"
            val decodedTeamName = Uri.decode(rawTeamName).takeIf { it.isNotBlank() } ?: "Transfer History"
            TransferHistoryScreen(navController, managerId, decodedTeamName)
        }
    }
}
