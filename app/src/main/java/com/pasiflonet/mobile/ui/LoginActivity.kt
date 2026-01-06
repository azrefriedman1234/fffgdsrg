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

        // Placeholder wiring – no assumptions about IDs inside layout
        lifecycleScope.launch {
            vm.status.collect { _ ->
                // update UI later
            }
        }
    }
}
