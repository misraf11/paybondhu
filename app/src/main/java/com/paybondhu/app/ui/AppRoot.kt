@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.paybondhu.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.paybondhu.app.data.Msg
import com.paybondhu.app.data.Prefs
import com.paybondhu.app.data.T
import com.paybondhu.app.sync.SyncEngine
import com.paybondhu.app.sync.SyncService
import kotlinx.coroutines.launch

@Composable
fun AppRoot(prefill: Pair<String, String>?) {
    var loggedIn by remember { mutableStateOf(Prefs.loggedIn) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!loggedIn) {
            LoginScreen(prefill) { loggedIn = true }
        } else {
            MainShell(onLoggedOut = { loggedIn = false })
        }
    }
}

@Composable
private fun MainShell(onLoggedOut: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var perms by remember { mutableStateOf(Perms.read(ctx)) }
    var detail by remember { mutableStateOf<Msg?>(null) }
    var smsTries by remember { mutableIntStateOf(0) }

    val runtimeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        perms = Perms.read(ctx)
        scope.launch {
            SyncService.start(ctx)
            SyncEngine.run(ctx, "manual")
        }
    }
    val batteryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        perms = Perms.read(ctx)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        perms = Perms.read(ctx)
        scope.launch {
            SyncService.start(ctx)
            SyncEngine.run(ctx, "open")
        }
    }
    LaunchedEffect(Unit) {
        if (!Prefs.permsAsked) {
            Prefs.permsAsked = true
            runtimeLauncher.launch(Perms.runtimeList())
        }
    }
    BackHandler(enabled = tab != 0) { tab = 0 }

    val allowSms = {
        if (smsTries >= 1) {
            ctx.startActivity(Perms.appSettings(ctx))
        } else {
            smsTries++
            runtimeLauncher.launch(Perms.runtimeList())
        }
    }
    val allowNotif = {
        if (android.os.Build.VERSION.SDK_INT >= 33 && smsTries < 1) {
            smsTries++
            runtimeLauncher.launch(Perms.runtimeList())
        } else {
            ctx.startActivity(Perms.appSettings(ctx))
        }
    }
    val allowBattery = {
        try {
            batteryLauncher.launch(Perms.batteryIntent(ctx))
        } catch (_: Exception) {
            ctx.startActivity(Perms.appSettings(ctx))
        }
    }
    val loggedOut = {
        onLoggedOut()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text(T.tabHome) },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(PbIcons.History, contentDescription = null) },
                    label = { Text(T.tabHistory) },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(T.tabSettings) },
                )
            }
        },
    ) { padding ->
        when (tab) {
            0 -> HomeScreen(
                padding = padding,
                perms = perms,
                onAllowSms = allowSms,
                onAllowNotif = allowNotif,
                onAllowBattery = allowBattery,
                onViewAll = { tab = 1 },
                onOpen = { detail = it },
                onReconnect = {
                    SyncService.stop(ctx)
                    Prefs.deviceKey = ""
                    Prefs.authError = ""
                    loggedOut()
                },
            )
            1 -> HistoryScreen(padding = padding, onOpen = { detail = it })
            else -> SettingsScreen(
                padding = padding,
                perms = perms,
                onAllowSms = allowSms,
                onAllowNotif = allowNotif,
                onAllowBattery = allowBattery,
                onDisconnected = loggedOut,
            )
        }
    }

    detail?.let { DetailSheet(it) { detail = null } }
}
