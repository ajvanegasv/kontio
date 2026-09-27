package dev.ajvanegasv.kontio.presentation.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.ajvanegasv.kontio.presentation.dashboard.DashboardColors

/**
 * Paleta de colores Dark neutra, elegante y sofisticada construida en torno a la paleta:
 * #191A1E (Fondo grafito oscuro), #2D2A35 (Superficies pizarra carbón),
 * #4D627F (Acento pizarra azulado acero), #4D342F (Café moca cálido),
 * #EFFFFA (Blanco hielo mentolado para tipografía y alto contraste).
 */
val DarkColorScheme = darkColorScheme(
    primary = DashboardColors.Primary,
    onPrimary = DashboardColors.OnPrimary,
    primaryContainer = DashboardColors.PrimaryContainer,
    onPrimaryContainer = DashboardColors.OnPrimaryContainer,
    inversePrimary = DashboardColors.InversePrimary,
    secondary = DashboardColors.Secondary,
    onSecondary = DashboardColors.OnSecondary,
    secondaryContainer = DashboardColors.SecondaryContainer,
    onSecondaryContainer = DashboardColors.OnSecondaryContainer,
    tertiary = DashboardColors.Tertiary,
    onTertiary = DashboardColors.OnTertiary,
    tertiaryContainer = DashboardColors.TertiaryContainer,
    onTertiaryContainer = DashboardColors.OnTertiaryContainer,
    background = DashboardColors.Background,
    onBackground = DashboardColors.OnBackground,
    surface = DashboardColors.Surface,
    onSurface = DashboardColors.OnSurface,
    surfaceDim = DashboardColors.SurfaceDim,
    surfaceBright = DashboardColors.SurfaceBright,
    surfaceVariant = DashboardColors.SurfaceVariant,
    onSurfaceVariant = DashboardColors.OnSurfaceVariant,
    surfaceContainerLowest = DashboardColors.SurfaceContainerLowest,
    surfaceContainerLow = DashboardColors.SurfaceContainerLow,
    surfaceContainer = DashboardColors.SurfaceContainer,
    surfaceContainerHigh = DashboardColors.SurfaceContainerHigh,
    surfaceContainerHighest = DashboardColors.SurfaceContainerHighest,
    inverseSurface = DashboardColors.InverseSurface,
    inverseOnSurface = DashboardColors.InverseOnSurface,
    outline = DashboardColors.Outline,
    outlineVariant = DashboardColors.OutlineVariant,
    error = DashboardColors.Error,
    onError = DashboardColors.OnError,
    errorContainer = DashboardColors.ErrorContainer,
    onErrorContainer = DashboardColors.OnErrorContainer
)

/**
 * Paleta de colores Light refinada y luminosa armonizada con la paleta neutra:
 * Superficies limpias con tintes porcelana (#EFFFFA), acentos primarios pizarra acero (#4D627F),
 * acentos terciarios moca cálido (#4D342F) y tipografía neutral profunda (#191A1E / #2D2A35).
 */
val LightColorScheme = lightColorScheme(
    background = Color(0xFFF4F7F6),
    surface = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF4F2),
    surfaceContainer = Color(0xFFE4EDE9),
    surfaceContainerHigh = Color(0xFFDCE5E1),
    surfaceContainerHighest = Color(0xFFD2DDD8),
    surfaceDim = Color(0xFFE7ECEB),
    surfaceBright = Color(0xFFFFFFFF),
    onBackground = Color(0xFF191A1E),
    onSurface = Color(0xFF191A1E),
    onSurfaceVariant = Color(0xFF4E565E),
    primary = Color(0xFF4D627F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E3F2),
    onPrimaryContainer = Color(0xFF101E2E),
    inversePrimary = Color(0xFF9FB5D2),
    secondary = Color(0xFF2E6B5C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCEEDE3),
    onSecondaryContainer = Color(0xFF0F3229),
    tertiary = Color(0xFF4D342F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEEDCD7),
    onTertiaryContainer = Color(0xFF2E1713),
    outline = Color(0xFF869099),
    outlineVariant = Color(0xFFCBD4D8),
    inverseSurface = Color(0xFF191A1E),
    inverseOnSurface = Color(0xFFEFFFFA),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

/**
 * Configuración de colores para los orbes ambientales que alimentan el fondo Mesh y la refracción del cristal.
 */
@Immutable
data class KontioMeshColors(
    val background: Color,
    val topStartOrb: Color,
    val topCenterOrb: Color,
    val topEndOrb: Color,
    val bottomStartOrb: Color
)

val DarkMeshColors = KontioMeshColors(
    background = Color(0xFF191A1E),
    topStartOrb = Color(0xFF2D2A35).copy(alpha = 0.85f),
    topCenterOrb = Color(0xFF4D627F).copy(alpha = 0.35f),
    topEndOrb = Color(0xFF4D342F).copy(alpha = 0.30f),
    bottomStartOrb = Color(0xFF7EC8B5).copy(alpha = 0.12f)
)

val LightMeshColors = KontioMeshColors(
    background = Color(0xFFF4F7F6),
    topStartOrb = Color(0xFFD3E0EE).copy(alpha = 0.50f),
    topCenterOrb = Color(0xFFE7DDD9).copy(alpha = 0.45f),
    topEndOrb = Color(0xFFD4EDE5).copy(alpha = 0.50f),
    bottomStartOrb = Color(0xFFDFEAE5).copy(alpha = 0.40f)
)

val LocalKontioMeshColors = staticCompositionLocalOf { DarkMeshColors }
