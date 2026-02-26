package com.conkeep.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
fun EvenlyTextTopBar(
    middleText: String,
    leftButtonConfigs: List<TopBarButtonConfig> = emptyList(),
    rightButtonConfigs: List<TopBarButtonConfig> = emptyList(),
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
                    .padding(24.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                when {
                    leftButtonConfigs.isEmpty() -> {
                        Box(modifier = Modifier.size(45.dp))
                    }
                    else -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            leftButtonConfigs.forEach { leftButtonConfig ->
                                TopBarButton(
                                    imageVector = ImageVector.vectorResource(leftButtonConfig.iconResId),
                                    contentDescription = leftButtonConfig.contentDescription,
                                    onClick = leftButtonConfig.onClick,
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = middleText,
                        style = PretendardBold24,
                    )
                }

                when {
                    rightButtonConfigs.isEmpty() -> {
                        Box(modifier = Modifier.size(45.dp))
                    }
                    else -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            rightButtonConfigs.forEach { rightButtonConfig ->
                                TopBarButton(
                                    imageVector = ImageVector.vectorResource(rightButtonConfig.iconResId),
                                    contentDescription = rightButtonConfig.contentDescription,
                                    onClick = rightButtonConfig.onClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EvenlyTextTopBarPreview() {
    ConKeepTheme {
        Surface {
            EvenlyTextTopBar(
                middleText = "프리뷰",
                leftButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                    ),
                rightButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EvenlyTextTopBarRightDoublePreview() {
    ConKeepTheme {
        Surface {
            EvenlyTextTopBar(
                middleText = "프리뷰",
                leftButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                    ),
                rightButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EvenlyTextTopBarLeftDoublePreview() {
    ConKeepTheme {
        Surface {
            EvenlyTextTopBar(
                middleText = "프리뷰",
                leftButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                    ),
                rightButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EvenlyTextTopBarBothDoublePreview() {
    ConKeepTheme {
        Surface {
            EvenlyTextTopBar(
                middleText = "프리뷰",
                leftButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                    ),
                rightButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EvenlyTextTopBarLeftNullPreview() {
    ConKeepTheme {
        Surface {
            EvenlyTextTopBar(
                middleText = "프리뷰",
                rightButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EvenlyTextTopBarRightNullPreview() {
    ConKeepTheme {
        Surface {
            EvenlyTextTopBar(
                middleText = "프리뷰",
                leftButtonConfigs =
                    listOf(
                        TopBarButtonConfig(
                            iconResId = R.drawable.ic_back,
                            contentDescription = "뒤로가기",
                            onClick = {},
                        ),
                    ),
            )
        }
    }
}
