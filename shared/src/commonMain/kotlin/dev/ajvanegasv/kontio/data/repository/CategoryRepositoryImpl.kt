package dev.ajvanegasv.kontio.data.repository

import dev.ajvanegasv.kontio.data.local.dao.CategoryDao
import dev.ajvanegasv.kontio.data.local.entity.CategoryEntity
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getCategories(type: TransactionType?): Flow<List<Category>> {
        return if (type != null) {
            categoryDao.getCategoriesByType(type.name).map { list -> list.map { it.toDomain() } }
        } else {
            categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }
        }
    }

    override fun getCategoryById(id: String): Flow<Category?> {
        return categoryDao.getCategoryById(id).map { it?.toDomain() }
    }

    override suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(CategoryEntity.fromDomain(category))
    }

    override suspend fun deleteCategory(id: String) {
        categoryDao.deleteCategoryById(id)
    }

    override suspend fun seedDefaultCategoriesIfEmpty() {
        val count = categoryDao.getCategoriesCount()
        if (count == 0) {
            val defaultEntities = Category.defaultCategories().map { CategoryEntity.fromDomain(it) }
            categoryDao.insertCategories(defaultEntities)
        }
    }

    override suspend fun getCategoriesCount(): Int {
        return categoryDao.getCategoriesCount()
    }
}
