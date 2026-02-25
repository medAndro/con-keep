package com.conkeep.ui.feature.coupon.component

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.R
import com.conkeep.ui.theme.ConKeepColors.bgInput
import com.conkeep.ui.theme.ConKeepColors.borderFocused
import com.conkeep.ui.theme.ConKeepColors.borderSubtle
import com.conkeep.ui.theme.ConKeepColors.textHint
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardMedium16
import com.conkeep.ui.theme.PretendardMedium20
import com.conkeep.ui.util.ThousandsSeparatorTransformation
import kotlinx.coroutines.delay

@Composable
fun AmountInputField(
    amount: Int,
    onAmountChange: (Int) -> Unit,
    onSave: (Int) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "현재 잔액을 입력하세요",
) {
    var isFocused by remember { mutableStateOf(false) }
    val currentAmount by rememberUpdatedState(amount)
    val focusManager = LocalFocusManager.current

    // 상태는 숫자로 관리하지만, 텍스트 필드 입력을 위해 String으로 변환
    val amountString = if (amount == 0) "" else amount.toString()

    // 포커스가 있을 떄, 1초 뒤에 자동 저장 (Debounce)
    LaunchedEffect(amount) {
        if (!isFocused) return@LaunchedEffect
        delay(1000L) // 1초 대기
        onSave(amount)
    }

    OutlinedTextField(
        value = amountString,
        onValueChange = { input ->
            // 숫자만 남기고 필터링
            val cleanedInput = input.filter { it.isDigit() }
            if (cleanedInput.isEmpty()) {
                onAmountChange(0)
            } else {
                val parsed = cleanedInput.toLongOrNull() ?: 0L
                if (parsed <= Int.MAX_VALUE) {
                    onAmountChange(parsed.toInt())
                }
            }
        },
        visualTransformation =
            if (isFocused) {
                VisualTransformation.None
            } else {
                ThousandsSeparatorTransformation()
            },
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    // 포커스가 있다가 사라지는 순간 저장
                    if (isFocused && !focusState.isFocused) {
                        onSave(currentAmount)
                    }
                    isFocused = focusState.isFocused
                },
        placeholder = {
            Text(
                text = placeholder,
                style = PretendardMedium16,
            )
        },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number, // [핵심] 숫자 키패드 강제
                imeAction = ImeAction.Done, // 키보드 우하단 버튼을 '완료'로 설정
            ),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    // '완료' 버튼을 눌렀을 때 포커스 해제 및 저장 트리거
                    focusManager.clearFocus()
                    onSave(currentAmount)
                },
            ),
        // 아이콘 설정
        leadingIcon = {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_won), // 원화 아이콘 등
                contentDescription = "금액 아이콘",
                tint = textPrimary,
            )
        },
        trailingIcon = {
            if (amount > 0) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_write), // 수정 아이콘 등
                    contentDescription = "수정 중",
                    tint = textHint,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        textStyle = PretendardMedium20,
        shape = RoundedCornerShape(10.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = borderFocused, // 포커스 되었을 때 선 색상
                unfocusedBorderColor = borderSubtle, // 포커스 없을 때 선 색상
                focusedPlaceholderColor = textHint, // 포커스 시 힌트 색상
                unfocusedPlaceholderColor = textHint, // 포커스 없을 때 힌트 색상
                focusedContainerColor = bgInput,
                unfocusedContainerColor = bgInput,
            ),
        singleLine = true,
    )
}

@Preview(showBackground = true, name = "기본 - 플레이스홀더")
@Composable
private fun AmountInputInputFieldPlaceholderPreview() {
    ConKeepTheme {
        Surface(
            modifier = Modifier.padding(20.dp),
            color = Color.White,
        ) {
            AmountInputField(
                amount = 0,
                onAmountChange = {},
                onSave = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "내용 입력됨")
@Composable
private fun AmountInputInputFieldContentPreview() {
    ConKeepTheme {
        Surface(
            modifier = Modifier.padding(20.dp),
            color = Color.White,
        ) {
            AmountInputField(
                amount = 10000,
                onAmountChange = {},
                onSave = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "인터랙티브 테스트")
@Composable
private fun AmountInputInputFieldInteractivePreview() {
    // 실제 타이핑을 테스트해볼 수 있는 프리뷰
    var amount by remember { mutableIntStateOf(50000) }

    ConKeepTheme {
        Surface(
            modifier = Modifier.padding(20.dp),
            color = Color.White,
        ) {
            AmountInputField(
                amount = amount,
                onAmountChange = { amount = it },
                onSave = { Log.d("Preview", "저장 로직 실행: $it") },
            )
        }
    }
}
