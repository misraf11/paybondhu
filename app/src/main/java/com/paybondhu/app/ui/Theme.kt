package com.paybondhu.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Pb {
    val Blue = Color(0xFF0A57FF)
    val BlueLight = Color(0xFF4C86FF)
    val BlueDeep = Color(0xFF0838B8)
    val Green = Color(0xFF12B76A)
    val Amber = Color(0xFFF79009)
    val Red = Color(0xFFF04438)
    val Slate = Color(0xFF667085)
}

private val Light = lightColorScheme(
    primary = Pb.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EEFF),
    onPrimaryContainer = Color(0xFF0A2A80),
    background = Color(0xFFF5F7FB),
    onBackground = Color(0xFF101828),
    surface = Color.White,
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFEEF2FA),
    onSurfaceVariant = Color(0xFF667085),
    outline = Color(0xFFE4E7EC),
    outlineVariant = Color(0xFFEAECF0),
    error = Pb.Red,
)

private val Dark = darkColorScheme(
    primary = Color(0xFF6C9BFF),
    onPrimary = Color(0xFF00184D),
    primaryContainer = Color(0xFF16275A),
    onPrimaryContainer = Color(0xFFD6E3FF),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE6EAF5),
    surface = Color(0xFF131A2E),
    onSurface = Color(0xFFE6EAF5),
    surfaceVariant = Color(0xFF1B2440),
    onSurfaceVariant = Color(0xFF9AA4BF),
    outline = Color(0xFF263050),
    outlineVariant = Color(0xFF1F2846),
    error = Color(0xFFFF8A80),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
)

@Composable
fun PayBondhuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = AppShapes,
        content = content,
    )
}
