package com.conkeep.ui.component

import androidx.annotation.DrawableRes

data class TopBarButtonConfig(
    @param:DrawableRes val iconResId: Int,
    val contentDescription: String,
    val onClick: () -> Unit,
)
