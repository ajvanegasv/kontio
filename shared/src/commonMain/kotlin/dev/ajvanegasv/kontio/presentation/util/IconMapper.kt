package dev.ajvanegasv.kontio.presentation.util

import androidx.compose.ui.graphics.vector.ImageVector
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons

object IconMapper {
    fun getIconForCategory(iconName: String, type: TransactionType): ImageVector {
        return when (iconName.lowercase()) {
            "restaurant", "food" -> DashboardIcons.LocalCafe
            "shopping_bag", "shopping" -> DashboardIcons.ShoppingCart
            "entertainment", "movie" -> DashboardIcons.LiveTv
            "salary", "payments", "income" -> DashboardIcons.AccountBalanceWallet
            "car", "directions_car", "transport" -> DashboardIcons.DirectionsCar
            "home", "housing" -> DashboardIcons.Home
            "medical_services", "health" -> DashboardIcons.MedicalServices
            "school", "education" -> DashboardIcons.School
            "work", "freelance" -> DashboardIcons.Work
            "card_giftcard", "gifts" -> DashboardIcons.CardGiftcard
            "trending_up", "investments" -> DashboardIcons.TrendingUp
            "pets" -> DashboardIcons.Pets
            "fitness", "gym" -> DashboardIcons.FitnessCenter
            "flight", "travel" -> DashboardIcons.Flight
            "settings" -> DashboardIcons.Settings
            "more_horiz" -> DashboardIcons.MoreHoriz
            "category" -> DashboardIcons.Category
            else -> if (type == TransactionType.INCOME) DashboardIcons.ArrowUpward else DashboardIcons.ArrowDownward
        }
    }

    fun getSelectableIcons(): List<Pair<String, ImageVector>> {
        return listOf(
            "restaurant" to DashboardIcons.LocalCafe,
            "shopping_bag" to DashboardIcons.ShoppingCart,
            "entertainment" to DashboardIcons.LiveTv,
            "salary" to DashboardIcons.AccountBalanceWallet,
            "directions_car" to DashboardIcons.DirectionsCar,
            "home" to DashboardIcons.Home,
            "medical_services" to DashboardIcons.MedicalServices,
            "school" to DashboardIcons.School,
            "work" to DashboardIcons.Work,
            "card_giftcard" to DashboardIcons.CardGiftcard,
            "trending_up" to DashboardIcons.TrendingUp,
            "pets" to DashboardIcons.Pets,
            "fitness" to DashboardIcons.FitnessCenter,
            "flight" to DashboardIcons.Flight,
            "category" to DashboardIcons.Category,
            "more_horiz" to DashboardIcons.MoreHoriz
        )
    }
}
