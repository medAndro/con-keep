package com.conkeep.ui.feature.setting.normal

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
fun NormalSetting(
    onClickNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingListSection(
        "일반 설정",
        listOf(
            SettingItemData("공지사항", onClick = onClickNotice),
        ),
    )
}

@Preview
@Composable
fun NormalSettingPreview() {
    ConKeepTheme {
        Surface(color = brandSecondary) {
            NormalSetting(
                onClickNotice = {},
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}
