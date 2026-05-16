package pro.jsan.hermes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import pro.jsan.hermes.data.db.SyncRuleDao
import pro.jsan.hermes.data.db.SyncedFileDao
import pro.jsan.hermes.data.model.SyncedFile
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    syncRuleDao: SyncRuleDao,
    syncedFileDao: SyncedFileDao
) : ViewModel() {

    val rulesCount = syncRuleDao.getAll()
        .map { it.count { r -> r.enabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentFiles = syncedFileDao.getRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filesToday = syncedFileDao.getRecent().map { files ->
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        files.count { it.uploadedAt >= startOfDay }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
