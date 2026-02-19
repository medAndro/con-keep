package com.conkeep.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.R
import com.conkeep.ui.theme.ConKeepColors.brandPrimary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardBold24

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiddleTextTopBar(
    middleText: String,
    leftButtonConfig: TopBarButtonConfig? = null,
    rightButtonConfig: TopBarButtonConfig? = null,
) {
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
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    if (leftButtonConfig != null) {
                        TopBarButton(
                            imageVector = ImageVector.vectorResource(leftButtonConfig.iconResId),
                            contentDescription = leftButtonConfig.contentDescription,
                            onClick = leftButtonConfig.onClick,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = middleText,
                        style = PretendardBold24,
                    )
                }

                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    if (rightButtonConfig != null) {
                        TopBarButton(
                            imageVector = ImageVector.vectorResource(rightButtonConfig.iconResId),
                            contentDescription = rightButtonConfig.contentDescription,
                            onClick = rightButtonConfig.onClick,
                        )
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
fun MiddleTextTopBarPreview() {
    ConKeepTheme {
        Surface {
            MiddleTextTopBar(
                middleText = "프리뷰",
                leftButtonConfig =
                    TopBarButtonConfig(
                        iconResId = R.drawable.ic_back,
                        contentDescription = "뒤로가기",
                        onClick = {},
                    ),
                rightButtonConfig =
                    TopBarButtonConfig(
                        iconResId = R.drawable.ic_back,
                        contentDescription = "뒤로가기",
                        onClick = {},
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MiddleTextTopBarLeftNullPreview() {
    ConKeepTheme {
        Surface {
            MiddleTextTopBar(
                middleText = "프리뷰",
                rightButtonConfig =
                    TopBarButtonConfig(
                        iconResId = R.drawable.ic_back,
                        contentDescription = "뒤로가기",
                        onClick = {},
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MiddleTextTopBarRightNullPreview() {
    ConKeepTheme {
        Surface {
            MiddleTextTopBar(
                middleText = "프리뷰",
                leftButtonConfig =
                    TopBarButtonConfig(
                        iconResId = R.drawable.ic_back,
                        contentDescription = "뒤로가기",
                        onClick = {},
                    ),
            )
        }
    }
}
