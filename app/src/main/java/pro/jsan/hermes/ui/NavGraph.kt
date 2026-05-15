package pro.jsan.hermes.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pro.jsan.hermes.ui.screens.*

@Composable
fun NavGraph() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateRules = { nav.navigate("rules") },
                onNavigateLog = { nav.navigate("sync_log") },
                onNavigateSettings = { nav.navigate("settings") }
            )
        }
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
