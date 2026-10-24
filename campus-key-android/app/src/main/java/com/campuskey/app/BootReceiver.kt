package com.campuskey.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        Prefs.init(context)
        if (Prefs.autoMode && Prefs.bootStart) {
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, LoginService::class.java)
                )
            } catch (_: Exception) {
                // 某些系统会限制开机启动前台服务，忽略即可，打开 App 后会重新拉起。
            }
        }
    }
}
