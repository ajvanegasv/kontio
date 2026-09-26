package dev.ajvanegasv.kontio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.ajvanegasv.kontio.domain.model.Account
import dev.ajvanegasv.kontio.domain.model.AccountType
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: String, // AccountType.name
    val balance: Double,
    val currency: String,
    val colorHex: String,
    val iconName: String,
    val creditLimit: Double?,
    val cutoffDay: Int?,
    val dueDay: Int?,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Account = Account(
        id = id,
        name = name,
        type = try { AccountType.valueOf(type) } catch (e: Exception) { AccountType.SAVINGS },
        balance = balance,
        currency = currency,
        colorHex = colorHex,
        iconName = iconName,
        creditLimit = creditLimit,
        cutoffDay = cutoffDay,
        dueDay = dueDay,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(account: Account): AccountEntity = AccountEntity(
            id = account.id,
            name = account.name,
            type = account.type.name,
            balance = account.balance,
            currency = account.currency,
            colorHex = account.colorHex,
            iconName = account.iconName,
            creditLimit = account.creditLimit,
            cutoffDay = account.cutoffDay,
            dueDay = account.dueDay,
            isArchived = account.isArchived,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt
        )
    }
}
