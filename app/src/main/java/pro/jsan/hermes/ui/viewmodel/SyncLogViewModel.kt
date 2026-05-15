package pro.jsan.hermes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import pro.jsan.hermes.data.db.SyncedFileDao
import javax.inject.Inject

@HiltViewModel
class SyncLogViewModel @Inject constructor(dao: SyncedFileDao) : ViewModel() {

    val recentFiles = dao.getRecent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
