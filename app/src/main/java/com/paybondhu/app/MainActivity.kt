package com.paybondhu.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.paybondhu.app.data.Prefs
import com.paybondhu.app.ui.AppRoot
import com.paybondhu.app.ui.PayBondhuTheme

class MainActivity : ComponentActivity() {
    private val deepLink = mutableStateOf<Pair<String, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)
        enableEdgeToEdge()
        handle(intent)
        setContent {
            PayBondhuTheme {
                AppRoot(deepLink.value)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    /** paybondhu://connect?email=...&key=... pre-fills the login form. */
    private fun handle(i: Intent?) {
        val d = i?.data ?: return
        if (d.scheme == "paybondhu") {
            val email = d.getQueryParameter("email")
            val key = d.getQueryParameter("key")
            if (!email.isNullOrBlank() && !key.isNullOrBlank()) deepLink.value = email to key
        }
    }
}
