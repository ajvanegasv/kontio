package dev.ajvanegasv.kontio.presentation.designsystem.glass

import androidx.compose.runtime.compositionLocalOf
import dev.chrisbanes.haze.HazeState

/**
 * CompositionLocal que provee el [HazeState] activo de la pantalla o scaffold actual.
 * Permite que cualquier componente hijo (tarjetas, barras, modales) aplique efectos
 * esmerilados/blur automáticamente sin necesidad de pasar el estado como parámetro explícito.
 */
val LocalHazeState = compositionLocalOf<HazeState?> { null }
