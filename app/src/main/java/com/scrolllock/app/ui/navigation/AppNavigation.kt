package com.scrolllock.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.scrolllock.app.ui.dashboard.DashboardScreen
import com.scrolllock.app.ui.apps.AppsScreen
import com.scrolllock.app.ui.antiscroll.AntiScrollScreen
import com.scrolllock.app.ui.antireels.AntiReelsScreen
import com.scrolllock.app.ui.browser.BrowserScreen
import com.scrolllock.app.ui.schedules.SchedulesScreen
import com.scrolllock.app.ui.settings.SettingsScreen
import com.scrolllock.app.ui.statistics.StatisticsScreen
import com.scrolllock.app.ui.debug.DebugScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ScrollLock") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                Screen.allScreens.take(5).forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Apps.route) { AppsScreen() }
            composable(Screen.AntiScroll.route) { AntiScrollScreen() }
            composable(Screen.AntiReels.route) { AntiReelsScreen() }
            composable(Screen.Browser.route) { BrowserScreen() }
            composable(Screen.Schedules.route) { SchedulesScreen() }
            composable(Screen.Cooldown.route) {
                com.scrolllock.app.ui.cooldown.CooldownScreen()
            }
            composable(Screen.Statistics.route) { StatisticsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.Debug.route) { DebugScreen() }
        }
    }
}
