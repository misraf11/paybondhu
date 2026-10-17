@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.paybondhu.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paybondhu.app.data.Db
import com.paybondhu.app.data.Msg
import com.paybondhu.app.data.MsgState
import com.paybondhu.app.data.Provider
import com.paybondhu.app.data.T
import com.paybondhu.app.sync.SyncEngine
import com.paybondhu.app.sync.SyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface HRow {
    data class Header(val label: String) : HRow
    data class Item(val m: Msg) : HRow
}

private fun rowKey(r: HRow): String = when (r) {
    is HRow.Header -> "h" + r.label
    is HRow.Item -> "m" + r.m.id
}

@Composable
fun HistoryScreen(padding: PaddingValues, onOpen: (Msg) -> Unit) {
    val ctx = LocalContext.current
    val db = remember { Db.get(ctx) }
    val ver by db.version.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(0) }
    var provider by rememberSaveable { mutableStateOf<String?>(null) }
    var limit by rememberSaveable { mutableStateOf(100) }
    var items by remember { mutableStateOf<List<Msg>>(emptyList()) }

    LaunchedEffect(ver, query, filter, provider, limit) {
        if (query.isNotEmpty()) delay(250)
        items = withContext(Dispatchers.IO) { db.list(provider, filter, query, limit) }
    }

    val rows = remember(items) {
        val out = ArrayList<HRow>()
        var last = ""
        for (m in items) {
            val label = Fmt.dayLabel(m.receivedAt)
            if (label != last) {
                out.add(HRow.Header(label)); last = label
            }
            out.add(HRow.Item(m))
        }
        out
    }

    val filters = listOf(T.fAll, T.fPending, T.fSent, T.fMatched, T.fFailed)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = padding.calculateTopPadding())
    ) {
        Text(
            T.tabHistory,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; limit = 100 },
            placeholder = { Text(T.searchHint, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, contentDescription = null) }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filters.size) { i ->
                Chip(filters[i], selected = filter == i, onClick = { filter = i; limit = 100 })
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Chip(T.fAll, selected = provider == null, onClick = { provider = null; limit = 100 }) }
            items(listOf(Provider.BKASH, Provider.NAGAD, Provider.ROCKET, Provider.UPAY, Provider.TAP)) { p ->
                Chip(p.label, selected = provider == p.name, onClick = { provider = p.name; limit = 100 })
            }
        }
        Spacer(Modifier.height(8.dp))

        if (rows.isEmpty()) {
            EmptyBox(if (query.isNotEmpty() || filter != 0 || provider != null) T.noResults else T.emptyTitle, if (query.isEmpty() && filter == 0 && provider == null) T.emptySub else "")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = padding.calculateBottomPadding() + 20.dp),
            ) {
                items(rows, key = { r -> rowKey(r) }) { r ->
                    when (r) {
                        is HRow.Header -> Text(
                            r.label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp, start = 4.dp),
                        )
                        is HRow.Item -> Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            PbCard(Modifier.fillMaxWidth()) { TxnRow(r.m, onClick = { onOpen(r.m) }) }
                        }
                    }
                }
                if (items.size >= limit) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            OutlinedButton(onClick = { limit += 100 }, shape = RoundedCornerShape(12.dp)) { Text(T.loadMore) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailSheet(m: Msg, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val (label, color, _) = statusOf(m)
    val incoming = m.kind != "out"
    val explain = when {
        m.used -> T.explainMatched
        m.state == MsgState.REJECTED -> T.explainFailedPrefix + if (m.error.isNotEmpty()) ": " + m.error else ""
        m.state == MsgState.PENDING -> T.explainPending + if (m.error.isNotEmpty()) " (" + m.error + ")" else ""
        m.state == MsgState.LOCAL -> T.explainSaved
        else -> T.explainSent
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProviderDot(m.providerEnum, 46)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.providerEnum?.label ?: m.address, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        (if (incoming) "+" else "−") + Fmt.money(m.amount),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (incoming) Pb.Green else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Pill(label, color)
            }
            Spacer(Modifier.height(6.dp))
            Text(explain, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 19.sp)
            Spacer(Modifier.height(14.dp))
            Hairline()
            if (m.trx.isNotEmpty()) DetailLine(T.dTrx, m.trx) { copy(ctx, m.trx) }
            if (m.phone.isNotEmpty()) DetailLine(T.dFrom, m.phone) { copy(ctx, m.phone) }
            DetailLine(T.dTime, Fmt.dateTime(m.receivedAt), null)
            Hairline()
            Spacer(Modifier.height(12.dp))
            Text(T.dMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                m.body,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { copy(ctx, m.body) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Icon(PbIcons.Copy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(T.copy)
                }
                if (m.state == MsgState.REJECTED || m.state == MsgState.PENDING) {
                    Button(
                        onClick = {
                            Db.get(ctx).retry(m.id)
                            scope.launch {
                                SyncService.start(ctx)
                                SyncEngine.run(ctx, "manual")
                            }
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(),
                        modifier = Modifier.weight(1f).height(46.dp),
                    ) { Text(T.retry) }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, onCopy: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(84.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (onCopy != null) {
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(PbIcons.Copy, contentDescription = T.copy, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun copy(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("PayBondhu", text))
    Toast.makeText(ctx, T.copied, Toast.LENGTH_SHORT).show()
}
