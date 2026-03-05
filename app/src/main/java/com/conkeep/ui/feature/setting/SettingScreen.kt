package com.conkeep.ui.feature.setting

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.conkeep.BuildConfig
import com.conkeep.navigation.TabDestination
import com.conkeep.ui.component.BottomNavigationBar
import com.conkeep.ui.component.SettingTopBar
import com.conkeep.ui.theme.ConKeepColors.textSecondary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardMedium12
import kotlinx.datetime.LocalTime
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    settingBackStack: NavBackStack<NavKey>,
    onTabChange: (TabDestination) -> Unit,
    viewModel: SettingViewModel = hiltViewModel(),
) {
    SettingScreenContent(
        onTabChange = onTabChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreenContent(
    onTabChange: (TabDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    val notifications = remember { mutableStateListOf<CouponAlarmSetting>() }

    Scaffold(
        topBar = {
            SettingTopBar()
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
                modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                        })
                    },
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                NotificationSetting(
                    {
                        // todo: 실제 알림 추가 코드로 교체 필요
                        notifications.add(
                            CouponAlarmSetting(
                                daysBefore = Random().nextInt(10),
                                targetTime = LocalTime(Random().nextInt(24), 0),
                            ),
                        )
                    },
                    couponAlarmSettings = notifications,
                )
                NormalSetting()
                Text(
                    "현재 버전 v${BuildConfig.VERSION_NAME}",
                    style = PretendardMedium12,
                    color = textSecondary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingScreenContentPreview() {
    ConKeepTheme(darkTheme = false) {
        SettingScreenContent(
            onTabChange = {},
        )
    }
}
