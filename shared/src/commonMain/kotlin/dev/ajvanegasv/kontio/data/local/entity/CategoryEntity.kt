package dev.ajvanegasv.kontio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.TransactionType
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: String, // TransactionType.name
    val isDefault: Boolean
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        type = try { TransactionType.valueOf(type) } catch (e: Exception) { TransactionType.EXPENSE },
        isDefault = isDefault
    )

    companion object {
        fun fromDomain(category: Category): CategoryEntity = CategoryEntity(
            id = category.id,
            name = category.name,
            iconName = category.iconName,
            colorHex = category.colorHex,
            type = category.type.name,
            isDefault = category.isDefault
        )
    }
}
