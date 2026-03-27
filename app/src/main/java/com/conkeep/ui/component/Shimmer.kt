package com.conkeep.ui.component

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.conkeep.ui.theme.ConKeepColors.shimmerColor
import com.conkeep.ui.util.spToDp
import com.valentinilk.shimmer.shimmer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterRoundShimmer(
    width: Dp,
    height: Dp,
    shape: Shape = RoundedCornerShape(16.dp),
) {
    Row(
        modifier = Modifier.shimmer(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width, height)
                    .clip(shape)
                    .background(shimmerColor),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterRoundTextShimmer(
    width: Dp,
    context: Context,
    textSizeSp: Float,
    verticalPaddingDp: Dp = 0.dp,
    shape: Shape = RoundedCornerShape(16.dp),
) {
    Row(
        modifier = Modifier.shimmer(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width, textSizeSp.spToDp(context).dp + verticalPaddingDp)
                    .clip(shape)
                    .background(shimmerColor),
        )
    }
}
