package com.pasiflonet.mobile.td

import android.content.Context
import android.util.Log
import com.pasiflonet.mobile.data.AppPrefs
import com.pasiflonet.mobile.ui.SourceRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import org.drinkless.tdlib.TdApi
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TdRepository(
    private val ctx: Context,
    private val prefs: AppPrefs
) {
    private val _status = MutableStateFlow("Not started")
    val status: StateFlow<String> = _status

    private val _sources = MutableStateFlow<List<SourceRow>>(emptyList())
    val sources: StateFlow<List<SourceRow>> = _sources

    private var client: TdClient? = null

    suspend fun ensureClient(): TdClient {
        val apiId = prefs.apiId.first() ?: 0
        val apiHash = prefs.apiHash.first().orEmpty()

        require(apiId > 0) { "API ID חסר. תכניס במסך התחברות." }
        require(apiHash.isNotBlank()) { "API HASH חסר. תכניס במסך התחברות." }

        if (client == null) {
            client = TdClient(ctx, apiId, apiHash).also { c ->
                c.onUpdate = { obj ->
                    Log.d("TD", "update: ${obj.javaClass.simpleName}")
                    when (obj) {
                        is TdApi.UpdateAuthorizationState -> handleAuth(obj.authorizationState)
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
        val s = state.javaClass.simpleName
        Log.d("TdRepository", "Auth state: $s")
        _status.value = "Auth: $s"
    }

    suspend fun saveApi(apiId: Int, apiHash: String) {
        prefs.setApi(apiId, apiHash)
        _status.value = "API saved"
    }

    suspend fun sendPhone(phone: String) {
        prefs.setPhone(phone)
        val c = ensureClient()
        c.send(TdApi.SetAuthenticationPhoneNumber(phone, null))
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

    private suspend fun <T : TdApi.Object> await(function: TdApi.Function<out TdApi.Object>, expected: Class<T>): T {
        val c = ensureClient()
        return suspendCancellableCoroutine { cont ->
            c.send(function) { obj ->
                when {
                    expected.isInstance(obj) -> cont.resume(expected.cast(obj))
                    obj is TdApi.Error -> cont.resumeWithException(RuntimeException("TDLib error ${obj.code}: ${obj.message}"))
                    else -> cont.resumeWithException(RuntimeException("Unexpected TDLib response: ${obj.javaClass.simpleName}"))
                }
            }
        }
    }

    suspend fun loadSources(limit: Int = 80) {
        try {
            _status.value = "Loading sources…"
            val chats = await(TdApi.GetChats(TdApi.ChatListMain(), limit), TdApi.Chats::class.java)
            val out = mutableListOf<SourceRow>()
            for (id in chats.chatIds) {
                try {
                    val chat = await(TdApi.GetChat(id), TdApi.Chat::class.java)
                    val title = chat.title?.takeIf { it.isNotBlank() } ?: "Chat $id"
                    out.add(SourceRow(id, title))
                    _sources.value = out.toList()
                } catch (e: Throwable) {
                    Log.w("TdRepository", "GetChat($id) failed: ${e.message}")
                }
            }
            _status.value = "Sources loaded: ${out.size}"
        } catch (e: Throwable) {
            Log.e("TdRepository", "loadSources failed: ${e.message}", e)
            _status.value = "Load sources failed: ${e.message}"
        }
    }
}
