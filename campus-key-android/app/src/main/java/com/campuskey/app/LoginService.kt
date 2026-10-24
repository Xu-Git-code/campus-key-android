package com.campuskey.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class LoginService : Service() {

    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val checking = AtomicBoolean(false)
    private lateinit var connectivityManager: ConnectivityManager
    private var callback: ConnectivityManager.NetworkCallback? = null

    private val periodic = object : Runnable {
        override fun run() {
            if (Prefs.autoMode) {
                checkAndLogin("定时检查")
                handler.postDelayed(this, PERIOD_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        createChannel()
        startForeground(NOTIFY_ID, buildNotification("正在监听网络…"))

        callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handler.postDelayed({ checkAndLogin("网络已连接") }, 3000)
            }
        }
        try {
            connectivityManager.registerDefaultNetworkCallback(callback!!)
        } catch (_: Exception) {
        }

        handler.postDelayed({ checkAndLogin("服务启动") }, 2000)
        handler.postDelayed(periodic, PERIOD_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Prefs.autoMode) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun checkAndLogin(reason: String) {
        if (!Prefs.autoMode) return
        if (!checking.compareAndSet(false, true)) return

        executor.execute {
            try {
                val wifi = PortalClient.wifiNetwork(this)
                if (wifi == null) {
                    AppLog.add(this, "$reason：当前没有连接 WiFi")
                    updateNotification("未连接 WiFi")
                    return@execute
                }

                if (PortalClient.isOnline(this, wifi)) {
                    AppLog.add(this, "$reason：已在线")
                    updateNotification("已在线")
                    return@execute
                }

                val user = Prefs.username
                val pass = Prefs.password
                if (user.isBlank() || pass.isBlank()) {
                    AppLog.add(this, "$reason：还没有配置账号密码")
                    updateNotification("请先在 App 里填写账号密码")
                    return@execute
                }

                AppLog.add(this, "$reason：未登录，发送登录请求")
                val result = PortalClient.login(this, wifi, Prefs.portalUrl, user, pass)
                AppLog.add(this, "登录响应：$result")

                Thread.sleep(2000)
                if (PortalClient.isOnline(this, wifi)) {
                    AppLog.add(this, "登录成功，网络已恢复")
                    updateNotification("已自动登录")
                } else {
                    AppLog.add(this, "登录后仍无法上网，请检查 URL 或账号")
                    updateNotification("登录失败，请查看日志")
                }
            } catch (e: Exception) {
                AppLog.add(this, "$reason：异常 ${e.message}")
                updateNotification("登录异常")
            } finally {
                checking.set(false)
            }
        }
    }

    private fun updateNotification(text: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFY_ID, buildNotification(text))
        } catch (_: Exception) {
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CampusKey")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "自动登录",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try {
            callback?.let { connectivityManager.unregisterNetworkCallback(it) }
        } catch (_: Exception) {
        }
        handler.removeCallbacksAndMessages(null)
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "campus_key"
        private const val NOTIFY_ID = 1001
        private const val PERIOD_MS = 10 * 60 * 1000L
    }
}
