package dev.ajvanegasv.kontio.domain.usecase

import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.repository.TransactionRepository

class DeleteCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(categoryId: String): Result<Unit> {
        return runCatching {
            require(categoryId.isNotBlank()) { "El ID de la categoría no puede estar vacío" }
            val count = transactionRepository.getTransactionsCountByCategory(categoryId)
            if (count > 0) {
                throw IllegalStateException("No se puede eliminar la categoría porque tiene transacciones asociadas")
            }
            categoryRepository.deleteCategory(categoryId)
        }
    }
}
