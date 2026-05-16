package pro.jsan.hermes.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pro.jsan.hermes.service.SyncService
import pro.jsan.hermes.ui.components.GoldButton
import pro.jsan.hermes.ui.theme.*
import pro.jsan.hermes.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
private fun isSyncServiceRunning(context: Context): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return am.getRunningServices(Int.MAX_VALUE).any {
        it.service.className == SyncService::class.java.name
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val vm: HomeViewModel = hiltViewModel()
    var serviceRunning by remember { mutableStateOf(isSyncServiceRunning(context)) }
    val rulesCount by vm.rulesCount.collectAsState()
    val filesToday by vm.filesToday.collectAsState()
    val recentFiles by vm.recentFiles.collectAsState()
    val fmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Header
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("HERMES", fontSize = 28.sp, fontWeight = FontWeight.Black, color = OnSurface, letterSpacing = 2.sp)
                Surface(color = if (serviceRunning) Primary.copy(alpha = 0.15f) else SurfaceContainerHigh, shape = RoundedCornerShape(9999.dp)) {
                    Text(
                        if (serviceRunning) "● Watching" else "○ Stopped",
                        color = if (serviceRunning) Primary else OnSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Big stat
        item {
            Column {
                Text("${filesToday} Files Today", fontSize = 36.sp, fontWeight = FontWeight.Black, color = OnSurface)
                Text("Your celestial vault is ${if (serviceRunning) "active" else "idle"}.", color = OnSurfaceVariant, fontSize = 14.sp)
            }
        }

        // Guard service card
        item {
            Row(
                Modifier.fillMaxWidth().background(SurfaceContainerLow, RoundedCornerShape(12.dp)).padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Guard Service", color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(if (serviceRunning) "Active Protocol" else "Inactive", color = OnSurfaceVariant, fontSize = 12.sp)
                }
                GoldButton(if (serviceRunning) "Stop" else "Start") {
                    if (serviceRunning) context.stopService(Intent(context, SyncService::class.java))
                    else context.startForegroundService(Intent(context, SyncService::class.java))
                    serviceRunning = !serviceRunning
                }
            }
        }

        // Stats row
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("$rulesCount", "Rules Active", Modifier.weight(1f))
                StatCard("$filesToday", "Files Today", Modifier.weight(1f))
            }
        }

        // Recent log
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recent Flight Log", color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }

        if (recentFiles.isEmpty()) {
            item { Text("No files synced yet.", color = OnSurfaceVariant, fontSize = 13.sp) }
        } else {
            items(recentFiles.take(5)) { file ->
                Row(
                    Modifier.fillMaxWidth().background(SurfaceContainerLow, RoundedCornerShape(8.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(file.localUri.substringAfterLast('/'), color = OnSurface, fontSize = 14.sp, maxLines = 1)
                        Text("Synced to cloud", color = OnSurfaceVariant, fontSize = 12.sp)
                    }
                    Text(fmt.format(Date(file.uploadedAt)), color = Primary, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(SurfaceContainerLow, RoundedCornerShape(12.dp)).padding(16.dp)
    ) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Black, color = Primary)
        Text(label, color = OnSurfaceVariant, fontSize = 12.sp)
    }
}
