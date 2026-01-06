package com.pasiflonet.mobile.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.pasiflonet.mobile.data.Prefs
import com.pasiflonet.mobile.data.PrefsKeys
import com.pasiflonet.mobile.databinding.ActivitySettingsBinding
import com.pasiflonet.mobile.model.LogoPreset
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    private lateinit var b: ActivitySettingsBinding
    private val prefs by lazy { Prefs(this) }

    private val pickLogo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        lifecycleScope.launch {
            prefs.setString(PrefsKeys.logoUri, uri.toString())
            b.tvCurrentLogo.text = "LogoUri: $uri"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)

        val presets = LogoPreset.values().map { it.key }
        b.spPreset.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, presets)

        b.btnPickLogo.setOnClickListener {
            pickLogo.launch(arrayOf("image/*"))
        }

        lifecycleScope.launch {
            prefs.flow().collect { p ->
                b.swLogo.isChecked = p[PrefsKeys.logoEnabled] ?: false
                b.tvCurrentLogo.text = "LogoUri: ${p[PrefsKeys.logoUri].orEmpty()}"
                b.etApiId.setText((p[PrefsKeys.apiId] ?: 0).toString())
                b.etApiHash.setText(p[PrefsKeys.apiHash].orEmpty())

                val presetKey = p[PrefsKeys.logoPreset] ?: LogoPreset.BOTTOM_RIGHT.key
                val idx = presets.indexOf(presetKey).coerceAtLeast(0)
                b.spPreset.setSelection(idx)

                b.etX.setText(((p[PrefsKeys.logoX] ?: 80f)).toString())
                b.etY.setText(((p[PrefsKeys.logoY] ?: 80f)).toString())
                b.etScale.setText(((p[PrefsKeys.logoScale] ?: 25f)).toString())
                b.etOpacity.setText(((p[PrefsKeys.logoOpacity] ?: 90f)).toString())
            }
        }

        b.btnSave.setOnClickListener {
            lifecycleScope.launch {
                prefs.setBoolean(PrefsKeys.logoEnabled, b.swLogo.isChecked)
                prefs.setString(PrefsKeys.logoPreset, b.spPreset.selectedItem?.toString() ?: LogoPreset.BOTTOM_RIGHT.key)

                val x = b.etX.text?.toString()?.toFloatOrNull() ?: 80f
                val y = b.etY.text?.toString()?.toFloatOrNull() ?: 80f
                val scale = b.etScale.text?.toString()?.toFloatOrNull() ?: 25f
                val opacity = b.etOpacity.text?.toString()?.toFloatOrNull() ?: 90f
                prefs.setFloat(PrefsKeys.logoX, x)
                prefs.setFloat(PrefsKeys.logoY, y)
                prefs.setFloat(PrefsKeys.logoScale, scale)
                prefs.setFloat(PrefsKeys.logoOpacity, opacity)

                val apiId = b.etApiId.text?.toString()?.toIntOrNull() ?: 0
                val apiHash = b.etApiHash.text?.toString().orEmpty()
                prefs.setInt(PrefsKeys.apiId, apiId)
                prefs.setString(PrefsKeys.apiHash, apiHash)

                Toast.makeText(this@SettingsActivity, "נשמר ✅", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
