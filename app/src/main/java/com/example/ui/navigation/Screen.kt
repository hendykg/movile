package com.example.ui.navigation

sealed class Screen(val route: String) {
    object LoginRegister : Screen("login_register")
    object HomeContainer : Screen("home_container")
    object GroupDetails : Screen("group_details/{groupId}") {
        fun createRoute(groupId: Int) = "group_details/$groupId"
    }
    object MatchDetails : Screen("match_details/{matchId}") {
        fun createRoute(matchId: Int) = "match_details/$matchId"
    }
    object StadiumDetails : Screen("stadium_details/{stadiumId}") {
        fun createRoute(stadiumId: Int) = "stadium_details/$stadiumId"
    }
    object SyncLogs : Screen("sync_logs")
}
