package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import kotlinx.datetime.Clock
import kotlin.random.Random

class CreateCategoryUseCase(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(
        name: String,
        iconName: String,
        colorHex: String,
        type: TransactionType
    ): Result<Category> {
        return runCatching {
            val trimmedName = name.trim()
            require(trimmedName.isNotBlank()) { "El nombre de la categoría no puede estar vacío" }

            val now = Clock.System.now().toEpochMilliseconds()
            val randomSuffix = Random.nextInt(100, 1000)
            val id = "cat_${now}_$randomSuffix"

            val category = Category(
                id = id,
                name = trimmedName,
                iconName = iconName.ifBlank { "category" },
                colorHex = colorHex.ifBlank { "#3B82F6" },
                type = type,
                isDefault = false
            )

            categoryRepository.insertCategory(category)
            category
        }
    }

    suspend operator fun invoke(category: Category): Result<Unit> {
        return runCatching {
            require(category.name.trim().isNotBlank()) { "El nombre de la categoría no puede estar vacío" }
            categoryRepository.insertCategory(category)
        }
    }
}
