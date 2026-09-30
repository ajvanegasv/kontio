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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ajvanegasv.kontio.presentation.accounts.AccountsScreen
import dev.ajvanegasv.kontio.presentation.accounts.AccountsViewModel
import dev.ajvanegasv.kontio.presentation.accounts.components.AddAccountBottomSheet
import dev.ajvanegasv.kontio.presentation.analytics.AnalyticsScreen
import dev.ajvanegasv.kontio.presentation.backup.BackupViewModel
import dev.ajvanegasv.kontio.presentation.budgets.BudgetsScreen
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardBottomNavBar
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardIcons
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardTab
import dev.ajvanegasv.kontio.presentation.dashboard.components.DashboardTransaction
import dev.ajvanegasv.kontio.presentation.dashboard.components.MainBalanceCard
import dev.ajvanegasv.kontio.presentation.dashboard.components.QuickStatsSection
import dev.ajvanegasv.kontio.presentation.dashboard.components.RecentTransactionsSection
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassBottomSheetContainer
import dev.ajvanegasv.kontio.presentation.designsystem.glass.KontioGlassCard
import dev.ajvanegasv.kontio.presentation.designsystem.glass.LocalHazeState
import dev.ajvanegasv.kontio.presentation.designsystem.theme.LocalKontioMeshColors
import dev.ajvanegasv.kontio.domain.model.TransactionType
import dev.ajvanegasv.kontio.presentation.categories.CategoriesScreen
import dev.ajvanegasv.kontio.presentation.more.MoreScreen
import dev.ajvanegasv.kontio.presentation.profile.ProfileScreen
import dev.ajvanegasv.kontio.presentation.transactions.TransactionFilter
import dev.ajvanegasv.kontio.presentation.transactions.TransactionsScreen
import dev.ajvanegasv.kontio.presentation.transactions.TransactionViewModel
import dev.ajvanegasv.kontio.presentation.transactions.components.AddTransactionBottomSheet
import dev.ajvanegasv.kontio.presentation.statement.ImportStatementBottomSheet
import dev.ajvanegasv.kontio.presentation.statement.ImportStatementViewModel
import dev.ajvanegasv.kontio.presentation.transactions.voice.VoiceTransactionViewModel
import dev.ajvanegasv.kontio.presentation.transactions.voice.components.CreateTransactionChoiceBottomSheet
import dev.ajvanegasv.kontio.presentation.transactions.voice.components.VoiceTransactionBottomSheet
import dev.ajvanegasv.kontio.presentation.util.BackHandler
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
    voiceTransactionViewModel: VoiceTransactionViewModel = viewModel { VoiceTransactionViewModel() },
    backupViewModel: BackupViewModel = viewModel { BackupViewModel() },
    importStatementViewModel: ImportStatementViewModel = viewModel { ImportStatementViewModel() },
    onNotificationClick: () -> Unit = {},
    onSeeAllTransactionsClick: () -> Unit = {},
    onTransactionClick: (DashboardTransaction) -> Unit = {}
) {
    val hazeState = remember { HazeState() }
    var selectedTab by remember { mutableStateOf(DashboardTab.HOME) }
    var isCreateOptionsOpen by remember { mutableStateOf(false) }
    var isVoiceTransactionOpen by remember { mutableStateOf(false) }
    var isAddTransactionOpen by remember { mutableStateOf(false) }
    var isImportStatementOpen by remember { mutableStateOf(false) }
    var transactionPendingDelete by remember { mutableStateOf<DashboardTransaction?>(null) }
    var isShowingTransactions by remember { mutableStateOf(false) }
    var isShowingCategories by remember { mutableStateOf(false) }
    var isShowingBudgets by remember { mutableStateOf(false) }
    var transactionsInitialFilter by remember { mutableStateOf(TransactionFilter.ALL) }

    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val accountsState by accountsViewModel.uiState.collectAsState()
    val txCreationState by transactionViewModel.uiState.collectAsState()

    val isAnyModalOpen = isCreateOptionsOpen ||
        isVoiceTransactionOpen ||
        isAddTransactionOpen ||
        accountsState.isAccountFormOpen ||
        isImportStatementOpen ||
        transactionPendingDelete != null

    BackHandler(
        enabled = selectedTab != DashboardTab.HOME &&
            !isShowingTransactions &&
            !isShowingCategories &&
            !isShowingBudgets &&
            !isAnyModalOpen
    ) {
        selectedTab = DashboardTab.HOME
    }

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
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            if (tab == DashboardTab.ADD) {
                                isCreateOptionsOpen = true
                            } else {
                                isShowingTransactions = false
                                isShowingCategories = false
                                isShowingBudgets = false
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
                    val handleDashboardTransactionClick: (DashboardTransaction) -> Unit = { tx ->
                        transactionViewModel.prepareEditTransactionById(tx.id)
                        isAddTransactionOpen = true
                        onTransactionClick(tx)
                    }

                    // Contenido según la pestaña activa
                    when (selectedTab) {
                        DashboardTab.HOME -> {
                            if (isShowingBudgets) {
                                BudgetsScreen(
                                    onBackClick = { isShowingBudgets = false },
                                    modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f)
                                )
                            } else if (isShowingTransactions) {
                                TransactionsScreen(
                                    onBackClick = { isShowingTransactions = false },
                                    initialFilter = transactionsInitialFilter,
                                    onAddTransactionClick = { type ->
                                        transactionViewModel.setTransactionType(type)
                                        isAddTransactionOpen = true
                                    },
                                    onTransactionClick = { txUiModel ->
                                        transactionViewModel.prepareEditTransaction(txUiModel.originalTransaction)
                                        isAddTransactionOpen = true
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
                                    onImportStatementClick = { isImportStatementOpen = true },
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

                                    // 1.5 Botón / Acceso directo a Presupuesto
                                    item {
                                        KontioGlassCard(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isShowingBudgets = true },
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = DashboardIcons.AccountBalanceWallet,
                                                            contentDescription = "Presupuesto",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(14.dp))

                                                    Column {
                                                        Text(
                                                            text = "Presupuesto",
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "Controla tus metas de gasto y registra pagos",
                                                            fontSize = 12.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                                    ) {
                                                        Text(
                                                            text = "Gestionar",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(6.dp))

                                                    Icon(
                                                        imageVector = DashboardIcons.ChevronRight,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
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
                                            onTransactionClick = handleDashboardTransactionClick,
                                            onDeleteTransaction = { tx -> transactionPendingDelete = tx }
                                        )
                                    }
                                }
                            }
                        }

                        DashboardTab.CARDS -> {
                            AccountsScreen(
                                viewModel = accountsViewModel,
                                onAddTransactionForAccount = { accountId, type ->
                                    transactionViewModel.prepareTransaction(type = type, accountId = accountId)
                                    isAddTransactionOpen = true
                                },
                                onEditTransaction = { tx ->
                                    transactionViewModel.prepareEditTransaction(tx)
                                    isAddTransactionOpen = true
                                },
                                modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f)
                            )
                        }

                        DashboardTab.MORE -> {
                            if (isShowingBudgets) {
                                BudgetsScreen(
                                    onBackClick = { isShowingBudgets = false },
                                    modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f)
                                )
                            } else if (isShowingCategories) {
                                CategoriesScreen(
                                    onBackClick = { isShowingCategories = false },
                                    modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f)
                                )
                            } else {
                                MoreScreen(
                                    onNavigateToCategories = { isShowingCategories = true },
                                    onNavigateToBudgets = { isShowingBudgets = true },
                                    backupViewModel = backupViewModel,
                                    onImportStatementClick = { isImportStatementOpen = true },
                                    modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f)
                                )
                            }
                        }

                        DashboardTab.STATS -> {
                            AnalyticsScreen(
                                modifier = Modifier.hazeSource(state = hazeState, zIndex = 0f)
                            )
                        }

                        DashboardTab.ADD -> {
                            // El botón ADD en el navbar abre directamente el modal
                        }
                    }
                }
            }

            // Modal inferior para seleccionar modalidad de creación (Manual vs Voz con IA)
            KontioGlassBottomSheetContainer(
                visible = isCreateOptionsOpen,
                onDismissRequest = { isCreateOptionsOpen = false }
            ) {
                CreateTransactionChoiceBottomSheet(
                    onManualClick = {
                        isCreateOptionsOpen = false
                        val activeAccountId = accountsState.selectedAccountId
                        transactionViewModel.prepareTransaction(
                            type = TransactionType.EXPENSE,
                            accountId = activeAccountId
                        )
                        isAddTransactionOpen = true
                    },
                    onVoiceClick = {
                        isCreateOptionsOpen = false
                        voiceTransactionViewModel.startDictation()
                        isVoiceTransactionOpen = true
                    },
                    onDismiss = { isCreateOptionsOpen = false }
                )
            }

            // Modal inferior para Crear Transacción con Voz e IA
            KontioGlassBottomSheetContainer(
                visible = isVoiceTransactionOpen,
                onDismissRequest = { isVoiceTransactionOpen = false }
            ) {
                VoiceTransactionBottomSheet(
                    viewModel = voiceTransactionViewModel,
                    onDismiss = { isVoiceTransactionOpen = false },
                    onSuccess = {
                        isVoiceTransactionOpen = false
                    }
                )
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
                    onDateSelect = { transactionViewModel.setDate(it) },
                    onNoteChange = { transactionViewModel.setNote(it) },
                    onSubmit = {
                        transactionViewModel.submitTransaction(
                            onSuccess = { isAddTransactionOpen = false }
                        )
                    },
                    onDismiss = { isAddTransactionOpen = false }
                )
            }

            // Modal inferior para Agregar o Editar Cuenta con animación fluida y fondo difuminado
            KontioGlassBottomSheetContainer(
                visible = accountsState.isAccountFormOpen,
                onDismissRequest = { accountsViewModel.closeAccountForm() }
            ) {
                val editingAccount = accountsState.editingAccount
                AddAccountBottomSheet(
                    onDismiss = { accountsViewModel.closeAccountForm() },
                    accountToEdit = editingAccount,
                    onSaveAccount = { name, type, balance, creditLimit, colorHex, cutoffDay, dueDay ->
                        if (editingAccount != null) {
                            accountsViewModel.updateAccount(
                                editingAccount.copy(
                                    name = name,
                                    type = type,
                                    balance = balance,
                                    creditLimit = creditLimit,
                                    colorHex = colorHex,
                                    cutoffDay = cutoffDay,
                                    dueDay = dueDay
                                )
                            )
                        } else {
                            accountsViewModel.createAccount(
                                name = name,
                                type = type,
                                initialBalance = balance,
                                creditLimit = creditLimit,
                                colorHex = colorHex,
                                cutoffDay = cutoffDay,
                                dueDay = dueDay
                            )
                        }
                    },
                    errorMessage = accountsState.errorMessage
                )
            }

            // Modal inferior para Importar Extracto Bancario con IA
            KontioGlassBottomSheetContainer(
                visible = isImportStatementOpen,
                onDismissRequest = { isImportStatementOpen = false }
            ) {
                ImportStatementBottomSheet(
                    viewModel = importStatementViewModel,
                    onDismiss = { isImportStatementOpen = false },
                    onSuccess = {
                        isImportStatementOpen = false
                    }
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