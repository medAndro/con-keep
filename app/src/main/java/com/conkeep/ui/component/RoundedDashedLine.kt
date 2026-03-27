package com.conkeep.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.conkeep.ui.theme.ConKeepTheme

/**
 * 양 끝이 둥근(Round) 형태의 점선을 그리는 컴포저블입니다.
 * 주로 절취선 효과를 줄 때 사용합니다.
 *
 * @param color 점선의 색상
 * @param modifier 레이아웃 조정을 위한 Modifier
 * @param strokeWidth 선의 두께 (기본값 2.dp)
 * @param dashLength 실선 부분의 길이 (기본값 8.dp)
 * @param dashGap 실선 사이의 빈 공간 길이 (기본값 8.dp)
 */
@Composable
fun RoundedDashedLine(
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
    dashLength: Dp = 8.dp,
    dashGap: Dp = 8.dp,
) {
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(strokeWidth),
    ) {
        val path =
            Path().apply {
                moveTo(0f, size.height / 2)
                lineTo(size.width, size.height / 2)
            }

        val dashedPathEffect =
            PathEffect.dashPathEffect(
                floatArrayOf(dashLength.toPx(), dashGap.toPx()),
                0f,
            )

        drawPath(
            path = path,
            color = color,
            style =
                Stroke(
                    width = strokeWidth.toPx(),
                    pathEffect = dashedPathEffect,
                    cap = StrokeCap.Round,
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RoundedDashedLinePreview() {
    ConKeepTheme {
        Surface(color = Color.White) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 기본 스타일
                RoundedDashedLine(color = Color.LightGray)

                // 굵은 스타일
                RoundedDashedLine(
                    color = Color.Gray,
                    strokeWidth = 4.dp,
                    dashLength = 10.dp,
                    dashGap = 10.dp,
                )

                // 촘촘한 스타일
                RoundedDashedLine(
                    color = Color(0xFF6200EE),
                    strokeWidth = 2.dp,
                    dashLength = 4.dp,
                    dashGap = 4.dp,
                )
            }
        }
    }
}
