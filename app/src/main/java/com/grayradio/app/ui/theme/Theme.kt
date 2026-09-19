package com.grayradio.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Strict grayscale Material 3 scheme — no colorful accents.
 */
private val GrayScaleScheme = darkColorScheme(
    primary = Gray200,
    onPrimary = PureBlack,
    primaryContainer = Gray700,
    onPrimaryContainer = Gray100,
    secondary = Gray400,
    onSecondary = PureBlack,
    secondaryContainer = Gray650,
    onSecondaryContainer = Gray100,
    tertiary = Gray300,
    onTertiary = PureBlack,
    tertiaryContainer = Gray600,
    onTertiaryContainer = Gray100,
    background = Gray900,
    onBackground = Gray100,
    surface = Gray850,
    onSurface = Gray100,
    surfaceVariant = Gray700,
    onSurfaceVariant = Gray300,
    surfaceContainerHighest = Gray700,
    surfaceContainerHigh = Gray750,
    surfaceContainer = Gray800,
    surfaceContainerLow = Gray850,
    surfaceContainerLowest = PureBlack,
    outline = Gray500,
    outlineVariant = Gray600,
    error = Gray300,
    onError = PureBlack,
    errorContainer = Gray700,
    onErrorContainer = Gray200,
    inverseSurface = Gray100,
    inverseOnSurface = Gray900,
    inversePrimary = Gray600,
    scrim = Color(0xCC000000),
)

@Composable
fun GrayRadioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GrayScaleScheme,
        typography = Typography,
        content = content,
    )
}
