package com.conkeep.ui.feature.setting

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.darkokoa.datetimewheelpicker.core.SelectorProperties
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import dev.darkokoa.datetimewheelpicker.core.WheelTextPicker
import kotlinx.datetime.LocalTime
import kotlin.math.abs

@Composable
fun CouponAlarmWheelPicker(
    modifier: Modifier = Modifier,
    initialSetting: CouponAlarmSetting = CouponAlarmSetting(0, LocalTime(9, 0)),
    maxDaysBefore: Int = 30, // 최대 며칠 전까지 보여줄지 설정
    size: DpSize = DpSize(256.dp, 128.dp),
    rowCount: Int = 3,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    textColor: Color = LocalContentColor.current,
    selectorProperties: SelectorProperties = WheelPickerDefaults.selectorProperties(),
    onAlarmSettingChanged: (CouponAlarmSetting) -> Unit,
) {
    // 1. 데이터 리스트 생성
    val daysOptions =
        remember(maxDaysBefore) {
            (0..maxDaysBefore).map { if (it == 0) "당일" else "${it}일 전" }
        }
    val amPmOptions = listOf("오전", "오후")
    val hourOptions =
        remember {
            listOf("12") + (1..11).map { it.toString() }
        }

    // 2. 초기 상태 설정
    var currentDaysBefore by remember { mutableStateOf(initialSetting.daysBefore) }
    val (initialAmPm, initialHour12) = initialSetting.targetTime.toAmPmAndHour12()
    var currentAmPm by remember { mutableStateOf(initialAmPm) }
    var currentHour12 by remember { mutableStateOf(initialHour12) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 선택 영역 하이라이트 (Surface)
        if (selectorProperties.enabled().value) {
            Surface(
                modifier = Modifier.size(size.width, size.height / rowCount),
                shape = selectorProperties.shape().value,
                color = selectorProperties.color().value,
                border = selectorProperties.border().value,
            ) {}
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(size.width),
        ) {
            // [열 1] 날짜 변위 (당일, 1일 전...)
            WheelTextPicker(
                modifier = Modifier.weight(1.2f),
                size = DpSize(Dp.Infinity, size.height),
                texts = daysOptions,
                rowCount = rowCount,
                style = textStyle,
                color = textColor,
                startIndex = currentDaysBefore,
                onScrollFinished = { index ->
                    currentDaysBefore = index
                    onAlarmSettingChanged(
                        CouponAlarmSetting(
                            currentDaysBefore,
                            createLocalTime(currentAmPm, currentHour12),
                        ),
                    )
                    return@WheelTextPicker index
                },
            )

            // [열 2] 오전/오후
            WheelTextPicker(
                modifier = Modifier.weight(1f),
                size = DpSize(Dp.Infinity, size.height),
                texts = amPmOptions,
                rowCount = rowCount,
                style = textStyle,
                color = textColor,
                startIndex = amPmOptions.indexOf(currentAmPm),
                onScrollFinished = { index ->
                    currentAmPm = amPmOptions[index]
                    onAlarmSettingChanged(
                        CouponAlarmSetting(
                            currentDaysBefore,
                            createLocalTime(currentAmPm, currentHour12),
                        ),
                    )
                    return@WheelTextPicker index
                },
            )

            // [열 3] 시간 + "시" 접미사
            // WheelTextPicker -> WheelTextPickerWithSuffix 로 변경
            WheelTextPickerWithSuffix(
                modifier = Modifier.weight(1f),
                size = DpSize(Dp.Infinity, size.height),
                texts = hourOptions,
                suffix = "시", // 접미사 추가
                textToSuffixSpacing = 4.dp, // 숫자와 "시" 사이의 간격
                rowCount = rowCount,
                style = textStyle,
                color = textColor,
                selectorProperties = WheelPickerDefaults.selectorProperties(enabled = false), // Row 전체에 이미 Surface가 있으므로 false
                startIndex = hourOptions.indexOf(currentHour12.toString()),
                onScrollFinished = { index ->
                    currentHour12 = hourOptions[index].toInt()
                    onAlarmSettingChanged(
                        CouponAlarmSetting(
                            currentDaysBefore,
                            createLocalTime(currentAmPm, currentHour12),
                        ),
                    )
                    return@WheelTextPickerWithSuffix index
                },
            )
        }
    }
}

// LocalTime을 오전/오후 및 12시간제 숫자로 변환
fun LocalTime.toAmPmAndHour12(): Pair<String, Int> {
    val amPm = if (hour < 12) "오전" else "오후"
    val hour12 =
        when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
    return amPm to hour12
}

// 오전/오후 및 12시간제 숫자를 LocalTime으로 변환
fun createLocalTime(
    amPm: String,
    hour12: Int,
): LocalTime {
    val hour24 =
        if (amPm == "오후") {
            if (hour12 == 12) 12 else hour12 + 12
        } else {
            if (hour12 == 12) 0 else hour12
        }
    return LocalTime(hour24, 0)
}

@Composable
internal fun WheelTextPickerWithSuffix(
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    size: DpSize = DpSize(128.dp, 128.dp),
    texts: List<String>,
    rowCount: Int,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = LocalContentColor.current,
    suffix: String = "",
    suffixStyle: TextStyle = style,
    suffixColor: Color = color,
    textToSuffixSpacing: Dp = 8.dp,
    selectorProperties: SelectorProperties = WheelPickerDefaults.selectorProperties(),
    onScrollFinished: (snappedIndex: Int) -> Int? = { null },
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val suffixWidth =
        remember(suffix, suffixStyle) {
            if (suffix.isNotEmpty()) {
                val textLayoutResult =
                    textMeasurer.measure(
                        text = AnnotatedString(suffix),
                        style = suffixStyle,
                    )
                with(density) { textLayoutResult.size.width.toDp() }
            } else {
                0.dp
            }
        }

    val textWidth =
        remember(style) {
            val textLayoutResult =
                textMeasurer.measure(
                    text = AnnotatedString(texts.last()),
                    style = style,
                )
            with(density) { textLayoutResult.size.width.toDp() }
        }

    Box(modifier = modifier) {
        WheelPicker(
            startIndex = startIndex,
            size = size,
            count = texts.size,
            rowCount = rowCount,
            selectorProperties = selectorProperties,
            onScrollFinished = onScrollFinished,
        ) { index ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = texts[index],
                    style = style,
                    color = color,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                if (suffix.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(suffixWidth + textToSuffixSpacing))
                }
            }
        }

        if (suffix.isNotEmpty()) {
            Text(
                text = suffix,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(start = textWidth + textToSuffixSpacing),
                style = suffixStyle,
                color = suffixColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun WheelPicker(
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    count: Int,
    rowCount: Int,
    size: DpSize = DpSize(128.dp, 128.dp),
    selectorProperties: SelectorProperties = WheelPickerDefaults.selectorProperties(),
    onScrollFinished: (snappedIndex: Int) -> Int? = { null },
    content: @Composable LazyItemScope.(index: Int) -> Unit,
) {
    val lazyListState = rememberLazyListState(startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState)
    val isScrollInProgress = lazyListState.isScrollInProgress

    LaunchedEffect(isScrollInProgress, count) {
        if (!isScrollInProgress) {
            onScrollFinished(calculateSnappedItemIndex(lazyListState))?.let {
                lazyListState.scrollToItem(it)
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (selectorProperties.enabled().value) {
            Surface(
                modifier =
                    Modifier
                        .size(size.width, size.height / rowCount),
                shape = selectorProperties.shape().value,
                color = selectorProperties.color().value,
                border = selectorProperties.border().value,
            ) {}
        }
        LazyColumn(
            modifier =
                Modifier
                    .height(size.height)
                    .width(size.width),
            state = lazyListState,
            contentPadding = PaddingValues(vertical = size.height / rowCount * ((rowCount - 1) / 2)),
            flingBehavior = flingBehavior,
        ) {
            items(count) { index ->
                val (newAlpha, newRotationX) =
                    calculateAnimatedAlphaAndRotationX(
                        lazyListState = lazyListState,
                        index = index,
                        rowCount = rowCount,
                    )

                Box(
                    modifier =
                        Modifier
                            .height(size.height / rowCount)
                            .width(size.width)
                            .alpha(newAlpha)
                            .graphicsLayer {
                                rotationX = newRotationX
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    content(index)
                }
            }
        }
    }
}

@Composable
private fun calculateAnimatedAlphaAndRotationX(
    lazyListState: LazyListState,
    index: Int,
    rowCount: Int,
): Pair<Float, Float> {
    val layoutInfo = remember { derivedStateOf { lazyListState.layoutInfo } }.value
    val viewPortHeight = layoutInfo.viewportSize.height.toFloat()
    val singleViewPortHeight = viewPortHeight / rowCount

    val centerIndex = remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }.value
    val centerIndexOffset =
        remember { derivedStateOf { lazyListState.firstVisibleItemScrollOffset } }.value

    val distanceToCenterIndex = index - centerIndex

    val distanceToIndexSnap =
        distanceToCenterIndex * singleViewPortHeight.toInt() - centerIndexOffset
    val distanceToIndexSnapAbs = abs(distanceToIndexSnap)

    val animatedAlpha =
        if (abs(distanceToIndexSnap) in 0..singleViewPortHeight.toInt()) {
            1.2f - (distanceToIndexSnapAbs / singleViewPortHeight)
        } else {
            0.2f
        }

    val animatedRotationX =
        (-20 * (distanceToIndexSnap / singleViewPortHeight)).takeUnless { it.isNaN() } ?: 0f

    return animatedAlpha to animatedRotationX
}

private fun calculateSnappedItemIndex(lazyListState: LazyListState): Int {
    val currentItemIndex = lazyListState.firstVisibleItemIndex
    val itemCount = lazyListState.layoutInfo.totalItemsCount
    val offset = lazyListState.firstVisibleItemScrollOffset
    val itemHeight =
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull()
            ?.size ?: return currentItemIndex

    return if (offset > itemHeight / 2 && currentItemIndex < itemCount - 1) {
        currentItemIndex + 1
    } else {
        currentItemIndex
    }
}
