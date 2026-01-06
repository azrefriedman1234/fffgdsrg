package com.pasiflonet.mobile.ui

import android.content.Intent
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
            vm.saveApiAndInit(
                apiIdStr = b.etApiId.text?.toString() ?: "",
                apiHashStr = b.etApiHash.text?.toString() ?: ""
            )
        }

        b.btnSendPhone.setOnClickListener {
            vm.sendPhone(b.etPhone.text?.toString() ?: "")
        }

        b.btnSendCode.setOnClickListener {
            vm.sendCode(b.etCode.text?.toString() ?: "")
        }

        b.btnSendPassword.setOnClickListener {
            vm.sendPassword(b.etPassword.text?.toString() ?: "")
        }

        b.btnContinue.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        lifecycleScope.launch {
            vm.state.collect { st ->
                b.tvStatus.text = st.status

                // טען ערכים שמורים לתוך השדות אם ריקים (לא לדרוס הקלדה)
                if (b.etApiId.text.isNullOrEmpty() && st.savedApiId.isNotBlank()) b.etApiId.setText(st.savedApiId)
                if (b.etApiHash.text.isNullOrEmpty() && st.savedApiHash.isNotBlank()) b.etApiHash.setText(st.savedApiHash)
                if (b.etPhone.text.isNullOrEmpty() && st.savedPhone.isNotBlank()) b.etPhone.setText(st.savedPhone)

                // הפעלות כפתורים לפי שלב
                b.btnSendPhone.isEnabled = st.step == LoginUiState.Step.NEED_PHONE
                b.btnSendCode.isEnabled = st.step == LoginUiState.Step.NEED_CODE
                b.btnSendPassword.isEnabled = st.step == LoginUiState.Step.NEED_PASSWORD
                b.btnContinue.isEnabled = st.ready
            }
        }
    }
}
