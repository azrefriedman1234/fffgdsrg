package com.pasiflonet.mobile.ui

import android.os.Bundle
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
}
