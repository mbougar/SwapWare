package com.mbougar.swapware.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val DefaultTypography = Typography()

val AppTypography = Typography(
    displayLarge = DefaultTypography.displayLarge.copy(fontFamily = FontFamily.SansSerif /* or YourCustomFontFamily */),
    displayMedium = DefaultTypography.displayMedium.copy(fontFamily = FontFamily.SansSerif),
    displaySmall = DefaultTypography.displaySmall.copy(fontFamily = FontFamily.SansSerif),

    headlineLarge = DefaultTypography.headlineLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    headlineMedium = DefaultTypography.headlineMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),
    headlineSmall = DefaultTypography.headlineSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold),

    titleLarge = DefaultTypography.titleLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
    titleMedium = DefaultTypography.titleMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
    titleSmall = DefaultTypography.titleSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),

    bodyLarge = DefaultTypography.bodyLarge.copy(fontFamily = FontFamily.SansSerif, lineHeight = 24.sp),
    bodyMedium = DefaultTypography.bodyMedium.copy(fontFamily = FontFamily.SansSerif, lineHeight = 22.sp),
    bodySmall = DefaultTypography.bodySmall.copy(fontFamily = FontFamily.SansSerif, lineHeight = 20.sp),

    labelLarge = DefaultTypography.labelLarge.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
    labelMedium = DefaultTypography.labelMedium.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium),
    labelSmall = DefaultTypography.labelSmall.copy(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium)
)