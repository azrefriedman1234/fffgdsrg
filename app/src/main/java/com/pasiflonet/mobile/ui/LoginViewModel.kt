package com.pasiflonet.mobile.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pasiflonet.mobile.store.SettingsStore
import com.pasiflonet.mobile.td.TdClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File

data class LoginUiState(
    val status: String = "טוען...",
    val step: Step = Step.NEED_API,
    val savedApiId: String = "",
    val savedApiHash: String = "",
    val savedPhone: String = "",
    val ready: Boolean = false
) {
    enum class Step { NEED_API, NEED_PHONE, NEED_CODE, NEED_PASSWORD, READY }
}

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val store = SettingsStore(ctx)

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private var td: TdClient? = null
    private var apiId: Int = 0
    private var apiHash: String = ""

    init {
        viewModelScope.launch {
            store.authFlow.collect { saved ->
                // לא לדרוס אם המשתמש כבר התחיל להקליד (שומרים פשוט ערכים לתצוגה)
                _state.value = _state.value.copy(
                    savedApiId = if (saved.apiId == 0) "" else saved.apiId.toString(),
                    savedApiHash = saved.apiHash,
                    savedPhone = saved.phone,
                    status = "מוכן. אם יש API שמור – לחץ 'שמור API' או ערוך והמשך."
                )
            }
        }
    }

    fun saveApiAndInit(apiIdStr: String, apiHashStr: String) {
        val id = apiIdStr.trim().toIntOrNull() ?: 0
        val hash = apiHashStr.trim()
        if (id <= 0 || hash.isBlank()) {
            _state.value = _state.value.copy(status = "❌ חסר API ID/HASH", step = LoginUiState.Step.NEED_API, ready = false)
            return
        }

        apiId = id
        apiHash = hash

        viewModelScope.launch {
            store.saveApi(id, hash)
        }

        startTdlib()
    }

    private fun startTdlib() {
        _state.value = _state.value.copy(status = "מאתחל TDLib...", ready = false)

        val client = TdClient(ctx, apiId, apiHash)
        td = client

        client.onUpdate = { obj ->
            if (obj is TdApi.UpdateAuthorizationState) {
                handleAuth(obj.authorizationState)
            }
        }

        // טריגר: אם TDLib ישלח WaitTdlibParameters – נטפל שם.
        // לפעמים צריך "פינג" קטן כדי לקבל עדכונים ראשונים:
        client.send(TdApi.GetAuthorizationState()) { }
    }

    private fun handleAuth(s: TdApi.AuthorizationState?) {
        when (s) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val dbDir = File(ctx.filesDir, "tdlib").apply { mkdirs() }.absolutePath
                val filesDir = File(ctx.filesDir, "tdfiles").apply { mkdirs() }.absolutePath
                td?.setTdlibParameters(dbDir, filesDir)
                postStatus("הוגדרו פרמטרים. מחכה לטלפון/או המשך אוטומטי אם שמור.")
            }

            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                _state.value = _state.value.copy(
                    step = LoginUiState.Step.NEED_PHONE,
                    ready = false,
                    status = "הזן טלפון ולחץ 'שלח טלפון'"
                )
            }

            is TdApi.AuthorizationStateWaitCode -> {
                _state.value = _state.value.copy(
                    step = LoginUiState.Step.NEED_CODE,
                    ready = false,
                    status = "הזן קוד SMS/Telegram ולחץ 'שלח קוד'"
                )
            }

            is TdApi.AuthorizationStateWaitPassword -> {
                _state.value = _state.value.copy(
                    step = LoginUiState.Step.NEED_PASSWORD,
                    ready = false,
                    status = "נדרשת סיסמת 2FA. הזן ולחץ 'שלח סיסמה'"
                )
            }

            is TdApi.AuthorizationStateReady -> {
                _state.value = _state.value.copy(
                    step = LoginUiState.Step.READY,
                    ready = true,
                    status = "✅ התחברת בהצלחה. לחץ 'התחבר (המשך)'"
                )
            }

            is TdApi.AuthorizationStateLoggingOut -> postStatus("מתנתק...")
            is TdApi.AuthorizationStateClosing -> postStatus("סוגר TDLib...")
            is TdApi.AuthorizationStateClosed -> postStatus("נסגר.")
            else -> postStatus("מצב: ${s?.javaClass?.simpleName ?: "null"}")
        }
    }

    fun sendPhone(phone: String) {
        val p = phone.trim()
        if (p.isBlank()) {
            postStatus("❌ חסר טלפון")
            return
        }
        viewModelScope.launch { store.savePhone(p) }

        val settings = TdApi.PhoneNumberAuthenticationSettings()
        td?.send(TdApi.SetAuthenticationPhoneNumber(p, settings)) { obj ->
            Log.d("LoginVM", "SetAuthenticationPhoneNumber -> ${obj.javaClass.simpleName}")
        }
        postStatus("נשלח טלפון. מחכה לקוד...")
    }

    fun sendCode(code: String) {
        val c = code.trim()
        if (c.isBlank()) {
            postStatus("❌ חסר קוד")
            return
        }
        td?.send(TdApi.CheckAuthenticationCode(c)) { obj ->
            Log.d("LoginVM", "CheckAuthenticationCode -> ${obj.javaClass.simpleName}")
        }
        postStatus("נשלח קוד. מחכה לאישור/סיסמה אם צריך...")
    }

    fun sendPassword(password: String) {
        val pw = password
        if (pw.isBlank()) {
            postStatus("❌ חסרה סיסמה")
            return
        }
        td?.send(TdApi.CheckAuthenticationPassword(pw)) { obj ->
            Log.d("LoginVM", "CheckAuthenticationPassword -> ${obj.javaClass.simpleName}")
        }
        postStatus("נשלחה סיסמה. מחכה לאישור...")
    }

    private fun postStatus(msg: String) {
        _state.value = _state.value.copy(status = msg)
        Log.d("LoginVM", msg)
    }
}
