package com.example.ar_project_1.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Motyw aplikacji: szaro-biały, kafelki białe, akcent w medycznym czerwonym.
 * Motyw jest zawsze jasny — niezależnie od ustawień systemowych — bo taki
 * wygląd jest czytelniejszy w świetle dziennym i spójny na wszystkich ekranach.
 */
private val AppColorScheme = lightColorScheme(
    primary = MedicalRed,
    onPrimary = Color.White,
    primaryContainer = MedicalRedLight,
    onPrimaryContainer = MedicalRedDark,
    inversePrimary = Color(0xFFFFB4AB),

    secondary = TextSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDEFF2),
    onSecondaryContainer = Color(0xFF2B2F33),

    tertiary = WarningContent,
    onTertiary = Color.White,
    tertiaryContainer = WarningBackground,
    onTertiaryContainer = WarningContent,

    background = AppBackground,
    onBackground = TextPrimary,

    surface = AppSurface,
    onSurface = TextPrimary,
    surfaceVariant = AppSurface,
    onSurfaceVariant = TextSecondary,
    surfaceTint = MedicalRed,

    surfaceBright = AppSurface,
    surfaceDim = AppBackground,
    surfaceContainerLowest = AppSurface,
    surfaceContainerLow = AppSurface,
    surfaceContainer = AppSurface,
    surfaceContainerHigh = AppSurfaceMuted,
    surfaceContainerHighest = AppSurfaceMuted,

    inverseSurface = Color(0xFF2E3133),
    inverseOnSurface = Color(0xFFF1F3F4),

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    outline = Color(0xFFD7DADE),
    outlineVariant = AppDivider,
    scrim = Color.Black,
)

@Composable
fun AR_PROJECT_1Theme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content,
    )
}
