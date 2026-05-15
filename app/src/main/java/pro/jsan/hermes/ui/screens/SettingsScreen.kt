package pro.jsan.hermes.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import pro.jsan.hermes.ui.components.GoldButton
import pro.jsan.hermes.ui.components.RecessedField
import pro.jsan.hermes.ui.theme.*
import pro.jsan.hermes.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen() {
    val vm: SettingsViewModel = hiltViewModel()
    val uploadOnMobile by vm.uploadOnMobileData.collectAsState(initial = false)

    Scaffold(containerColor = Background) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnSurface)

            Text("ACCOUNT", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp))

            RecessedField(vm.email, { vm.email = it }, "Email")
            RecessedField(vm.password, { vm.password = it }, "Password", visualTransformation = PasswordVisualTransformation())
            RecessedField(vm.twoFactorCode, { vm.twoFactorCode = it }, "2FA Code (if enabled)")

            GoldButton("Sign In", Modifier.fillMaxWidth()) { vm.login() }

            if (vm.loginError.isNotEmpty()) {
                Text(vm.loginError, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))
            Text("SYNC", color = OnSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp))

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Upload on mobile data", color = OnSurface, modifier = Modifier.weight(1f))
                Switch(
                    checked = uploadOnMobile,
                    onCheckedChange = { vm.setUploadOnMobileData(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = OnPrimary, checkedTrackColor = Primary)
                )
            }
        }
    }
}
