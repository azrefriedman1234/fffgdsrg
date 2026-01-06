package com.pasiflonet.mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pasiflonet.mobile.data.AppPrefs
import com.pasiflonet.mobile.td.TdRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = AppPrefs(app)
    private val repo = TdRepository(app, prefs)

    val status: StateFlow<String> = repo.status
    val sources: StateFlow<List<SourceRow>> = repo.sources

    fun refreshSources() {
        viewModelScope.launch {
            repo.loadSources()
        }
    }
}
