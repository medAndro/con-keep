package com.conkeep.ui.feature.coupon.list.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.conkeep.R
import com.conkeep.ui.theme.ConKeepColors.bgSurface
import com.conkeep.ui.theme.ConKeepColors.textHint
import com.conkeep.ui.theme.ConKeepColors.textSecondary
import com.conkeep.ui.theme.PretendardMedium16

@Composable
fun SearchBar(
    query: String,
    onQueryUpdate: (String) -> Unit,
    onSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val searchImageVector: ImageVector = ImageVector.vectorResource(id = R.drawable.ic_search)

    BasicTextField(
        value = query,
        onValueChange = onQueryUpdate,
        modifier =
            modifier
                .height(45.dp)
                .clip(RoundedCornerShape(100.dp)),
        textStyle = PretendardMedium16,
        singleLine = singleLine,
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch() }
        ),
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = query,
                innerTextField = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = searchImageVector,
                            contentDescription = null,
                            tint = textHint,
                            modifier =
                                Modifier
                                    .padding(start = 10.dp)
                                    .size(24.dp),
                        )
                        Spacer(modifier = Modifier.size(7.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            innerTextField()
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.coupon_list_screen_search_bar_placeholder),
                                    style = PretendardMedium16,
                                    color = textSecondary,
                                )
                            }
                        }
                    }
                },
                enabled = true,
                singleLine = singleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                contentPadding = PaddingValues(start = 0.dp, end = 16.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interactionSource,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = bgSurface,
                                unfocusedContainerColor = bgSurface,
                                focusedBorderColor = bgSurface,
                                unfocusedBorderColor = bgSurface,
                            ),
                        shape = RoundedCornerShape(100.dp),
                    )
                },
            )
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0000)
@Composable
private fun SearchBarEmptyPreview() {
    SearchBar(
        query = "",
        onQueryUpdate = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0000)
@Composable
private fun SearchBarPreview() {
    SearchBar(
        query = "아메리카노",
        onQueryUpdate = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0000)
@Composable
private fun SearchBarLongTextPreview() {
    SearchBar(
        query = "스타벅스 [간편한 한끼(HOT)] 카페 아메리카노T+탕종 파마산 치즈 베이글",
        onQueryUpdate = {},
    )
}
