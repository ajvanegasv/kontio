package dev.ajvanegasv.kontio.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

private var applicationContext: Context? = null

fun initKontioAndroidContext(context: Context) {
    applicationContext = context.applicationContext
}

fun getKontioAndroidContext(): Context {
    return requireNotNull(applicationContext) {
        "initKontioAndroidContext must be called in MainActivity or Application before accessing database"
    }
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<KontioDatabase> {
    val context = getKontioAndroidContext()
    val dbFile = context.getDatabasePath("kontio.db")
    return Room.databaseBuilder<KontioDatabase>(
        context = context,
        name = dbFile.absolutePath
    )
}
