package com.conkeep.ui.feature.setting.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.brandSecondary
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardSemibold20

@Composable
fun SettingListSection(
    title: String,
    items: List<SettingItemData>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = title,
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
            items.forEach { item ->
                NormalSettingItem(
                    title = item.title,
                    onClick = item.onClick,
                )
            }
        }
    }
}

@Preview
@Composable
fun NormalSettingPreview() {
    ConKeepTheme {
        Surface(color = brandSecondary) {
            SettingListSection(
                "일반 설정",
                listOf(
                    SettingItemData("공지사항", onClick = {}),
                ),
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}
