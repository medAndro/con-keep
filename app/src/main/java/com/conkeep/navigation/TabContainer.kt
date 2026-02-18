package com.conkeep.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

/**
 * 탭 콘텐츠를 항상 렌더링하면서 Crossfade 애니메이션으로 전환
 */
@Composable
fun <T> TabContainer(
    currentTab: T,
    tabs: List<T>,
    modifier: Modifier = Modifier,
    animationDuration: Int = 300,
    content: @Composable (T) -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        tabs.forEach { tab ->
            val alpha by animateFloatAsState(
                targetValue = if (currentTab == tab) 1f else 0f,
                animationSpec =
                    tween(
                        durationMillis = animationDuration,
                        easing = FastOutSlowInEasing,
                    ),
                label = "tab_crossfade_${tab.hashCode()}",
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.alpha = alpha
                        }.zIndex(if (currentTab == tab) 1f else 0f),
            ) {
                content(tab)
            }
        }
    }
}
