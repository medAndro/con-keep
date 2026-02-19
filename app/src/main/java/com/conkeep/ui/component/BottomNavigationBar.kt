package com.conkeep.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.conkeep.navigation.TabDestination
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.textDisabled
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.PretendardSemibold12
import com.conkeep.util.rememberNoRippleInteractionSource

@Composable
fun BottomNavigationBar(
    currentTab: TabDestination,
    onTabReselect: (TabDestination) -> Unit,
    onTabChange: (TabDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        NavigationBar(
            containerColor = bgSurface,
            windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        ) {
            TabDestination.items.forEach { tab ->
                val isSelected = currentTab == tab

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        when {
                            isSelected -> onTabReselect(tab)
                            else -> onTabChange(tab)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = tab.iconRes),
                            contentDescription = stringResource(id = tab.titleRes),
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(id = tab.titleRes),
                            style = PretendardSemibold12,
                        )
                    },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = textPrimary,
                            selectedTextColor = textPrimary,
                            unselectedIconColor = textDisabled,
                            unselectedTextColor = textDisabled,
                            indicatorColor = bgSurface,
                        ),
                    interactionSource = rememberNoRippleInteractionSource(),
                )
            }
        }
    }
}
