package com.conkeep.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat

class ThousandsSeparatorTransformation : VisualTransformation {
    private val formatter = DecimalFormat("#,###")

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val number = originalText.toLongOrNull() ?: 0L
        val formattedText = formatter.format(number)

        val offsetMapping =
            object : OffsetMapping {
                // 원본의 N번째 자리 → formatted 에서 동일 자리까지 커서 이동
                override fun originalToTransformed(offset: Int): Int {
                    var digits = 0
                    for ((index, char) in formattedText.withIndex()) {
                        if (digits == offset) return index
                        if (char != ',') digits++
                    }
                    return formattedText.length
                }

                // formatted 의 커서 위치 → 콤마를 제외한 실제 원본 위치
                override fun transformedToOriginal(offset: Int): Int {
                    val commasBeforeOffset =
                        formattedText
                            .substring(0, offset.coerceAtMost(formattedText.length))
                            .count { it == ',' }
                    return offset - commasBeforeOffset
                }
            }
        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
