package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = FinanceRepository(
            db.transactionDao(),
            db.budgetDao(),
            db.syncSettingsDao()
        )
        // Check and generate high-fidelity demo data if database is brand new
        viewModelScope.launch {
            repository.checkForDemoData()
        }
    }

    // Custom dark mode toggle state
    private val _isDarkMode = MutableStateFlow(true) // defaults to premium dark theme!
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // Active tracking month - defaults to "2026-05" which contains the rich datasets
    private val _activeMonth = MutableStateFlow("2026-05")
    val activeMonth: StateFlow<String> = _activeMonth.asStateFlow()

    fun setActiveMonth(month: String) {
        _activeMonth.value = month
    }

    // Sheet Sync Settings
    val syncSettings: StateFlow<SyncSettings> = repository.syncSettings
        .map { it ?: SyncSettings() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncSettings()
        )

    // All available tracking months found in transactions
    val availableMonths: StateFlow<List<String>> = repository.allItemsFlow()
        .map { list ->
            // extract all distinct YYYY-MM values from dates
            list.mapNotNull {
                if (it.date.length >= 7) it.date.substring(0, 7) else null
            }.distinct().sortedDescending()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("2026-05", "2026-04", "2026-03")
        )

    // Raw/full transactions list for the active month
    val activeMonthTransactions: StateFlow<List<Transaction>> = _activeMonth
        .flatMapLatest { month -> repository.getTransactionsByMonth(month) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filter, Search details
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    // Filtered transactions computed reactively
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        activeMonthTransactions,
        _searchQuery,
        _selectedCategoryFilter
    ) { txList, query, catFilter ->
        txList.filter { tx ->
            val matchQuery = query.isBlank() || tx.name.contains(query, ignoreCase = true) || tx.category.contains(query, ignoreCase = true)
            val matchCat = catFilter == null || tx.category.equals(catFilter, ignoreCase = true)
            matchQuery && matchCat
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current Monthly Budget
    val monthlyBudget: StateFlow<MonthlyBudget> = _activeMonth
        .flatMapLatest { month ->
            repository.getMonthlyBudget(month).map { it ?: MonthlyBudget(month, 3500.0) } // Standard default
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MonthlyBudget("2026-05", 3500.0)
        )

    // Category budgets
    val categoryBudgets: StateFlow<List<CategoryBudget>> = _activeMonth
        .flatMapLatest { month -> repository.getCategoryBudgets(month) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Main Calculations (Analytics) State
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        activeMonthTransactions,
        monthlyBudget,
        categoryBudgets,
        _activeMonth
    ) { txs, mainBudget, catBudgets, month ->
        calculateMetrics(txs, mainBudget, catBudgets, month)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardMetrics()
    )

    // Historic totals for graph representation
    val historicalMetrics: StateFlow<Map<String, MonthSummary>> = repository.allTransactions
        .map { allTxs ->
            // Group transactions by month
            val grouped = allTxs.groupBy { if (it.date.length >= 7) it.date.substring(0, 7) else "unknown" }
            grouped.mapValues { (_, txs) ->
                val expenses = txs.filter { it.isExpense }.sumOf { it.amount }
                val income = txs.filter { !it.isExpense }.sumOf { it.amount }
                MonthSummary(expenses = expenses, income = income)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // Actions
    fun addTransaction(name: String, category: String, amount: Double, isExpense: Boolean, date: String) {
        viewModelScope.launch {
            val dateStr = if (date.isBlank()) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            } else date

            repository.insertTransaction(
                Transaction(
                    name = name,
                    category = category,
                    amount = amount,
                    isExpense = isExpense,
                    date = dateStr,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun setOverallMonthlyBudget(limit: Double) {
        viewModelScope.launch {
            repository.saveMonthlyBudget(MonthlyBudget(_activeMonth.value, limit))
        }
    }

    fun saveCategoryLimit(category: String, limit: Double) {
        viewModelScope.launch {
            repository.saveCategoryBudget(
                CategoryBudget(
                    month = _activeMonth.value,
                    category = category,
                    limit = limit
                )
            )
        }
    }

    fun deleteCategoryLimit(id: Int) {
        viewModelScope.launch {
            repository.deleteCategoryBudget(id)
        }
    }

    // Google Sheets Sync
    private val _syncingState = MutableStateFlow(false)
    val syncingState: StateFlow<Boolean> = _syncingState.asStateFlow()

    private val _syncResult = MutableStateFlow<Result<Unit>?>(null)
    val syncResult: StateFlow<Result<Unit>?> = _syncResult.asStateFlow()

    fun resetSyncResult() {
        _syncResult.value = null
    }

    fun performGoogleSheetSync(url: String, tabName: String) {
        viewModelScope.launch {
            _syncingState.value = true
            val result = repository.syncWithGoogleSheet(url, tabName)
            _syncResult.value = result
            _syncingState.value = false
        }
    }

    fun populateRealisticMockForEmptyUrl() {
        viewModelScope.launch {
            repository.clearAllTransactions()
            repository.checkForDemoData()
            // Reset sync status to CONNECTED representing successfully connected local simulated set
            repository.updateSyncSettings(
                SyncSettings(
                    spreadsheetUrl = "https://docs.google.com/spreadsheets/d/demo_personal_finance/edit",
                    tabName = "Dashboard",
                    lastSyncedTime = System.currentTimeMillis(),
                    connectionStatus = ConnectionStatus.CONNECTED,
                    errorMessage = null
                )
            )
        }
    }

    private fun calculateMetrics(
        txs: List<Transaction>,
        budget: MonthlyBudget,
        categoryLimits: List<CategoryBudget>,
        month: String
    ): DashboardMetrics {
        val expenses = txs.filter { it.isExpense }
        val incomeList = txs.filter { !it.isExpense }

        val totalExpensesSum = expenses.sumOf { it.amount }
        val totalIncomeSum = incomeList.sumOf { it.amount }

        val categorySums = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val topCategory = categorySums.maxByOrNull { it.value }?.key ?: "None"

        val avgTransactionAmount = if (expenses.isNotEmpty()) {
            totalExpensesSum / expenses.size
        } else 0.0

        // Remaining limit / percent
        val remainingBudget = budget.limit - totalExpensesSum
        val budgetUsedPercentage = if (budget.limit > 0) {
            (totalExpensesSum / budget.limit) * 100
        } else 0.0

        // Velocity & prediction:
        // How many days are parsed in this selected calendar month
        val cal = Calendar.getInstance()
        val currentYearMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
        val isCurrentMonth = month == currentYearMonthStr

        val totalDays = if (isCurrentMonth) {
            cal.get(Calendar.DAY_OF_MONTH) // elapsed days in this current month for real speed
        } else {
            // past month days
            val parts = month.split("-")
            val yr = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val mn = parts.getOrNull(1)?.toIntOrNull() ?: 5
            val queryCal = Calendar.getInstance()
            queryCal.set(yr, mn - 1, 1)
            queryCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        val parsedDays = if (totalDays <= 0) 1 else totalDays
        val velocity = totalExpensesSum / parsedDays // average expense per day
        val totalMonthDaysMax = if (isCurrentMonth) {
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        } else {
            totalDays
        }
        val predictedTotalExpenses = velocity * totalMonthDaysMax

        return DashboardMetrics(
            totalSpend = totalExpensesSum,
            totalIncome = totalIncomeSum,
            avgTransactionAmount = avgTransactionAmount,
            topCategory = topCategory,
            remainingBudget = remainingBudget,
            percentageUsed = budgetUsedPercentage,
            spendingVelocity = velocity, // Spend rate per day
            predictedExpenses = predictedTotalExpenses,
            categoryDistribution = categorySums
        )
    }
}

// Data Classes for Analytics
data class DashboardMetrics(
    val totalSpend: Double = 0.0,
    val totalIncome: Double = 0.0,
    val avgTransactionAmount: Double = 0.0,
    val topCategory: String = "None",
    val remainingBudget: Double = 0.0,
    val percentageUsed: Double = 0.0,
    val spendingVelocity: Double = 0.0,
    val predictedExpenses: Double = 0.0,
    val categoryDistribution: Map<String, Double> = emptyMap()
)

data class MonthSummary(
    val expenses: Double = 0.0,
    val income: Double = 0.0
)

// Simple mapping helper for all transactions
private fun FinanceRepository.allItemsFlow() = allTransactions
