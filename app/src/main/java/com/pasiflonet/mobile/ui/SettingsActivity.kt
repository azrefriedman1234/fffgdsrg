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

        b.btnClearTemp.setOnClickListener {
            val deleted = clearTempFilesOnly()
            Toast.makeText(this, "נמחקו $deleted קבצים זמניים", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * מנקה רק קבצים זמניים:
     * - cacheDir
     * - externalCacheDir
     * - filesDir/temp , filesDir/tmp , filesDir/exports (אם קיימים)
     *
     * לא נוגע ב:
     * - databases/
     * - shared_prefs/
     * - datastore/
     * - תיקיות tdlib קבועות אם קיימות
     */
    private fun clearTempFilesOnly(): Int {
        var count = 0

        fun deleteChildren(dir: File?): Int {
            if (dir == null || !dir.exists()) return 0
            var local = 0
            dir.listFiles()?.forEach { f ->
                local += deleteRecursivelySafe(f)
            }
            return local
        }

        fun safeDir(name: String): File = File(filesDir, name)

        // cache dirs
        count += deleteChildren(cacheDir)
        count += deleteChildren(externalCacheDir)

        // common temp dirs used by apps
        count += deleteChildren(safeDir("temp"))
        count += deleteChildren(safeDir("tmp"))
        count += deleteChildren(safeDir("exports"))

        return count
    }

    private fun deleteRecursivelySafe(f: File): Int {
        // extra safety: never delete critical app dirs by mistake
        val path = f.absolutePath
        if (path.contains("/databases") || path.contains("/shared_prefs") || path.contains("/datastore")) {
            return 0
        }
        // also avoid tdlib DB directories if you keep them under filesDir
        if (path.contains("tdlib", ignoreCase = true) && (path.contains("db", ignoreCase = true) || path.contains("database", ignoreCase = true))) {
            return 0
        }

        var deleted = 0
        if (f.isDirectory) {
            f.listFiles()?.forEach { child ->
                deleted += deleteRecursivelySafe(child)
            }
        }
        if (f.exists() && f.delete()) deleted += 1
        return deleted
    }
}
