package com.conkeep.ui.feature.coupon.edit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.ui.theme.ConKeepColors.bgInput
import com.conkeep.ui.theme.ConKeepColors.borderFocused
import com.conkeep.ui.theme.ConKeepColors.borderSubtle
import com.conkeep.ui.theme.ConKeepColors.textHint
import com.conkeep.ui.theme.ConKeepColors.textPrimary
import com.conkeep.ui.theme.ConKeepTheme
import com.conkeep.ui.theme.PretendardMedium16
import com.conkeep.ui.theme.PretendardMedium20

@Composable
fun TextInputField(
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIconVector: ImageVector? = null,
    leadingIconDescription: String? = null,
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            onTextChange(input)
        },
        modifier =
            modifier
                .fillMaxWidth(),
        placeholder = {
            Text(
                text = placeholder,
                style = PretendardMedium16,
            )
        },
        leadingIcon =
            if (leadingIconVector != null) {
                {
                    Icon(
                        imageVector = leadingIconVector,
                        contentDescription = leadingIconDescription,
                        tint = textPrimary,
                    )
                }
            } else {
                null
            },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    // '완료' 버튼을 눌렀을 때 포커스 해제
                    focusManager.clearFocus()
                },
            ),
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
            TextInputField(
                text = "",
                onTextChange = {},
                placeholder = "플레이스홀더 텍스트",
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
            TextInputField(
                text = "배달의 민족",
                onTextChange = {},
                placeholder = "",
            )
        }
    }
}

@Preview(showBackground = true, name = "인터랙티브 테스트")
@Composable
private fun AmountInputInputFieldInteractivePreview() {
    // 실제 타이핑을 테스트해볼 수 있는 프리뷰
    var amount by remember { mutableStateOf("스타벅스") }

    ConKeepTheme {
        Surface(
            modifier = Modifier.padding(20.dp),
            color = Color.White,
        ) {
            TextInputField(
                text = "스타벅스",
                onTextChange = { amount = it },
                placeholder = "브랜드명을 입력하세요",
            )
        }
    }
}
