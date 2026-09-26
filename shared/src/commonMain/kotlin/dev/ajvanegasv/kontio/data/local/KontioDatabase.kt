package dev.ajvanegasv.kontio.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import dev.ajvanegasv.kontio.data.local.dao.AccountDao
import dev.ajvanegasv.kontio.data.local.dao.CategoryDao
import dev.ajvanegasv.kontio.data.local.dao.TransactionDao
import dev.ajvanegasv.kontio.data.local.entity.AccountEntity
import dev.ajvanegasv.kontio.data.local.entity.CategoryEntity
import dev.ajvanegasv.kontio.data.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@ConstructedBy(KontioDatabaseConstructor::class)
abstract class KontioDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object KontioDatabaseConstructor : RoomDatabaseConstructor<KontioDatabase> {
    override fun initialize(): KontioDatabase
}
