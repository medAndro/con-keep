package com.conkeep.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun DisableHaptic(content: @Composable () -> Unit) {
    val emptyHapticFeedback =
        remember {
            object : HapticFeedback {
                override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { }
            }
        }

    CompositionLocalProvider(
        LocalHapticFeedback provides emptyHapticFeedback,
        content = content,
    )
}
