package pro.jsan.hermes.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pro.jsan.hermes.data.model.SyncRule
import pro.jsan.hermes.ui.components.GoldButton
import pro.jsan.hermes.ui.components.RecessedField
import pro.jsan.hermes.ui.theme.*
import pro.jsan.hermes.ui.viewmodel.RulesViewModel

@Composable
fun RuleEditorScreen(ruleId: Int, onSaved: () -> Unit) {
    val vm: RulesViewModel = hiltViewModel()
    val context = LocalContext.current
    val existing = if (ruleId != -1) vm.rules.collectAsState().value.find { it.id == ruleId } else null

    var localPath by remember { mutableStateOf(existing?.localPath ?: "") }
    var cloudPath by remember { mutableStateOf(existing?.cloudPath ?: "") }
    var deleteAfter by remember { mutableStateOf(existing?.deleteAfterUpload ?: false) }
    var retainFilter by remember { mutableStateOf(existing?.retainFilter ?: "") }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                localPath = uri.toString()
            }
        }
    }

    Scaffold(containerColor = Background) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(if (ruleId == -1) "New Rule" else "Edit Rule",
                fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnSurface)

            Text("Local Folder", color = OnSurfaceVariant, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val displayPath = if (localPath.startsWith("content://"))
                    android.net.Uri.parse(localPath).lastPathSegment?.substringAfter(':') ?: localPath
                else localPath
                RecessedField(displayPath, { localPath = it }, "e.g. DCIM/Camera", Modifier.weight(1f))
                OutlinedButton(onClick = {
                    folderPicker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
                }) { Text("Browse", color = Primary) }
            }

            Text("Cloud Path", color = OnSurfaceVariant, fontSize = 12.sp)
            RecessedField(cloudPath, { cloudPath = it }, "e.g. /Phone/Camera")

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Delete after upload", color = OnSurface, modifier = Modifier.weight(1f))
                Switch(
                    checked = deleteAfter,
                    onCheckedChange = { deleteAfter = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = OnPrimary, checkedTrackColor = Primary)
                )
            }

            AnimatedVisibility(visible = deleteAfter) {
                Column {
                    Text("Retain extensions (e.g. raw, dng)", color = OnSurfaceVariant, fontSize = 12.sp)
                    RecessedField(retainFilter, { retainFilter = it }, "raw, dng")
                }
            }

            Spacer(Modifier.weight(1f))

            GoldButton("Save", Modifier.fillMaxWidth()) {
                if (localPath.isNotBlank() && cloudPath.isNotBlank()) {
                    vm.upsert(SyncRule(
                        id = existing?.id ?: 0,
                        localPath = localPath,
                        cloudPath = cloudPath,
                        deleteAfterUpload = deleteAfter,
                        retainFilter = retainFilter.ifBlank { null }
                    ))
                    onSaved()
                }
            }
        }
    }
}
