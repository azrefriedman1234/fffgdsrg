package com.pasiflonet.mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = AppGraph(app)
    private val repo = graph.tdRepository

    val status: StateFlow<String> = repo.status

    fun saveApi(apiId: Int, apiHash: String) = viewModelScope.launch {
        repo.saveApi(apiId, apiHash)
    }

    fun sendPhone(phone: String) = viewModelScope.launch {
        repo.sendPhone(phone)
    }

    fun sendCode(code: String) = viewModelScope.launch {
        repo.sendCode(code)
    }

    fun sendPassword(password: String) = viewModelScope.launch {
        repo.sendPassword(password)
    }
}
