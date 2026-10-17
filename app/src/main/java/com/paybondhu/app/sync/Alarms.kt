package com.paybondhu.app.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.paybondhu.app.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A 5-minute wake-up chain. Coroutine delays stop while the CPU sleeps, so this alarm is what keeps the
 * "device online" heartbeat going on a phone that is idle with the screen off.
 */
object Alarms {
    private const val ACTION = "com.paybondhu.app.HEARTBEAT"
    private const val EVERY_MS = 5 * 60 * 1000L

    private fun intent(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, 7,
            Intent(c, AlarmReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun schedule(c: Context) {
        try {
            val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + EVERY_MS, intent(c))
        } catch (_: Exception) {
        }
    }

    fun cancel(c: Context) {
        try {
            (c.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(intent(c))
        } catch (_: Exception) {
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.init(context)
        if (!Prefs.loggedIn) return
        val app = context.applicationContext
        Alarms.schedule(app)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                SyncService.start(app)
                withTimeoutOrNull(9000) { SyncEngine.run(app, "alarm") }
            } finally {
                pending.finish()
            }
        }
    }
}
