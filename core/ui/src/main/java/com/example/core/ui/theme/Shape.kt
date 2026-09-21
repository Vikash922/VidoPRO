package com.example.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design system radius tokens matching UI_DESIGN_SYSTEM.md Section 10 and 28
 */
object AppRadius {
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val large: Dp = 16.dp
    val sheet: Dp = 24.dp
    val clip: Dp = 10.dp
    val card: Dp = 16.dp
    val dialog: Dp = 16.dp
    val button: Dp = 999.dp
}

/**
 * Material 3 shapes mapping using design system radius tokens
 */
val AppShapes = Shapes(
    small = RoundedCornerShape(AppRadius.small),
    medium = RoundedCornerShape(AppRadius.medium),
    large = RoundedCornerShape(AppRadius.large),
    extraLarge = RoundedCornerShape(AppRadius.sheet)
)
