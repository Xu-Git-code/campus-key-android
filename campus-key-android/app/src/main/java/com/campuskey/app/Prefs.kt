package com.campuskey.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object Prefs {
    private const val FILE = "campus_key"
    private lateinit var sp: SharedPreferences

    const val DEFAULT_URL =
        "http://172.16.255.3/drcom/login?callback=dr1003" +
            "&DDDDD={username_urlencoded}&upass={password_urlencoded}" +
            "&0MKKey=123456&R1=0&R2=&R3=1&R6=0&para=00&v6ip=&R7=0" +
            "&user_account={username_urlencoded}&user_password={password_urlencoded}" +
            "&wlan_user_ip={ip}&wlan_user_ipv6=&authex_enable=" +
            "&wlan_user_mac=000000000000&wlan_ac_name=&jsVersion=4.5.1" +
            "&terminal_type=1&lang=zh-cn" +
            "&user_agent=Mozilla%2F5.0%20(Windows%20NT%2010.0%3B%20Win64%3B%20x64)" +
            "%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)" +
            "%20Chrome%2F155.0.0.0%20Safari%2F537.36%20Edg%2F155.0.0.0" +
            "&enable_r3=1&mac_type=0&rcn=&operate=portal_login&business_type=1" +
            "&program_index=S8VGGW1788178366&page_index=vVzMZG1788178378&v=4052&lang=zh"

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(FILE + "_fallback", Context.MODE_PRIVATE)
        }
    }

    var username: String
        get() = sp.getString("username", "") ?: ""
        set(value) {
            sp.edit().putString("username", value).apply()
        }

    var password: String
        get() = sp.getString("password", "") ?: ""
        set(value) {
            sp.edit().putString("password", value).apply()
        }

    var portalUrl: String
        get() = sp.getString("portal_url", DEFAULT_URL) ?: DEFAULT_URL
        set(value) {
            sp.edit().putString("portal_url", value).apply()
        }

    var autoMode: Boolean
        get() = sp.getBoolean("auto_mode", false)
        set(value) {
            sp.edit().putBoolean("auto_mode", value).apply()
        }

    var bootStart: Boolean
        get() = sp.getBoolean("boot_start", true)
        set(value) {
            sp.edit().putBoolean("boot_start", value).apply()
        }
}
