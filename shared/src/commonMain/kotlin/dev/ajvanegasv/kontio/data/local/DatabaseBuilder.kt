package dev.ajvanegasv.kontio.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE transactions ADD COLUMN budgetId TEXT DEFAULT NULL")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_budgetId ON transactions (budgetId)")
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budgets` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `name` TEXT NOT NULL,
                `categoryId` TEXT NOT NULL,
                `limitAmount` REAL NOT NULL,
                `period` TEXT NOT NULL,
                `currency` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_categoryId ON budgets (categoryId)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budget_transactions` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `budgetId` TEXT NOT NULL,
                `transactionId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_budget_transactions_budgetId ON budget_transactions (budgetId)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS index_budget_transactions_transactionId ON budget_transactions (transactionId)")
        connection.execSQL(
            """
            INSERT OR IGNORE INTO `budget_transactions` (`id`, `budgetId`, `transactionId`, `createdAt`)
            SELECT 'btx_' || id, budgetId, id, timestamp FROM `transactions` WHERE budgetId IS NOT NULL
            """.trimIndent()
        )
    }
}

expect fun getDatabaseBuilder(): RoomDatabase.Builder<KontioDatabase>

fun createRoomDatabase(
    builder: RoomDatabase.Builder<KontioDatabase> = getDatabaseBuilder()
): KontioDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
