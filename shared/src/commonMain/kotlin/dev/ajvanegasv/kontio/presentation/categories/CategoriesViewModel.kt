package dev.ajvanegasv.kontio.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ajvanegasv.kontio.di.AppContainer
import dev.ajvanegasv.kontio.domain.model.Category
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.domain.repository.CategoryRepository
import dev.ajvanegasv.kontio.domain.usecase.CreateCategoryUseCase
import dev.ajvanegasv.kontio.domain.usecase.DeleteCategoryUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CategoryFilter {
    ALL,
    EXPENSE,
    INCOME
}

data class CategoriesUiState(
    val allCategories: List<Category> = emptyList(),
    val filteredCategories: List<Category> = emptyList(),
    val filter: CategoryFilter = CategoryFilter.ALL,
    val isAddCategoryOpen: Boolean = false,
    val categoryPendingDelete: Category? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class CategoriesViewModel(
    private val categoryRepository: CategoryRepository = AppContainer.categoryRepository,
    private val createCategoryUseCase: CreateCategoryUseCase = AppContainer.createCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase = AppContainer.deleteCategoryUseCase,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val scope = coroutineScope ?: viewModelScope

    private val _filter = MutableStateFlow(CategoryFilter.ALL)
    private val _isAddCategoryOpen = MutableStateFlow(false)
    private val _categoryPendingDelete = MutableStateFlow<Category?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CategoriesUiState> = combine(
        categoryRepository.getCategories(),
        _filter,
        _isAddCategoryOpen,
        _categoryPendingDelete,
        _errorMessage
    ) { categories, filter, isAddOpen, pendingDelete, error ->
        val filtered = when (filter) {
            CategoryFilter.ALL -> categories
            CategoryFilter.EXPENSE -> categories.filter { it.type == TransactionType.EXPENSE }
            CategoryFilter.INCOME -> categories.filter { it.type == TransactionType.INCOME }
        }
        CategoriesUiState(
            allCategories = categories,
            filteredCategories = filtered,
            filter = filter,
            isAddCategoryOpen = isAddOpen,
            categoryPendingDelete = pendingDelete,
            isLoading = false,
            errorMessage = error
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState(isLoading = true)
    )

    fun setFilter(filter: CategoryFilter) {
        _filter.value = filter
    }

    fun openAddCategory() {
        _isAddCategoryOpen.value = true
        _errorMessage.value = null
    }

    fun closeAddCategory() {
        _isAddCategoryOpen.value = false
        _errorMessage.value = null
    }

    fun requestDeleteCategory(category: Category) {
        _categoryPendingDelete.value = category
        _errorMessage.value = null
    }

    fun cancelDeleteCategory() {
        _categoryPendingDelete.value = null
    }

    fun confirmDeleteCategory() {
        val category = _categoryPendingDelete.value ?: return
        scope.launch {
            val result = deleteCategoryUseCase(category.id)
            if (result.isSuccess) {
                _categoryPendingDelete.value = null
                _errorMessage.value = null
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al eliminar la categoría"
                _categoryPendingDelete.value = null
            }
        }
    }

    fun createCategory(
        name: String,
        iconName: String,
        colorHex: String,
        type: TransactionType,
        onSuccess: () -> Unit = {}
    ) {
        scope.launch {
            val result = createCategoryUseCase(name, iconName, colorHex, type)
            if (result.isSuccess) {
                _isAddCategoryOpen.value = false
                _errorMessage.value = null
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al crear la categoría"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
