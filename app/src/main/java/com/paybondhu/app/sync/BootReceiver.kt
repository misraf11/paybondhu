package com.paybondhu.app.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.paybondhu.app.data.Prefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Prefs.init(context)
        SyncScheduler.ensurePeriodic(context)
        if (Prefs.loggedIn) {
            SyncService.start(context)
            SyncScheduler.kick(context)
        }
    }
}
