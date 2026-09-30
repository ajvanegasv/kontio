package dev.ajvanegasv.kontio

import android.app.Application
import dev.ajvanegasv.kontio.data.local.initKontioAndroidContext
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.widget.KontioWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class KontioApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        initKontioAndroidContext(this)
        AppContainer.initializeApp()
        KontioWidgetUpdater.startObserving(this, applicationScope)
    }
}
