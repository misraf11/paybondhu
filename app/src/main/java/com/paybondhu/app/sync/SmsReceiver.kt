package com.paybondhu.app.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.paybondhu.app.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        Prefs.init(context)
        if (!Prefs.loggedIn) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts == null || parts.isEmpty()) return
        val address = parts[0].originatingAddress ?: return
        val body = parts.joinToString("") { it.messageBody ?: "" }
        val time = parts[0].timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()

        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (SyncEngine.ingest(app, address, body, time, upload = true)) {
                    SyncEngine.touch(app)
                    SyncService.start(app)
                    val done = withTimeoutOrNull(9000) { SyncEngine.run(app, "sms") }
                    if (done != true) SyncScheduler.kick(app)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
