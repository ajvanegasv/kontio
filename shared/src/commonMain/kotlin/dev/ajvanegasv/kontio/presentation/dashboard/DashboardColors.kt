package dev.ajvanegasv.kontio.presentation.dashboard

import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores extraída directamente de la configuración de Tailwind del HTML proporcionado.
 * Asegura correspondencia tonal 1:1 con el diseño del dashboard de finanzas personales.
 */
object DashboardColors {
    // Primary
    val Primary = Color(0xFFC0C1FF)
    val OnPrimary = Color(0xFF1000A9)
    val PrimaryContainer = Color(0xFF8083FF)
    val OnPrimaryContainer = Color(0xFF0D0096)
    val InversePrimary = Color(0xFF494BD6)
    val PrimaryFixed = Color(0xFFE1E0FF)
    val PrimaryFixedDim = Color(0xFFC0C1FF)
    val OnPrimaryFixed = Color(0xFF07006C)
    val OnPrimaryFixedVariant = Color(0xFF2F2EBE)

    // Secondary (Cyan / Mint)
    val Secondary = Color(0xFF44E2CD)
    val SecondaryContainer = Color(0xFF03C6B2)
    val OnSecondary = Color(0xFF003731)
    val OnSecondaryContainer = Color(0xFF004D44)
    val SecondaryFixed = Color(0xFF62FAE3)
    val SecondaryFixedDim = Color(0xFF3CDDC7)
    val OnSecondaryFixed = Color(0xFF00201C)
    val OnSecondaryFixedVariant = Color(0xFF005047)

    // Tertiary (Purple / Lavender)
    val Tertiary = Color(0xFFDDB7FF)
    val TertiaryContainer = Color(0xFFB76DFF)
    val OnTertiary = Color(0xFF490080)
    val OnTertiaryContainer = Color(0xFF400071)
    val TertiaryFixed = Color(0xFFF0DBFF)
    val TertiaryFixedDim = Color(0xFFDDB7FF)
    val OnTertiaryFixed = Color(0xFF2C0051)
    val OnTertiaryFixedVariant = Color(0xFF6900B3)

    // Surfaces & Background
    val Background = Color(0xFF0B1326)
    val OnBackground = Color(0xFFDAE2FD)
    val Surface = Color(0xFF0B1326)
    val SurfaceDim = Color(0xFF0B1326)
    val SurfaceBright = Color(0xFF31394D)
    val SurfaceContainerLowest = Color(0xFF060E20)
    val SurfaceContainerLow = Color(0xFF131B2E)
    val SurfaceContainer = Color(0xFF171F33)
    val SurfaceContainerHigh = Color(0xFF222A3D)
    val SurfaceContainerHighest = Color(0xFF2D3449)
    val SurfaceVariant = Color(0xFF2D3449)
    val OnSurface = Color(0xFFDAE2FD)
    val OnSurfaceVariant = Color(0xFFC7C4D7)
    val InverseSurface = Color(0xFFDAE2FD)
    val InverseOnSurface = Color(0xFF283044)

    // Functional & Outline
    val Outline = Color(0xFF908FA0)
    val OutlineVariant = Color(0xFF464554)
    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)

    // Mesh Ambient Colors
    val MeshNavy = Color(0xFF0B1326)
    val MeshIndigo = Color(0xFF131118)
    val MeshCoolBlue = Color(0xFF2E3E6B)
    val MeshMagenta = Color(0xFF722744)
}
