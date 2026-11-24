package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = SlateNavy900,
    onPrimary = Color.White,
    primaryContainer = IndustrialAmberBgLight,
    onPrimaryContainer = SlateNavy900,
    secondary = IndustrialCobalt,
    onSecondary = Color.White,
    secondaryContainer = IndustrialCobaltBgLight,
    onSecondaryContainer = SlateNavy900,
    tertiary = IndustrialAmber,
    onTertiary = Color.White,
    tertiaryContainer = IndustrialAmberBgLight,
    onTertiaryContainer = SlateNavy900,
    background = CanvasLight,
    onBackground = SlateNavy900,
    surface = SurfaceWhite,
    onSurface = SlateNavy900,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = SlateNavy700,
    outline = SlateBorderLight,
    outlineVariant = SlateBorderLight,
    error = PriorityHighRed,
    onError = Color.White,
    errorContainer = PriorityHighBgLight,
    onErrorContainer = PriorityHighRed
)

private val DarkColorScheme = darkColorScheme(
    primary = IndustrialAmberBright,
    onPrimary = SlateNavy950,
    primaryContainer = IndustrialAmberBgDark,
    onPrimaryContainer = PriorityMidTextDark,
    secondary = IndustrialCobaltLight,
    onSecondary = SlateNavy950,
    secondaryContainer = IndustrialCobaltBgDark,
    onSecondaryContainer = Color(0xFFBFDBFE),
    tertiary = IndustrialAmberBright,
    onTertiary = SlateNavy950,
    background = SlateNavy950,
    onBackground = Color(0xFFF1F5F9),
    surface = SurfaceDarkCard,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = SurfaceDarkVariant,
    onSurfaceVariant = SlateGray400,
    outline = SlateBorderDark,
    outlineVariant = SlateBorderDark,
    error = Color(0xFFF87171),
    onError = SlateNavy950,
    errorContainer = PriorityHighBgDark,
    onErrorContainer = PriorityHighTextDark
)

val JsmShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun JsmIndustriesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = JsmShapes,
        content = content
    )
}
