package com.pasiflonet.mobile.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pasiflonet.mobile.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private lateinit var adapter: SourcesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        title = "מקורות"

        adapter = SourcesAdapter { chatId ->
            startActivity(Intent(this, ChatActivity::class.java).putExtra("chatId", chatId))
        }

        val rv = findRecycler()
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        wireSettingsButton()

        val repo = AppGraph.tdRepository(this)

        lifecycleScope.launch {
            repo.sources.collect { list ->
                adapter.submit(list)
            }
        }
    }

    private fun findRecycler(): androidx.recyclerview.widget.RecyclerView {
        val candidates = listOf("recyclerView", "rvSources", "rvChats", "rv")
        for (name in candidates) {
            val id = resources.getIdentifier(name, "id", packageName)
            if (id != 0) {
                val v = findViewById<View>(id)
                if (v is androidx.recyclerview.widget.RecyclerView) return v
            }
        }
        return b.recyclerView
    }

    private fun wireSettingsButton() {
        val ids = listOf("btnSettings","buttonSettings","ivSettings","imgSettings","settings")
        val v = ids.firstNotNullOfOrNull { name ->
            val id = resources.getIdentifier(name, "id", packageName)
            if (id != 0) findViewById<View?>(id) else null
        } ?: return

        v.isEnabled = true
        v.isClickable = true
        v.isFocusable = true

        v.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
