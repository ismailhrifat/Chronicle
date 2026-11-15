package com.chronicle.expense.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

class PermissionState(val granted: Boolean, val request: () -> Unit)

/** Tracks a set of runtime permissions and re-checks when the screen resumes. */
@Composable
fun rememberPermissions(vararg permissions: String): PermissionState {
    val context = LocalContext.current
    fun check() = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    var granted by remember { mutableStateOf(check()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = check()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = check() }
    return PermissionState(granted) { launcher.launch(arrayOf(*permissions)) }
}

val SmsPermissions = arrayOf(Manifest.permission.RECEIVE_SMS)
val NotificationPermissions: Array<String> =
    if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
