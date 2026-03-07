package com.conkeep.ui.feature.setting

import android.widget.Toast
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    val notifications = rememberSaveable { mutableStateSetOf<CouponAlarmSetting>() }

    val showSettingBottomSheet = rememberSaveable { mutableStateOf<CouponAlarmSetting?>(null) }

    showSettingBottomSheet.value?.let { initAlarmSetting: CouponAlarmSetting ->
        AlarmSettingBottomSheet(
            onDismiss = {
                showSettingBottomSheet.value = null
            },
            onConfirm = { selectedAlarm ->
                val addResult =
                    notifications.add(
                        selectedAlarm,
                    )
                when (addResult) {
                    true -> {
                        Toast.makeText(context, "알림이 추가되었습니다.", Toast.LENGTH_SHORT).show()
                    }

                    false -> {
                        Toast.makeText(context, "해당 알림은 이미 추가되어 있습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                showSettingBottomSheet.value = null
            },
            initAlarmSetting = initAlarmSetting,
            onCancel = {
                showSettingBottomSheet.value = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        )
    }

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
                        showSettingBottomSheet.value = CouponAlarmSetting(0, LocalTime(9, 0))
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
