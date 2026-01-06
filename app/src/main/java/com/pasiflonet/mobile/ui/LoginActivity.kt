package com.pasiflonet.mobile.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.pasiflonet.mobile.R
import com.pasiflonet.mobile.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private lateinit var b: ActivityLoginBinding
    private val vm: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnSendPhone.setOnClickListener { vm.sendPhone(b.etPhone.text?.toString().orEmpty()) }
        b.btnSendCode.setOnClickListener { vm.sendCode(b.etCode.text?.toString().orEmpty()) }
        b.btnSendPassword.setOnClickListener { vm.sendPassword(b.etPassword.text?.toString().orEmpty()) }

        lifecycleScope.launch {
            vm.authState.collect { state ->
                b.tvStatus.text = when (state) {
                    "MISSING_API" -> getString(R.string.missing_api_credentials)
                    else -> "מצב TDLib: $state"
                }
                if (state == "READY") {
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}
