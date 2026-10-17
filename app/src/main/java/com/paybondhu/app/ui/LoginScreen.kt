@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.paybondhu.app.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybondhu.app.R
import com.paybondhu.app.data.Lang
import com.paybondhu.app.data.Prefs
import com.paybondhu.app.data.T
import com.paybondhu.app.net.Api
import com.paybondhu.app.sync.SyncScheduler
import com.paybondhu.app.sync.SyncService
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(prefill: Pair<String, String>?, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf(Prefs.email) }
    var key by rememberSaveable { mutableStateOf("") }
    var server by rememberSaveable { mutableStateOf(Prefs.server) }
    var showServer by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(prefill) {
        if (prefill != null) {
            email = prefill.first
            key = prefill.second
        }
    }

    fun submit() {
        if (busy) return
        val e = email.trim()
        val k = key.trim()
        if (e.isEmpty() || k.isEmpty()) {
            error = T.errEmpty
            return
        }
        var s = server.trim()
        if (s.isEmpty()) s = Prefs.DEFAULT_SERVER
        if (!s.startsWith("http")) s = "https://$s"
        busy = true
        error = ""
        scope.launch {
            val id = Prefs.androidId(ctx)
            val r = Api.connect(s, e, k, id)
            busy = false
            when {
                r.ok -> {
                    Prefs.server = s
                    Prefs.email = e
                    Prefs.deviceKey = k
                    Prefs.deviceId = id
                    Prefs.deviceName = r.json?.optString("device_name") ?: ""
                    Prefs.authError = ""
                    Prefs.lastSyncAt = 0L
                    SyncScheduler.ensurePeriodic(ctx)
                    SyncService.start(ctx)
                    onDone()
                }
                r.network -> error = T.errNetwork
                r.code == "bad_response" -> error = T.errServer
                r.code == "auth_expired" -> error = T.errExpired
                r.code == "auth_other_device" -> error = T.errOther
                else -> error = T.errInvalid
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { Prefs.lang = if (Lang.bn) "en" else "bn" }) {
                Text(if (Lang.bn) "English" else "বাংলা", fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.size(84.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(20.dp))
        Text(T.loginTitle, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(
            T.loginSub,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(T.email) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text(T.deviceKey) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val t = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString()?.trim()
                    if (!t.isNullOrEmpty()) key = t
                }) { Text(T.paste, fontSize = 13.sp) }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (showServer) {
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = server,
                onValueChange = { server = it },
                label = { Text(T.serverUrl) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text(
                error,
                color = Pb.Red,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Pb.Red.copy(alpha = 0.10f))
                    .padding(12.dp),
            )
        }

        Spacer(Modifier.height(22.dp))
        Button(
            onClick = { submit() },
            enabled = !busy,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.size(10.dp))
                Text(T.connecting, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            } else {
                Text(T.connect, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        TextButton(onClick = { showServer = !showServer }) {
            Text(T.advanced, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
