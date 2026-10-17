package com.paybondhu.app.ui

import com.paybondhu.app.data.T
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Fmt {
    private val en = Locale.ENGLISH

    fun money(v: Double): String = "৳" + String.format(en, "%,.2f", v)

    fun clock(ms: Long): String = SimpleDateFormat("hh:mm a", en).format(Date(ms))

    fun dateTime(ms: Long): String = SimpleDateFormat("dd MMM yyyy, hh:mm a", en).format(Date(ms))

    fun startOfToday(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun ago(ms: Long): String {
        if (ms <= 0) return T.neverSynced
        val s = (System.currentTimeMillis() - ms) / 1000
        return when {
            s < 45 -> T.justNow
            s < 3600 -> T.minAgo(((s + 30) / 60).toInt().coerceAtLeast(1))
            s < 86400 -> T.hourAgo((s / 3600).toInt())
            else -> T.dayAgo((s / 86400).toInt())
        }
    }

    fun dayLabel(ms: Long): String {
        val today = startOfToday()
        val day = 24L * 60 * 60 * 1000
        return when {
            ms >= today -> T.today
            ms >= today - day -> T.yesterday
            else -> SimpleDateFormat("dd MMM yyyy", en).format(Date(ms))
        }
    }

    fun maskPhone(p: String): String = if (p.length >= 11) p.substring(0, 5) + "••••" + p.takeLast(2) else p
}
