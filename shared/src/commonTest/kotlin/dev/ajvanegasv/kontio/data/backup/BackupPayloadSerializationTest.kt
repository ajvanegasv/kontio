package dev.ajvanegasv.kontio.data.backup

import dev.ajvanegasv.kontio.data.local.entity.AccountEntity
import dev.ajvanegasv.kontio.data.local.entity.CategoryEntity
import dev.ajvanegasv.kontio.data.local.entity.TransactionEntity
import dev.ajvanegasv.kontio.domain.model.BackupMetadata
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupPayloadSerializationTest {

    @Test
    fun testSerializationAndDeserializationFidelity() {
        val metadata = BackupMetadata(
            backupId = "test_backup_123",
            timestamp = 1727370000000L,
            sizeBytes = 1024L,
            accountCount = 1,
            transactionCount = 1,
            categoryCount = 1
        )

        val accounts = listOf(
            AccountEntity(
                id = "acc_test",
                name = "Ahorros Test",
                type = "SAVINGS",
                balance = 1500.50,
                currency = "USD",
                colorHex = "#3B82F6",
                iconName = "account_balance",
                creditLimit = null,
                cutoffDay = null,
                dueDay = null,
                isArchived = false,
                createdAt = 1727370000000L,
                updatedAt = 1727370000000L
            )
        )

        val categories = listOf(
            CategoryEntity(
                id = "cat_food",
                name = "Alimentación",
                iconName = "restaurant",
                colorHex = "#F59E0B",
                type = "EXPENSE",
                isDefault = true
            )
        )

        val transactions = listOf(
            TransactionEntity(
                id = "tx_123",
                accountId = "acc_test",
                categoryId = "cat_food",
                type = "EXPENSE",
                amount = 45.0,
                currency = "USD",
                timestamp = 1727370000000L,
                note = "Cena fin de semana",
                targetAccountId = null,
                aiMetadataJson = null
            )
        )

        val payload = KontioBackupPayload(
            metadata = metadata,
            accounts = accounts,
            categories = categories,
            transactions = transactions
        )

        val bytes = payload.toBytes()
        val deserialized = KontioBackupPayload.fromBytes(bytes)

        assertEquals(payload.metadata.backupId, deserialized.metadata.backupId)
        assertEquals(payload.accounts.size, deserialized.accounts.size)
        assertEquals(payload.accounts[0].name, deserialized.accounts[0].name)
        assertEquals(payload.categories.size, deserialized.categories.size)
        assertEquals(payload.transactions.size, deserialized.transactions.size)
        assertEquals(payload.transactions[0].amount, deserialized.transactions[0].amount)
    }
}
