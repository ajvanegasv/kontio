package dev.ajvanegasv.kontio.presentation.dashboard

import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores extraída directamente de la configuración de Tailwind del HTML proporcionado.
 * Asegura correspondencia tonal 1:1 con el diseño del dashboard de finanzas personales.
 */
object DashboardColors {
    // Primary (Slate Steel Tone-80 on Dark, containers use #4D627F)
    val Primary = Color(0xFF9FB5D2)
    val OnPrimary = Color(0xFF191A1E)
    val PrimaryContainer = Color(0xFF4D627F)
    val OnPrimaryContainer = Color(0xFFEFFFFA)
    val InversePrimary = Color(0xFF4D627F)
    val PrimaryFixed = Color(0xFFD6E3F2)
    val PrimaryFixedDim = Color(0xFF9FB5D2)
    val OnPrimaryFixed = Color(0xFF0F1B2A)
    val OnPrimaryFixedVariant = Color(0xFF35485E)

    // Secondary (Sage / Mint - harmonized with #EFFFFA)
    val Secondary = Color(0xFF7EC8B5)
    val SecondaryContainer = Color(0xFF234B41)
    val OnSecondary = Color(0xFF0F2E25)
    val OnSecondaryContainer = Color(0xFFC7EDE3)
    val SecondaryFixed = Color(0xFFC7EDE3)
    val SecondaryFixedDim = Color(0xFF7EC8B5)
    val OnSecondaryFixed = Color(0xFF0A231C)
    val OnSecondaryFixedVariant = Color(0xFF225145)

    // Tertiary (Warm Espresso / Mocha - user #4D342F)
    val Tertiary = Color(0xFFDBA499)
    val TertiaryContainer = Color(0xFF4D342F)
    val OnTertiary = Color(0xFF301612)
    val OnTertiaryContainer = Color(0xFFFBECE8)
    val TertiaryFixed = Color(0xFFFBECE8)
    val TertiaryFixedDim = Color(0xFFDBA499)
    val OnTertiaryFixed = Color(0xFF25100D)
    val OnTertiaryFixedVariant = Color(0xFF5A3631)

    // Surfaces & Background (Neutral Dark Graphite #191A1E, Slate Charcoal #2D2A35, Mint White #EFFFFA)
    val Background = Color(0xFF191A1E)
    val OnBackground = Color(0xFFEFFFFA)
    val Surface = Color(0xFF191A1E)
    val SurfaceDim = Color(0xFF131417)
    val SurfaceBright = Color(0xFF383542)
    val SurfaceContainerLowest = Color(0xFF121316)
    val SurfaceContainerLow = Color(0xFF222027)
    val SurfaceContainer = Color(0xFF2D2A35)
    val SurfaceContainerHigh = Color(0xFF373340)
    val SurfaceContainerHighest = Color(0xFF433E4E)
    val SurfaceVariant = Color(0xFF35323E)
    val OnSurface = Color(0xFFEFFFFA)
    val OnSurfaceVariant = Color(0xFFC4C8D2)
    val InverseSurface = Color(0xFFEFFFFA)
    val InverseOnSurface = Color(0xFF191A1E)

    // Functional & Outline
    val Outline = Color(0xFF767B88)
    val OutlineVariant = Color(0xFF3E3C47)
    val Error = Color(0xFFE57373)
    val OnError = Color(0xFF400B0B)
    val ErrorContainer = Color(0xFF6E2222)
    val OnErrorContainer = Color(0xFFFBD4D4)

    // Mesh Ambient Colors
    val MeshNeutralDark = Color(0xFF191A1E)
    val MeshSlateCharcoal = Color(0xFF2D2A35)
    val MeshSteelSlate = Color(0xFF4D627F)
    val MeshEspresso = Color(0xFF4D342F)

    // Aliases to preserve backward compatibility
    val MeshNavy = MeshNeutralDark
    val MeshIndigo = MeshSlateCharcoal
    val MeshCoolBlue = MeshSteelSlate
    val MeshMagenta = MeshEspresso
}
