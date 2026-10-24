package com.campuskey.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.URL

object PortalClient {
    private val checks = listOf(
        "http://example.com" to "Example Domain",
        "http://www.baidu.com" to "百度"
    )

    /** 找到当前连接的 WiFi 网络；找不到返回 null。 */
    fun wifiNetwork(context: Context): Network? {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            var fallback: Network? = null
            for (network in cm.allNetworks) {
                val caps = cm.getNetworkCapabilities(network) ?: continue
                if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    return network
                }
                if (fallback == null) fallback = network
            }
            fallback
        } catch (_: Exception) {
            null
        }
    }

    fun currentIp(context: Context, network: Network?): String {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val target = network ?: cm.activeNetwork ?: return ""
            val props = cm.getLinkProperties(target) ?: return ""
            for (link in props.linkAddresses) {
                val address = link.address
                if (address is Inet4Address && !address.isLoopbackAddress) {
                    return address.hostAddress ?: ""
                }
            }
        } catch (_: Exception) {
        }
        return ""
    }

    fun isOnline(context: Context, network: Network?): Boolean {
        for ((url, marker) in checks) {
            var conn: HttpURLConnection? = null
            try {
                conn = open(url, network)
                val code = conn.responseCode
                if (code in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    if (body.contains(marker)) return true
                }
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }
        }
        return false
    }

    fun buildUrl(
        context: Context,
        network: Network?,
        template: String,
        username: String,
        password: String
    ): String {
        val ip = currentIp(context, network)
        return template
            .replace("{username_urlencoded}", Uri.encode(username))
            .replace("{password_urlencoded}", Uri.encode(password))
            .replace("{username}", username)
            .replace("{password}", password)
            .replace("{ip}", ip)
    }

    fun login(
        context: Context,
        network: Network?,
        template: String,
        username: String,
        password: String
    ): String {
        val url = buildUrl(context, network, template, username, password)
        var conn: HttpURLConnection? = null
        return try {
            conn = open(url, network)
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }
                ?.replace("\n", " ")
                ?.take(200)
                ?: ""
            "HTTP $code $body"
        } catch (e: Exception) {
            "请求出错：${e.message}"
        } finally {
            conn?.disconnect()
        }
    }

    private fun open(url: String, network: Network?): HttpURLConnection {
        val target = URL(url)
        val conn = (network?.openConnection(target) ?: target.openConnection()) as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 8000
        conn.readTimeout = 12000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) CampusKey/1.0")
        return conn
    }
}
