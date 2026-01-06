package com.pasiflonet.mobile.td

import android.content.Context
import android.util.Log
import com.pasiflonet.mobile.data.AppPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import org.drinkless.tdlib.TdApi
import java.io.File

class TdRepository(
    private val ctx: Context,
    private val prefs: AppPrefs
) {
    private val _status = MutableStateFlow("Not started")
    val status: StateFlow<String> = _status

    private val _loggedIn = MutableStateFlow(false)
    val loggedIn: StateFlow<Boolean> = _loggedIn

    private var client: TdClient? = null

    suspend fun ensureClient(): TdClient {
        val apiId = prefs.apiId.first() ?: 0
        val apiHash = prefs.apiHash.first().orEmpty()

        require(apiId > 0) { "API ID חסר. תכניס במסך התחברות." }
        require(apiHash.isNotBlank()) { "API HASH חסר. תכניס במסך התחברות." }

        if (client == null) {
            client = TdClient(ctx, apiId, apiHash).also { c ->
                c.onUpdate = { obj ->
                    if (obj is TdApi.UpdateAuthorizationState) {
                        handleAuth(obj.authorizationState)
                    }
                }

                val dbDir = File(ctx.filesDir, "tdlib").apply { mkdirs() }.absolutePath
                val filesDir = File(ctx.filesDir, "td_files").apply { mkdirs() }.absolutePath
                c.setTdlibParameters(dbDir, filesDir)

                _status.value = "TDLib initialized"
            }
        }
        return client!!
    }

    private fun handleAuth(state: TdApi.AuthorizationState) {
        val name = state.javaClass.simpleName
        Log.d("TdRepository", "Auth state: $name")
        _status.value = "Auth: $name"

        when (state) {
            is TdApi.AuthorizationStateReady -> {
                _loggedIn.value = true
                _status.value = "✅ מחובר"
            }
            else -> {
                // אם יצאת/עדיין לא התחברת
                _loggedIn.value = false
            }
        }
    }

    suspend fun saveApi(apiId: Int, apiHash: String) {
        prefs.setApi(apiId, apiHash)
        _status.value = "API saved"
    }

    suspend fun sendPhone(phone: String) {
        prefs.setPhone(phone)
        val c = ensureClient()
        val settings = TdApi.PhoneNumberAuthenticationSettings()
        c.send(TdApi.SetAuthenticationPhoneNumber(phone, settings))
        _status.value = "Phone sent, waiting for code"
    }

    suspend fun sendCode(code: String) {
        val c = ensureClient()
        c.send(TdApi.CheckAuthenticationCode(code))
        _status.value = "Code sent"
    }

    suspend fun sendPassword(password: String) {
        val c = ensureClient()
        c.send(TdApi.CheckAuthenticationPassword(password))
        _status.value = "Password sent"
    }
}
