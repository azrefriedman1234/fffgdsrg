package com.pasiflonet.mobile.td

import android.content.Context
import android.util.Log
import com.pasiflonet.mobile.BuildConfig
import com.pasiflonet.mobile.data.Prefs
import com.pasiflonet.mobile.data.PrefsKeys
import com.pasiflonet.mobile.model.ChatUi
import com.pasiflonet.mobile.model.MediaType
import com.pasiflonet.mobile.model.MessageUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import java.io.File

class TdRepository(private val context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val prefs = Prefs(context)
    private val td = TdClient(context)

    private val _authState = MutableStateFlow<String>("INIT")
    val authState: StateFlow<String> = _authState

    private val _chats = MutableStateFlow<List<ChatUi>>(emptyList())
    val chats: StateFlow<List<ChatUi>> = _chats

    private val _messages = MutableStateFlow<List<MessageUi>>(emptyList())
    val messages: StateFlow<List<MessageUi>> = _messages

    private val chatCache = HashMap<Long, TdApi.Chat>()

    init {
        appScope.launch {
            td.updatesFlow.collect { obj ->
                when (obj.constructor) {
                    TdApi.UpdateAuthorizationState.CONSTRUCTOR -> {
                        val state = (obj as TdApi.UpdateAuthorizationState).authorizationState
                        onAuthState(state)
                    }
                    TdApi.UpdateNewChat.CONSTRUCTOR -> {
                        val chat = (obj as TdApi.UpdateNewChat).chat
                        chatCache[chat.id] = chat
                        refreshChatsList()
                    }
                    TdApi.UpdateChatTitle.CONSTRUCTOR -> refreshChatsList()
                    TdApi.UpdateChatLastMessage.CONSTRUCTOR -> refreshChatsList()
                    TdApi.UpdateFile.CONSTRUCTOR -> {
                        // files updated; UI can reload thumbnails via fileId
                    }
                }
            }
        }
    }

    private suspend fun getApiIdHash(): Pair<Int, String> {
        // Prefer Settings (DataStore). Fallback to BuildConfig constants.
        val p = prefs.flow().first()
        val apiId = p[PrefsKeys.apiId] ?: BuildConfig.TELEGRAM_API_ID
        val apiHash = p[PrefsKeys.apiHash] ?: BuildConfig.TELEGRAM_API_HASH
        return apiId to apiHash
    }

    private fun onAuthState(state: TdApi.AuthorizationState) {
        when (state.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                appScope.launch {
                    val (apiId, apiHash) = getApiIdHash()
                    if (apiId <= 0 || apiHash.isBlank()) {
                        _authState.value = "MISSING_API"
                        Log.w("TdRepository", "Missing API_ID / API_HASH")
                        return@launch
                    }
                    val dbDir = File(context.filesDir, "tdlib").absolutePath
                    val filesDir = File(context.filesDir, "tdlib_files").absolutePath
                    File(dbDir).mkdirs()
                    File(filesDir).mkdirs()
                    td.initParameters(apiId, apiHash, dbDir, filesDir)
                    _authState.value = "PARAMS_SENT"
                }
            }
            TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> _authState.value = "WAIT_PHONE"
            TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> _authState.value = "WAIT_CODE"
            TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> _authState.value = "WAIT_PASSWORD"
            TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
                _authState.value = "READY"
                // Load chats
                loadChats()
            }
            TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR -> _authState.value = "LOGGING_OUT"
            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> _authState.value = "CLOSED"
            else -> _authState.value = "AUTH_${state.constructor}"
        }
    }

    fun tdClient(): TdClient = td

    fun setPhone(phone: String) = td.setPhoneNumber(phone)
    fun sendCode(code: String) = td.checkCode(code)
    fun sendPassword(password: String) = td.checkPassword(password)
    fun logOut() = td.logOut()

    fun loadChats(limit: Int = 200) {
        // TDLib chat list is loaded by GetChats + receiving UpdateNewChat etc.
        td.send(TdApi.GetChats(null, limit)) { _ ->
            refreshChatsList()
        }
    }

    private fun refreshChatsList() {
        // For simplicity: list from cache.
        val list = chatCache.values
            .sortedByDescending { it.lastMessage?.date ?: 0 }
            .map { ChatUi(id = it.id, title = it.title ?: "(ללא שם)", subtitle = it.lastMessage?.let { "הודעה אחרונה: ${it.date}" } ?: "") }
        _chats.value = list
    }

    fun loadMessages(chatId: Long, limit: Int = 50) {
        td.send(TdApi.GetChatHistory(chatId, 0, 0, limit, false)) { obj ->
            val result = obj as? TdApi.Messages ?: return@send
            val ui = result.messages.orEmpty().map { m -> mapMessage(chatId, m) }
            _messages.value = ui
        }
    }

    private fun mapMessage(chatId: Long, m: TdApi.Message): MessageUi {
        var mediaType = MediaType.NONE
        var fileId: Int? = null
        var fileName: String? = null
        var thumbFileId: Int? = null
        var durationSec: Int? = null
        var sizeBytes: Long? = null

        val text = when (val c = m.content) {
            is TdApi.MessageText -> c.text?.text ?: ""
            is TdApi.MessagePhoto -> {
                mediaType = MediaType.PHOTO
                val p = c.photo
                val best = p?.sizes?.maxByOrNull { it.photo?.expectedSize ?: 0 }
                fileId = best?.photo?.id
                sizeBytes = best?.photo?.size?.toLong()
                val thumb = p?.sizes?.minByOrNull { it.photo?.expectedSize ?: Int.MAX_VALUE }
                thumbFileId = thumb?.photo?.id
                ""
            }
            is TdApi.MessageVideo -> {
                mediaType = MediaType.VIDEO
                val v = c.video
                fileId = v?.video?.id
                sizeBytes = v?.video?.size?.toLong()
                durationSec = v?.duration
                fileName = v?.fileName
                thumbFileId = v?.thumbnail?.file?.id
                ""
            }
            is TdApi.MessageDocument -> {
                mediaType = MediaType.DOCUMENT
                val d = c.document
                fileId = d?.document?.id
                sizeBytes = d?.document?.size?.toLong()
                fileName = d?.fileName
                thumbFileId = d?.thumbnail?.file?.id
                ""
            }
            else -> ""
        }

        val visibleText = if (text.isNotBlank()) text else "(מדיה: $mediaType)"
        return MessageUi(
            chatId = chatId,
            messageId = m.id,
            text = visibleText,
            mediaType = mediaType,
            fileId = fileId,
            fileName = fileName,
            thumbnailFileId = thumbFileId,
            durationSec = durationSec,
            sizeBytes = sizeBytes
        )
    }

    fun requestThumbnailDownload(fileId: Int) {
        td.send(TdApi.DownloadFile(fileId, 1, 0, 0, true))
    }

    fun requestFileDownload(fileId: Int) {
        td.send(TdApi.DownloadFile(fileId, 32, 0, 0, false))
    }

    fun getFileLocalPath(fileId: Int, cb: (String?) -> Unit) {
        td.send(TdApi.GetFile(fileId)) { obj ->
            val f = obj as? TdApi.File
            cb(f?.local?.path?.takeIf { it.isNotBlank() && f.local.isDownloadingCompleted })
        }
    }

    fun sendFileToChat(chatId: Long, localPath: String, isVideo: Boolean, caption: String = "") {
        val inputFile = TdApi.InputFileLocal(localPath)
        val content: TdApi.InputMessageContent = if (isVideo) {
            TdApi.InputMessageVideo(inputFile, null, null, 0, 0, 0, false, false, false)
        } else {
            TdApi.InputMessagePhoto(inputFile, null, null, 0, 0, 0)
        }
        val send = TdApi.SendMessage(chatId, 0, 0, null, null, content)
        td.send(send) { obj ->
            Log.d("TdRepository", "SendMessage result: ${obj.constructor}")
        }
    }
}
