package com.conkeep.util

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun rememberNoRippleInteractionSource(): MutableInteractionSource =
    remember {
        object : MutableInteractionSource {
            override val interactions: Flow<Interaction> = emptyFlow()

            override suspend fun emit(interaction: Interaction) {}

            override fun tryEmit(interaction: Interaction) = true
        }
    }
