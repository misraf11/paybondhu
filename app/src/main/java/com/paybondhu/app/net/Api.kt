package com.paybondhu.app.net

import com.paybondhu.app.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * @property network true when the server could not be reached at all (offline, DNS, timeout).
 * @property status  server "status" field (1 = ok), -1 when there was no usable answer.
 * @property code    server "code" field: ok, duplicate, invalid, rate_limited, busy, auth_*.
 */
data class ApiResult(
    val network: Boolean,
    val http: Int,
    val status: Int,
    val code: String,
    val message: String,
    val json: JSONObject?,
) {
    val ok: Boolean get() = !network && status == 1
    val auth: Boolean get() = code.startsWith("auth_")
}

object Api {
    private fun form(params: Map<String, String>): String =
        params.entries.joinToString("&") { URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8") }

    private fun credentials(extra: Map<String, String> = emptyMap()): Map<String, String> =
        mapOf(
            "user_email" to Prefs.email,
            "device_key" to Prefs.deviceKey,
            "device_ip" to Prefs.deviceId,
        ) + extra

    private suspend fun post(base: String, path: String, params: Map<String, String>): ApiResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(base.trimEnd('/') + "/api/" + path)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15000
                readTimeout = 25000
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "PayBondhu-Android/2")
            }
            conn.outputStream.use { it.write(form(params).toByteArray(Charsets.UTF_8)) }
            val http = conn.responseCode
            val stream = if (http in 200..399) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            parse(http, text)
        } catch (e: IOException) {
            ApiResult(true, 0, -1, "network", e.message ?: "Network error", null)
        } catch (e: Exception) {
            ApiResult(true, 0, -1, "network", e.message ?: "Error", null)
        } finally {
            conn?.disconnect()
        }
    }

    private fun parse(http: Int, text: String): ApiResult {
        // PHP notices can precede the JSON, so take the outermost {...}.
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) {
            return ApiResult(false, http, -1, "bad_response", if (http >= 500) "Server error ($http)" else "Unexpected server response", null)
        }
        return try {
            val j = JSONObject(text.substring(start, end + 1))
            val status = j.optInt("status", -1)
            var code = j.optString("code", "")
            if (code.isEmpty()) {
                code = when (status) {
                    1 -> "ok"
                    2 -> "auth_expired"
                    3 -> "auth_other_device"
                    else -> "failed"
                }
            }
            ApiResult(false, http, status, code, j.optString("message", ""), j)
        } catch (e: Exception) {
            ApiResult(false, http, -1, "bad_response", "Unexpected server response", null)
        }
    }

    /** Login: used with explicit values before anything is stored. */
    suspend fun connect(server: String, email: String, key: String, deviceId: String): ApiResult =
        post(server, "device-connect", mapOf("user_email" to email, "device_key" to key, "device_ip" to deviceId))

    /** Heartbeat + queue summary (+ latest stored SMS when [recent]). */
    suspend fun status(recent: Boolean): ApiResult =
        post(Prefs.server, "device-status", credentials(if (recent) mapOf("recent" to "1") else emptyMap()))

    suspend fun send(address: String, body: String): ApiResult =
        post(Prefs.server, "add-data", credentials(mapOf("address" to address, "message" to body)))

    suspend fun disconnect(): ApiResult = post(Prefs.server, "device-disconnect", credentials())
}
