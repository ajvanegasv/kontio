package dev.ajvanegasv.kontio

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import dev.ajvanegasv.kontio.presentation.dashboard.Dashboard

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),
    secondary = Color(0xFF10B981),
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surfaceContainer = Color(0xFF21262D)
)

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = DarkColorScheme) {
        Dashboard()
    }
}