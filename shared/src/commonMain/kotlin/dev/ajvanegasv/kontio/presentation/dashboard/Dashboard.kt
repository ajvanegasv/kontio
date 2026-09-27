package dev.ajvanegasv.kontio.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ajvanegasv.kontio.presentation.accounts.AccountsScreen
import dev.ajvanegasv.kontio.presentation.accounts.AccountsViewModel
import dev.ajvanegasv.kontio.presentation.accounts.components.AddAccountBottomSheet
import dev.ajvanegasv.kontio.presentation.backup.BackupViewModel
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardBottomNavBar
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardTab
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardTransaction
import dev.ajvanegasv.kontio.presentation.dashboard.components.MainBalanceCard
import dev.ajvanegasv.kontio.presentation.dashboard.components.QuickStatsSection
import dev.ajvanegasv.kontio.presentation.dashboard.components.RecentTransactionsSection
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassBottomSheetContainer
import dev.ajvanegasv.kontio.presentation.designsystem.glass.LocalHazeState
import dev.ajvanegasv.kontio.presentation.designsystem.theme.LocalKontioMeshColors
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.profile.ProfileScreen
import dev.ajvanegasv.kontio.presentation.transactions.TransactionFilter
import dev.ajvanegasv.kontio.presentation.transactions.TransactionsScreen
import dev.ajvanegasv.kontio.presentation.transactions.TransactionViewModel
import dev.ajvanegasv.kontio.presentation.transactions.components.AddTransactionBottomSheet
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

typealias Transaction = DashboardTransaction

/**
 * Pantalla principal de Kontio conectada a la base de datos local y ViewModels reactivos,
 * conservando la estética Glassmorphism y navegación entre Dashboard, Cuentas, y Backups de Google Drive.
 */
@Composable
@Preview
fun Dashboard(
    modifier: Modifier = Modifier,
    dashboardViewModel: DashboardViewModel = viewModel { DashboardViewModel() },
    accountsViewModel: AccountsViewModel = viewModel { AccountsViewModel() },
    transactionViewModel: TransactionViewModel = viewModel { TransactionViewModel() },
    backupViewModel: BackupViewModel = viewModel { BackupViewModel() },
    onNotificationClick: () -> Unit = {},
    onSeeAllTransactionsClick: () -> Unit = {},
    onTransactionClick: (DashboardTransaction) -> Unit = {}
) {
    val hazeState = remember { HazeState() }
    var selectedTab by remember { mutableStateOf(DashboardTab.HOME) }
    var isAddTransactionOpen by remember { mutableStateOf(false) }
    var transactionPendingDelete by remember { mutableStateOf<DashboardTransaction?>(null) }
    var isShowingTransactions by remember { mutableStateOf(false) }
    var transactionsInitialFilter by remember { mutableStateOf(TransactionFilter.ALL) }

    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val accountsState by accountsViewModel.uiState.collectAsState()
    val txCreationState by transactionViewModel.uiState.collectAsState()

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Fondo ambiental Mesh-gradient adaptativo según el tema actual
            DashboardMeshBackground(
                modifier = Modifier.hazeSource(state = hazeState, zIndex = -1f)
            )

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.statusBars,
                bottomBar = {
                    DashboardBottomNavBar(
                        selectedTab = if (isShowingTransactions) DashboardTab.STATS else selectedTab,
                        onTabSelected = { tab ->
                            if (tab == DashboardTab.ADD) {
                                isAddTransactionOpen = true
                            } else if (tab == DashboardTab.STATS) {
                                transactionsInitialFilter = TransactionFilter.ALL
                                isShowingTransactions = true
                                selectedTab = DashboardTab.STATS
                            } else {
                                isShowingTransactions = false
                                selectedTab = tab
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Contenido según la pestaña activa
                    when (selectedTab) {
                        DashboardTab.HOME -> {
                            if (isShowingTransactions) {
                                TransactionsScreen(
                                    onBackClick = { isShowingTransactions = false },
                                    initialFilter = transactionsInitialFilter,
                                    onAddTransactionClick = { type ->
                                        transactionViewModel.setTransactionType(type)
                                        isAddTransactionOpen = true
                                    },
                                    onTransactionClick = { txUiModel ->
                                        onTransactionClick(
                                            DashboardTransaction(
                                                id = txUiModel.id,
                                                title = txUiModel.title,
                                                category = txUiModel.categoryName,
                                                amount = txUiModel.amountFormatted,
                                                isIncome = txUiModel.isIncome,
                                                icon = txUiModel.icon
                                            )
                                        )
                                    },
                                    modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .hazeSource(state = hazeState, zIndex = 0f),
                                    contentPadding = PaddingValues(
                                        top = 16.dp,
                                        bottom = 16.dp,
                                        start = 20.dp,
                                        end = 20.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(32.dp)
                                ) {
                                    // 1. Tarjeta de balance principal consolidado en tiempo real
                                    item {
                                        MainBalanceCard(
                                            balance = dashboardState.balance,
                                            onAddFundsClick = { isAddTransactionOpen = true }
                                        )
                                    }

                                    // 2. Sección de estadísticas rápidas (Ingresos y Gastos del mes)
                                    item {
                                        QuickStatsSection(
                                            incomeAmount = dashboardState.incomeAmount,
                                            expensesAmount = dashboardState.expensesAmount,
                                            onIncomeClick = {
                                                transactionsInitialFilter = TransactionFilter.INCOME
                                                isShowingTransactions = true
                                            },
                                            onExpensesClick = {
                                                transactionsInitialFilter = TransactionFilter.EXPENSE
                                                isShowingTransactions = true
                                            }
                                        )
                                    }

                                    // 3. Sección de transacciones recientes desde Room DB
                                    item {
                                        RecentTransactionsSection(
                                            transactions = dashboardState.transactions,
                                            onSeeAllClick = {
                                                transactionsInitialFilter = TransactionFilter.ALL
                                                isShowingTransactions = true
                                                onSeeAllTransactionsClick()
                                            },
                                            onTransactionClick = onTransactionClick,
                                            onDeleteTransaction = { tx -> transactionPendingDelete = tx }
                                        )
                                    }
                                }
                            }
                        }

                        DashboardTab.CARDS -> {
                            AccountsScreen(
                                viewModel = accountsViewModel,
                                modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f)
                            )
                        }

                        DashboardTab.PROFILE -> {
                            ProfileScreen(
                                backupViewModel = backupViewModel,
                                modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f)
                            )
                        }

                        DashboardTab.STATS -> {
                            TransactionsScreen(
                                onBackClick = {
                                    selectedTab = DashboardTab.HOME
                                    isShowingTransactions = false
                                },
                                initialFilter = transactionsInitialFilter,
                                onAddTransactionClick = { type ->
                                    transactionViewModel.setTransactionType(type)
                                    isAddTransactionOpen = true
                                },
                                onTransactionClick = { txUiModel ->
                                    onTransactionClick(
                                        DashboardTransaction(
                                            id = txUiModel.id,
                                            title = txUiModel.title,
                                            category = txUiModel.categoryName,
                                            amount = txUiModel.amountFormatted,
                                            isIncome = txUiModel.isIncome,
                                            icon = txUiModel.icon
                                        )
                                    )
                                },
                                modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f)
                            )
                        }

                        DashboardTab.ADD -> {
                            // El botón ADD en el navbar abre directamente el modal
                        }
                    }
                }
            }

            // Modal inferior de Registro de Transacción con animación fluida y fondo difuminado
            KontioGlassBottomSheetContainer(
                visible = isAddTransactionOpen,
                onDismissRequest = { isAddTransactionOpen = false }
            ) {
                AddTransactionBottomSheet(
                    state = txCreationState,
                    onTypeSelect = { transactionViewModel.setTransactionType(it) },
                    onNumberPadClick = { transactionViewModel.onNumberPadClick(it) },
                    onBackspaceClick = { transactionViewModel.onBackspaceClick() },
                    onAccountSelect = { transactionViewModel.selectAccount(it) },
                    onCategorySelect = { transactionViewModel.selectCategory(it) },
                    onNoteChange = { transactionViewModel.setNote(it) },
                    onSubmit = {
                        transactionViewModel.submitTransaction(
                            onSuccess = { isAddTransactionOpen = false }
                        )
                    },
                    onDismiss = { isAddTransactionOpen = false }
                )
            }

            // Modal inferior para Agregar Cuenta con animación fluida y fondo difuminado
            KontioGlassBottomSheetContainer(
                visible = accountsState.isAddAccountOpen,
                onDismissRequest = { accountsViewModel.closeAddAccount() }
            ) {
                AddAccountBottomSheet(
                    onDismiss = { accountsViewModel.closeAddAccount() },
                    onSaveAccount = { name, type, balance, creditLimit, colorHex ->
                        accountsViewModel.createAccount(
                            name = name,
                            type = type,
                            initialBalance = balance,
                            creditLimit = creditLimit,
                            colorHex = colorHex
                        )
                    },
                    errorMessage = accountsState.errorMessage
                )
            }

            // Diálogo de confirmación para eliminar transacción
            val txToDelete = transactionPendingDelete
            if (txToDelete != null) {
                AlertDialog(
                    onDismissRequest = { transactionPendingDelete = null },
                    title = {
                        Text(
                            text = "¿Eliminar transacción?",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Text(
                            text = "¿Estás seguro de que deseas eliminar la transacción \"${txToDelete.title}\" (${txToDelete.amount})? El saldo de la cuenta será recalculado automáticamente.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                dashboardViewModel.deleteTransaction(txToDelete.id)
                                transactionPendingDelete = null
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Eliminar", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { transactionPendingDelete = null }
                        ) {
                            Text("Cancelar")
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

/**
 * Fondo ambiental con malla de gradientes radiales adaptada dinámicamente mediante LocalKontioMeshColors.
 */
@Composable
private fun DashboardMeshBackground(
    modifier: Modifier = Modifier
) {
    val meshColors = LocalKontioMeshColors.current

    Box(modifier = modifier.fillMaxSize()) {
        // Orbe superior izquierdo
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            meshColors.topStartOrb,
                            Color.Transparent
                        )
                    )
                )
        )

        // Orbe superior central
        Box(
            modifier = Modifier
                .size(450.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            meshColors.topCenterOrb,
                            Color.Transparent
                        )
                    )
                )
        )

        // Orbe superior derecho
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            meshColors.topEndOrb,
                            Color.Transparent
                        )
                    )
                )
        )

        // Orbe ambiental inferior izquierdo para refracción del cristal
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.BottomStart)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            meshColors.bottomStartOrb,
                            Color.Transparent
                        )
                    )
                )
        )
    }
}