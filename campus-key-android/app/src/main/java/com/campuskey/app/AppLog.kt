package com.campuskey.app

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLog {
    private const val TAG = "CampusKey"
    private const val MAX_BYTES = 200_000L
    private val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    @Synchronized
    fun add(context: Context, message: String) {
        Log.i(TAG, message)
        try {
            val file = File(context.filesDir, "campus.log")
            if (file.length() > MAX_BYTES) file.delete()
            file.appendText(timeFormat.format(Date()) + "  " + message + "\n")
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun tail(context: Context, maxLines: Int = 300): String {
        return try {
            val file = File(context.filesDir, "campus.log")
            if (!file.exists()) {
                "(暂无日志)"
            } else {
                file.readLines().takeLast(maxLines).joinToString("\n")
            }
        } catch (e: Exception) {
            "(读取日志失败：${e.message})"
        }
    }

    @Synchronized
    fun clear(context: Context) {
        try {
            File(context.filesDir, "campus.log").delete()
        } catch (_: Exception) {
        }
    }
}
