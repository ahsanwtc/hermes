package pro.jsan.hermes.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import pro.jsan.hermes.data.FilenApiClient
import pro.jsan.hermes.data.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val api: FilenApiClient
) : ViewModel() {

    var email by mutableStateOf(settings.email)
    var password by mutableStateOf("")
    var twoFactorCode by mutableStateOf("")
    var loginError by mutableStateOf("")

    var isLoggedIn by mutableStateOf(settings.isLoggedIn)
        private set
    val uploadOnMobileData = settings.uploadOnMobileData

    fun login() = viewModelScope.launch {
        loginError = ""
        runCatching { api.login(email, password, twoFactorCode.ifBlank { "XXXXXX" }) }
            .onSuccess { isLoggedIn = true }
            .onFailure {
                Log.e("Hermes", "Login failed", it)
                loginError = it.message ?: "Login failed"
            }
    }

    fun logout() {
        settings.apiKey = ""
        settings.masterKey = ""
        settings.email = ""
        email = ""
        password = ""
        twoFactorCode = ""
        loginError = ""
        isLoggedIn = false
    }

    fun setUploadOnMobileData(value: Boolean) = viewModelScope.launch {
        settings.setUploadOnMobileData(value)
    }
}
