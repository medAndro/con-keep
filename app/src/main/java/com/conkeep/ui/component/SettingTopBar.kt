package com.conkeep.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.R
import com.conkeep.ui.theme.ConKeepColors.brandPrimary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardBold24

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingTopBar() {
    val cornEmojiPainter: Painter = painterResource(R.drawable.ic_corn_ms_emoji)
    Surface(
        color = brandPrimary,
        shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(24.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = cornEmojiPainter,
                        contentDescription = stringResource(R.string.topbar_conkeep_logo_description),
                        modifier = Modifier.size(45.dp),
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = PretendardBold24,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingTopBarPreview() {
    ConKeepTheme {
        Surface {
            SettingTopBar()
        }
    }
}
