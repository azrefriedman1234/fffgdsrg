package com.pasiflonet.mobile.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pasiflonet.mobile.databinding.ActivityChatBinding
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var b: ActivityChatBinding
    private val msgAdapter = SimpleMessagesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityChatBinding.inflate(layoutInflater)
        setContentView(b.root)

        val chatId = intent.getLongExtra("chatId", 0L)
        if (chatId == 0L) { finish(); return }

        b.recyclerView.layoutManager = LinearLayoutManager(this)
        b.recyclerView.adapter = msgAdapter

        val repo = AppGraph.tdRepository(this)

        lifecycleScope.launch { repo.openSource(chatId) }

        lifecycleScope.launch {
            repo.messages.collect { list ->
                msgAdapter.submit(list)
            }
        }

        lifecycleScope.launch {
            repo.sources.collect { src ->
                val row = src.firstOrNull { it.chatId == chatId }
                if (row != null) b.toolbar.title = row.title
            }
        }

        b.btnBack.setOnClickListener { finish() }
    }
}
