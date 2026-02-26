package com.conkeep.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.conkeep.R
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.dialogNormalBg
import com.conkeep.ui.theme.ConKeepColors.dialogNormalText
import com.conkeep.ui.theme.ConKeepColors.dialogWarnBg
import com.conkeep.ui.theme.ConKeepColors.dialogWarnText
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.ConKeepColors.textSecondary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardMedium14
import com.conkeep.ui.theme.PretendardSemibold14
import com.conkeep.ui.theme.PretendardSemibold18

@Composable
fun ConKeepConfirmDialog(
    title: String,
    description: String,
    confirmText: String,
    confirmTextColor: Color = dialogWarnText,
    confirmBackgroundColor: Color = dialogWarnBg,
    cancelText: String,
    cancelTextColor: Color = dialogNormalText,
    cancelBackGroundColor: Color = dialogNormalBg,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onCancel: () -> Unit = onDismiss,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bgSurface,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onDismiss,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(32.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.common_dialog_cancel_title),
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = PretendardSemibold18,
                        color = textPrimary,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = description,
                        style = PretendardMedium14,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))

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
                            Text(cancelText, style = PretendardSemibold14, color = cancelTextColor)
                        }
                        // 확인(삭제) 버튼
                        Button(
                            onClick = {
                                onConfirm()
                                onCancel()
                            },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = confirmBackgroundColor),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(confirmText, style = PretendardSemibold14, color = confirmTextColor)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "삭제 확인 다이얼로그")
@Composable
fun DeleteConfirmDialogPreview() {
    ConKeepTheme {
        ConKeepConfirmDialog(
            title = "쿠폰 삭제",
            description = "정말로 이 쿠폰을 삭제하시겠습니까?\n삭제된 쿠폰은 복구할 수 없습니다.",
            confirmText = "삭제하기",
            cancelText = "취소",
            onConfirm = { },
            onDismiss = { },
        )
    }
}
