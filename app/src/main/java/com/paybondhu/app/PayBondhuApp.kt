package com.paybondhu.app

import android.app.Application
import com.paybondhu.app.data.Prefs
import com.paybondhu.app.sync.SyncScheduler
import com.paybondhu.app.sync.SyncService

class PayBondhuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        SyncScheduler.ensurePeriodic(this)
        if (Prefs.loggedIn) {
            SyncService.start(this)
        }
    }
}
