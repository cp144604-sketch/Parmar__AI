package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val ParmarDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = VoidBlack,
    primaryContainer = DeepCobalt,
    onPrimaryContainer = IceWhite,
    secondary = RoyalElectricBlue,
    onSecondary = IceWhite,
    secondaryContainer = ElevatedGraphite,
    onSecondaryContainer = ElectricCyan,
    tertiary = NeonBlue,
    onTertiary = VoidBlack,
    background = VoidBlack,
    onBackground = IceWhite,
    surface = ObsidianSurface,
    onSurface = IceWhite,
    surfaceVariant = DeepGraphite,
    onSurfaceVariant = CoolSlate,
    outline = GlassBorder,
    outlineVariant = DeepCobalt,
    error = CrimsonAlert,
    onError = IceWhite
)

private val ParmarShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ParmarAiTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = ParmarDarkColorScheme,
        typography = ParmarTypography,
        shapes = ParmarShapes,
        content = content
    )
}
