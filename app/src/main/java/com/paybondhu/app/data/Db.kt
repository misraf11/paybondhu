package com.paybondhu.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow

object MsgState {
    const val PENDING = 0
    const val SYNCED = 1
    const val REJECTED = 2
    const val LOCAL = 3 // history only, too old to upload
}

data class Msg(
    val id: Long,
    val hash: String,
    val address: String,
    val provider: String,
    val body: String,
    val amount: Double,
    val trx: String,
    val phone: String,
    val kind: String,
    val receivedAt: Long,
    val state: Int,
    val used: Boolean,
    val attempts: Int,
    val error: String,
    val syncedAt: Long,
) {
    val providerEnum: Provider? get() = Provider.fromKey(provider)
}

data class DayStats(val count: Int, val amountIn: Double)

class Db private constructor(context: Context) : SQLiteOpenHelper(context.applicationContext, "paybondhu.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE msg(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                hash TEXT NOT NULL UNIQUE,
                address TEXT NOT NULL,
                provider TEXT NOT NULL,
                body TEXT NOT NULL,
                amount REAL NOT NULL DEFAULT 0,
                trx TEXT NOT NULL DEFAULT '',
                phone TEXT NOT NULL DEFAULT '',
                kind TEXT NOT NULL DEFAULT 'in',
                received_at INTEGER NOT NULL,
                state INTEGER NOT NULL DEFAULT 0,
                used INTEGER NOT NULL DEFAULT 0,
                attempts INTEGER NOT NULL DEFAULT 0,
                error TEXT NOT NULL DEFAULT '',
                synced_at INTEGER NOT NULL DEFAULT 0
            )"""
        )
        db.execSQL("CREATE INDEX idx_msg_time ON msg(received_at DESC)")
        db.execSQL("CREATE INDEX idx_msg_state ON msg(state)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    /** Bumped on every write; the UI watches it to reload. */
    val version = MutableStateFlow(0L)

    private fun changed() {
        version.value = version.value + 1
    }

    private fun row(c: Cursor) = Msg(
        id = c.getLong(0),
        hash = c.getString(1),
        address = c.getString(2),
        provider = c.getString(3),
        body = c.getString(4),
        amount = c.getDouble(5),
        trx = c.getString(6),
        phone = c.getString(7),
        kind = c.getString(8),
        receivedAt = c.getLong(9),
        state = c.getInt(10),
        used = c.getInt(11) == 1,
        attempts = c.getInt(12),
        error = c.getString(13),
        syncedAt = c.getLong(14),
    )

    private val cols = "id,hash,address,provider,body,amount,trx,phone,kind,received_at,state,used,attempts,error,synced_at"

    /** Inserts a message unless the same SMS is already stored. Returns true if it was new. */
    @Synchronized
    fun insert(address: String, provider: Provider, body: String, receivedAt: Long, state: Int): Boolean {
        val parsed = SmsParser.parse(body)
        val v = ContentValues().apply {
            put("hash", SmsParser.hash(provider.serverAddress, body))
            put("address", address)
            put("provider", provider.name)
            put("body", SmsParser.normalize(body))
            put("amount", parsed.amount)
            put("trx", parsed.trx)
            put("phone", parsed.phone)
            put("kind", parsed.kind)
            put("received_at", receivedAt)
            put("state", state)
        }
        val id = writableDatabase.insertWithOnConflict("msg", null, v, SQLiteDatabase.CONFLICT_IGNORE)
        if (id != -1L) changed()
        return id != -1L
    }

    fun pending(limit: Int = 100): List<Msg> {
        val out = ArrayList<Msg>()
        readableDatabase.rawQuery("SELECT $cols FROM msg WHERE state=0 ORDER BY received_at ASC LIMIT ?", arrayOf(limit.toString())).use { c ->
            while (c.moveToNext()) out.add(row(c))
        }
        return out
    }

    fun count(state: Int): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM msg WHERE state=?", arrayOf(state.toString())).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun total(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM msg", null).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun markSynced(id: Long, now: Long) {
        writableDatabase.execSQL("UPDATE msg SET state=1, error='', synced_at=?, attempts=attempts+1 WHERE id=?", arrayOf<Any>(now, id))
        changed()
    }

    fun markRejected(id: Long, error: String) {
        writableDatabase.execSQL("UPDATE msg SET state=2, error=?, attempts=attempts+1 WHERE id=?", arrayOf<Any>(error.take(200), id))
        changed()
    }

    fun markAttempt(id: Long, error: String) {
        writableDatabase.execSQL("UPDATE msg SET error=?, attempts=attempts+1 WHERE id=?", arrayOf<Any>(error.take(200), id))
        changed()
    }

    fun retry(id: Long) {
        writableDatabase.execSQL("UPDATE msg SET state=0, error='', attempts=0 WHERE id=?", arrayOf<Any>(id))
        changed()
    }

    fun setUsed(hashes: Collection<String>) {
        if (hashes.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (h in hashes) db.execSQL("UPDATE msg SET used=1, state=CASE WHEN state=0 THEN 1 ELSE state END WHERE hash=? AND used=0", arrayOf<Any>(h))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        changed()
    }

    fun clearAll() {
        writableDatabase.execSQL("DELETE FROM msg")
        changed()
    }

    /**
     * @param provider provider key or null for all
     * @param filter   0 all, 1 pending, 2 synced (sent, not matched yet), 3 matched, 4 failed
     */
    fun list(provider: String?, filter: Int, query: String, limit: Int): List<Msg> {
        val where = ArrayList<String>()
        val args = ArrayList<String>()
        if (provider != null) {
            where.add("provider=?"); args.add(provider)
        }
        when (filter) {
            1 -> where.add("state=0")
            2 -> where.add("state=1 AND used=0")
            3 -> where.add("used=1")
            4 -> where.add("state=2")
        }
        val q = query.trim()
        if (q.isNotEmpty()) {
            where.add("(trx LIKE ? OR phone LIKE ? OR body LIKE ?)")
            val like = "%" + q.replace("%", "") + "%"
            args.add(like); args.add(like); args.add(like)
        }
        val sql = "SELECT $cols FROM msg" + (if (where.isEmpty()) "" else " WHERE " + where.joinToString(" AND ")) + " ORDER BY received_at DESC LIMIT $limit"
        val out = ArrayList<Msg>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { c ->
            while (c.moveToNext()) out.add(row(c))
        }
        return out
    }

    fun since(from: Long): DayStats =
        readableDatabase.rawQuery(
            "SELECT COUNT(*), COALESCE(SUM(CASE WHEN kind='in' THEN amount ELSE 0 END),0) FROM msg WHERE received_at>=? AND state<>2",
            arrayOf(from.toString())
        ).use { c -> if (c.moveToFirst()) DayStats(c.getInt(0), c.getDouble(1)) else DayStats(0, 0.0) }

    companion object {
        @Volatile
        private var inst: Db? = null
        fun get(context: Context): Db = inst ?: synchronized(this) {
            inst ?: Db(context).also { inst = it }
        }
    }
}
