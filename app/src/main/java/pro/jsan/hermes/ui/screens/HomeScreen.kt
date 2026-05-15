package pro.jsan.hermes.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jsan.hermes.service.SyncService
import pro.jsan.hermes.ui.components.GoldButton
import pro.jsan.hermes.ui.theme.*

@Suppress("DEPRECATION")
private fun isSyncServiceRunning(context: Context): Boolean {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return am.getRunningServices(Int.MAX_VALUE).any {
        it.service.className == SyncService::class.java.name
    }
}

@Composable
fun HomeScreen(onNavigateRules: () -> Unit, onNavigateLog: () -> Unit, onNavigateSettings: () -> Unit) {
    val context = LocalContext.current
    var serviceRunning by remember { mutableStateOf(isSyncServiceRunning(context)) }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            Box(Modifier.fillMaxWidth().background(SurfaceVariant.copy(alpha = 0.6f))) {
                NavigationBar(containerColor = androidx.compose.ui.graphics.Color.Transparent) {
                    NavigationBarItem(selected = true, onClick = {}, icon = {
                        Icon(Icons.Default.Home, null)
                    }, label = { Text("Home") })
                    NavigationBarItem(selected = false, onClick = onNavigateRules, icon = {
                        Icon(Icons.Default.List, null)
                    }, label = { Text("Rules") })
                    NavigationBarItem(selected = false, onClick = onNavigateLog, icon = {
                        Icon(Icons.Default.History, null)
                    }, label = { Text("Log") })
                    NavigationBarItem(selected = false, onClick = onNavigateSettings, icon = {
                        Icon(Icons.Default.Settings, null)
                    }, label = { Text("Settings") })
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text("Hermes", fontSize = 36.sp, fontWeight = FontWeight.Black, color = OnSurface)
                Surface(
                    color = if (serviceRunning) Primary.copy(alpha = 0.15f) else SurfaceContainerHigh,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(9999.dp)
                ) {
                    Text(
                        if (serviceRunning) "● Running" else "○ Stopped",
                        color = if (serviceRunning) Primary else OnSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            GoldButton(
                text = if (serviceRunning) "Stop Sync" else "Start Sync",
                modifier = Modifier.fillMaxWidth()
            ) {
                if (serviceRunning) {
                    context.stopService(Intent(context, SyncService::class.java))
                } else {
                    context.startForegroundService(Intent(context, SyncService::class.java))
                }
                serviceRunning = !serviceRunning
            }
        }
    }
}
