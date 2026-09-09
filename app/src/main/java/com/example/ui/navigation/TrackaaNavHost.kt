package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.ui.analytics.AnalyticsScreen
import com.example.ui.analytics.AnalyticsViewModel
import com.example.ui.focus.FocusScreen
import com.example.ui.focus.FocusViewModel
import com.example.ui.goals.GoalsScreen
import com.example.ui.goals.GoalsViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.more.MoreScreen
import com.example.ui.more.MoreViewModel

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun TrackaaApp() {
    val navController = rememberNavController()

    val navItems = listOf(
        BottomNavItem(Screen.Home, Icons.Default.Home, "Home"),
        BottomNavItem(Screen.Focus, Icons.Default.PlayArrow, "Focus"),
        BottomNavItem(Screen.Goals, Icons.Default.CheckCircle, "Goals"),
        BottomNavItem(Screen.Analytics, Icons.Default.DateRange, "Analytics"),
        BottomNavItem(Screen.More, Icons.Default.Menu, "More")
    )

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Shared ViewModels
    val homeViewModel: HomeViewModel = viewModel()
    val focusViewModel: FocusViewModel = viewModel()
    val goalsViewModel: GoalsViewModel = viewModel()
    val analyticsViewModel: AnalyticsViewModel = viewModel()
    val moreViewModel: MoreViewModel = viewModel()

    var focusTaskId by remember { mutableStateOf<Long?>(null) }
    var focusTaskTitle by remember { mutableStateOf<String?>("Quick Focus") }
    var focusWorkItemId by remember { mutableStateOf<Long?>(null) }
    var focusWorkItemName by remember { mutableStateOf<String?>("General") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                navItems.forEach { item ->
                    val selected = currentRoute == item.screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != item.screen.route) {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToFocus = { taskId, taskTitle, workItemId, workItemName ->
                        focusTaskId = taskId
                        focusTaskTitle = taskTitle
                        focusWorkItemId = workItemId
                        focusWorkItemName = workItemName
                        navController.navigate(Screen.Focus.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToGoals = {
                        navController.navigate(Screen.Goals.route)
                    },
                    onNavigateToAnalytics = {
                        navController.navigate(Screen.Analytics.route)
                    }
                )
            }

            composable(Screen.Focus.route) {
                FocusScreen(
                    viewModel = focusViewModel,
                    initialTaskId = focusTaskId,
                    initialTaskTitle = focusTaskTitle,
                    initialWorkItemId = focusWorkItemId,
                    initialWorkItemName = focusWorkItemName
                )
            }

            composable(Screen.Goals.route) {
                GoalsScreen(
                    viewModel = goalsViewModel,
                    onNavigateToFocus = { taskId, taskTitle, workItemId, workItemName ->
                        focusTaskId = taskId
                        focusTaskTitle = taskTitle
                        focusWorkItemId = workItemId
                        focusWorkItemName = workItemName
                        navController.navigate(Screen.Focus.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = analyticsViewModel)
            }

            composable(Screen.More.route) {
                MoreScreen(viewModel = moreViewModel)
            }
        }
    }
}
