package com.pasiflonet.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pasiflonet.mobile.td.TdRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.pasiflonet.mobile.td.SourceRow

class MainViewModel(
    private val repo: TdRepository
) : ViewModel() {

    val sources: StateFlow<List<SourceRow>> = repo.sources
    val status = repo.status

    fun refreshSources() {
        viewModelScope.launch {
            repo.loadSources()
        }
    }
}
