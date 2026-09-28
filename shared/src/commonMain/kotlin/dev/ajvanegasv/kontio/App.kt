package dev.ajvanegasv.kontio

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.ThemeMode
import dev.ajvanegasv.kontio.presentation.dashboard.Dashboard
import dev.ajvanegasv.kontio.presentation.designsystem.theme.KontioTheme

@Composable
@Preview
fun App(
    darkTheme: Boolean? = null
) {
    val themeMode by AppContainer.themeConfigStorage.themeModeFlow.collectAsState(
        initial = AppContainer.themeConfigStorage.getThemeMode()
    )
    val systemInDark = isSystemInDarkTheme()
    val isDark = darkTheme ?: when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    KontioTheme(darkTheme = isDark) {
        Dashboard()
    }
}