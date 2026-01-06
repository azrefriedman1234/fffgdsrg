package com.pasiflonet.mobile.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pasiflonet.mobile.databinding.ActivityChatBinding
import com.pasiflonet.mobile.model.MessageUi
import kotlinx.coroutines.launch

class ChatActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CHAT_ID = "chatId"
        const val EXTRA_CHAT_TITLE = "chatTitle"
    }

    private lateinit var b: ActivityChatBinding
    private val repo by lazy { AppGraph.tdRepository(this) }
    private var chatId: Long = 0L

    private val adapter = MessagesAdapter(
        onDetails = { openDetails(it) },
        requestThumb = { fileId -> repo.requestThumbnailDownload(fileId) },
        resolveLocalThumb = { fileId, cb -> repo.getFileLocalPath(fileId, cb) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityChatBinding.inflate(layoutInflater)
        setContentView(b.root)

        chatId = intent.getLongExtra(EXTRA_CHAT_ID, 0L)
        val title = intent.getStringExtra(EXTRA_CHAT_TITLE).orEmpty()
        b.toolbar.title = title
        b.toolbar.setNavigationOnClickListener { finish() }

        b.rvMessages.layoutManager = LinearLayoutManager(this)
        b.rvMessages.adapter = adapter

        lifecycleScope.launch {
            repo.messages.collect { adapter.submit(it) }
        }

        repo.loadMessages(chatId, 50)
    }

    private fun openDetails(item: MessageUi) {
        if (item.fileId == null) return
        startActivity(Intent(this, DetailsActivity::class.java).apply {
            putExtra(DetailsActivity.EXTRA_CHAT_ID, item.chatId)
            putExtra(DetailsActivity.EXTRA_FILE_ID, item.fileId)
            putExtra(DetailsActivity.EXTRA_MEDIA_TYPE, item.mediaType.name)
            putExtra(DetailsActivity.EXTRA_FILE_NAME, item.fileName ?: "")
            putExtra(DetailsActivity.EXTRA_DURATION, item.durationSec ?: 0)
            putExtra(DetailsActivity.EXTRA_SIZE, item.sizeBytes ?: 0L)
        })
    }
}
