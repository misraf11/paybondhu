@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.paybondhu.app.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybondhu.app.BuildConfig
import com.paybondhu.app.data.Lang
import com.paybondhu.app.data.Prefs
import com.paybondhu.app.data.T
import com.paybondhu.app.net.Api
import com.paybondhu.app.sync.Alarms
import com.paybondhu.app.sync.SyncEngine
import com.paybondhu.app.sync.SyncScheduler
import com.paybondhu.app.sync.SyncService
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    perms: PermState,
    onAllowSms: () -> Unit,
    onAllowNotif: () -> Unit,
    onAllowBattery: () -> Unit,
    onDisconnected: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ui by SyncEngine.state.collectAsState()
    var scanOn by remember { mutableStateOf(Prefs.scanInbox) }
    var importing by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val updateReady = Prefs.latestCode > BuildConfig.VERSION_CODE
    // reading ui keeps this screen fresh after a status refresh
    @Suppress("UNUSED_VARIABLE") val tick = ui.lastSyncAt

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text(T.tabSettings, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) }

        // Account
        item {
            Section(T.account) {
                InfoRow(T.email, Prefs.email)
                if (Prefs.deviceName.isNotEmpty()) InfoRow(T.device, Prefs.deviceName)
                InfoRow(T.deviceKey, Prefs.deviceKey.let { if (it.length > 10) it.take(4) + "••••••" + it.takeLast(4) else it })
                InfoRow(T.server, Prefs.server.removePrefix("https://").trimEnd('/'))
                if (Prefs.planExpire.isNotEmpty()) InfoRow(T.planUntil, Prefs.planExpire.take(10))
            }
        }

        // Sync
        item {
            Section(T.syncSection) {
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(T.autoScan, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(T.autoScanSub, fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(checked = scanOn, onCheckedChange = { scanOn = it; Prefs.scanInbox = it })
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        if (!perms.readSms) {
                            Toast.makeText(ctx, T.needSmsRead, Toast.LENGTH_SHORT).show()
                            onAllowSms()
                        } else {
                            importing = true
                            scope.launch {
                                val n = SyncEngine.scanInbox(ctx, 30)
                                Prefs.historyImported = true
                                SyncEngine.touch(ctx)
                                importing = false
                                Toast.makeText(ctx, T.imported(n), Toast.LENGTH_SHORT).show()
                                SyncEngine.run(ctx, "manual")
                            }
                        }
                    },
                    enabled = !importing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                ) {
                    if (importing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text(T.importNow)
                }
            }
        }

        // Keep running
        item {
            Section(T.keepAlive) {
                PermRow(T.permSms, T.permSmsSub, perms.smsOk, onAllowSms)
                PermRow(T.permNotif, T.permNotifSub, perms.notif, onAllowNotif)
                PermRow(T.permBattery, T.permBatterySub, perms.battery, onAllowBattery)
                Spacer(Modifier.height(6.dp))
                Text(T.keepAliveTip, fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { ctx.startActivity(Perms.appSettings(ctx)) }) { Text(T.openSettings) }
            }
        }

        // Language
        item {
            Section(T.language) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("English", selected = !Lang.bn, onClick = { Prefs.lang = "en"; SyncService.ensureChannel(ctx); SyncEngine.touch(ctx) })
                    Chip("বাংলা", selected = Lang.bn, onClick = { Prefs.lang = "bn"; SyncService.ensureChannel(ctx); SyncEngine.touch(ctx) })
                }
            }
        }

        // About
        item {
            Section(T.about) {
                InfoRow(T.version, BuildConfig.VERSION_NAME)
                Spacer(Modifier.height(6.dp))
                if (updateReady && Prefs.apkUrl.isNotEmpty()) {
                    Button(
                        onClick = {
                            try {
                                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Prefs.apkUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            } catch (_: Exception) {
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                    ) { Text(T.updateSub(Prefs.latestName)) }
                } else {
                    OutlinedButton(
                        onClick = {
                            checking = true
                            scope.launch {
                                SyncEngine.run(ctx, "manual", full = true)
                                checking = false
                                if (Prefs.latestCode <= BuildConfig.VERSION_CODE) {
                                    Toast.makeText(ctx, T.upToDate, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !checking,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                    ) {
                        if (checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text(T.checkUpdate)
                    }
                }
            }
        }

        // Disconnect
        item {
            OutlinedButton(
                onClick = { confirm = true },
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Pb.Red.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Pb.Red),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) { Text(T.disconnect, fontWeight = FontWeight.Medium) }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(T.disconnectAsk) },
            text = { Text(T.disconnectSub) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    scope.launch {
                        try {
                            Api.disconnect()
                        } catch (_: Exception) {
                        }
                        SyncService.stop(ctx)
                        SyncScheduler.cancelAll(ctx)
                        Alarms.cancel(ctx)
                        val keepEmail = Prefs.email
                        Prefs.logout()
                        Prefs.email = keepEmail
                        onDisconnected()
                    }
                }) { Text(T.yes, color = Pb.Red) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(T.cancel) } },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title, Modifier.padding(start = 4.dp))
        PbCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(110.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 2)
    }
}
