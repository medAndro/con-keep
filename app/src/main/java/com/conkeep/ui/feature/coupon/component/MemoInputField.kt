package com.conkeep.ui.feature.coupon.component

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.ui.theme.ConKeepColors.bgInput
import com.conkeep.ui.theme.ConKeepColors.borderFocused
import com.conkeep.ui.theme.ConKeepColors.borderSubtle
import com.conkeep.ui.theme.ConKeepColors.textHint
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardMedium14
import kotlinx.coroutines.delay

@Composable
fun MemoInputField(
    memo: String,
    onMemoChange: (String) -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "쿠폰과 관련된 메모를 남겨주세요\n(예: 친구 선물, 생일 쿠폰)",
) {
    var isFocused by remember { mutableStateOf(false) }
    val currentMemo by rememberUpdatedState(memo)

    // 로직 1: 입력이 멈춘 후 1초 뒤에 자동 저장 (Debounce)
    LaunchedEffect(memo) {
        if (memo.isBlank()) return@LaunchedEffect
        delay(1000L) // 1초 대기
        onSave(memo)
    }

    OutlinedTextField(
        value = memo,
        onValueChange = onMemoChange,
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    // 로직 2: 포커스가 있다가 사라지는 순간 저장
                    if (isFocused && !focusState.isFocused) {
                        onSave(currentMemo)
                    }
                    isFocused = focusState.isFocused
                },
        placeholder = {
            Text(
                text = placeholder,
                style = PretendardMedium14,
            )
        },
        textStyle = PretendardMedium14,
        shape = RoundedCornerShape(10.dp), // 1. 곡률 조정 (12.dp -> 20.dp로 변경 시 더 둥글어짐)
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = borderFocused, // 포커스 되었을 때 선 색상
                unfocusedBorderColor = borderSubtle, // 포커스 없을 때 선 색상
                focusedPlaceholderColor = textHint, // 포커스 시 힌트 색상
                unfocusedPlaceholderColor = textHint, // 포커스 없을 때 힌트 색상
                focusedContainerColor = bgInput,
                unfocusedContainerColor = bgInput,
            ),
        minLines = 5, // 메모장이니 최소 높이 확보
        maxLines = 12,
    )
}

@Preview(showBackground = true, name = "기본 - 플레이스홀더")
@Composable
private fun MemoInputFieldPlaceholderPreview() {
    ConKeepTheme {
        Surface(
            modifier = Modifier.padding(20.dp),
            color = Color.White,
        ) {
            MemoInputField(
                memo = "",
                onMemoChange = {},
                onSave = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "내용 입력됨")
@Composable
private fun MemoInputFieldContentPreview() {
    ConKeepTheme {
        Surface(
            modifier = Modifier.padding(20.dp),
            color = Color.White,
        ) {
            MemoInputField(
                memo = "친구에게 생일선물로 받은 쿠폰입니다.",
                onMemoChange = {},
                onSave = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "인터랙티브 테스트")
@Composable
private fun MemoInputFieldInteractivePreview() {
    // 실제 타이핑을 테스트해볼 수 있는 프리뷰
    var text by remember { mutableStateOf("") }

    ConKeepTheme {
        Surface(
            modifier = Modifier.padding(20.dp),
            color = Color.White,
        ) {
            MemoInputField(
                memo = text,
                onMemoChange = { text = it },
                onSave = { Log.d("Preview", "저장 로직 실행: $it") },
            )
        }
    }
}
