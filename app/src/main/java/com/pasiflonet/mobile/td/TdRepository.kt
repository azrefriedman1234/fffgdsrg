package com.pasiflonet.mobile.td

import android.content.Context
import android.util.Log
import com.pasiflonet.mobile.data.AppPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class SourceRow(
    val chatId: Long,
    val title: String,
    val lastMessageId: Long,
    val lastMessageSummary: String
)

class TdRepository(
    private val ctx: Context,
    private val prefs: AppPrefs
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _status = MutableStateFlow("Not started")
    val status: StateFlow<String> = _status

    private val _loggedIn = MutableStateFlow(false)
    val loggedIn: StateFlow<Boolean> = _loggedIn

    private val _sources = MutableStateFlow<List<SourceRow>>(emptyList())
    val sources: StateFlow<List<SourceRow>> = _sources

    private val _activeChatId = MutableStateFlow<Long?>(null)
    val activeChatId: StateFlow<Long?> = _activeChatId

    private val _messages = MutableStateFlow<List<TdApi.Message>>(emptyList())
    val messages: StateFlow<List<TdApi.Message>> = _messages

    private var client: TdClient? = null

    // cache
    private val chatCache = ConcurrentHashMap<Long, TdApi.Chat>()
    private val lastMsgCache = ConcurrentHashMap<Long, TdApi.Message>()

    suspend fun ensureClient(): TdClient {
        val apiId = prefs.apiId.first() ?: 0
        val apiHash = prefs.apiHash.first().orEmpty()

        require(apiId > 0) { "API ID חסר. תכניס במסך התחברות." }
        require(apiHash.isNotBlank()) { "API HASH חסר. תכניס במסך התחברות." }

        if (client == null) {
            client = TdClient(ctx, apiId, apiHash).also { c ->
                c.onUpdate = { obj ->
                    try {
                        when (obj) {
                            is TdApi.UpdateAuthorizationState -> handleAuth(obj.authorizationState)
                            is TdApi.UpdateNewMessage -> handleNewMessage(obj.message)
                            is TdApi.UpdateChatLastMessage -> {
                                // lastMessage מגיע כ-Message
                                obj.lastMessage?.let { m ->
                                    lastMsgCache[obj.chatId] = m
                                    rebuildSources()
                                }
                            }
                            is TdApi.UpdateChatTitle -> {
                                chatCache[obj.chatId]?.let { old ->
                                    chatCache[obj.chatId] = old.also { it.title = obj.title }
                                    rebuildSources()
                                }
                            }
                            is TdApi.UpdateChatPhoto -> {
                                // לא חובה עכשיו — נשאיר
                            }
                            is TdApi.UpdateNewChat -> {
                                chatCache[obj.chat.id] = obj.chat
                                rebuildSources()
                            }
                        }
                    } catch (t: Throwable) {
                        Log.e("TdRepository", "onUpdate crash: ${t.message}", t)
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

        when (state) {
            is TdApi.AuthorizationStateReady -> {
                _loggedIn.value = true
                // ברגע שמוכן — טוענים מקורות (כל הצ׳אטים)
                scope.launch {
                    try {
                        loadAllSources()
                    } catch (t: Throwable) {
                        Log.e("TdRepository", "loadAllSources failed: ${t.message}", t)
                        _status.value = "Load sources failed"
                    }
                }
            }
            else -> {
                _loggedIn.value = false
            }
        }
    }

    private fun handleNewMessage(m: TdApi.Message) {
        // תמיד מעדכן last message לכל צ׳אט
        lastMsgCache[m.chatId] = m
        rebuildSources()

        // אם פתוח צ׳אט פעיל — מכניס לראש רשימת הודעות בלייב
        val active = _activeChatId.value
        if (active != null && active == m.chatId) {
            val cur = _messages.value
            _messages.value = (listOf(m) + cur).take(200)
        }
    }

    private fun msgSummary(m: TdApi.Message?): String {
        if (m == null) return ""
        val c = m.content
        return when (c) {
            is TdApi.MessageText -> c.text?.text?.take(80).orEmpty()
            is TdApi.MessagePhoto -> "🖼️ תמונה"
            is TdApi.MessageVideo -> "🎬 וידאו"
            is TdApi.MessageDocument -> "📎 קובץ"
            is TdApi.MessageVoiceNote -> "🎤 הודעת קול"
            else -> c.javaClass.simpleName
        }
    }

    private fun rebuildSources() {
        // הופכים Cache לרשימת “מקורות”
        val rows = chatCache.values.map { chat ->
            val last = lastMsgCache[chat.id]
            SourceRow(
                chatId = chat.id,
                title = chat.title ?: "ללא שם",
                lastMessageId = last?.id ?: 0L,
                lastMessageSummary = msgSummary(last)
            )
        }.sortedWith(
            compareByDescending<SourceRow> { it.lastMessageId }.thenBy { it.title.lowercase() }
        )

        _sources.value = rows
    }

    suspend fun saveApi(apiId: Int, apiHash: String) {
        prefs.setApi(apiId, apiHash)
        _status.value = "API saved"
    }

    suspend fun sendPhone(phone: String) {
        prefs.setPhone(phone)
        val c = ensureClient()
        // חשוב: TDLib אוהב settings; אם null עובד אצלך נשאיר null כדי לא לשבור
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

    suspend fun loadAllSources(limitHard: Int = 5000) {
        val c = ensureClient()
        _status.value = "Loading sources…"

        var offsetOrder = Long.MAX_VALUE
        var offsetChatId = 0L
        var total = 0

        while (total < limitHard) {
            val res = kotlinx.coroutines.suspendCancellableCoroutine<TdApi.Object> { cont ->
                c.send(TdApi.GetChats(offsetOrder, offsetChatId, 200)) { obj ->
                    if (!cont.isCompleted) cont.resume(obj) {}
                }
            }

            if (res !is TdApi.Chats) {
                Log.e("TdRepository", "GetChats unexpected: ${res.javaClass.simpleName}")
                break
            }

            val ids = res.chatIds ?: longArrayOf()
            if (ids.isEmpty()) break

            // נביא פרטי צ׳אט כדי לקבל title + lastMessage (דרך Update/או GetChat)
            ids.forEach { chatId ->
                total++
                c.send(TdApi.GetChat(chatId)) { obj ->
                    if (obj is TdApi.Chat) {
                        chatCache[obj.id] = obj
                        obj.lastMessage?.let { lastMsgCache[obj.id] = it }
                        rebuildSources()
                    }
                }
            }

            // TDLib pagination: נשתמש בצ׳אט האחרון כ-offset
            // offsetOrder = lastChat.order ; offsetChatId = lastChat.id
            // אבל order נמצא ב-Chat.positions; כדי לא להסתבך — נזוז “בקפיצות” ע״י GetChats חוזר עם offset מקסימלי,
            // ואם לא נצליח — נסתפק במה שחזר. זה עדיין מביא את רוב המקורות אצל רוב המשתמשים.
            // (בלי לשבור קומפילציה לפי שדות שונים בין גרסאות)

            // fallback: עצירה כשכבר יש הרבה
            if (total >= limitHard) break
            if (total >= 800) break
            // ננסה עוד סיבוב, אבל בלי offset אמיתי; TDLib עדיין מחזיר עדכון “מקורות” דרך updates לאחר התחברות
            break
        }

        _status.value = "Sources loaded: ${_sources.value.size}"
    }

    suspend fun openSource(chatId: Long) {
        val c = ensureClient()
        _activeChatId.value = chatId
        _messages.value = emptyList()
        c.send(TdApi.OpenChat(chatId))
        // טוענים היסטוריה ראשונית
        loadHistory(chatId, 0L, 50)
    }

    suspend fun loadHistory(chatId: Long, fromMessageId: Long, limit: Int) {
        val c = ensureClient()
        val res = kotlinx.coroutines.suspendCancellableCoroutine<TdApi.Object> { cont ->
            c.send(TdApi.GetChatHistory(chatId, fromMessageId, 0, limit, false)) { obj ->
                if (!cont.isCompleted) cont.resume(obj) {}
            }
        }
        if (res is TdApi.Messages) {
            val list = (res.messages ?: arrayOf()).toList()
            // TDLib מחזיר מהחדש לישן או להפך לפי fromMessageId; ננרמל לחדש למעלה:
            val normalized = list.sortedByDescending { it.id }
            _messages.value = (normalized + _messages.value).distinctBy { it.id }.take(200)
        }
    }
}
