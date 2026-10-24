package com.campuskey.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var switchAuto: SwitchCompat

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        switchAuto = findViewById(R.id.switchAuto)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        switchAuto.isChecked = Prefs.autoMode
        switchAuto.setOnCheckedChangeListener { _, checked ->
            Prefs.autoMode = checked
            if (checked) {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, LoginService::class.java)
                )
            } else {
                stopService(Intent(this, LoginService::class.java))
            }
            refresh()
        }

        findViewById<MaterialButton>(R.id.btnLoginNow).setOnClickListener { loginNow() }
        findViewById<MaterialButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnLogs).setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btnBattery).setOnClickListener { requestBattery() }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val user = Prefs.username.ifBlank { "未设置" }
        val host = try {
            Uri.parse(Prefs.portalUrl).host ?: "-"
        } catch (_: Exception) {
            "-"
        }
        tvStatus.text = "账号：$user\n门户：$host\n自动模式：${if (Prefs.autoMode) "开" else "关"}"
        switchAuto.isChecked = Prefs.autoMode
    }

    private fun loginNow() {
        if (Prefs.username.isBlank() || Prefs.password.isBlank()) {
            Toast.makeText(this, "请先在设置里填写账号密码", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "正在检测…", Toast.LENGTH_SHORT).show()
        Thread {
            val message = try {
                val wifi = PortalClient.wifiNetwork(this)
                if (wifi == null) {
                    "当前没有连接 WiFi"
                } else if (PortalClient.isOnline(this, wifi)) {
                    "当前已在线，无需登录"
                } else {
                    val result = PortalClient.login(
                        this,
                        wifi,
                        Prefs.portalUrl,
                        Prefs.username,
                        Prefs.password
                    )
                    Thread.sleep(2000)
                    if (PortalClient.isOnline(this, wifi)) "登录成功" else "登录失败：$result"
                }
            } catch (e: Exception) {
                "出错：${e.message}"
            }
            AppLog.add(this, "手动检测：$message")
            runOnUiThread {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                refresh()
            }
        }.start()
    }

    private fun requestBattery() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isIgnoringBatteryOptimizations(packageName)) {
                Toast.makeText(this, "已经忽略电池优化", Toast.LENGTH_SHORT).show()
                return
            }
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
            }
        }
    }
}
