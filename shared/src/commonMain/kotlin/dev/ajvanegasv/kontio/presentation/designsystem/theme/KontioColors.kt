package dev.ajvanegasv.kontio.presentation.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.ajvanegasv.kontio.presentation.dashboard.DashboardColors

/**
 * Paleta de colores Dark fiel a la estética visual existente de Kontio.
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
    surfaceVariant = DashboardColors.SurfaceVariant,
    onSurfaceVariant = DashboardColors.OnSurfaceVariant,
    surfaceContainer = DashboardColors.SurfaceContainer,
    surfaceContainerHigh = DashboardColors.SurfaceContainerHigh,
    surfaceContainerHighest = DashboardColors.SurfaceContainerHighest,
    outline = DashboardColors.Outline,
    outlineVariant = DashboardColors.OutlineVariant,
    error = DashboardColors.Error,
    onError = DashboardColors.OnError
)

/**
 * Paleta de colores Light refinada que preserva la estética glassmorphism con luminiscencia y alto contraste.
 */
val LightColorScheme = lightColorScheme(
    background = Color(0xFFF4F6FB),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0D9488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF4C1D95),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF)
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
    background = Color(0xFF0B1326),
    topStartOrb = Color(0xFF131118).copy(alpha = 0.90f),
    topCenterOrb = Color(0xFF2E3E6B).copy(alpha = 0.55f),
    topEndOrb = Color(0xFF722744).copy(alpha = 0.50f),
    bottomStartOrb = Color(0xFF03C6B2).copy(alpha = 0.15f)
)

val LightMeshColors = KontioMeshColors(
    background = Color(0xFFF4F6FB),
    topStartOrb = Color(0xFFC7D2FE).copy(alpha = 0.55f),
    topCenterOrb = Color(0xFFBAE6FD).copy(alpha = 0.50f),
    topEndOrb = Color(0xFFFBCFE8).copy(alpha = 0.45f),
    bottomStartOrb = Color(0xFFA7F3D0).copy(alpha = 0.40f)
)

val LocalKontioMeshColors = staticCompositionLocalOf { DarkMeshColors }
