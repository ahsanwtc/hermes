package pro.jsan.hermes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pro.jsan.hermes.ui.screens.*import pro.jsan.hermes.ui.theme.*

@Composable
fun NavGraph() {
    val nav = rememberNavController()
    val currentRoute by nav.currentBackStackEntryAsState()
    val route = currentRoute?.destination?.route

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (route != null && !route.startsWith("rule_editor")) {
                Box(Modifier.background(SurfaceVariant.copy(alpha = 0.6f))) {
                    NavigationBar(containerColor = androidx.compose.ui.graphics.Color.Transparent) {
                        NavigationBarItem(
                            selected = route == "home",
                            onClick = { nav.navigate("home") { launchSingleTop = true } },
                            icon = { Icon(Icons.Default.Home, null) },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = route == "download",
                            onClick = { nav.navigate("download") { launchSingleTop = true } },
                            icon = { Icon(Icons.Default.Download, null) },
                            label = { Text("Download") }
                        )
                        NavigationBarItem(
                            selected = route == "rules",
                            onClick = { nav.navigate("rules") { launchSingleTop = true } },
                            icon = { Icon(Icons.Default.List, null) },
                            label = { Text("Rules") }
                        )
                        NavigationBarItem(
                            selected = route == "sync_log",
                            onClick = { nav.navigate("sync_log") { launchSingleTop = true } },
                            icon = { Icon(Icons.Default.History, null) },
                            label = { Text("Log") }
                        )
                        NavigationBarItem(
                            selected = route == "settings",
                            onClick = { nav.navigate("settings") { launchSingleTop = true } },
                            icon = { Icon(Icons.Default.Settings, null) },
                            label = { Text("Settings") }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen() }
            composable("download") { DownloadScreen() }
            composable("rules") {
                RulesScreen(
                    onAddRule = { nav.navigate("rule_editor/-1") },
                    onEditRule = { nav.navigate("rule_editor/$it") }
                )
            }
            composable(
                "rule_editor/{ruleId}",
                arguments = listOf(navArgument("ruleId") { type = NavType.IntType })
            ) { back ->
                RuleEditorScreen(
                    ruleId = back.arguments!!.getInt("ruleId"),
                    onSaved = { nav.popBackStack() }
                )
            }
            composable("sync_log") { SyncLogScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}
