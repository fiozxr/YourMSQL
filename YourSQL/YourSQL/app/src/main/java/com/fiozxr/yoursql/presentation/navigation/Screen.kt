package com.fiozxr.yoursql.presentation.navigation

sealed class Screen(val route: String, val title: String, val icon: String) {
    object Home : Screen("home", "Home", "home")
    object Tables : Screen("tables", "Tables", "table")
    object Query : Screen("query", "Query", "code")
    object Schema : Screen("schema", "Schema", "schema")
    object Logs : Screen("logs", "Logs", "list")
    object Auth : Screen("auth", "Auth", "lock")
    object Storage : Screen("storage", "Storage", "folder")
    object Settings : Screen("settings", "Settings", "settings")

    companion object {
        val bottomNavItems = listOf(Home, Tables, Query, Logs, Settings)
    }
}
