package com.conkeep.ui.feature.setting.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.conkeep.R
import com.conkeep.navigation.Route
import com.conkeep.navigation.TabDestination
import com.conkeep.ui.component.BottomNavigationBar
import com.conkeep.ui.component.EvenlyTextTopBar
import com.conkeep.ui.component.TopBarButtonConfig
import com.conkeep.ui.theme.ConKeepTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    settingBackStack: NavBackStack<NavKey>,
    onTabChange: (TabDestination) -> Unit,
) {
    Scaffold(
        topBar = {
            EvenlyTextTopBar(
                middleText = "회원탈퇴",
                leftButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = stringResource(R.string.topbar_back_description),
                            onClick = {
                                settingBackStack.removeLastOrNull()
                            },
                        ),
                    ),
            )
        },
        bottomBar = {
            BottomNavigationBar(
                currentTab = TabDestination.Setting,
                onTabChange = onTabChange,
                onTabReselect = {},
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .padding(top = 0.dp, bottom = 20.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
        }
    }
}

@Preview
@Composable
fun DeleteAccountScreenPreview() {
    ConKeepTheme {
        DeleteAccountScreen(
            settingBackStack = NavBackStack(Route.SettingScreen),
            onTabChange = {},
        )
    }
}
