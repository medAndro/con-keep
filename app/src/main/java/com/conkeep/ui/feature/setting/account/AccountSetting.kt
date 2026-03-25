package com.conkeep.ui.feature.setting.account

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.ui.feature.setting.component.SettingItemData
import com.conkeep.ui.feature.setting.component.SettingListSection
import com.conkeep.ui.theme.ConKeepColors.brandSecondary
import com.conkeep.ui.theme.ConKeepTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSetting(
    onClickLogout: () -> Unit,
    onClickWithdrawal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingListSection(
        "계정 설정",
        listOf(
            SettingItemData("로그아웃", onClick = onClickLogout),
            SettingItemData("회원탈퇴", onClick = onClickWithdrawal),
        ),
    )
}

@Preview
@Composable
fun NormalSettingPreview() {
    ConKeepTheme {
        Surface(color = brandSecondary) {
            AccountSetting(
                onClickLogout = {},
                onClickWithdrawal = {},
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}
