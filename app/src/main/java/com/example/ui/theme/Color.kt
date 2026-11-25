package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Brand Palette: #1d1d1d (background), #f59115 (primary accent/logo), #46332e (secondary)
val BrandCharcoal = Color(0xFF1D1D1D)
val BrandAmber = Color(0xFFF59115)
val BrandEspresso = Color(0xFF46332E)

// Dark-First Unified Brand Palette (default app theme)
val PrimaryDark = BrandAmber
val OnPrimaryDark = BrandCharcoal
val PrimaryContainerDark = Color(0xFF5C3707)
val OnPrimaryContainerDark = Color(0xFFFFE5C4)

val SecondaryDark = BrandEspresso
val OnSecondaryDark = Color(0xFFF5ECE6)
val SecondaryContainerDark = BrandEspresso
val OnSecondaryContainerDark = Color(0xFFFFD8A8)

val TertiaryDark = Color(0xFF34D399)
val OnTertiaryDark = Color(0xFF053827)
val TertiaryContainerDark = Color(0xFF064E3B)
val OnTertiaryContainerDark = Color(0xFFD1FAE5)

val ErrorDark = Color(0xFFF87171)
val OnErrorDark = Color(0xFF450A0A)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFEE2E2)

val BackgroundDark = BrandCharcoal
val OnBackgroundDark = Color(0xFFF5F1EC)
val SurfaceDark = Color(0xFF262221)
val OnSurfaceDark = Color(0xFFF5F1EC)
val SurfaceVariantDark = Color(0xFF342A27)
val OnSurfaceVariantDark = Color(0xFFD0C4BD)

val SurfaceContainerLowestDark = Color(0xFF171717)
val SurfaceContainerLowDark = Color(0xFF211E1D)
val SurfaceContainerDark = Color(0xFF282322)
val SurfaceContainerHighDark = Color(0xFF322A27)
val SurfaceContainerHighestDark = BrandEspresso

val OutlineDark = Color(0xFF5A453F)
val OutlineVariantDark = Color(0xFF3D2F2B)
val InverseSurfaceDark = Color(0xFFF5F1EC)
val InverseOnSurfaceDark = BrandCharcoal
val InversePrimaryDark = Color(0xFFB36300)

// Light Palette fallback (when explicitly requested)
val PrimaryLight = BrandAmber
val OnPrimaryLight = BrandCharcoal
val PrimaryContainerLight = Color(0xFFFFEBD2)
val OnPrimaryContainerLight = Color(0xFF3E2200)

val SecondaryLight = BrandEspresso
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFEFE6E2)
val OnSecondaryContainerLight = Color(0xFF2D1E1B)

val TertiaryLight = Color(0xFF0F766E)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFCCFBF1)
val OnTertiaryContainerLight = Color(0xFF0A433E)

val ErrorLight = Color(0xFFDC2626)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF7F1D1D)

val BackgroundLight = Color(0xFFFAF7F4)
val OnBackgroundLight = BrandCharcoal
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = BrandCharcoal
val SurfaceVariantLight = Color(0xFFF2ECE6)
val OnSurfaceVariantLight = Color(0xFF5C4E49)
val OutlineLight = Color(0xFFD8CEC7)
val OutlineVariantLight = Color(0xFFEBE3DC)

// Semantic Status Colors (harmonized with #1d1d1d, #f59115, #46332e)
val WorkingGreenLight = Color(0xFF059669)
val WorkingGreenBgLight = Color(0xFFECFDF5)
val WorkingGreenDark = Color(0xFF34D399)
val WorkingGreenBgDark = Color(0xFF064E3B)

val BreakAmberLight = Color(0xFFD97706)
val BreakAmberBgLight = Color(0xFFFFF4E3)
val BreakAmberDark = BrandAmber
val BreakAmberBgDark = Color(0xFF4A2E07)

val ClockedOutTealLight = Color(0xFF0284C7)
val ClockedOutTealBgLight = Color(0xFFE0F2FE)
val ClockedOutTealDark = Color(0xFF38BDF8)
val ClockedOutTealBgDark = Color(0xFF0C4A6E)

val IdleSlateLight = Color(0xFF78716C)
val IdleSlateBgLight = Color(0xFFF2ECE6)
val IdleSlateDark = Color(0xFFA8A29E)
val IdleSlateBgDark = Color(0xFF2E2927)
