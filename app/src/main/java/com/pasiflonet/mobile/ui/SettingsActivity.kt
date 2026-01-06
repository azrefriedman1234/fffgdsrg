package com.pasiflonet.mobile.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pasiflonet.mobile.databinding.ActivitySettingsBinding
import java.io.File

class SettingsActivity : AppCompatActivity() {

    private lateinit var b: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        // כפתור ניקוי זמניים (לא מוחק התחברות / DataStore)
        // אם ה-ID קיים ב-layout (הוספנו אותו), נחבר אותו.
        runCatching {
            b.btnClearTemp.setOnClickListener {
                val freedBytes = clearTempFilesSafe()
                val mb = freedBytes / (1024.0 * 1024.0)
                Toast.makeText(
                    this,
                    "✅ נוקו קבצים זמניים (${String.format("%.2f", mb)}MB)",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.onFailure {
            // אם מסיבה כלשהי אין את ה-ID בפועל, לא נקריס את האפליקציה
        }
    }

    /**
     * ניקוי בטוח:
     * - cacheDir
     * - externalCacheDir
     * - ותיקיות temp "שלנו" בתוך filesDir
     *
     * לא נוגעים ב:
     * - DataStore / SharedPreferences / DB
     * - קבצי התחברות
     */
    private fun clearTempFilesSafe(): Long {
        var freed = 0L

        fun sizeOf(f: File): Long {
            if (!f.exists()) return 0L
            if (f.isFile) return f.length()
            val children = f.listFiles() ?: return 0L
            var s = 0L
            for (c in children) s += sizeOf(c)
            return s
        }

        fun deleteContents(dir: File): Long {
            if (!dir.exists() || !dir.isDirectory) return 0L
            val before = sizeOf(dir)
            val children = dir.listFiles() ?: emptyArray()
            for (c in children) c.deleteRecursively()
            val after = sizeOf(dir)
            return (before - after).coerceAtLeast(0L)
        }

        // 1) cacheDir (בטוח)
        freed += deleteContents(cacheDir)

        // 2) externalCacheDir (אם יש)
        externalCacheDir?.let { freed += deleteContents(it) }

        // 3) תיקיות זמניות ייעודיות בתוך filesDir (לא DataStore)
        val safeTempNames = listOf(
            "temp",
            "tmp",
            "exports_tmp",
            "work_tmp",
            "media_tmp",
            "render_tmp",
            "pasiflonet_tmp"
        )
        for (name in safeTempNames) {
            val d = File(filesDir, name)
            // מוחקים את התוכן בלבד, לא את התיקיה עצמה (אם קיימת)
            freed += deleteContents(d)
        }

        return freed
    }
}
