package com.conkeep.ui.feature.setting.account

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.conkeep.R
import com.conkeep.navigation.TabDestination
import com.conkeep.ui.component.BottomNavigationBar
import com.conkeep.ui.component.ConKeepConfirmDialog
import com.conkeep.ui.component.EvenlyTextTopBar
import com.conkeep.ui.component.TopBarButtonConfig
import com.conkeep.ui.theme.ConKeepColors.badgeExpiringBg
import com.conkeep.ui.theme.ConKeepColors.brandSecondary
import com.conkeep.ui.theme.ConKeepColors.buttonPositiveBg
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.ConKeepColors.textSecondary
import com.conkeep.ui.theme.ConKeepColors.textWhite
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardBold18
import com.conkeep.ui.theme.PretendardBold24
import com.conkeep.ui.theme.PretendardSemibold16

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    settingBackStack: NavBackStack<NavKey>,
    onTabChange: (TabDestination) -> Unit,
    viewModel: DeleteAccountViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val showDeleteAccountDialog = rememberSaveable { mutableStateOf(false) }

    fun cancelDeleteAccount() {
        settingBackStack.removeLastOrNull()
        onTabChange(TabDestination.Coupon)
        Toast.makeText(context, "콘킾을 계속 사용해주셔서 감사합니다!", Toast.LENGTH_SHORT).show()
    }

    DeleteAccountScreenContents(
        isShowDeleteAccountDialog = showDeleteAccountDialog.value,
        setDeleteDialogShowStatus = { showDeleteAccountDialog.value = it },
        cancelDeleteAccount = { cancelDeleteAccount() },
        onTabChange = onTabChange,
        onBackClick = { settingBackStack.removeLastOrNull() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreenContents(
    isShowDeleteAccountDialog: Boolean,
    setDeleteDialogShowStatus: (Boolean) -> Unit,
    cancelDeleteAccount: () -> Unit,
    onTabChange: (TabDestination) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val placeholderPainter = painterResource(R.drawable.img_conkeep_byebye)
    if (isShowDeleteAccountDialog) {
        ConKeepConfirmDialog(
            title = "마지막 알림",
            description = "탈퇴하기 버튼을 누르면 탈퇴 처리됩니다.\n이 작업은 되돌릴 수 없습니다.",
            confirmText = "돌아가기",
            cancelText = "탈퇴하기",
            onConfirm = {
                // 탈퇴 취소 로직
                setDeleteDialogShowStatus(false)
                cancelDeleteAccount()
            },
            onDismiss = {
                // 닫기 로직
                setDeleteDialogShowStatus(false)
            },
            onCancel = {
                // 탈퇴 처리 로직
                setDeleteDialogShowStatus(false)
                Toast.makeText(context, "그동안 콘킾을 이용해주셔서 감사합니다.", Toast.LENGTH_SHORT).show()
            },
            confirmTextColor = textWhite,
            confirmBackgroundColor = buttonPositiveBg,
            cancelTextColor = textSecondary,
            cancelBackGroundColor = brandSecondary,
        )
    }
    Scaffold(
        topBar = {
            EvenlyTextTopBar(
                middleText = "회원탈퇴",
                leftButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = stringResource(R.string.topbar_back_description),
                            onClick = onBackClick,
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
                    .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "정말 탈퇴하시겠습니까?",
                    style = PretendardBold24,
                    color = textPrimary,
                )
                Text(
                    text = "쿠폰 정보를 포함한 회원님의 소중한 정보는\n탈퇴 즉시 파기되며 복구할 수 없습니다.",
                    style = PretendardSemibold16,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = placeholderPainter,
                    contentDescription = "탈퇴를 아쉬워하는 캐릭터",
                    modifier = Modifier.fillMaxWidth(0.9f),
                    contentScale = ContentScale.Fit,
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { cancelDeleteAccount() },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = buttonPositiveBg,
                            contentColor = textWhite,
                        ),
                ) {
                    Text(
                        text = "돌아가기",
                        style = PretendardBold18,
                    )
                }

                Button(
                    onClick = { setDeleteDialogShowStatus(true) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = badgeExpiringBg,
                            contentColor = textSecondary,
                        ),
                ) {
                    Text(
                        text = "그래도 탈퇴하기",
                        style = PretendardSemibold16,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun DeleteAccountScreenPreview() {
    ConKeepTheme {
        DeleteAccountScreenContents(
            isShowDeleteAccountDialog = false,
            setDeleteDialogShowStatus = {},
            cancelDeleteAccount = {},
            onTabChange = {},
            onBackClick = {},
        )
    }
}
