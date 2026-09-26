package dev.ajvanegasv.kontio.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: TransactionType,
    val isDefault: Boolean = false
) {
    companion object {
        fun defaultCategories(): List<Category> = listOf(
            // Gastos (Expenses)
            Category(id = "cat_food", name = "Alimentación", iconName = "restaurant", colorHex = "#F59E0B", type = TransactionType.EXPENSE, isDefault = true),
            Category(id = "cat_transport", name = "Transporte", iconName = "directions_car", colorHex = "#3B82F6", type = TransactionType.EXPENSE, isDefault = true),
            Category(id = "cat_housing", name = "Vivienda & Servicios", iconName = "home", colorHex = "#8B5CF6", type = TransactionType.EXPENSE, isDefault = true),
            Category(id = "cat_entertainment", name = "Ocio & Salidas", iconName = "movie", colorHex = "#EC4899", type = TransactionType.EXPENSE, isDefault = true),
            Category(id = "cat_shopping", name = "Compras", iconName = "shopping_bag", colorHex = "#10B981", type = TransactionType.EXPENSE, isDefault = true),
            Category(id = "cat_health", name = "Salud & Bienestar", iconName = "medical_services", colorHex = "#EF4444", type = TransactionType.EXPENSE, isDefault = true),
            Category(id = "cat_education", name = "Educación", iconName = "school", colorHex = "#6366F1", type = TransactionType.EXPENSE, isDefault = true),
            Category(id = "cat_other_exp", name = "Otros Gastos", iconName = "more_horiz", colorHex = "#6B7280", type = TransactionType.EXPENSE, isDefault = true),

            // Ingresos (Incomes)
            Category(id = "cat_salary", name = "Salario / Sueldo", iconName = "payments", colorHex = "#10B981", type = TransactionType.INCOME, isDefault = true),
            Category(id = "cat_freelance", name = "Freelance / Negocio", iconName = "work", colorHex = "#06B6D4", type = TransactionType.INCOME, isDefault = true),
            Category(id = "cat_investments", name = "Inversiones", iconName = "trending_up", colorHex = "#8B5CF6", type = TransactionType.INCOME, isDefault = true),
            Category(id = "cat_gifts", name = "Premios & Regalos", iconName = "card_giftcard", colorHex = "#F59E0B", type = TransactionType.INCOME, isDefault = true),
            Category(id = "cat_other_inc", name = "Otros Ingresos", iconName = "account_balance_wallet", colorHex = "#6B7280", type = TransactionType.INCOME, isDefault = true)
        )
    }
}
