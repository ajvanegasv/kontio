package dev.ajvanegasv.kontio

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.ajvanegasv.kontio.presentation.dashboard.Dashboard
import dev.ajvanegasv.kontio.presentation.dashboard.DashboardColors

private val DarkColorScheme = darkColorScheme(
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
    error = DashboardColors.Error,
    onError = DashboardColors.OnError
)

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = DarkColorScheme) {
        Dashboard()
    }
}