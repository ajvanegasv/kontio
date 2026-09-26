package dev.ajvanegasv.kontio

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.ajvanegasv.kontio.presentation.dashboard.Dashboard
import dev.ajvanegasv.kontio.presentation.designsystem.theme.KontioTheme

@Composable
@Preview
fun App(
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    KontioTheme(darkTheme = darkTheme) {
        Dashboard()
    }
}