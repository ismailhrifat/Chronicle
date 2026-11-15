package com.chronicle.expense

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chronicle.expense.notify.Notifier
import com.chronicle.expense.ui.auth.SignInScreen
import com.chronicle.expense.ui.nav.MainScaffold
import com.chronicle.expense.ui.theme.ChronicleTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            val theme by vm.themeMode.collectAsStateWithLifecycle()
            val signedIn by vm.signedIn.collectAsStateWithLifecycle()
            ChronicleTheme(theme) {
                if (signedIn) MainScaffold(vm) else SignInScreen(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val debuggable = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (debuggable && intent?.getBooleanExtra("demo", false) == true) vm.enableDemo()
        intent?.getStringExtra(Notifier.EXTRA_EDIT_TX)?.let(vm::openTransaction)
    }
}
