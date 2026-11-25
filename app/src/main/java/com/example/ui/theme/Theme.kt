package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatus

@Immutable
data class StatusPalette(
    val workingFg: Color,
    val workingBg: Color,
    val onBreakFg: Color,
    val onBreakBg: Color,
    val clockedOutFg: Color,
    val clockedOutBg: Color,
    val notClockedInFg: Color,
    val notClockedInBg: Color
) {
    fun colorsFor(status: AttendanceStatus): Pair<Color, Color> = when (status) {
        AttendanceStatus.WORKING -> workingFg to workingBg
        AttendanceStatus.ON_BREAK -> onBreakFg to onBreakBg
        AttendanceStatus.CLOCKED_OUT -> clockedOutFg to clockedOutBg
        AttendanceStatus.NOT_CLOCKED_IN -> notClockedInFg to notClockedInBg
    }
}

val LocalStatusPalette = staticCompositionLocalOf {
    StatusPalette(
        workingFg = WorkingGreenDark,
        workingBg = WorkingGreenBgDark,
        onBreakFg = BreakAmberDark,
        onBreakBg = BreakAmberBgDark,
        clockedOutFg = ClockedOutTealDark,
        clockedOutBg = ClockedOutTealBgDark,
        notClockedInFg = IdleSlateDark,
        notClockedInBg = IdleSlateBgDark
    )
}

private val BrandDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    surfaceTint = BrandAmber,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

val TimeTrackShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TimeTrackTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BrandDarkColorScheme else LightColorScheme
    val statusPalette = if (darkTheme) {
        StatusPalette(
            workingFg = WorkingGreenDark,
            workingBg = WorkingGreenBgDark,
            onBreakFg = BreakAmberDark,
            onBreakBg = BreakAmberBgDark,
            clockedOutFg = ClockedOutTealDark,
            clockedOutBg = ClockedOutTealBgDark,
            notClockedInFg = IdleSlateDark,
            notClockedInBg = IdleSlateBgDark
        )
    } else {
        StatusPalette(
            workingFg = WorkingGreenLight,
            workingBg = WorkingGreenBgLight,
            onBreakFg = BreakAmberLight,
            onBreakBg = BreakAmberBgLight,
            clockedOutFg = ClockedOutTealLight,
            clockedOutBg = ClockedOutTealBgLight,
            notClockedInFg = IdleSlateLight,
            notClockedInBg = IdleSlateBgLight
        )
    }

    CompositionLocalProvider(LocalStatusPalette provides statusPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TimeTrackTypography,
            shapes = TimeTrackShapes,
            content = content
        )
    }
}
