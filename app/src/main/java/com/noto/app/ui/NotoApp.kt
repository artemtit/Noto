package com.noto.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.noto.app.R
import com.noto.app.di.ServiceContainer
import com.noto.app.ui.navigation.Routes
import com.noto.app.ui.screens.calendar.CalendarScreen
import com.noto.app.ui.screens.inbox.InboxScreen
import com.noto.app.ui.screens.projects.ProjectDetailScreen
import com.noto.app.ui.screens.projects.ProjectsScreen
import com.noto.app.ui.screens.review.ReviewScreen
import com.noto.app.ui.screens.search.SearchScreen
import com.noto.app.ui.screens.settings.SettingsScreen
import com.noto.app.ui.screens.taskdetails.TaskDetailsScreen
import com.noto.app.ui.screens.today.TodayScreen
import com.noto.app.ui.screens.voice.VoiceCaptureScreen

private data class BottomTab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun NotoApp(container: ServiceContainer) {
    val navController = rememberNavController()

    val tabs = listOf(
        BottomTab(Routes.TODAY, R.string.tab_today, Icons.Rounded.CalendarToday),
        BottomTab(Routes.CALENDAR, R.string.tab_calendar, Icons.Rounded.CalendarMonth),
        BottomTab(Routes.INBOX, R.string.tab_inbox, Icons.Rounded.Inbox),
        BottomTab(Routes.PROJECTS, R.string.tab_projects, Icons.Rounded.Folder),
        BottomTab(Routes.SETTINGS, R.string.tab_settings, Icons.Rounded.Settings),
    )

    Scaffold(
        bottomBar = {
            val backStack by navController.currentBackStackEntryAsState()
            val current = backStack?.destination
            val onTab = current?.hierarchy?.any { it.route in tabs.map { t -> t.route } } == true
            if (onTab) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = current?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = Routes.TODAY,
            modifier = Modifier.padding(inner),
        ) {
            composable(Routes.TODAY) {
                TodayScreen(
                    container = container,
                    onOpenTask = { navController.navigate(Routes.taskDetails(it)) },
                    onMic = { navController.navigate(Routes.VOICE) },
                    onSearch = { navController.navigate(Routes.SEARCH) },
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(
                    container = container,
                    onOpenTask = { navController.navigate(Routes.taskDetails(it)) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    container = container,
                    onOpenTask = { navController.navigate(Routes.taskDetails(it)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.INBOX) {
                InboxScreen(
                    container = container,
                    onOpenTask = { navController.navigate(Routes.taskDetails(it)) },
                    onMic = { navController.navigate(Routes.VOICE) },
                )
            }
            composable(Routes.PROJECTS) {
                ProjectsScreen(
                    container = container,
                    onOpen = { navController.navigate(Routes.project(it)) },
                )
            }
            composable(
                route = Routes.PROJECT,
                arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("projectId") ?: 0L
                ProjectDetailScreen(
                    container = container,
                    projectId = id,
                    onBack = { navController.popBackStack() },
                    onOpenTask = { navController.navigate(Routes.taskDetails(it)) },
                )
            }
            composable(
                route = Routes.TASK_DETAILS,
                arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("taskId") ?: 0L
                TaskDetailsScreen(
                    container = container,
                    taskId = id,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.VOICE) {
                VoiceCaptureScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onRecognized = { text ->
                        navController.navigate(Routes.review(text)) {
                            popUpTo(Routes.VOICE) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = Routes.REVIEW,
                arguments = listOf(navArgument("transcript") { type = NavType.StringType }),
            ) { entry ->
                val encoded = entry.arguments?.getString("transcript").orEmpty()
                val transcript = runCatching { java.net.URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(encoded)
                ReviewScreen(
                    container = container,
                    transcript = transcript,
                    onDone = {
                        navController.popBackStack(Routes.TODAY, inclusive = false)
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(container = container)
            }
        }
    }
}
