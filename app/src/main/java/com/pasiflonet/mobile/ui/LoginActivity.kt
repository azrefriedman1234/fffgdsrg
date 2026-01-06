package com.pasiflonet.mobile.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pasiflonet.mobile.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding
    private val vm: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnSaveApi.setOnClickListener {
            val id = b.etApiId.text?.toString()?.trim()?.toIntOrNull() ?: 0
            val hash = b.etApiHash.text?.toString()?.trim().orEmpty()
            vm.saveApi(id, hash)
        }

        b.btnSendPhone.setOnClickListener {
            vm.sendPhone(b.etPhone.text?.toString()?.trim().orEmpty())
        }

        b.btnSendCode.setOnClickListener {
            vm.sendCode(b.etCode.text?.toString()?.trim().orEmpty())
        }

        b.btnSendPassword.setOnClickListener {
            vm.sendPassword(b.etPassword.text?.toString()?.trim().orEmpty())
        }

        lifecycleScope.launch {
            vm.status.collect { s ->
                b.tvStatus.text = "סטטוס: $s"
            }
        }
    }
}
