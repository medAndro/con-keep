package com.conkeep.ui.feature.setting

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.conkeep.BuildConfig
import com.conkeep.navigation.Route
import com.conkeep.navigation.TabDestination
import com.conkeep.ui.component.BottomNavigationBar
import com.conkeep.ui.component.ConKeepConfirmDialog
import com.conkeep.ui.component.SettingTopBar
import com.conkeep.ui.feature.setting.account.AccountInfo
import com.conkeep.ui.feature.setting.account.AccountProvider
import com.conkeep.ui.feature.setting.account.AccountSetting
import com.conkeep.ui.feature.setting.normal.NormalSetting
import com.conkeep.ui.feature.setting.notification.AlarmSettingDialog
import com.conkeep.ui.feature.setting.notification.CouponAlarmSetting
import com.conkeep.ui.feature.setting.notification.NotificationSetting
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
    moveDeleteAccountScreen: () -> Unit,
    viewModel: SettingViewModel = hiltViewModel(),
) {
    val couponAlarmSettings by viewModel.couponAlarmSettings.collectAsStateWithLifecycle(
        initialValue = emptySet(),
    )
    val accountInfo by viewModel.accountInfoFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as Activity
    val addCouponAlarmSettingSuccessMessage = "알림이 추가되었습니다."
    val addCouponAlarmSettingErrorMessage = "해당 알림은 이미 추가되어 있습니다."
    val removeCouponAlarmSettingSuccessMessage = "알림이 삭제되었습니다."
    val removeCouponAlarmSettingFailedMessage = "알림이 삭제되지 않았습니다."
    val logoutSettingMessage = "로그아웃 되었습니다."

    var showSettingBottomSheet by
        rememberSaveable(stateSaver = CouponAlarmSetting.Saver) {
            mutableStateOf(null)
        }
    var showNotificationSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showExactAlarmSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var pendingExactAlarmCheck by rememberSaveable { mutableStateOf(false) }
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var hasNotificationPermission by rememberSaveable {
        mutableStateOf(checkNotificationPermission(context))
    }
    var pendingPermissionCheckOnly by rememberSaveable { mutableStateOf(false) }

    fun checkExactAlarmAndShowSheet() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            if (alarmManager.canScheduleExactAlarms()) {
                if (pendingPermissionCheckOnly) return
                showSettingBottomSheet = CouponAlarmSetting(0, LocalTime(9, 0))
            } else {
                showExactAlarmSettingsDialog = true
            }
        } else {
            // Android 11 이하
            if (pendingPermissionCheckOnly) return
            showSettingBottomSheet = CouponAlarmSetting(0, LocalTime(9, 0))
        }
    }

    // 권한 요청을 위한 런처 정의
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            if (isGranted) {
                checkExactAlarmAndShowSheet()
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                ) {
                    showNotificationSettingsDialog = true
                } else {
                    showNotificationSettingsDialog = true
                    Toast.makeText(context, "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    // 권한 체크 및 실행 로직을 별도 함수로 분리
    fun checkPermissionAndShowSheet(isPermissionCheckOnly: Boolean = false) {
        pendingPermissionCheckOnly = isPermissionCheckOnly

        // 모든 안드로이드 버전 호환 알림 켜짐 여부 확인
        val areNotificationsEnabled =
            NotificationManagerCompat.from(context).areNotificationsEnabled()

        if (areNotificationsEnabled) {
            // 알림이 켜져 있으면, 정확한 알람(Exact Alarm) 권한 체크로 넘어감
            checkExactAlarmAndShowSheet()
        } else {
            // 알림이 꺼져 있는 경우 분기 처리
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13 이상: 아직 권한을 요청할 수 있는 상태인지 확인
                val permissionStatus =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )

                if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                    // 권한은 있으나 시스템 설정에서 꺼버린 경우
                    showNotificationSettingsDialog = true
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                ) {
                    // 사용자가 명시적으로 거부했던 경우 -> 설정 다이얼로그
                    showNotificationSettingsDialog = true
                } else {
                    // 아직 권한을 요청한 적이 없는 경우 -> 시스템 권한 팝업 띄우기
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                // Android 12 이하: 런타임 팝업이 없으므로 무조건 설정 앱으로 유도하는 다이얼로그 띄움
                showNotificationSettingsDialog = true
            }
        }
    }

    DisposableEffect(LocalLifecycleOwner.current) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    // 앱에 돌아올 때마다 권한 상태 갱신
                    hasNotificationPermission = checkNotificationPermission(context)

                    // 기존 ExactAlarm 체크 로직
                    if (pendingExactAlarmCheck) {
                        pendingExactAlarmCheck = false
                        if (pendingPermissionCheckOnly) {
                            pendingPermissionCheckOnly = false
                            return@LifecycleEventObserver
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val alarmManager = context.getSystemService(AlarmManager::class.java)
                            if (alarmManager.canScheduleExactAlarms()) {
                                showSettingBottomSheet = CouponAlarmSetting(0, LocalTime(9, 0))
                            }
                        } else {
                            showSettingBottomSheet = CouponAlarmSetting(0, LocalTime(9, 0))
                        }
                    }
                }
            }
        val lifecycle = (context as LifecycleOwner).lifecycle
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

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

                SettingEvent.LogoutSuccess -> {
                    Toast
                        .makeText(
                            context,
                            logoutSettingMessage,
                            Toast.LENGTH_SHORT,
                        ).show()
                }
            }
        }
    }
    SettingScreenContent(
        couponAlarmSettings = couponAlarmSettings,
        onClickNotice = { settingBackStack.add(Route.NoticeScreen) },
        onTabChange = onTabChange,
        addCouponAlarmSetting = viewModel::addCouponAlarmSetting,
        removeCouponAlarmSetting = viewModel::removeCouponAlarmSetting,
        checkPermissionAndShowSheet = { checkPermissionAndShowSheet() },
        checkPermissionOnly = { checkPermissionAndShowSheet(true) },
        isPermissionOverlyEnabled = hasNotificationPermission.not() && couponAlarmSettings.isNotEmpty(),
        updatePendingExactAlarmCheck = { pendingExactAlarmCheck = it },
        showNotificationSettingsDialog = showNotificationSettingsDialog,
        onDismissNotificationDialog = { showNotificationSettingsDialog = false },
        showExactAlarmSettingsDialog = showExactAlarmSettingsDialog,
        onDismissExactAlarmDialog = { showExactAlarmSettingsDialog = false },
        showSettingBottomSheet = showSettingBottomSheet,
        moveDeleteAccountScreen = moveDeleteAccountScreen,
        onUpdateBottomSheet = { showSettingBottomSheet = it },
        accountInfo = accountInfo,
        showLogoutDialog = showLogoutDialog,
        onShowLogoutDialog = { showLogoutDialog = true },
        onLogout = viewModel::logout,
        onDismissLogoutDialog = {
            showLogoutDialog = false
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreenContent(
    // 데이터 및 기본 콜백
    couponAlarmSettings: Set<CouponAlarmSetting>,
    onClickNotice: () -> Unit,
    onTabChange: (TabDestination) -> Unit,
    addCouponAlarmSetting: (CouponAlarmSetting) -> Unit,
    removeCouponAlarmSetting: (CouponAlarmSetting) -> Unit,
    // 권한 관련 액션
    checkPermissionAndShowSheet: () -> Unit,
    checkPermissionOnly: () -> Unit,
    isPermissionOverlyEnabled: Boolean,
    updatePendingExactAlarmCheck: (Boolean) -> Unit,
    // UI 상태 제어 (State & 이벤트를 쌍으로 전달)
    showNotificationSettingsDialog: Boolean,
    onDismissNotificationDialog: () -> Unit,
    showExactAlarmSettingsDialog: Boolean,
    onDismissExactAlarmDialog: () -> Unit,
    showSettingBottomSheet: CouponAlarmSetting?,
    onUpdateBottomSheet: (CouponAlarmSetting?) -> Unit,
    // 계정 설정 관련
    accountInfo: AccountInfo?,
    moveDeleteAccountScreen: () -> Unit,
    showLogoutDialog: Boolean,
    onShowLogoutDialog: () -> Unit,
    onLogout: () -> Unit,
    onDismissLogoutDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // POST_NOTIFICATIONS 거부 시 다이얼로그
    if (showNotificationSettingsDialog) {
        ConKeepConfirmDialog(
            title = "알림 권한 없음",
            description = "쿠폰 만료 알림을 표시하기 위해서\n설정에서 알림 권한 허용이 필요합니다.",
            confirmText = "설정하기",
            cancelText = "취소",
            onConfirm = {
                onDismissNotificationDialog()
                val intent =
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                context.startActivity(intent)
            },
            onDismiss = { onDismissNotificationDialog() },
            confirmTextColor = textPrimary,
            confirmBackgroundColor = brandPrimary,
        )
    }

    // 알람 및 리마인더 다이얼로그 canScheduleExactAlarms() false 시 (Android 12+)
    if (showExactAlarmSettingsDialog) {
        ConKeepConfirmDialog(
            title = "알람 및 리마인더 권한 없음",
            description = "정확한 쿠폰 만료 알림을 받기 위해서\n설정에서 알람 및 리마인더 권한 허용이 필요합니다.",
            confirmText = "설정하기",
            cancelText = "취소",
            onConfirm = {
                onDismissExactAlarmDialog()
                updatePendingExactAlarmCheck(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent =
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                    context.startActivity(intent)
                }
            },
            onDismiss = { onDismissExactAlarmDialog() },
            confirmTextColor = textPrimary,
            confirmBackgroundColor = brandPrimary,
        )
    }

    showSettingBottomSheet?.let { initAlarmSetting: CouponAlarmSetting ->
        AlarmSettingDialog(
            onConfirm = { selectedAlarm ->
                addCouponAlarmSetting(selectedAlarm)
                onUpdateBottomSheet(null)
            },
            initAlarmSetting = initAlarmSetting,
            onCancel = {
                onUpdateBottomSheet(null)
            },
        )
    }

    // 로그아웃 다이얼로그
    if (showLogoutDialog) {
        ConKeepConfirmDialog(
            title = "로그아웃 확인",
            description = "다시 로그인하면\n저장된 쿠폰을 불러올 수 있어요",
            confirmText = "로그아웃",
            cancelText = "취소",
            onConfirm = {
                onLogout()
                onDismissLogoutDialog()
            },
            onDismiss = { onDismissLogoutDialog() },
            confirmTextColor = textPrimary,
            confirmBackgroundColor = brandPrimary,
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
                    onOverlayClick = {
                        checkPermissionOnly()
                    },
                    isOverlayEnabled = isPermissionOverlyEnabled,
                )
                NormalSetting(onClickNotice)
                AccountSetting(
                    onShowLogoutDialog,
                    moveDeleteAccountScreen,
                    accountInfo = accountInfo,
                )
                Text(
                    "현재 버전 v${BuildConfig.VERSION_NAME}",
                    style = PretendardMedium12,
                    color = textSecondary,
                )
            }
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    // - Android 13 이상: POST_NOTIFICATIONS 권한 허용 여부 및 채널 차단 여부까지 종합 확인
    // - Android 12 이하: 시스템 설정에서 해당 앱의 알림을 수동으로 껐는지 확인
    val hasPostNotifications = NotificationManagerCompat.from(context).areNotificationsEnabled()

    // 정확한 알람(리마인더) 권한 체크 (Android 12 / API 31 이상)
    val hasExactAlarm =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 11 이하는 자동 허용됨
        }

    // 두 가지 권한을 모두 가지고 있어야 true 반환
    return hasPostNotifications && hasExactAlarm
}

@Preview(showBackground = true)
@Composable
private fun SettingScreenContentPreview() {
    ConKeepTheme(darkTheme = false) {
        SettingScreenContent(
            couponAlarmSettings = emptySet(),
            onClickNotice = {},
            onTabChange = {},
            addCouponAlarmSetting = {},
            removeCouponAlarmSetting = {},
            checkPermissionAndShowSheet = {},
            checkPermissionOnly = {},
            isPermissionOverlyEnabled = false,
            updatePendingExactAlarmCheck = {},
            showNotificationSettingsDialog = false,
            onDismissNotificationDialog = {},
            showExactAlarmSettingsDialog = false,
            onDismissExactAlarmDialog = {},
            showSettingBottomSheet = null,
            onUpdateBottomSheet = {},
            accountInfo =
                AccountInfo(
                    email = "conkeepconkeepconke@conkeep.com",
                    provider = AccountProvider.Google,
                ),
            moveDeleteAccountScreen = {},
            showLogoutDialog = false,
            onShowLogoutDialog = {},
            onLogout = {},
            onDismissLogoutDialog = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingScreenContentPermissionDialogPreview() {
    ConKeepTheme(darkTheme = false) {
        SettingScreenContent(
            couponAlarmSettings = emptySet(),
            onClickNotice = {},
            onTabChange = {},
            addCouponAlarmSetting = {},
            removeCouponAlarmSetting = {},
            checkPermissionAndShowSheet = {},
            checkPermissionOnly = {},
            isPermissionOverlyEnabled = true,
            updatePendingExactAlarmCheck = {},
            showNotificationSettingsDialog = true,
            onDismissNotificationDialog = {},
            showExactAlarmSettingsDialog = false,
            onDismissExactAlarmDialog = {},
            showSettingBottomSheet = null,
            onUpdateBottomSheet = {},
            accountInfo = null,
            moveDeleteAccountScreen = {},
            showLogoutDialog = false,
            onShowLogoutDialog = {},
            onLogout = {},
            onDismissLogoutDialog = {},
        )
    }
}
