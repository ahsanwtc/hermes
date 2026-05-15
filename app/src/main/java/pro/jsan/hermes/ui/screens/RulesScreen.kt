package pro.jsan.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pro.jsan.hermes.data.model.SyncRule
import pro.jsan.hermes.ui.theme.*
import pro.jsan.hermes.ui.viewmodel.RulesViewModel

@Composable
fun RulesScreen(onAddRule: () -> Unit, onEditRule: (Int) -> Unit) {
    val vm: RulesViewModel = hiltViewModel()
    val rules by vm.rules.collectAsState()

    Scaffold(
        containerColor = Background,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRule, containerColor = SurfaceContainerHighest) {
                Icon(Icons.Default.Add, null, tint = Primary)
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item { Spacer(Modifier.height(16.dp)) }
            item {
                Text("Sync Rules", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = OnSurface, modifier = Modifier.padding(bottom = 16.dp))
            }
            items(rules) { rule -> RuleCard(rule, vm, onEditRule) }
        }
    }
}

@Composable
private fun RuleCard(rule: SyncRule, vm: RulesViewModel, onEdit: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(SurfaceContainerLow, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(rule.localPath, color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("→ ${rule.cloudPath}", color = OnSurfaceVariant, fontSize = 12.sp)
        }
        Switch(
            checked = rule.enabled,
            onCheckedChange = { vm.upsert(rule.copy(enabled = it)) },
            colors = SwitchDefaults.colors(checkedThumbColor = OnPrimary, checkedTrackColor = Primary)
        )
    }
    Spacer(Modifier.height(16.dp))
}
