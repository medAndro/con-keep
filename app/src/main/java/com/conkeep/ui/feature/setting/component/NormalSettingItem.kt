package com.conkeep.ui.feature.setting.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.conkeep.R
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.PretendardSemibold16

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
                modifier =
                    Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd),
            )
        }
    }
}

data class SettingItemData(
    val title: String,
    val onClick: () -> Unit,
)
