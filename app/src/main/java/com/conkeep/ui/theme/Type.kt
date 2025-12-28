package com.conkeep.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PretendardLight =
    TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Light,
    )

val PretendardRegular =
    TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Normal,
    )

val PretendardMedium =
    TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Medium,
    )
val PretendardMedium12 = PretendardMedium.copy(fontSize = 12.sp)
val PretendardMedium14 = PretendardMedium.copy(fontSize = 14.sp)
val PretendardMedium16 = PretendardMedium.copy(fontSize = 16.sp)

val PretendardSemibold =
    TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.SemiBold,
    )
val PretendardSemibold12 = PretendardSemibold.copy(fontSize = 12.sp)
val PretendardSemibold16 = PretendardSemibold.copy(fontSize = 16.sp)

val PretendardBold =
    TextStyle(
        fontFamily = PretendardFontFamily,
        fontWeight = FontWeight.Bold,
    )
val PretendardBold24 = PretendardBold.copy(fontSize = 24.sp)
