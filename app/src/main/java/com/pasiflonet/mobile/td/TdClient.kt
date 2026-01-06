package com.pasiflonet.mobile.td

import android.content.Context
import android.util.Log
import com.pasiflonet.mobile.BuildConfig
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

/**
 * Thin wrapper around TDLib Client.
 *
 * NOTE: TDLib requires a Telegram API_ID + API_HASH (NOT bot api).
 * You can provide them via:
 *  - Gradle properties TELEGRAM_API_ID / TELEGRAM_API_HASH (recommended for CI), or
 *  - Settings screen (DataStore).
 */
class TdClient(private val appContext: Context) {

    private val updates = MutableSharedFlow<TdApi.Object>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val updatesFlow: SharedFlow<TdApi.Object> = updates

    private val client: Client

    init {
        Client.execute(TdApi.SetLogVerbosityLevel(1))
        client = Client.create(
            { obj ->
                try { updates.tryEmit(obj) } catch (_: Throwable) {}
            },
            { e -> Log.e("TDLib", "TDLib exception", e) },
            { msg -> Log.d("TDLibLog", msg) }
        )
    }

    fun send(request: TdApi.Function, handler: (TdApi.Object) -> Unit = {}) {
        client.send(request) { obj -> handler(obj) }
    }

    fun initParameters(
        apiId: Int,
        apiHash: String,
        databaseDir: String,
        filesDir: String
    ) {
        val params = TdApi.TdlibParameters().apply {
            this.databaseDirectory = databaseDir
            this.filesDirectory = filesDir
            this.useMessageDatabase = true
            this.useSecretChats = false
            this.apiId = apiId
            this.apiHash = apiHash
            this.systemLanguageCode = "he"
            this.deviceModel = android.os.Build.MODEL ?: "Android"
            this.systemVersion = android.os.Build.VERSION.RELEASE ?: "Unknown"
            this.applicationVersion = BuildConfig.VERSION_NAME
            this.enableStorageOptimizer = true
        }
        send(TdApi.SetTdlibParameters(params))
    }

    fun setPhoneNumber(phone: String) {
        send(TdApi.SetAuthenticationPhoneNumber(phone, null))
    }

    fun checkCode(code: String) {
        send(TdApi.CheckAuthenticationCode(code))
    }

    fun checkPassword(password: String) {
        send(TdApi.CheckAuthenticationPassword(password))
    }

    fun logOut() {
        send(TdApi.LogOut())
    }
}
