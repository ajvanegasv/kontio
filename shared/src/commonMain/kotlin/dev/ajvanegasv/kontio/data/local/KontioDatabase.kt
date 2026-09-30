package dev.ajvanegasv.kontio.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import dev.ajvanegasv.kontio.data.local.dao.AccountDao
import dev.ajvanegasv.kontio.data.local.dao.BudgetDao
import dev.ajvanegasv.kontio.data.local.dao.BudgetTransactionDao
import dev.ajvanegasv.kontio.data.local.dao.CategoryDao
import dev.ajvanegasv.kontio.data.local.dao.TransactionDao
import dev.ajvanegasv.kontio.data.local.entity.AccountEntity
import dev.ajvanegasv.kontio.data.local.entity.BudgetEntity
import dev.ajvanegasv.kontio.data.local.entity.BudgetTransactionEntity
import dev.ajvanegasv.kontio.data.local.entity.CategoryEntity
import dev.ajvanegasv.kontio.data.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        BudgetTransactionEntity::class
    ],
    version = 3,
    exportSchema = false
)
@ConstructedBy(KontioDatabaseConstructor::class)
abstract class KontioDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun budgetTransactionDao(): BudgetTransactionDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object KontioDatabaseConstructor : RoomDatabaseConstructor<KontioDatabase> {
    override fun initialize(): KontioDatabase
}
