package com.conkeep.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * ConKeep 시맨틱 색상 시스템
 * 피그마 "ConKeep 시맨틱" 컬렉션과 1:1 매핑
 *
 * 현재 Light Mode만 지원
 * TODO: 다크모드는 추후 구현
 */
object ConKeepColors {
    // ========== Brand (고정 색상) ==========
    val brandPrimary: Color
        get() = ColorPalette.YellowBase

    val brandSecondary: Color
        get() = ColorPalette.YellowLight

    val brandAccent: Color
        get() = ColorPalette.OrangeVivid

    val brandHighlight: Color
        get() = ColorPalette.GreenSoft

    // ========== Background ==========
    val bgDefault: Color
        get() = ColorPalette.YellowBg

    val bgSurface: Color
        get() = ColorPalette.NeutralWhite

    val bgInput: Color
        get() = ColorPalette.NeutralCreamMedium

    // ========== Text ==========
    val textPrimary: Color
        get() = ColorPalette.NavyDark

    val textBrandGray: Color
        get() = ColorPalette.GrayOrangeGray

    val textSecondary: Color
        get() = ColorPalette.GrayDark

    val textDisabled: Color
        get() = ColorPalette.GrayMedium

    val textHint: Color
        get() = ColorPalette.GrayBlueGray

    val textWhite: Color
        get() = ColorPalette.NeutralWhite

    // ========== Border ==========
    val borderDefault: Color
        get() = ColorPalette.GrayLighter

    val borderSubtle: Color
        get() = ColorPalette.GrayLight

    // ========== Badge ==========
    val badgeSafe: Color
        get() = ColorPalette.GreenBase

    val badgeSafeBg: Color
        get() = ColorPalette.GreenPale

    val badgeExpiring: Color
        get() = ColorPalette.OrangeDark

    val badgeExpiringBg: Color
        get() = ColorPalette.OrangePale

    val badgeWarning: Color
        get() = ColorPalette.OrangeBright

    val badgeWarningBg: Color
        get() = ColorPalette.OrangePastel

    val badgeCommon: Color
        get() = ColorPalette.GrayDark

    val badgeCommonBg: Color
        get() = ColorPalette.GrayLight

    // ========== Interactive ==========
    val buttonNegativeBg: Color
        get() = ColorPalette.OrangeDark

    val buttonPositiveBg: Color
        get() = ColorPalette.NavyDark
}
