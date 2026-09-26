package dev.ajvanegasv.kontio.domain.repository

import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(type: TransactionType? = null): Flow<List<Category>>
    fun getCategoryById(id: String): Flow<Category?>
    suspend fun insertCategory(category: Category)
    suspend fun seedDefaultCategoriesIfEmpty()
    suspend fun getCategoriesCount(): Int
}
