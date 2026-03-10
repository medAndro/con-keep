package com.conkeep.ui.feature.setting

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.conkeep.BuildConfig
import com.conkeep.navigation.TabDestination
import com.conkeep.ui.component.BottomNavigationBar
import com.conkeep.ui.component.ConKeepConfirmDialog
import com.conkeep.ui.component.SettingTopBar
import com.conkeep.ui.theme.ConKeepColors.brandPrimary
import com.conkeep.ui.theme.ConKeepColors.textPrimary
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
    val couponAlarmSettings by viewModel.couponAlarmSettings.collectAsStateWithLifecycle(
        initialValue = emptySet(),
    )
    val context = LocalContext.current
    val addCouponAlarmSettingSuccessMessage = "알림이 추가되었습니다."
    val addCouponAlarmSettingErrorMessage = "해당 알림은 이미 추가되어 있습니다."

    val removeCouponAlarmSettingSuccessMessage = "알림이 삭제되었습니다."
    val removeCouponAlarmSettingFailedMessage = "알림이 삭제되지 않았습니다."

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { toastMessage ->
            when (toastMessage) {
                SettingEvent.AddCouponAlarmSettingSuccess ->
                    Toast
                        .makeText(
                            context,
                            addCouponAlarmSettingSuccessMessage,
                            Toast.LENGTH_SHORT,
                        ).show()

                SettingEvent.DuplicatedCouponAlarmSetting ->
                    Toast
                        .makeText(
                            context,
                            addCouponAlarmSettingErrorMessage,
                            Toast.LENGTH_SHORT,
                        ).show()

                SettingEvent.RemoveCouponAlarmSettingFailed ->
                    Toast
                        .makeText(
                            context,
                            removeCouponAlarmSettingFailedMessage,
                            Toast.LENGTH_SHORT,
                        ).show()

                SettingEvent.RemoveCouponAlarmSettingSuccess ->
                    Toast
                        .makeText(
                            context,
                            removeCouponAlarmSettingSuccessMessage,
                            Toast.LENGTH_SHORT,
                        ).show()
            }
        }
    }
    SettingScreenContent(
        onTabChange = onTabChange,
        couponAlarmSettings = couponAlarmSettings,
        addCouponAlarmSetting = viewModel::addCouponAlarmSetting,
        removeCouponAlarmSetting = viewModel::removeCouponAlarmSetting,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreenContent(
    onTabChange: (TabDestination) -> Unit,
    addCouponAlarmSetting: (CouponAlarmSetting) -> Unit,
    removeCouponAlarmSetting: (CouponAlarmSetting) -> Unit,
    modifier: Modifier = Modifier,
    couponAlarmSettings: Set<CouponAlarmSetting> = emptySet(),
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context as Activity
    val showSettingBottomSheet = rememberSaveable { mutableStateOf<CouponAlarmSetting?>(null) }
    val showSettingsDialog = rememberSaveable { mutableStateOf(false) }

    // 권한 요청을 위한 런처 정의
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                // 권한 허용 시 바로 바텀시트 띄우기
                showSettingBottomSheet.value = CouponAlarmSetting(0, LocalTime(9, 0))
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                    )
                ) {
                    showSettingsDialog.value = true
                } else {
                    Toast.makeText(context, "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // 2. 권한 체크 및 실행 로직을 별도 함수로 분리
    val checkPermissionAndShowSheet = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus =
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                )

            if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                // 이미 권한이 있음
                showSettingBottomSheet.value = CouponAlarmSetting(0, LocalTime(9, 0))
            } else {
                // 권한 요청 실행
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Android 13 미만은 권한 체크 없이 바로 실행
            showSettingBottomSheet.value = CouponAlarmSetting(0, LocalTime(9, 0))
        }
    }

    if (showSettingsDialog.value) {
        ConKeepConfirmDialog(
            title = "알림 권한 없음",
            description = "쿠폰 만료 알림을 받기 위해서\n설정에서 알림 권한 허용이 필요합니다.",
            confirmText = "설정하기",
            cancelText = "취소",
            onConfirm = {
                showSettingsDialog.value = false
                // 시스템 설정 페이지로 이동
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                context.startActivity(intent)
            },
            onDismiss = { showSettingsDialog.value = false },
            confirmTextColor = textPrimary,
            confirmBackgroundColor = brandPrimary,
        )
    }

    showSettingBottomSheet.value?.let { initAlarmSetting: CouponAlarmSetting ->
        AlarmSettingBottomSheet(
            onDismiss = {
                showSettingBottomSheet.value = null
            },
            onConfirm = { selectedAlarm ->
                addCouponAlarmSetting(selectedAlarm)
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
                        checkPermissionAndShowSheet()
                    },
                    {
                        removeCouponAlarmSetting(it)
                    },
                    couponAlarmSettings = couponAlarmSettings,
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
            addCouponAlarmSetting = {},
            removeCouponAlarmSetting = {},
        )
    }
}
