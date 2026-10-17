package com.paybondhu.app.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.paybondhu.app.data.Db
import com.paybondhu.app.data.MsgState
import com.paybondhu.app.data.Prefs
import com.paybondhu.app.data.Provider
import com.paybondhu.app.data.SmsParser
import com.paybondhu.app.net.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** What the UI and the notification show. */
data class SyncUi(
    val running: Boolean = false,
    val online: Boolean = true,
    val lastSyncAt: Long = 0L,
    val pending: Int = 0,
    val auth: String = "",
    val error: String = "",
    val serverWaiting: Int = 0,
    val serverUsed: Int = 0,
)

object SyncEngine {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(SyncUi())
    val state = _state.asStateFlow()

    private const val DAY = 24L * 60 * 60 * 1000
    private const val UPLOAD_WINDOW = 2 * DAY      // older inbox SMS are kept for history only
    private const val HISTORY_WINDOW = 30 * DAY
    private const val MAX_ATTEMPTS = 8

    fun hasSmsRead(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    /** Called for every SMS that reaches the phone. Returns true when it was stored for upload. */
    fun ingest(context: Context, address: String?, body: String, receivedAt: Long, upload: Boolean): Boolean {
        val provider = Provider.match(address) ?: return false
        if (body.isBlank() || !SmsParser.isTransaction(body)) return false
        val db = Db.get(context)
        val state = if (upload) MsgState.PENDING else MsgState.LOCAL
        return db.insert(address ?: provider.serverAddress, provider, body, receivedAt, state)
    }

    /** Reads the SMS inbox for payment messages the broadcast receiver may have missed. */
    suspend fun scanInbox(context: Context, days: Int?): Int = withContext(Dispatchers.IO) {
        if (!hasSmsRead(context)) return@withContext 0
        val now = System.currentTimeMillis()
        val from = when {
            days != null -> now - days * DAY
            Prefs.lastScanAt > 0 -> Prefs.lastScanAt - 15 * 60 * 1000
            else -> now - UPLOAD_WINDOW
        }
        var added = 0
        try {
            context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(from.toString()),
                "${Telephony.Sms.DATE} ASC"
            )?.use { c ->
                val iA = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val iB = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val iD = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (c.moveToNext()) {
                    val date = c.getLong(iD)
                    val upload = date >= now - UPLOAD_WINDOW
                    if (ingest(context, c.getString(iA), c.getString(iB) ?: "", date, upload)) added++
                }
            }
            Prefs.lastScanAt = now
        } catch (e: SecurityException) {
            // permission revoked while running
        } catch (e: Exception) {
            // unreadable provider on some OEM ROMs: ignore, live receiver still works
        }
        added
    }

    /** One-time import of up to 30 days so History is complete from day one. */
    suspend fun importHistory(context: Context) {
        if (Prefs.historyImported || !hasSmsRead(context)) return
        scanInbox(context, (HISTORY_WINDOW / DAY).toInt())
        Prefs.historyImported = true
    }

    private fun refreshUi(context: Context, mutate: (SyncUi) -> SyncUi) {
        val pending = Db.get(context).count(MsgState.PENDING)
        _state.value = mutate(_state.value).copy(pending = pending, lastSyncAt = Prefs.lastSyncAt, auth = Prefs.authError)
        SyncService.refresh(context)
    }

    fun touch(context: Context) = refreshUi(context) { it }

    fun setOnline(context: Context, online: Boolean) = refreshUi(context) { it.copy(online = online) }

    /**
     * Full cycle: inbox scan (optional) -> heartbeat -> upload queue.
     * @param full also asks the server for its recent SMS to mark matched payments
     */
    suspend fun run(context: Context, reason: String, full: Boolean = false): Boolean {
        val app = context.applicationContext
        Prefs.init(app)
        if (!Prefs.loggedIn) return false
        return mutex.withLock {
            refreshUi(app) { it.copy(running = true) }
            val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "paybondhu:sync")
            wl.setReferenceCounted(false)
            try {
                wl.acquire(90_000L)
            } catch (_: Exception) {
            }
            var ok = false
            try {
                if (Prefs.scanInbox && hasSmsRead(app)) {
                    val due = reason == "manual" || reason == "open" || System.currentTimeMillis() - Prefs.lastScanAt > 5 * 60 * 1000
                    if (due) {
                        importHistory(app)
                        scanInbox(app, null)
                    }
                }

                val wantRecent = full || System.currentTimeMillis() - Prefs.lastFullAt > 10 * 60 * 1000
                var st = Api.status(wantRecent)
                if (!st.network && !st.ok && !st.auth && st.code == "bad_response") {
                    // Older server without api/device-status: device-connect also refreshes the online timestamp.
                    st = Api.connect(Prefs.server, Prefs.email, Prefs.deviceKey, Prefs.deviceId)
                }
                if (st.network) {
                    refreshUi(app) { it.copy(running = false, online = false, error = st.message) }
                    return@withLock false
                }
                if (st.ok) {
                    Prefs.authError = ""
                    val j = st.json
                    if (j != null) {
                        Prefs.planExpire = j.optString("plan_expire", Prefs.planExpire)
                        j.optString("device_name").takeIf { it.isNotEmpty() }?.let { Prefs.deviceName = it }
                        j.optJSONObject("app")?.let { a ->
                            Prefs.latestCode = a.optInt("latest_code", 0)
                            Prefs.latestName = a.optString("latest_name", "")
                            Prefs.apkUrl = a.optString("apk_url", "")
                        }
                        val q = j.optJSONObject("queue")
                        val arr = j.optJSONArray("recent")
                        if (arr != null) {
                            val used = ArrayList<String>()
                            for (i in 0 until arr.length()) {
                                val r = arr.optJSONObject(i) ?: continue
                                if (r.optInt("used", 0) == 1) {
                                    used.add(SmsParser.hash(r.optString("address"), r.optString("message")))
                                }
                            }
                            Db.get(app).setUsed(used)
                            Prefs.lastFullAt = System.currentTimeMillis()
                        }
                        if (q != null) {
                            val w = q.optInt("waiting"); val u = q.optInt("used")
                            _state.value = _state.value.copy(serverWaiting = w, serverUsed = u)
                        }
                    }
                    Prefs.lastSyncAt = System.currentTimeMillis()
                } else if (st.auth) {
                    Prefs.authError = st.code
                    refreshUi(app) { it.copy(running = false, online = true, error = st.message) }
                    return@withLock false
                } else {
                    refreshUi(app) { it.copy(running = false, online = true, error = st.message) }
                    return@withLock false
                }

                ok = uploadPending(app)
            } catch (e: Exception) {
                refreshUi(app) { it.copy(running = false, error = e.message ?: "Error") }
                return@withLock false
            } finally {
                try {
                    if (wl.isHeld) wl.release()
                } catch (_: Exception) {
                }
            }
            refreshUi(app) { it.copy(running = false, online = true, error = "") }
            ok
        }
    }

    /** @return true when the queue was fully drained (or empty) */
    private suspend fun uploadPending(context: Context): Boolean {
        val db = Db.get(context)
        var guard = 0
        while (guard++ < 20) {
            val batch = db.pending(50)
            if (batch.isEmpty()) return true
            for (m in batch) {
                val r = Api.send(m.address, m.body)
                when {
                    r.network -> {
                        _state.value = _state.value.copy(online = false)
                        return false
                    }
                    r.ok -> db.markSynced(m.id, System.currentTimeMillis())
                    r.auth -> {
                        Prefs.authError = r.code
                        return false
                    }
                    r.code == "invalid" -> db.markRejected(m.id, r.message.ifBlank { "Rejected by server" })
                    r.code == "rate_limited" || r.code == "busy" -> {
                        db.markAttempt(m.id, r.message)
                        return false
                    }
                    else -> {
                        if (m.attempts + 1 >= MAX_ATTEMPTS) db.markRejected(m.id, r.message.ifBlank { "Server error" })
                        else db.markAttempt(m.id, r.message.ifBlank { "Server error" })
                        if (r.http >= 500 || r.code == "bad_response") return false
                    }
                }
            }
        }
        return db.count(MsgState.PENDING) == 0
    }

    fun clearAuthIfAny() {
        Prefs.authError = ""
    }
}
