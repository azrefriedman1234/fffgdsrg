package com.pasiflonet.mobile.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pasiflonet.mobile.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        title = "מקורות"

        wireSettingsButton() // גם אם יש overlay
        setupSourcesList()

        // טען מקורות ישר בפתיחה (אם כבר מחובר)
        vm.refreshSources()
    }

    private fun setupSourcesList() {
        val rv = findRecycler()
        if (rv == null) return

        val adapter = SourcesAdapter { row ->
            // כרגע רק placeholder לכניסה למקור
            android.widget.Toast.makeText(this, "נבחר מקור: ${row.title}", android.widget.Toast.LENGTH_SHORT).show()
        }

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.sources.collect { list ->
                        adapter.submit(list)
                    }
                }
                launch {
                    vm.status.collect { s ->
                        // אם יש לך TextView סטטוס בעתיד – אפשר לשים כאן
                        android.util.Log.d("MainActivity", "status: $s")
                    }
                }
            }
        }
    }

    private fun findRecycler(): RecyclerView? {
        val ids = listOf("rvSources", "rvChats", "recycler", "recyclerView", "rv", "list")
        val v = ids.firstNotNullOfOrNull { name ->
            val id = resources.getIdentifier(name, "id", packageName)
            if (id != 0) findViewById<RecyclerView?>(id) else null
        }
        if (v == null) {
            android.util.Log.w("MainActivity", "RecyclerView not found by common IDs")
        }
        return v
    }

    private fun wireSettingsButton() {
        val ids = listOf("btnSettings", "buttonSettings", "ivSettings", "imgSettings", "settings")
        val v = ids.firstNotNullOfOrNull { name ->
            val id = resources.getIdentifier(name, "id", packageName)
            if (id != 0) findViewById<android.view.View?>(id) else null
        }

        if (v == null) {
            android.util.Log.w("MainActivity", "Settings button not found in layout")
            return
        }

        // קריטי נגד overlay:
        v.bringToFront()
        v.isEnabled = true
        v.isClickable = true
        v.isFocusable = true

        v.setOnClickListener {
            android.widget.Toast.makeText(this, "פותח הגדרות…", android.widget.Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}
