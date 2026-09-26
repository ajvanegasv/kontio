package dev.ajvanegasv.kontio.domain.model

import kotlinx.serialization.Serializable

enum class AccountType {
    SAVINGS,
    CHECKING,
    CREDIT_CARD,
    CASH,
    DIGITAL_WALLET
}

@Serializable
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val currency: String = "USD",
    val colorHex: String = "#3B82F6",
    val iconName: String = "account_balance",
    val creditLimit: Double? = null,
    val cutoffDay: Int? = null,
    val dueDay: Int? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val availableCredit: Double?
        get() = if (type == AccountType.CREDIT_CARD && creditLimit != null) {
            (creditLimit - balance).coerceAtLeast(0.0)
        } else null
}
