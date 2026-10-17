package com.paybondhu.app.data

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Language choice, backed by Compose state so the UI recomposes when it changes. */
object Lang {
    var code by mutableStateOf("en")
    val bn: Boolean get() = code == "bn"
}

object Prefs {
    val DEFAULT_SERVER: String = com.paybondhu.app.BuildConfig.DEFAULT_SERVER

    private const val FILE = "pb_prefs"
    private lateinit var sp: SharedPreferences
    private var inited = false

    @Synchronized
    fun init(context: Context) {
        if (inited) return
        sp = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        Lang.code = sp.getString("lang", defaultLang()) ?: "en"
        inited = true
    }

    private fun defaultLang(): String = if (java.util.Locale.getDefault().language == "bn") "bn" else "en"

    private fun str(key: String, def: String = ""): String = sp.getString(key, def) ?: def

    var server: String
        get() = str("server", DEFAULT_SERVER).let { if (it.endsWith("/")) it else "$it/" }
        set(v) = sp.edit().putString("server", v.trim()).apply()

    var email: String
        get() = str("email")
        set(v) = sp.edit().putString("email", v.trim()).apply()

    var deviceKey: String
        get() = str("device_key")
        set(v) = sp.edit().putString("device_key", v.trim()).apply()

    var deviceId: String
        get() = str("device_id")
        set(v) = sp.edit().putString("device_id", v).apply()

    var deviceName: String
        get() = str("device_name")
        set(v) = sp.edit().putString("device_name", v).apply()

    var planExpire: String
        get() = str("plan_expire")
        set(v) = sp.edit().putString("plan_expire", v).apply()

    var lang: String
        get() = str("lang", defaultLang())
        set(v) {
            sp.edit().putString("lang", v).apply()
            Lang.code = v
        }

    var scanInbox: Boolean
        get() = sp.getBoolean("scan_inbox", true)
        set(v) = sp.edit().putBoolean("scan_inbox", v).apply()

    var lastSyncAt: Long
        get() = sp.getLong("last_sync_at", 0L)
        set(v) = sp.edit().putLong("last_sync_at", v).apply()

    var lastScanAt: Long
        get() = sp.getLong("last_scan_at", 0L)
        set(v) = sp.edit().putLong("last_scan_at", v).apply()

    var lastFullAt: Long
        get() = sp.getLong("last_full_at", 0L)
        set(v) = sp.edit().putLong("last_full_at", v).apply()

    var historyImported: Boolean
        get() = sp.getBoolean("history_imported", false)
        set(v) = sp.edit().putBoolean("history_imported", v).apply()

    /** "" = authorised, otherwise the server's code: auth_expired / auth_other_device / auth_invalid */
    var authError: String
        get() = str("auth_error")
        set(v) = sp.edit().putString("auth_error", v).apply()

    var latestCode: Int
        get() = sp.getInt("latest_code", 0)
        set(v) = sp.edit().putInt("latest_code", v).apply()

    var latestName: String
        get() = str("latest_name")
        set(v) = sp.edit().putString("latest_name", v).apply()

    var apkUrl: String
        get() = str("apk_url")
        set(v) = sp.edit().putString("apk_url", v).apply()

    var batteryAsked: Boolean
        get() = sp.getBoolean("battery_asked", false)
        set(v) = sp.edit().putBoolean("battery_asked", v).apply()

    var permsAsked: Boolean
        get() = sp.getBoolean("perms_asked", false)
        set(v) = sp.edit().putBoolean("perms_asked", v).apply()

    val loggedIn: Boolean
        get() = inited && email.isNotBlank() && deviceKey.isNotBlank()

    fun androidId(context: Context): String {
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return if (id.isNullOrBlank()) "pb-" + android.os.Build.MODEL.replace(Regex("[^A-Za-z0-9]"), "") else id
    }

    /** Wipes the account (keeps language). Local history is kept. */
    fun logout() {
        val lang = lang
        sp.edit().clear().putString("lang", lang).apply()
        Lang.code = lang
    }
}
