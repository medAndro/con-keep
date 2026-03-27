package com.conkeep.ui.feature.coupon.common

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

@Composable
fun rememberStoragePermissionAction(): (action: () -> Unit) -> Unit {
    val context = LocalContext.current
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                pendingAction?.invoke()
                pendingAction = null
            }
        }

    return { action ->
        val hasPermission =
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        if (hasPermission) {
            action()
        } else {
            pendingAction = action
            launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}
