package dev.ajvanegasv.kontio.data.backup

import dev.ajvanegasv.kontio.data.local.entity.AccountEntity
import dev.ajvanegasv.kontio.data.local.entity.BudgetEntity
import dev.ajvanegasv.kontio.data.local.entity.CategoryEntity
import dev.ajvanegasv.kontio.data.local.entity.TransactionEntity
import dev.ajvanegasv.kontio.domain.model.BackupMetadata
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class KontioBackupPayload(
    val metadata: BackupMetadata,
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity> = emptyList()
) {
    fun toBytes(): ByteArray {
        val jsonString = json.encodeToString(serializer(), this)
        return jsonString.encodeToByteArray()
    }

    companion object {
        private val json = Json {
            prettyPrint = false
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun fromBytes(bytes: ByteArray): KontioBackupPayload {
            val jsonString = bytes.decodeToString()
            return json.decodeFromString(serializer(), jsonString)
        }
    }
}
