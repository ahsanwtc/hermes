package pro.jsan.hermes.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pro.jsan.hermes.ui.components.GoldButton
import pro.jsan.hermes.ui.components.RecessedField
import pro.jsan.hermes.ui.theme.*
import pro.jsan.hermes.ui.viewmodel.DownloadViewModel

@Composable
fun DownloadScreen() {
    val vm: DownloadViewModel = hiltViewModel()

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { vm.onFolderPicked(it) }
        }
    }

    val folderLabel = vm.folderUri?.lastPathSegment?.substringAfter(':') ?: "Select folder"

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Download", fontSize = 24.sp, fontWeight = FontWeight.Black, color = OnSurface)
        Text("Paste a URL to download a file into any folder.", color = OnSurfaceVariant, fontSize = 14.sp)

        RecessedField(vm.url, { vm.url = it }, "https://example.com/file.jpg")

        // Folder picker button
        OutlinedButton(
            onClick = { folderPicker.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (vm.folderUri != null) Primary else OnSurfaceVariant)
        ) {
            Text(folderLabel)
        }

        GoldButton(
            text = if (vm.isDownloading) "Downloading…" else "Download",
            modifier = Modifier.fillMaxWidth()
        ) {
            vm.download()
        }

        if (vm.status.isNotEmpty()) {
            Text(
                vm.status,
                color = if (vm.status.startsWith("✓")) Primary else MaterialTheme.colorScheme.error,
                fontSize = 13.sp
            )
        }
    }
}
