package com.conkeep.ui.feature.coupon.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.conkeep.R
import com.conkeep.ui.feature.coupon.model.CouponUiModel
import com.conkeep.ui.theme.ConKeepTheme
import java.io.File

@Composable
fun CouponCard(
    couponUiModel: CouponUiModel,
    onClick: () -> Unit,
) {
    val isPreview = LocalInspectionMode.current

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable(onClick = onClick),
    ) {
        Row {
            AsyncImage(
                model =
                    if (isPreview) {
                        R.drawable.ic_corn_ms_emoji
                    } else {
                        couponUiModel.localImagePath?.let { File(it) }
                    },
                contentDescription = "쿠폰 이미지",
                placeholder = painterResource(R.drawable.ic_corn_ms_emoji),
                error = painterResource(R.drawable.ic_corn_ms_emoji),
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "${if (couponUiModel.isUsed) "[사용]" else "[미사용]"} ${couponUiModel.name}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "유효기간: ${couponUiModel.expiryDate}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview
@Composable
fun CouponCardPreview() {
    ConKeepTheme {
        CouponCard(
            couponUiModel =
                CouponUiModel(
                    id = "1",
                    number = "1234567890",
                    name = "이디야 커피",
                    expiryDate = "2026-12-31",
                    isUsed = false,
                    localImagePath = null,
                ),
            onClick = {},
        )
    }
}
