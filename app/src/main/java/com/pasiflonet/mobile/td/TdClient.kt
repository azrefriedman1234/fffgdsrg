package com.pasiflonet.mobile.td

import android.content.Context
import android.os.Build
import android.util.Log
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

/**
 * Thin wrapper around org.drinkless.tdlib.Client for TDLib 1.8.56 AAR.
 */
class TdClient(
    private val context: Context,
    private val apiId: Int,
    private val apiHash: String
) {

    var onUpdate: ((TdApi.Object) -> Unit)? = null

    private val client: Client = Client.create(
        { obj -> onUpdate?.invoke(obj) },
        { e -> Log.e("TDLib", "TDLib exception", e) },
        { e -> Log.e("TDLib", "TDLib fatal exception", e) }
    )

    fun send(request: TdApi.Function, handler: (TdApi.Object) -> Unit = {}) {
        client.send(request) { result -> handler(result) }
    }

    /**
     * TDLib 1.8.56: אין TdlibParameters class.
     * משתמשים ישירות ב-SetTdlibParameters(...) constructor עם 14 פרמטרים.
     */
    fun setTdlibParameters(databaseDir: String, filesDir: String) {
        val params = TdApi.SetTdlibParameters(
            false,                // useTestDc
            databaseDir,          // databaseDirectory
            filesDir,             // filesDirectory
            ByteArray(0),         // databaseEncryptionKey
            true,                 // useFileDatabase
            true,                 // useChatInfoDatabase
            true,                 // useMessageDatabase
            true,                 // useSecretChats
            apiId,                // apiId
            apiHash,              // apiHash
            "en",                 // systemLanguageCode
            Build.MODEL ?: "Android",
            Build.VERSION.RELEASE ?: "Android",
            "1.0"                 // applicationVersion
        )
        send(params)
    }

    fun setPhoneNumber(phone: String, handler: (TdApi.Object) -> Unit = {}) {
        send(TdApi.SetAuthenticationPhoneNumber(phone, null), handler)
    }

    fun checkCode(code: String, handler: (TdApi.Object) -> Unit = {}) {
        send(TdApi.CheckAuthenticationCode(code), handler)
    }

    fun checkPassword(password: String, handler: (TdApi.Object) -> Unit = {}) {
        send(TdApi.CheckAuthenticationPassword(password), handler)
    }

    fun logout(handler: (TdApi.Object) -> Unit = {}) {
        send(TdApi.LogOut(), handler)
    }
}
