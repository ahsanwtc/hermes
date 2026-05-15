package pro.jsan.hermes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pro.jsan.hermes.data.db.SyncRuleDao
import pro.jsan.hermes.data.model.SyncRule
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(private val dao: SyncRuleDao) : ViewModel() {

    val rules = dao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun upsert(rule: SyncRule) = viewModelScope.launch { dao.upsert(rule) }

    fun delete(rule: SyncRule) = viewModelScope.launch { dao.delete(rule) }
}
