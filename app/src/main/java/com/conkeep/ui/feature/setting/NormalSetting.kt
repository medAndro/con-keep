package com.conkeep.ui.feature.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.R
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.brandSecondary
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardSemibold16
import com.conkeep.ui.theme.PretendardSemibold20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalSetting(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "일반 설정",
            style = PretendardSemibold20,
            color = textPrimary,
        )
        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .background(color = bgSurface, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            NormalSettingItem("설정1", onClick = {})
            NormalSettingItem("설정2", onClick = {})
            NormalSettingItem("설정3", onClick = {})
        }
    }
}

@Composable
fun NormalSettingItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(40.dp),
        onClick = onClick,
    ) {
        Box(
            modifier =
                Modifier
                    .wrapContentHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title,
                style = PretendardSemibold16,
                color = textPrimary,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right_small),
                contentDescription = "삭제",
                tint = textPrimary,
                modifier = Modifier.size(20.dp).align(Alignment.TopEnd),
            )
        }
    }
}

@Preview
@Composable
fun NormalSettingPreview() {
    ConKeepTheme {
        Surface(color = brandSecondary) {
            NormalSetting()
        }
    }
}
