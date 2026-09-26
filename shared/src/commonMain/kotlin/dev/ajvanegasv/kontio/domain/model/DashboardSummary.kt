package dev.ajvanegasv.kontio.domain.model

data class DashboardSummary(
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val accounts: List<Account> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val currency: String = "USD"
)
