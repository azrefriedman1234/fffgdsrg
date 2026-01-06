package com.pasiflonet.mobile.td
import com.pasiflonet.mobile.BuildConfig

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.TdApi
import java.io.File

data class ChatUi(val id: Long, val title: String, val lastMessage: String)
data class MessageUi(val id: Long, val sender: String, val text: String, val date: Long)

/**
 * Minimal repository that COMPILES and provides the API that UI/Workers expect.
 * Later we can implement real TDLib logic (get chats/messages, download files, etc.).
 */
class TdRepository(private val ctx: Context) {

    private val td: TdClient by lazy {
        val apiId = BuildConfig.TELEGRAM_API_ID
        val apiHash = BuildConfig.TELEGRAM_API_HASH

        TdClient(ctx, apiId, apiHash).also { client ->
            client.onUpdate = { obj -> handleUpdate(obj) }

            val dbDir = File(ctx.filesDir, "tdlib-db").absolutePath
            val filesDir = File(ctx.filesDir, "tdlib-files").absolutePath
            File(dbDir).mkdirs()
            File(filesDir).mkdirs()

            client.setTdlibParameters(dbDir, filesDir)
        }
    }

    // UI expects these
    private val _chats = MutableStateFlow<List<ChatUi>>(emptyList())
    val chats: StateFlow<List<ChatUi>> = _chats

    private val _messages = MutableStateFlow<List<MessageUi>>(emptyList())
    val messages: StateFlow<List<MessageUi>> = _messages

    private fun handleUpdate(obj: TdApi.Object) {
        // Placeholder: later parse UpdateNewMessage / UpdateChatLastMessage etc.
        if (obj is TdApi.UpdateAuthorizationState) {
            Log.d("TDRepo", "AuthState: ${obj.authorizationState.javaClass.simpleName}")
        }
    }

    // ---- UI hooks (placeholders) ----

    fun loadChats() {
        // Placeholder: later call GetChats/GetChat etc.
        // Keeping empty list for compile.
    }

    fun loadMessages(chatId: Long) {
        // Placeholder: later call GetChatHistory.
        // Keeping empty list for compile.
    }

    fun requestThumbnailDownload(fileId: Int, onDone: (Boolean) -> Unit = {}) {
        // Placeholder. In real implementation: DownloadFile(fileId, ...)
        onDone(false)
    }

    fun requestFileDownload(fileId: Int, onDone: (Boolean) -> Unit = {}) {
        // Placeholder. In real implementation: DownloadFile(fileId, ...)
        onDone(false)
    }

    fun getFileLocalPath(fileId: Int): String? {
        // Placeholder. In real implementation: keep map from fileId->localPath via updates.
        return null
    }

    // ---- send helpers ----

    fun sendTextMessage(chatId: Long, text: String, onDone: (Boolean) -> Unit = {}) {
        val content = TdApi.InputMessageText(
            TdApi.FormattedText(text, null),
            null,
            false
        )
        val req = TdApi.SendMessage(chatId, null, null, null, null, content)
        td.send(req) { res -> onDone(res is TdApi.Message) }
    }

    fun sendFileToChat(chatId: Long, filePath: String, caption: String? = null, onDone: (Boolean) -> Unit = {}) {
        val f = File(filePath)
        if (!f.exists()) {
            onDone(false)
            return
        }

        val inputFile = TdApi.InputFileLocal(f.absolutePath)
        val formatted = TdApi.FormattedText(caption ?: "", null)

        val lower = f.name.lowercase()
        val isVideo = lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".mov") || lower.endsWith(".webm")

        val content: TdApi.InputMessageContent =
            if (isVideo) {
                TdApi.InputMessageVideo(
                    inputFile,
                    null,
                    null,
                    0,
                    intArrayOf(),
                    0,
                    0,
                    0,
                    true,
                    formatted,
                    false,
                    null,
                    false
                )
            } else {
                TdApi.InputMessagePhoto(
                    inputFile,
                    null,
                    intArrayOf(),
                    0,
                    0,
                    formatted,
                    false,
                    null,
                    false
                )
            }

        val req = TdApi.SendMessage(chatId, null, null, null, null, content)
        td.send(req) { res -> onDone(res is TdApi.Message) }
    }

    fun sendMedia(chatId: Long, uri: Uri, caption: String?, onDone: (Boolean) -> Unit = {}) {
        // Minimal: copy uri->cache then send
        val path = copyUriToCache(uri) ?: run { onDone(false); return }
        sendFileToChat(chatId, path, caption, onDone)
    }

    private fun copyUriToCache(uri: Uri): String? {
        return try {
            val outFile = File(ctx.cacheDir, "upload_${System.currentTimeMillis()}")
            ctx.contentResolver.openInputStream(uri)?.use { inp ->
                outFile.outputStream().use { out -> inp.copyTo(out) }
            } ?: return null
            outFile.absolutePath
        } catch (t: Throwable) {
            Log.e("TDRepo", "copyUriToCache failed", t)
            null
        }
    }
}
