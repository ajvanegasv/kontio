package dev.ajvanegasv.kontio.domain.model

data class SavingsCashWidgetSummary(
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val netMonthlyFlow: Double = 0.0,
    val accountsCount: Int = 0,
    val currency: String = "USD"
)

data class CreditCardWidgetSummary(
    val totalDebt: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val totalCreditLimit: Double = 0.0,
    val availableCredit: Double = 0.0,
    val cardsCount: Int = 0,
    val currency: String = "USD"
)
