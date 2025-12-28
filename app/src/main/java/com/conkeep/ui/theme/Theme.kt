package com.conkeep.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * ConKeep Light ColorScheme
 * Material3 기본 컴포넌트 동작을 위한 최소 설정
 */
private val LightColorScheme =
    lightColorScheme(
        // Primary (브랜드 메인)
        primary = ColorPalette.YellowBase,
        onPrimary = ColorPalette.NavyDark,
        primaryContainer = ColorPalette.YellowLight,
        onPrimaryContainer = ColorPalette.NavyDark,
        // Secondary (강조)
        secondary = ColorPalette.OrangeVivid,
        onSecondary = ColorPalette.NeutralWhite,
        secondaryContainer = ColorPalette.OrangePale,
        onSecondaryContainer = ColorPalette.OrangeDark,
        // Tertiary (보조)
        tertiary = ColorPalette.GreenBase,
        onTertiary = ColorPalette.NeutralWhite,
        tertiaryContainer = ColorPalette.GreenPale,
        onTertiaryContainer = ColorPalette.GreenBase,
        // Background & Surface
        background = ColorPalette.YellowBg,
        onBackground = ColorPalette.NavyDark,
        surface = ColorPalette.NeutralWhite,
        onSurface = ColorPalette.NavyDark,
        surfaceVariant = ColorPalette.NeutralCreamMedium,
        onSurfaceVariant = ColorPalette.GrayDark,
        // Error
        error = ColorPalette.OrangeDark,
        onError = ColorPalette.NeutralWhite,
        errorContainer = ColorPalette.OrangePale,
        onErrorContainer = ColorPalette.OrangeDark,
        // Outline
        outline = ColorPalette.GrayLight,
        outlineVariant = ColorPalette.GrayLighter,
        // Scrim
        scrim = ColorPalette.NeutralBlack.copy(alpha = 0.5f),
    )

/**
 * ConKeep Dark ColorScheme (추후 구현)
 */
private val DarkColorScheme =
    darkColorScheme(
        primary = ColorPalette.YellowBase,
        onPrimary = ColorPalette.NeutralBlack,
        // TODO: 다크모드 색상 정의
    )

/**
 * ConKeep 테마
 *
 * @param darkTheme 다크모드 활성화 여부
 * @param content Composable 콘텐츠
 */
@Composable
fun ConKeepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
