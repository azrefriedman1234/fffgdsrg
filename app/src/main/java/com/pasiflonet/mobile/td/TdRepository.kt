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

    /**
     * מקורות = כל הצ'אטים שיש למשתמש (ללא סינון).
     * זה טוען רשימת IDs ואז מביא לכל אחד כותרת (GetChat) ומעדכן Live ל-Flow.
     */
    suspend fun loadSources(limit: Int = 200) {
        val c = ensureClient()

        // GetChats חייב ChatList + limit (זה מה שתיקן את שגיאת ה-CI)
        c.send(TdApi.GetChats(TdApi.ChatListMain(), limit)) { obj ->
            if (obj !is TdApi.Chats) {
                Log.w("TdRepository", "GetChats result: ${obj.javaClass.simpleName}")
                return@send
            }

            val ids = (obj.chatIds ?: longArrayOf()).toList()
            _sources.value = ids.map { SourceRow(chatId = it, title = "טוען…", subtitle = "") }
            _status.value = "Loaded ${ids.size} chat IDs"

            // עכשיו נביא Titles – ייכנסו ל-Flow בהדרגה (Live)
            ids.forEach { id ->
                c.send(TdApi.GetChat(id)) { o ->
                    if (o is TdApi.Chat) {
                        val cur = _sources.value.toMutableList()
                        val idx = cur.indexOfFirst { it.chatId == id }
                        if (idx >= 0) {
                            val title = o.title ?: "Chat $id"
                            cur[idx] = cur[idx].copy(title = title)
                            _sources.value = cur
                        }
                    }
                }
            }
        }
    }
}
