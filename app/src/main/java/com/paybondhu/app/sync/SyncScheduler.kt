package com.paybondhu.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.paybondhu.app.data.Prefs
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Prefs.init(applicationContext)
        if (!Prefs.loggedIn) return Result.success()
        SyncService.start(applicationContext)
        val ok = SyncEngine.run(applicationContext, "worker")
        return if (ok) Result.success() else Result.retry()
    }
}

object SyncScheduler {
    private val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Safety net: every 15 minutes, even if the service was killed. */
    fun ensurePeriodic(context: Context) {
        val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(net)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("pb_periodic", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    /** Retry soon when there is network again (used when an SMS could not be uploaded immediately). */
    fun kick(context: Context) {
        val req = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(net)
            .setBackoffCriteria(androidx.work.BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("pb_kick", ExistingWorkPolicy.REPLACE, req)
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("pb_periodic")
        WorkManager.getInstance(context).cancelUniqueWork("pb_kick")
    }
}
