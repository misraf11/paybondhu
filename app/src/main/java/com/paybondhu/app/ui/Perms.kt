package com.paybondhu.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class PermState(val sms: Boolean, val readSms: Boolean, val notif: Boolean, val battery: Boolean) {
    val smsOk: Boolean get() = sms && readSms
    val allOk: Boolean get() = smsOk && notif && battery
}

object Perms {
    private fun granted(c: Context, p: String) = ContextCompat.checkSelfPermission(c, p) == PackageManager.PERMISSION_GRANTED

    fun read(c: Context): PermState {
        val notif = if (Build.VERSION.SDK_INT >= 33) granted(c, Manifest.permission.POST_NOTIFICATIONS)
        else NotificationManagerCompat.from(c).areNotificationsEnabled()
        val pm = c.getSystemService(Context.POWER_SERVICE) as PowerManager
        return PermState(
            sms = granted(c, Manifest.permission.RECEIVE_SMS),
            readSms = granted(c, Manifest.permission.READ_SMS),
            notif = notif,
            battery = pm.isIgnoringBatteryOptimizations(c.packageName),
        )
    }

    fun runtimeList(): Array<String> {
        val l = arrayListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= 33) l.add(Manifest.permission.POST_NOTIFICATIONS)
        return l.toTypedArray()
    }

    fun batteryIntent(c: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + c.packageName))

    fun appSettings(c: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + c.packageName))
}
