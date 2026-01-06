package com.pasiflonet.mobile.td

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.pasiflonet.mobile.data.PreferencesRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.drinkless.tdlib.TdApi
import java.io.File

data class ChatUi(val id: Long, val title: String, val lastMessage: String)
data class MessageUi(val id: Long, val sender: String, val text: String, val date: Long)

class TdRepository(
    private val ctx: Context,
    private val prefs: PreferencesRepo
) {

    private val apiId: Int get() = prefs.getApiId()
    private val apiHash: String get() = prefs.getApiHash()

    private val td: TdClient by lazy {
        TdClient(ctx, apiId, apiHash).also { client ->
            client.onUpdate = { obj -> handleUpdate(obj) }

            // folders
            val dbDir = File(ctx.filesDir, "tdlib-db").absolutePath
            val filesDir = File(ctx.filesDir, "tdlib-files").absolutePath
            File(dbDir).mkdirs()
            File(filesDir).mkdirs()

            client.setTdlibParameters(dbDir, filesDir)
        }
    }

    private val _authState = MutableStateFlow("UNKNOWN")
    val authState: StateFlow<String> = _authState

    private fun handleUpdate(obj: TdApi.Object) {
        when (obj) {
            is TdApi.UpdateAuthorizationState -> {
                val s = obj.authorizationState
                _authState.value = s.javaClass.simpleName
            }
        }
    }

    fun setPhone(phone: String) {
        td.setPhoneNumber(phone) { res -> Log.d("TDRepo", "setPhoneNumber => ${res.javaClass.simpleName}") }
    }

    fun sendCode(code: String) {
        td.checkCode(code) { res -> Log.d("TDRepo", "checkCode => ${res.javaClass.simpleName}") }
    }

    fun sendPassword(pass: String) {
        td.checkPassword(pass) { res -> Log.d("TDRepo", "checkPassword => ${res.javaClass.simpleName}") }
    }

    fun logout() {
        td.logout()
    }

    // -------- sending --------

    fun sendTextMessage(chatId: Long, text: String, onDone: (Boolean) -> Unit = {}) {
        val content = TdApi.InputMessageText(
            TdApi.FormattedText(text, null),
            null,
            false
        )

        // TDLib חדש: SendMessage(chatId, messageTopic, replyTo, options, replyMarkup, content)
        val req = TdApi.SendMessage(chatId, null, null, null, null, content)
        td.send(req) { res ->
            onDone(res is TdApi.Message)
        }
    }

    fun sendMedia(chatId: Long, uri: Uri, caption: String?, onDone: (Boolean) -> Unit = {}) {
        val path = copyUriToCache(uri) ?: run {
            onDone(false); return
        }

        val inputFile = TdApi.InputFileLocal(path)
        val formatted = TdApi.FormattedText(caption ?: "", null)

        val mime = ctx.contentResolver.getType(uri) ?: ""
        val content: TdApi.InputMessageContent =
            if (mime.startsWith("video")) {
                // TDLib 1.8.56 ctor:
                // InputMessageVideo(inputFile, thumbnail, addedStickerFile, duration, addedStickerFileIds, width, height, supportsStreaming, caption, hasSpoiler, selfDestructType, showCaptionAboveMedia)
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
                // TDLib 1.8.56 ctor:
                // InputMessagePhoto(inputFile, thumbnail, addedStickerFileIds, width, height, caption, hasSpoiler, selfDestructType, showCaptionAboveMedia)
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
        td.send(req) { res ->
            onDone(res is TdApi.Message)
        }
    }

    private fun copyUriToCache(uri: Uri): String? {
        return try {
            val name = queryDisplayName(uri) ?: "upload.bin"
            val outFile = File(ctx.cacheDir, name)
            ctx.contentResolver.openInputStream(uri)?.use { inp ->
                outFile.outputStream().use { out -> inp.copyTo(out) }
            } ?: return null
            outFile.absolutePath
        } catch (t: Throwable) {
            Log.e("TDRepo", "copyUriToCache failed", t)
            null
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    // -------- thumbnails helpers (typed lambdas to avoid inference issues in CI) --------

    fun bestPhotoFileId(p: TdApi.Photo?): Int? {
        val sizes = p?.sizes ?: return null

        val best = sizes.maxByOrNull { ps: TdApi.PhotoSize ->
            ps.photo?.expectedSize ?: 0
        } ?: sizes.minByOrNull { ps: TdApi.PhotoSize ->
            ps.photo?.expectedSize ?: Int.MAX_VALUE
        }

        return best?.photo?.id
    }
}
