package com.pasiflonet.mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pasiflonet.mobile.td.TdRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val repo: TdRepository = AppGraph.tdRepository(app)
    val authState: StateFlow<String> = repo.authState

    fun sendPhone(phone: String) {
        if (phone.isBlank()) return
        repo.setPhone(phone)
    }

    fun sendCode(code: String) {
        if (code.isBlank()) return
        repo.sendCode(code)
    }

    fun sendPassword(pass: String) {
        if (pass.isBlank()) return
        repo.sendPassword(pass)
    }
}
