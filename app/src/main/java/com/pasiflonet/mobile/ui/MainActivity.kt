package com.pasiflonet.mobile.ui

import android.os.Bundle
import android.view.View
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.pasiflonet.mobile.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        
        // 🔧 ensure Settings button always works (even if ID changes)
        wireSettingsButton()
// Placeholder – UI wiring later
    }

    private fun wireSettingsButtonAuto() {
        // Try common IDs. Works even if you use viewBinding or findViewById.
        val candidates = listOf(
            "btnSettings", "buttonSettings", "ivSettings", "imgSettings", "settings",
            "btnConfig", "buttonConfig"
        )

        for (name in candidates) {
            val id = resources.getIdentifier(name, "id", packageName)
            if (id != 0) {
                val v = findViewById<View>(id)
                v?.setOnClickListener {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
            }
        }
    }

    private fun forceSourcesTitleIfChats() {
        // If some view/toolbar title still says "צאטים", force it to "מקורות"
        val titleCandidates = listOf("tvTitle", "tvChatsTitle", "toolbarTitle", "title")
        for (name in titleCandidates) {
            val id = resources.getIdentifier(name, "id", packageName)
            if (id != 0) {
                val v = findViewById<View>(id)
                try {
                    val m = v?.javaClass?.getMethod("getText")
                    val cur = m?.invoke(v)?.toString() ?: ""
                    if (cur.trim() == "צאטים") {
                        v.javaClass.getMethod("setText", CharSequence::class.java).invoke(v, "מקורות")
                    }
                } catch (_: Throwable) {}
            }
        }
        // Also set activity title (toolbar)
        if (title?.toString()?.trim() == "צאטים") title = "מקורות"
    }


    private fun wireSettingsButton() {
        val ids = listOf("btnSettings","buttonSettings","ivSettings","imgSettings","settings")
        val v = ids.firstNotNullOfOrNull { name ->
            val id = resources.getIdentifier(name, "id", packageName)
            if (id != 0) findViewById<android.view.View?>(id) else null
        }

        if (v == null) {
            android.util.Log.w("MainActivity", "Settings button not found in layout")
            return
        }

        // make sure it can receive touches
        v.isEnabled = true
        v.isClickable = true
        v.isFocusable = true

        // Touch debug: אם זה לא מופיע בלוג, יש Overlay
        v.setOnTouchListener { _, ev ->
            android.util.Log.d("MainActivity", "Settings touch: ${'$'}{ev.action}")
            false
        }

        v.setOnClickListener {
            android.widget.Toast.makeText(this, "פותח הגדרות…", android.widget.Toast.LENGTH_SHORT).show()
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
        }
    }

}
