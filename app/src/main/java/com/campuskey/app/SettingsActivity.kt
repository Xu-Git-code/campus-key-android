package com.campuskey.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SettingsActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etUrl: TextInputEditText
    private lateinit var switchBoot: SwitchCompat
    private lateinit var switchAuto: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setContentView(R.layout.activity_settings)

        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        etUrl = findViewById(R.id.etUrl)
        switchBoot = findViewById(R.id.switchBoot)
        switchAuto = findViewById(R.id.switchAuto)

        etUsername.setText(Prefs.username)
        etPassword.setText(Prefs.password)
        etUrl.setText(Prefs.portalUrl)
        switchBoot.isChecked = Prefs.bootStart
        switchAuto.isChecked = Prefs.autoMode

        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener { save() }
        findViewById<MaterialButton>(R.id.btnDefault).setOnClickListener {
            etUrl.setText(Prefs.DEFAULT_URL)
        }
        findViewById<MaterialButton>(R.id.btnTest).setOnClickListener { testLogin() }
    }

    private fun save() {
        val user = etUsername.text?.toString()?.trim().orEmpty()
        val pass = etPassword.text?.toString().orEmpty()
        val url = etUrl.text?.toString()?.trim().orEmpty()

        if (user.isBlank() || pass.isBlank()) {
            Toast.makeText(this, "账号和密码不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "登录 URL 必须以 http:// 或 https:// 开头", Toast.LENGTH_SHORT).show()
            return
        }

        Prefs.username = user
        Prefs.password = pass
        Prefs.portalUrl = url
        Prefs.bootStart = switchBoot.isChecked
        Prefs.autoMode = switchAuto.isChecked

        if (Prefs.autoMode) {
            ContextCompat.startForegroundService(this, Intent(this, LoginService::class.java))
        } else {
            stopService(Intent(this, LoginService::class.java))
        }

        Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun testLogin() {
        val user = etUsername.text?.toString()?.trim().orEmpty()
        val pass = etPassword.text?.toString().orEmpty()
        val url = etUrl.text?.toString()?.trim().orEmpty()

        if (user.isBlank() || pass.isBlank()) {
            Toast.makeText(this, "先填账号密码", Toast.LENGTH_SHORT).show()
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "URL 格式不对", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "正在测试…", Toast.LENGTH_SHORT).show()
        Thread {
            val message = try {
                val wifi = PortalClient.wifiNetwork(this)
                if (wifi == null) {
                    "当前没有连接 WiFi"
                } else if (PortalClient.isOnline(this, wifi)) {
                    "当前已在线，无需登录"
                } else {
                    val result = PortalClient.login(this, wifi, url, user, pass)
                    Thread.sleep(2000)
                    if (PortalClient.isOnline(this, wifi)) "登录成功" else "登录失败：$result"
                }
            } catch (e: Exception) {
                "出错：${e.message}"
            }
            AppLog.add(this, "测试登录：$message")
            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }.start()
    }
}
