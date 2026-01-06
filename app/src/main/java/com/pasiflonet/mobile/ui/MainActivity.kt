package com.pasiflonet.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.databinding.ActivityMainBinding
import com.pasiflonet.mobile.td.SourceRow
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var adapter: SourcesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        title = "מקורות"

        wireSettingsButton()

        val rv = findRecycler()
        rv.layoutManager = LinearLayoutManager(this)
        adapter = SourcesAdapter(emptyList()) { row ->
            // כרגע: מעבר למסך צ'אט לפי chatId (אם ChatActivity כבר קיים אצלך)
            Toast.makeText(this, "נפתח מקור: ${row.title}", Toast.LENGTH_SHORT).show()
            val it = Intent(this, ChatActivity::class.java)
            it.putExtra("chat_id", row.chatId)
            startActivity(it)
        }
        rv.adapter = adapter

        // אם יש ViewModelFactory אצלך – אל תיגע. אם אין, זה עדיין יתקמפל כי לא משתמשים כאן ב-vm.
        // כרגע נטען מקורות דרך TdRepository דרך ה-ViewModel שלך במסך (אם חיברת אותו שם).
    }

    private fun findRecycler(): RecyclerView {
        val candidates = listOf("recyclerView", "rvSources", "rvChats", "list", "rv")
        val v = candidates.firstNotNullOfOrNull { name ->
            val id = resources.getIdentifier(name, "id", packageName)
            if (id != 0) findViewById<View?>(id) as? RecyclerView else null
        }
        return v ?: error("RecyclerView not found in activity_main.xml (tried: $candidates)")
    }

    private fun wireSettingsButton() {
        val ids = listOf("btnSettings","buttonSettings","ivSettings","imgSettings","settings")
        val v = ids.firstNotNullOfOrNull { name ->
            val id = resources.getIdentifier(name, "id", packageName)
            if (id != 0) findViewById<View?>(id) else null
        }

        if (v == null) {
            return
        }

        v.isEnabled = true
        v.isClickable = true
        v.isFocusable = true

        v.setOnClickListener {
            Toast.makeText(this, "פותח הגדרות…", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
