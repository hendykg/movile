package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.viewmodel.QuinielaViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: QuinielaViewModel
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.HomeContainer.route else Screen.LoginRegister.route
    ) {
        // Auth entry screen
        composable(Screen.LoginRegister.route) {
            LoginRegisterScreen(
                viewModel = viewModel,
                onNavigateToHome = {
                    navController.navigate(Screen.HomeContainer.route) {
                        popUpTo(Screen.LoginRegister.route) { inclusive = true }
                    }
                }
            )
        }

        // Parent Bottom Navigation Container
        composable(Screen.HomeContainer.route) {
            HomeContainerScreen(
                viewModel = viewModel,
                onNavigateToGroupDetails = { groupId ->
                    navController.navigate(Screen.GroupDetails.createRoute(groupId))
                },
                onNavigateToMatchDetails = { matchId ->
                    navController.navigate(Screen.MatchDetails.createRoute(matchId))
                },
                onNavigateToStadiumDetails = { stadiumId ->
                    navController.navigate(Screen.StadiumDetails.createRoute(stadiumId))
                },
                onNavigateToLogs = {
                    navController.navigate(Screen.SyncLogs.route)
                },
                onLogout = {
                    navController.navigate(Screen.LoginRegister.route) {
                        popUpTo(Screen.HomeContainer.route) { inclusive = true }
                    }
                }
            )
        }

        // Group Leaderboards Details screen
        composable(
            route = Screen.GroupDetails.route,
            arguments = listOf(navArgument("groupId") { type = NavType.IntType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getInt("groupId") ?: 201
            GroupDetailScreen(
                viewModel = viewModel,
                groupId = groupId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Individual Match Detail screen
        composable(
            route = Screen.MatchDetails.route,
            arguments = listOf(navArgument("matchId") { type = NavType.IntType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getInt("matchId") ?: 101
            MatchDetailScreen(
                viewModel = viewModel,
                matchId = matchId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStadium = { stadiumId ->
                    navController.navigate(Screen.StadiumDetails.createRoute(stadiumId))
                }
            )
        }

        // Detailed Stadium screen with match calendar feeds
        composable(
            route = Screen.StadiumDetails.route,
            arguments = listOf(navArgument("stadiumId") { type = NavType.IntType })
        ) { backStackEntry ->
            val stadiumId = backStackEntry.arguments?.getInt("stadiumId") ?: 1
            StadiumDetailScreen(
                viewModel = viewModel,
                stadiumId = stadiumId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMatchDetails = { matchId ->
                    navController.navigate(Screen.MatchDetails.createRoute(matchId))
                }
            )
        }

        // Central SQLite sync logger runs history
        composable(Screen.SyncLogs.route) {
            SyncLogScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// Bottom tab visual manager container
@Composable
fun HomeContainerScreen(
    viewModel: QuinielaViewModel,
    onNavigateToGroupDetails: (Int) -> Unit,
    onNavigateToMatchDetails: (Int) -> Unit,
    onNavigateToStadiumDetails: (Int) -> Unit,
    onNavigateToLogs: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.SportsSoccer, contentDescription = "Partidos") },
                    label = { Text("Partidos") },
                    modifier = Modifier.testTag("tab_matches")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Group, contentDescription = "Grupos") },
                    label = { Text("Grupos") },
                    modifier = Modifier.testTag("tab_groups")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Map, contentDescription = "Mapa") },
                    label = { Text("Mapa") },
                    modifier = Modifier.testTag("tab_map")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
                    modifier = Modifier.testTag("tab_profile")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> MatchesScreen(
                    viewModel = viewModel,
                    onNavigateToMatchDetails = onNavigateToMatchDetails
                )
                1 -> GroupsScreen(
                    viewModel = viewModel,
                    onNavigateToGroupDetails = onNavigateToGroupDetails
                )
                2 -> StadiumsMapScreen(
                    viewModel = viewModel,
                    onNavigateToStadiumDetails = onNavigateToStadiumDetails
                )
                3 -> ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToLogs = onNavigateToLogs,
                    onLogout = onLogout
                )
            }
        }
    }
}
