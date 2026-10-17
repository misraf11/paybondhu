@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.paybondhu.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybondhu.app.BuildConfig
import com.paybondhu.app.data.DayStats
import com.paybondhu.app.data.Db
import com.paybondhu.app.data.Msg
import com.paybondhu.app.data.MsgState
import com.paybondhu.app.data.Prefs
import com.paybondhu.app.data.T
import com.paybondhu.app.sync.SyncEngine
import com.paybondhu.app.sync.SyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(
    padding: PaddingValues,
    perms: PermState,
    onAllowSms: () -> Unit,
    onAllowNotif: () -> Unit,
    onAllowBattery: () -> Unit,
    onViewAll: () -> Unit,
    onOpen: (Msg) -> Unit,
    onReconnect: () -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val ui by SyncEngine.state.collectAsState()
    val db = remember { Db.get(ctx) }
    val ver by db.version.collectAsState()

    var recent by remember { mutableStateOf<List<Msg>>(emptyList()) }
    var today by remember { mutableStateOf(DayStats(0, 0.0)) }
    var sent by remember { mutableStateOf(0) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(ver) {
        withContext(Dispatchers.IO) {
            val r = db.list(null, 0, "", 5)
            val t = db.since(Fmt.startOfToday())
            val s = db.count(MsgState.SYNCED)
            recent = r; today = t; sent = s
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    val action = ui.auth.isNotEmpty()
    val offline = !ui.online
    val title = when {
        action -> T.statusAction
        ui.running -> T.statusSyncing
        offline -> T.statusOffline
        else -> T.statusActive
    }
    val colors = when {
        action -> listOf(Pb.Red, Color(0xFFFF7A59))
        offline -> listOf(Color(0xFF475467), Color(0xFF667085))
        else -> listOf(Pb.BlueDeep, Pb.Blue, Pb.BlueLight)
    }
    val agoText = remember(ui.lastSyncAt, now) { Fmt.ago(ui.lastSyncAt) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 12.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(com.paybondhu.app.R.drawable.logo),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("PayBondhu", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Text(Prefs.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }

        // Hero
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(colors))
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PulseDot(Color.White, animate = !action && !offline)
                        Spacer(Modifier.width(10.dp))
                        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (offline && !action) T.offlineHint else T.lastSynced(agoText).let { if (ui.lastSyncAt <= 0) T.neverSynced else it },
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HeroStat(T.statToday, today.count.toString(), Modifier.weight(1f))
                        HeroStat(T.statPending, ui.pending.toString(), Modifier.weight(1f))
                        HeroStat(T.statSent, sent.toString(), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                SyncService.start(ctx)
                                SyncEngine.run(ctx, "manual", full = true)
                            }
                        },
                        enabled = !ui.running,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Pb.BlueDeep,
                            disabledContainerColor = Color.White.copy(alpha = 0.55f),
                            disabledContentColor = Pb.BlueDeep,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                    ) {
                        if (ui.running) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Pb.BlueDeep)
                        } else {
                            Icon(PbIcons.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(T.syncNow, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                }
            }
        }

        // Not authorised
        if (action) {
            item {
                AlertCard(
                    color = Pb.Red,
                    title = T.authTitle,
                    sub = when (ui.auth) {
                        "auth_expired" -> T.authExpired
                        "auth_other_device" -> T.authOther
                        else -> T.authInvalid
                    },
                    button = T.reconnect,
                    onClick = onReconnect,
                )
            }
        }

        // Update
        if (Prefs.latestCode > BuildConfig.VERSION_CODE && Prefs.apkUrl.isNotEmpty()) {
            item {
                AlertCard(
                    color = Pb.Blue,
                    title = T.updateTitle,
                    sub = T.updateSub(Prefs.latestName),
                    button = T.download,
                    onClick = {
                        try {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Prefs.apkUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } catch (_: Exception) {
                        }
                    },
                )
            }
        }

        // Setup checklist
        if (!perms.allOk) {
            item {
                PbCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(T.setupTitle, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(T.setupSub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        if (!perms.smsOk) PermRow(T.permSms, T.permSmsSub, false, onAllowSms)
                        if (!perms.notif) PermRow(T.permNotif, T.permNotifSub, false, onAllowNotif)
                        if (!perms.battery) PermRow(T.permBattery, T.permBatterySub, false, onAllowBattery)
                    }
                }
            }
        }

        // Received today
        item {
            PbCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(T.receivedToday, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(Fmt.money(today.amountIn), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Pill("${today.count}", Pb.Blue)
                }
            }
        }

        // Recent
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(T.recent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (recent.isNotEmpty()) TextButton(onClick = onViewAll) { Text(T.viewAll, fontSize = 13.sp) }
            }
        }
        item {
            PbCard(Modifier.fillMaxWidth()) {
                if (recent.isEmpty()) {
                    EmptyBox(T.emptyTitle, T.emptySub)
                } else {
                    Column {
                        recent.forEachIndexed { i, m ->
                            if (i > 0) Hairline(Modifier.padding(start = 70.dp))
                            TxnRow(m, onClick = { onOpen(m) }, showDate = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}

@Composable
fun AlertCard(color: Color, title: String, sub: String, button: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.11f))
            .padding(16.dp)
    ) {
        Text(title, color = color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(sub, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        ) { Text(button, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
    }
}

@Composable
fun PermRow(title: String, sub: String, ok: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (ok) {
            Pill(T.allowed, Pb.Green)
        } else {
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp),
            ) { Text(T.allow, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
        }
    }
}
