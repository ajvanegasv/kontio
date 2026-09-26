package dev.ajvanegasv.kontio.presentation.util

import androidx.compose.ui.graphics.vector.ImageVector
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons

object IconMapper {
    fun getIconForCategory(iconName: String, type: TransactionType): ImageVector {
        return when (iconName) {
            "restaurant", "food" -> DashboardIcons.LocalCafe
            "shopping_bag", "shopping" -> DashboardIcons.ShoppingCart
            "entertainment", "movie" -> DashboardIcons.LiveTv
            "salary", "payments", "income" -> DashboardIcons.AccountBalanceWallet
            "car", "directions_car", "transport" -> DashboardIcons.ShoppingCart
            "home", "housing" -> DashboardIcons.Home
            else -> if (type == TransactionType.INCOME) DashboardIcons.ArrowUpward else DashboardIcons.ArrowDownward
        }
    }
}
