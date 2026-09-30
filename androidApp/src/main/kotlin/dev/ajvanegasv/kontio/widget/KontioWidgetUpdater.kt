package dev.ajvanegasv.kontio.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dev.ajvanegasv.kontio.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object KontioWidgetUpdater {

    fun startObserving(context: Context, scope: CoroutineScope) {
        val appContext = context.applicationContext

        scope.launch {
            AppContainer.getSavingsCashWidgetDataUseCase().collect {
                try {
                    SavingsCashBalanceWidget().updateAll(appContext)
                } catch (_: Exception) {
                    // Ignorar excepciones si no hay widgets activos en el launcher
                }
            }
        }

        scope.launch {
            AppContainer.getCreditCardWidgetDataUseCase().collect {
                try {
                    CreditCardBalanceWidget().updateAll(appContext)
                } catch (_: Exception) {
                    // Ignorar excepciones si no hay widgets activos en el launcher
                }
            }
        }
    }

    suspend fun updateAllWidgets(context: Context) {
        val appContext = context.applicationContext
        try {
            SavingsCashBalanceWidget().updateAll(appContext)
            CreditCardBalanceWidget().updateAll(appContext)
        } catch (_: Exception) {
        }
    }
}
