package com.paybondhu.app.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.paybondhu.app.MainActivity
import com.paybondhu.app.R
import com.paybondhu.app.data.Db
import com.paybondhu.app.data.Prefs
import com.paybondhu.app.data.T
import com.paybondhu.app.ui.Fmt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps PayBondhu alive. Its permanent notification shows that
 * syncing is on, when the last sync happened and how many messages are waiting.
 */
class SyncService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null
    private var netJob: Job? = null
    private var callback: ConnectivityManager.NetworkCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        instance = this
        ensureChannel(this)
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
        registerNetwork()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Prefs.loggedIn) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_SYNC) {
            scope.launch { SyncEngine.run(applicationContext, "manual", full = true) }
        }
        if (loop == null) {
            loop = scope.launch {
                delay(800)
                while (isActive) {
                    SyncEngine.run(applicationContext, "loop")
                    delay(INTERVAL_MS)
                }
            }
        }
        updateNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        instance = null
        try {
            callback?.let { (getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).unregisterNetworkCallback(it) }
        } catch (_: Exception) {
        }
        scope.cancel()
        // If the system stopped us while the account is still connected, let WorkManager bring us back.
        if (Prefs.loggedIn) SyncScheduler.kick(applicationContext)
        super.onDestroy()
    }

    private fun registerNetwork() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                netJob?.cancel()
                netJob = scope.launch {
                    delay(1500)
                    SyncEngine.run(applicationContext, "network")
                }
            }

            override fun onLost(network: Network) {
                SyncEngine.setOnline(applicationContext, false)
            }
        }
        try {
            cm.registerDefaultNetworkCallback(cb)
            callback = cb
        } catch (_: Exception) {
        }
    }

    fun updateNotification() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, buildNotification())
        } catch (_: Exception) {
        }
    }

    private fun buildNotification(): Notification {
        val ui = SyncEngine.state.value
        val last = Prefs.lastSyncAt
        val today = try {
            Db.get(this).since(Fmt.startOfToday()).count
        } catch (_: Exception) {
            0
        }
        val action = Prefs.authError.isNotEmpty()
        val title = when {
            action -> T.notifAction
            !ui.online -> T.notifOffline
            else -> T.notifActive
        }
        val text = when {
            action -> when (Prefs.authError) {
                "auth_expired" -> T.authExpired
                "auth_other_device" -> T.authOther
                else -> T.authInvalid
            }
            !ui.online -> T.offlineHint
            else -> T.notifLine(if (last > 0) Fmt.clock(last) else "—", ui.pending, today)
        }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            flags
        )
        val sync = PendingIntent.getService(this, 1, Intent(this, SyncService::class.java).setAction(ACTION_SYNC), flags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_sync)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(0xFF0A57FF.toInt())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(open)
            .addAction(0, T.syncNow, sync)
            .build()
    }

    companion object {
        const val NOTIF_ID = 4101
        const val CHANNEL_ID = "pb_sync_status"
        const val ACTION_SYNC = "com.paybondhu.app.SYNC_NOW"
        private const val INTERVAL_MS = 2 * 60 * 1000L

        @Volatile
        private var instance: SyncService? = null

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val ch = NotificationChannel(CHANNEL_ID, T.channelName, NotificationManager.IMPORTANCE_LOW).apply {
                    description = T.channelDesc
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(ch)
            }
        }

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context.applicationContext, Intent(context.applicationContext, SyncService::class.java))
            } catch (_: Exception) {
                // Android may refuse a background start; the WorkManager job tries again.
            }
        }

        fun stop(context: Context) {
            try {
                context.applicationContext.stopService(Intent(context.applicationContext, SyncService::class.java))
            } catch (_: Exception) {
            }
        }

        /** Called by the engine after every state change. */
        fun refresh(context: Context) {
            instance?.updateNotification()
        }
    }
}
