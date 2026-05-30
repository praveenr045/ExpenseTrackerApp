package com.personal.expensetracker.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String, val label: String) {
    object Home      : Screen("home",      "Home")
    object Expenses  : Screen("expenses",  "Expenses")
    object Analytics : Screen("analytics", "Analytics")
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(Screen.Home, Screen.Expenses, Screen.Analytics)
    val icons = mapOf(
        Screen.Home.route      to Icons.Default.Home,
        Screen.Expenses.route  to Icons.Default.List,
        Screen.Analytics.route to Icons.Default.BarChart
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                // Read current destination outside the forEach so recomposition is stable
                val currentRoute by rememberUpdatedState(
                    navController.currentBackStackEntryAsState().value?.destination?.route
                )
                items.forEach { screen ->
                    NavigationBarItem(
                        icon    = { Icon(icons[screen.route]!!, contentDescription = screen.label) },
                        label   = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    // Pop everything up to (but not including) home so back-stack
                                    // stays clean; restoreState brings back scroll position etc.
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToExpenses = {
                        navController.navigate(Screen.Expenses.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    onNavigateToAnalytics = {
                        navController.navigate(Screen.Analytics.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                )
            }
            composable(Screen.Expenses.route) {
                ExpensesScreen(
                    onBack = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Analytics.route) { AnalyticsScreen() }
        }
    }
}