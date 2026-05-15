package pro.jsan.hermes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pro.jsan.hermes.ui.theme.*
import pro.jsan.hermes.ui.viewmodel.SyncLogViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SyncLogScreen() {
    val vm: SyncLogViewModel = hiltViewModel()
    val files by vm.recentFiles.collectAsState()
    val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Scaffold(containerColor = Background) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            item { Spacer(Modifier.height(16.dp)) }
            item {
                Text("Sync Log", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    color = OnSurface, modifier = Modifier.padding(bottom = 16.dp))
            }
            items(files) { file ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(SurfaceContainerLow, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            file.localUri.substringAfterLast('/'),
                            color = OnSurface, fontSize = 15.sp
                        )
                        Text(
                            fmt.format(Date(file.uploadedAt)),
                            color = OnSurfaceVariant.copy(alpha = 0.6f), fontSize = 12.sp
                        )
                    }
                    Icon(Icons.Default.CheckCircle, null, tint = Primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
