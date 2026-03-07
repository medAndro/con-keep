package com.conkeep.ui.feature.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.conkeep.ui.theme.ConKeepColors.bgInput
import com.conkeep.ui.theme.ConKeepColors.borderFocused
import com.conkeep.ui.theme.ConKeepColors.borderSubtle
import com.conkeep.ui.theme.ConKeepColors.brandPrimary
import com.conkeep.ui.theme.ConKeepColors.dialogNormalBg
import com.conkeep.ui.theme.ConKeepColors.dialogNormalText
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.PretendardMedium16
import com.conkeep.ui.theme.PretendardSemibold14
import com.conkeep.ui.theme.PretendardSemibold20
import dev.darkokoa.datetimewheelpicker.core.WheelPickerDefaults
import kotlinx.datetime.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingBottomSheet(
    onConfirm: (CouponAlarmSetting) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initAlarmSetting: CouponAlarmSetting = CouponAlarmSetting(0, LocalTime(9, 0)),
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var isInteracting by remember { mutableStateOf(false) }

    val cancelTextColor: Color = dialogNormalText
    val cancelBackGroundColor: Color = dialogNormalBg
    val confirmTextColor: Color = textPrimary
    val confirmBackgroundColor: Color = brandPrimary

    var selectedAlarm = CouponAlarmSetting(0, LocalTime(9, 0))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = modifier,
    ) {
        Column(
            modifier =
                modifier
                    .padding(horizontal = 10.dp)
                    .imePadding()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                "언제 만료일 알림을 보낼까요?",
                style = PretendardSemibold20,
                color = textPrimary,
            )
            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(bgInput, RoundedCornerShape(10.dp))
                        .border(
                            width = if (isInteracting) 2.dp else 1.dp,
                            color = if (isInteracting) borderFocused else borderSubtle,
                            shape = RoundedCornerShape(10.dp),
                        ).pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    when (event.type) {
                                        PointerEventType.Press -> isInteracting = true
                                        PointerEventType.Release, PointerEventType.Exit ->
                                            isInteracting =
                                                false
                                    }
                                }
                            }
                        },
                contentAlignment = Alignment.Center,
            ) {
                CouponAlarmWheelPicker(
                    modifier = Modifier.padding(vertical = 8.dp),
                    size = DpSize((maxWidth.value * 0.85).dp, 120.dp),
                    onAlarmSettingChanged = { select -> selectedAlarm = select },
                    rowCount = 3,
                    textStyle = PretendardMedium16,
                    textColor = textPrimary,
                    initialSetting = initAlarmSetting,
                    selectorProperties =
                        WheelPickerDefaults.selectorProperties(
                            enabled = true,
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                            border =
                                BorderStroke(
                                    if (isInteracting) 2.dp else 1.5.dp,
                                    brandPrimary.copy(alpha = 0.7f),
                                ),
                        ),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // 취소 버튼
                Button(
                    onClick = onCancel,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cancelBackGroundColor),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("취소", style = PretendardSemibold14, color = cancelTextColor)
                }
                // 확인 버튼
                Button(
                    onClick = {
                        onConfirm(selectedAlarm)
                    },
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = confirmBackgroundColor),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        "확인",
                        style = PretendardSemibold14,
                        color = confirmTextColor,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AlarmSettingBottomSheetPreview() {
    AlarmSettingBottomSheet(
        onDismiss = {},
        onConfirm = {},
        onCancel = {},
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    )
}
