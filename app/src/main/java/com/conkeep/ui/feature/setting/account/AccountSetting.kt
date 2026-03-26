package com.conkeep.ui.feature.setting.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.R
import com.conkeep.ui.feature.setting.component.NormalSettingItem
import com.conkeep.ui.theme.ColorPalette.NeutralWhite
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.brandSecondary
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardSemibold18
import com.conkeep.ui.theme.PretendardSemibold20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSetting(
    onClickLogout: () -> Unit,
    onClickDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    accountInfo: AccountInfo? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "계정 설정",
            style = PretendardSemibold20,
            color = textPrimary,
        )

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(color = bgSurface, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (accountInfo != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .background(color = NeutralWhite, shape = CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_g_logo),
                                contentDescription = "구글 로고",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = accountInfo.email,
                            style = PretendardSemibold18,
                        )
                    }
                }
            }
            NormalSettingItem(
                title = "로그아웃",
                onClick = onClickLogout,
            )
            NormalSettingItem(
                title = "회원탈퇴",
                onClick = onClickDeleteAccount,
            )
        }
    }
}

data class AccountInfo(
    val email: String,
    val provider: AccountProvider,
)

sealed interface AccountProvider {
    data object Google : AccountProvider
}

@Preview
@Composable
fun AccountSettingPreview() {
    ConKeepTheme {
        Surface(color = brandSecondary) {
            AccountSetting(
                onClickLogout = {},
                onClickDeleteAccount = {},
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Preview
@Composable
fun GoogleAccountSettingPreview() {
    ConKeepTheme {
        Surface(color = brandSecondary) {
            AccountSetting(
                onClickLogout = {},
                onClickDeleteAccount = {},
                modifier = Modifier.padding(20.dp),
                accountInfo =
                    AccountInfo(
                        email = "conkeepconkeepconkeep@gmail.com",
                        provider = AccountProvider.Google,
                    ),
            )
        }
    }
}
