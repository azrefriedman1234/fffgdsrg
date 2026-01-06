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

}
