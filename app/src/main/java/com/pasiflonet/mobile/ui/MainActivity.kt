package com.pasiflonet.mobile.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pasiflonet.mobile.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var b: ActivityMainBinding
    private val repo by lazy { AppGraph.tdRepository(this) }
    private val adapter = ChatsAdapter { chatId, title ->
        startActivity(Intent(this, ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_CHAT_ID, chatId)
            putExtra(ChatActivity.EXTRA_CHAT_TITLE, title)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.rvChats.layoutManager = LinearLayoutManager(this)
        b.rvChats.adapter = adapter

        b.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                com.pasiflonet.mobile.R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }

        lifecycleScope.launch {
            repo.chats.collect { adapter.submit(it) }
        }
    }
}
