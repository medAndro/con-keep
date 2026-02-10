package com.conkeep.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.R
import com.conkeep.ui.theme.ConKeepColors.brandPrimary
import com.conkeep.ui.theme.ConKeepColors.brandSecondary
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardBold24

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onClickSearchBarToggle: () -> Unit,
    onClickAdd: () -> Unit,
) {
    val cornEmojiPainter: Painter = painterResource(R.drawable.ic_corn_ms_emoji)
    val searchImageVector: ImageVector = ImageVector.vectorResource(id = R.drawable.ic_search)
    val plusImageVector: ImageVector = ImageVector.vectorResource(id = R.drawable.ic_plus)

    Surface(
        color = brandPrimary,
        shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 0.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = cornEmojiPainter,
                        contentDescription = stringResource(R.string.topbar_conkeep_logo_description),
                        modifier = Modifier.size(42.dp),
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = PretendardBold24,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }

                Row {
                    // 오른쪽: 검색 토글 버튼
                    Surface(
                        onClick = onClickSearchBarToggle,
                        color = brandSecondary,
                        shape = CircleShape,
                        modifier = Modifier.size(45.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = searchImageVector,
                                contentDescription = stringResource(R.string.topbar_search_toggle_description),
                                tint = textPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }

                    Spacer(Modifier.padding(end = 12.dp))

                    // 오른쪽: 추가 버튼
                    Surface(
                        onClick = onClickAdd,
                        color = brandSecondary,
                        shape = CircleShape,
                        modifier = Modifier.size(45.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = plusImageVector,
                                contentDescription = stringResource(R.string.topbar_add_coupon_description),
                                tint = textPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }

            // 하단 추가 여백
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopBarPreview() {
    ConKeepTheme {
        Surface {
            TopBar(
                onClickSearchBarToggle = {},
                onClickAdd = {},
            )
        }
    }
}
