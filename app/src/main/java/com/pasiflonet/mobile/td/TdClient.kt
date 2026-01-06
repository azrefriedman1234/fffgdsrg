package com.pasiflonet.mobile.td

import android.content.Context
import android.util.Log
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi

class TdClient(
    private val ctx: Context,
    private val apiId: Int,
    private val apiHash: String
) {
    var onUpdate: ((TdApi.Object) -> Unit)? = null

    private val client: Client = Client.create(
        Client.ResultHandler { obj -> onUpdate?.invoke(obj) },
        Client.ExceptionHandler { e -> Log.e("TdClient", "TDLib error: ${e.message}", e) },
        Client.ExceptionHandler { e -> Log.e("TdClient", "TDLib fatal: ${e.message}", e) }
    )

    fun send(query: TdApi.Function, cb: (TdApi.Object) -> Unit = {}) {
        client.send(query, Client.ResultHandler { obj -> cb(obj) })
    }

    fun setTdlibParameters(databaseDir: String, filesDir: String) {
        val req = TdApi.SetTdlibParameters(
            false,                       // useTestDc
            databaseDir,                 // databaseDirectory
            filesDir,                    // filesDirectory
            ByteArray(0),                // databaseEncryptionKey
            true,                        // useFileDatabase
            true,                        // useChatInfoDatabase
            true,                        // useMessageDatabase
            false,                       // useSecretChats
            apiId,                       // apiId
            apiHash,                     // apiHash
            "en",                        // systemLanguageCode
            android.os.Build.MODEL,      // deviceModel
            android.os.Build.VERSION.RELEASE, // systemVersion
            "1.0"                        // applicationVersion
        )

        send(req) { obj ->
            Log.d("TdClient", "SetTdlibParameters result: ${obj.javaClass.simpleName}")
        }
    }
}
