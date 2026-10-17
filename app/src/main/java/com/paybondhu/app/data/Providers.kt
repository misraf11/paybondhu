package com.paybondhu.app.data

import java.security.MessageDigest

/**
 * Mobile-financial-service senders the PayBondhu server accepts. [serverAddress] is the
 * canonical value the server stores in module_data.address (see ApiController::addMessage).
 */
enum class Provider(val label: String, val serverAddress: String, val color: Long, val aliases: List<String>) {
    BKASH("bKash", "bkash", 0xFFE2136E, listOf("bkash")),
    NAGAD("Nagad", "nagad", 0xFFF37021, listOf("nagad")),
    ROCKET("Rocket", "16216", 0xFF8C3494, listOf("rocket", "16216")),
    UPAY("Upay", "upay", 0xFF00A859, listOf("upay")),
    TAP("Tap", "tap", 0xFF1D9BF0, listOf("tap")),
    SURECASH("SureCash", "16495", 0xFFD7263D, listOf("surecash", "16495")),
    IPAY("iPay", "09638900800", 0xFF6C47FF, listOf("ipay", "09638900800")),
    OKWALLET("OK Wallet", "01401195496", 0xFF0072BC, listOf("okwallet", "01401195496")),
    MCASH("mCash", "mCash", 0xFF0B7A3E, listOf("mcash")),
    MYCASH("MyCash", "mycash", 0xFFEF8A00, listOf("mycash")),
    ISLAMI("Islami Bank", "Islami.Bank", 0xFF0A6B3D, listOf("cellfin", "islami.bank")),
    EASYPAISA("3737", "3737", 0xFF1A9E5C, listOf("easypaisa", "3737"));

    companion object {
        private val index: Map<String, Provider> by lazy {
            val m = HashMap<String, Provider>()
            values().forEach { p -> p.aliases.forEach { m[it] = p } }
            m
        }

        fun match(rawAddress: String?): Provider? {
            if (rawAddress.isNullOrBlank()) return null
            return index[rawAddress.lowercase().replace(" ", "")]
        }

        fun fromKey(key: String): Provider? = values().firstOrNull { it.name == key }
    }
}

/** What we understand about a transaction SMS. */
data class Parsed(
    val amount: Double,
    val trx: String,
    val phone: String,
    val kind: String,
)

object SmsParser {
    // Loose patterns mirror the server (ApiController::addMessage) and decide whether an SMS is a payment at all.
    private val amountLoose = Regex("""(?i)(?:Tk|BDT|Taka|Rs)\.?\s*:?\s*[0-9][0-9,]*(?:\.[0-9]{1,3})?""")
    private val amountAlt = Regex("""(?i)(?:amount|received|payment|cash\s*out|credited)[^0-9]{0,24}[0-9][0-9,]*(?:\.[0-9]{1,3})?""")
    private val trxLoose = Regex("""(?i)\b(?:trx|txn|transaction)\s*(?:id|no)?\s*[:#-]?\s*[A-Z0-9]{6,}\b""")

    // Strict patterns pull the actual values out for display.
    private val amountRe = Regex("""(?i)\b(?:tk|bdt|taka|rs)\.?\s*:?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
    private val amountAltRe = Regex("""(?i)(?:amount|received|payment|credited)[^0-9]{0,24}([0-9][0-9,]*(?:\.[0-9]{1,2})?)""")
    private val trxRe = Regex("""(?i)\b(?:trx|txn)\s*(?:id|no)?\s*[:#-]?\s*((?=[A-Z0-9]{6,}\b)[A-Z0-9]*[0-9][A-Z0-9]*)\b""")
    private val trxAltRe = Regex("""(?i)\btransaction\s*(?:id|no)\s*[:#-]?\s*((?=[A-Z0-9]{6,}\b)[A-Z0-9]*[0-9][A-Z0-9]*)\b""")
    private val phoneRe = Regex("""(?<![0-9])(?:(?:\+|00)?88)?(01[3-9](?:[\s-]?[0-9]){8})(?![0-9])""")

    fun normalize(body: String): String = body.trim().replace(Regex("\\s+"), " ")

    /** A real payment SMS has a reference and an amount. OTPs, promos etc. do not. */
    fun isTransaction(body: String): Boolean = trxLoose.containsMatchIn(body) && (amountLoose.containsMatchIn(body) || amountAlt.containsMatchIn(body))

    fun parse(body: String): Parsed {
        val amountText = amountRe.find(body)?.groupValues?.get(1) ?: amountAltRe.find(body)?.groupValues?.get(1) ?: "0"
        val amount = amountText.replace(",", "").toDoubleOrNull() ?: 0.0
        val trx = (trxRe.find(body) ?: trxAltRe.find(body))?.groupValues?.get(1)?.uppercase() ?: ""
        val phone = phoneRe.find(body)?.groupValues?.get(1)?.replace(Regex("[\\s-]"), "") ?: ""
        val low = body.lowercase()
        val kind = when {
            low.contains("cash out") || low.contains("cashout") || low.contains("withdraw") -> "out"
            low.contains("you have sent") || low.contains("send money") || low.contains("sent tk") || low.contains("payment to") || low.contains("you have paid") || low.contains("bill") -> "out"
            low.contains("received") || low.contains("cash in") || low.contains("cash-in") || low.contains("credited") || low.contains("payment") -> "in"
            else -> "in"
        }
        return Parsed(amount, trx, phone, kind)
    }

    fun hash(serverAddress: String, body: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest((serverAddress.lowercase() + "|" + normalize(body)).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
