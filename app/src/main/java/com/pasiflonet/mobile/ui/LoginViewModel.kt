package com.pasiflonet.mobile.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel : ViewModel() {
    private val _status = MutableStateFlow("idle")
    val status: StateFlow<String> = _status

    fun setPhone(phone: String) {
        _status.value = "phone_set"
    }

    fun sendCode(code: String) {
        _status.value = "code_sent"
    }

    fun sendPassword(password: String) {
        _status.value = "password_sent"
    }
}
