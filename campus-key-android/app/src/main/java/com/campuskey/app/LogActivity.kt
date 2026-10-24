package com.campuskey.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class LogActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        tvLog = findViewById(R.id.tvLog)
        load()

        findViewById<MaterialButton>(R.id.btnRefresh).setOnClickListener { load() }
        findViewById<MaterialButton>(R.id.btnClear).setOnClickListener {
            AppLog.clear(this)
            load()
        }
    }

    private fun load() {
        tvLog.text = AppLog.tail(this, 300)
    }
}
