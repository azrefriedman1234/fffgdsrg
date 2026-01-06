package com.pasiflonet.mobile.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.pasiflonet.mobile.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {

    private lateinit var b: ActivityChatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityChatBinding.inflate(layoutInflater)
        setContentView(b.root)

        val repo = AppGraph.repo(this)
        val chatId = intent.getLongExtra("chatId", 0L)

        b.btnSend.setOnClickListener {
            val text = b.etMessage.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                repo.sendTextMessage(chatId, text) { ok ->
                    runOnUiThread {
                        b.tvStatus.text = if (ok) "✅ sent" else "❌ failed"
                    }
                }
            }
        }
    }
}
