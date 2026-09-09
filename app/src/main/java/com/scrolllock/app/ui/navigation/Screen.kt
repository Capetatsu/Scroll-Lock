package com.scrolllock.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Apps : Screen("apps", "Apps", Icons.Default.Apps)
    data object AntiScroll : Screen("antiscroll", "Anti-Scroll", Icons.Default.Swipe)
    data object AntiReels : Screen("antireels", "Anti-Reels", Icons.Default.Videocam)
    data object Browser : Screen("browser", "Browser", Icons.Default.Language)
    data object Schedules : Screen("schedules", "Schedules", Icons.Default.Schedule)
    data object Cooldown : Screen("cooldown", "Cooldown", Icons.Default.Timer)
    data object Statistics : Screen("statistics", "Statistics", Icons.Default.BarChart)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Debug : Screen("debug", "Debug", Icons.Default.BugReport)

    companion object {
        val allScreens = listOf(
            Dashboard, Apps, AntiScroll, AntiReels, Browser,
            Schedules, Cooldown, Statistics, Settings
        )
    }
}
