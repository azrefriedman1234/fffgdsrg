package com.pasiflonet.mobile.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pasiflonet.mobile.databinding.ActivityChatBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var b: ActivityChatBinding

    // messages כדי שמה שיש אצלך בקוד שמצפה לזה יתקמפל (ובהמשך נחבר ל-TDLib)
    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityChatBinding.inflate(layoutInflater)
        setContentView(b.root)

        // פותחים מקור לפי chat_id מה-Intent
        openSource()

        // אם יש לך תצוגת טבלה/רשימה בפנים—נחבר אחר כך.
        // כרגע לפחות נריץ עדכון "לייב" בסיסי כדי שהמסך לא יהיה ריק.
        lifecycleScope.launch {
            // דוגמת placeholder שלא שוברת פיצ'רים – רק כדי שלא יהיה מסך מת
            if (_messages.value.isEmpty()) {
                _messages.value = listOf("טוען הודעות…")
            }
        }
    }

    private fun openSource() {
        val chatId = intent.getLongExtra("chat_id", 0L)
        title = if (chatId != 0L) "מקור: $chatId" else "מקור"
    }
}
