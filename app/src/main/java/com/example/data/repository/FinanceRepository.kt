package com.example.data.repository

import android.util.Log
import com.example.data.dao.BudgetDao
import com.example.data.dao.SyncSettingsDao
import com.example.data.dao.TransactionDao
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val syncSettingsDao: SyncSettingsDao
) {
    private val client = OkHttpClient()

    // Flows
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val syncSettings: Flow<SyncSettings?> = syncSettingsDao.getSettingsFlow()
    val allMonthlyBudgets: Flow<List<MonthlyBudget>> = budgetDao.getAllMonthlyBudgets()

    fun getTransactionsByMonth(month: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByMonth(month)
    }

    fun getMonthlyBudget(month: String): Flow<MonthlyBudget?> {
        return budgetDao.getMonthlyBudget(month)
    }

    fun getCategoryBudgets(month: String): Flow<List<CategoryBudget>> {
        return budgetDao.getCategoryBudgets(month)
    }

    // Insert / Writes
    suspend fun insertTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun clearAllTransactions() = withContext(Dispatchers.IO) {
        transactionDao.clearAllTransactions()
    }

    suspend fun saveMonthlyBudget(budget: MonthlyBudget) = withContext(Dispatchers.IO) {
        budgetDao.insertMonthlyBudget(budget)
    }

    suspend fun saveCategoryBudget(categoryBudget: CategoryBudget) = withContext(Dispatchers.IO) {
        budgetDao.insertCategoryBudget(categoryBudget)
    }

    suspend fun deleteCategoryBudget(id: Int) = withContext(Dispatchers.IO) {
        budgetDao.deleteCategoryBudget(id)
    }

    suspend fun updateSyncSettings(settings: SyncSettings) = withContext(Dispatchers.IO) {
        syncSettingsDao.insertOrUpdateSettings(settings)
    }

    /**
     * Initializer: Check if empty and populate realistic finance data
     */
    suspend fun checkForDemoData() = withContext(Dispatchers.IO) {
        val currentTx = transactionDao.getAllTransactions().firstOrNull()
        if (currentTx.isNullOrEmpty()) {
            populateDemoData()
        }
    }

    private suspend fun populateDemoData() {
        Log.d("FinanceRepository", "Populating premium Demo Data for dashboard styling...")
        // Base budgets
        val months = listOf("2026-05", "2026-04", "2026-03")
        for (month in months) {
            budgetDao.insertMonthlyBudget(MonthlyBudget(month, 4500.0))
            budgetDao.insertCategoryBudget(CategoryBudget(month = month, category = "Food & Dining", limit = 600.0))
            budgetDao.insertCategoryBudget(CategoryBudget(month = month, category = "Rent & Bills", limit = 1800.0))
            budgetDao.insertCategoryBudget(CategoryBudget(month = month, category = "Shopping", limit = 800.0))
            budgetDao.insertCategoryBudget(CategoryBudget(month = month, category = "Entertainment", limit = 500.0))
            budgetDao.insertCategoryBudget(CategoryBudget(month = month, category = "Transport", limit = 400.0))
        }

        // Transactions pool for 3 months
        val demoTransactions = listOf(
            // May 2026
            Transaction(name = "Salary Paycheck", category = "Salary", amount = 6500.0, isExpense = false, date = "2026-05-01"),
            Transaction(name = "Highrise Apartments Rent", category = "Rent & Bills", amount = 1500.0, isExpense = true, date = "2026-05-01"),
            Transaction(name = "Electricity Grid Corp", category = "Rent & Bills", amount = 145.50, isExpense = true, date = "2026-05-03"),
            Transaction(name = "Whole Foods Organic Market", category = "Food & Dining", amount = 182.30, isExpense = true, date = "2026-05-04"),
            Transaction(name = "Premium Tech Gear", category = "Shopping", amount = 420.00, isExpense = true, date = "2026-05-07"),
            Transaction(name = "Cyberpunk Neo Transit", category = "Transport", amount = 35.00, isExpense = true, date = "2026-05-09"),
            Transaction(name = "Netflix Standard Tech", category = "Entertainment", amount = 15.49, isExpense = true, date = "2026-05-10"),
            Transaction(name = "Sushiya Cyber-District", category = "Food & Dining", amount = 94.20, isExpense = true, date = "2026-05-12"),
            Transaction(name = "Freelance Consulting", category = "Salary", amount = 850.00, isExpense = false, date = "2026-05-14"),
            Transaction(name = "Gym Membership Neon", category = "Rent & Bills", amount = 75.00, isExpense = true, date = "2026-05-15"),
            Transaction(name = "E-Scooter Rental", category = "Transport", amount = 12.80, isExpense = true, date = "2026-05-18"),
            Transaction(name = "Steam Game Store", category = "Entertainment", amount = 59.99, isExpense = true, date = "2026-05-20"),
            Transaction(name = "Weekly Grocery Express", category = "Food & Dining", amount = 112.40, isExpense = true, date = "2026-05-22"),
            Transaction(name = "Terminal Apparel Inc", category = "Shopping", amount = 185.00, isExpense = true, date = "2026-05-25"),
            Transaction(name = "Subway Ride Subway", category = "Transport", amount = 4.50, isExpense = true, date = "2026-05-27"),
            Transaction(name = "Neon Bar Cocktail Lounge", category = "Entertainment", amount = 85.00, isExpense = true, date = "2026-05-28"),

            // April 2026
            Transaction(name = "Salary Paycheck", category = "Salary", amount = 6500.0, isExpense = false, date = "2026-04-01"),
            Transaction(name = "Highrise Apartments Rent", category = "Rent & Bills", amount = 1500.0, isExpense = true, date = "2026-04-01"),
            Transaction(name = "Whole Foods Organic Market", category = "Food & Dining", amount = 165.20, isExpense = true, date = "2026-04-04"),
            Transaction(name = "Uber Commute Express", category = "Transport", amount = 42.10, isExpense = true, date = "2026-04-05"),
            Transaction(name = "Digital Camera Gear", category = "Shopping", amount = 650.00, isExpense = true, date = "2026-04-09"),
            Transaction(name = "Netflix Standard Tech", category = "Entertainment", amount = 15.49, isExpense = true, date = "2026-04-10"),
            Transaction(name = "Starbucks Coffee Terminal", category = "Food & Dining", amount = 24.50, isExpense = true, date = "2026-04-12"),
            Transaction(name = "Water Utilities bill", category = "Rent & Bills", amount = 80.00, isExpense = true, date = "2026-04-15"),
            Transaction(name = "E-Bike City Ride", category = "Transport", amount = 14.50, isExpense = true, date = "2026-04-18"),
            Transaction(name = "Cyber Cinema VIP Seats", category = "Entertainment", amount = 46.00, isExpense = true, date = "2026-04-20"),
            Transaction(name = "Weekly Grocery Central", category = "Food & Dining", amount = 148.90, isExpense = true, date = "2026-04-22"),
            Transaction(name = "Mech Keyboard Shop", category = "Shopping", amount = 129.00, isExpense = true, date = "2026-04-25"),
            Transaction(name = "Ramen Diner Tokyo", category = "Food & Dining", amount = 38.50, isExpense = true, date = "2026-04-26"),

            // March 2026
            Transaction(name = "Salary Paycheck", category = "Salary", amount = 6500.0, isExpense = false, date = "2026-03-01"),
            Transaction(name = "Highrise Apartments Rent", category = "Rent & Bills", amount = 1500.0, isExpense = true, date = "2026-03-01"),
            Transaction(name = "Electricity Grid Corp", category = "Rent & Bills", amount = 120.00, isExpense = true, date = "2026-03-03"),
            Transaction(name = "Whole Foods Organic Market", category = "Food & Dining", amount = 195.40, isExpense = true, date = "2026-03-05"),
            Transaction(name = "Bus Monthly Pass", category = "Transport", amount = 80.00, isExpense = true, date = "2026-03-06"),
            Transaction(name = "Nintendo Digital Game", category = "Entertainment", amount = 59.99, isExpense = true, date = "2026-03-10"),
            Transaction(name = "Bistro Dinner With Team", category = "Food & Dining", amount = 150.00, isExpense = true, date = "2026-03-15"),
            Transaction(name = "Spring Jacket Outerwear", category = "Shopping", amount = 220.00, isExpense = true, date = "2026-03-20"),
            Transaction(name = "Weekly Groceries Corner", category = "Food & Dining", amount = 98.70, isExpense = true, date = "2026-03-24"),
            Transaction(name = "Concert Neon Tickets", category = "Entertainment", amount = 180.00, isExpense = true, date = "2026-03-27")
        )

        transactionDao.insertTransactions(demoTransactions)
        Log.d("FinanceRepository", "Successfully populated ${demoTransactions.size} transactions.")
    }

    /**
     * Extracts Spreadsheet ID from a Google Sheets URL
     */
    private fun extractSpreadsheetId(url: String): String? {
        val cleanUrl = url.trim()
        if (cleanUrl.contains("/d/")) {
            val partAfterD = cleanUrl.split("/d/").getOrNull(1) ?: return null
            return partAfterD.split("/").getOrNull(0)
        }
        return null
    }

    /**
     * Core Sync Action: Fetch spreadsheet data as a CSV, convert and store
     */
    suspend fun syncWithGoogleSheet(url: String, tabName: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentSettings = syncSettingsDao.getSettingsDirect() ?: SyncSettings(spreadsheetUrl = url, tabName = tabName)
        
        // Update status to SYNCING
        syncSettingsDao.insertOrUpdateSettings(
            currentSettings.copy(
                spreadsheetUrl = url,
                tabName = tabName,
                connectionStatus = ConnectionStatus.SYNCING,
                errorMessage = null
            )
        )

        val spreadsheetId = extractSpreadsheetId(url)
        if (spreadsheetId.isNullOrBlank()) {
            val errMsg = "Invalid URL. Make sure it is a valid Google Sheets URL referencing '/d/[ID]/'."
            syncSettingsDao.insertOrUpdateSettings(
                currentSettings.copy(
                    spreadsheetUrl = url,
                    tabName = tabName,
                    connectionStatus = ConnectionStatus.ERROR,
                    errorMessage = errMsg
                )
            )
            return@withContext Result.failure(Exception(errMsg))
        }

        // Build CSV export link
        // Append tab sheets parameter if provided
        val encodedTab = java.net.URLEncoder.encode(tabName, "UTF-8")
        val csvUrl = "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv&sheet=$encodedTab"
        
        Log.d("FinanceRepository", "Attempting fetch from Google Sheets CSV Address: $csvUrl")

        try {
            val request = Request.Builder()
                .url(csvUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Android; Mobile)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val fallbackUrl = "https://docs.google.com/spreadsheets/d/$spreadsheetId/export?format=csv"
                    Log.d("FinanceRepository", "Failed with tab name, trying default sheet CSV address: $fallbackUrl")
                    
                    val fallbackRequest = Request.Builder().url(fallbackUrl).build()
                    client.newCall(fallbackRequest).execute().use { fbResponse ->
                        if (!fbResponse.isSuccessful) {
                            throw Exception("HTTP Error: ${fbResponse.code}. Check if your Google Sheet is shared as 'Anyone with the link can view'.")
                        }
                        return@withContext parseAndStoreCsv(fbResponse.body!!.byteStream(), url, tabName)
                    }
                } else {
                    return@withContext parseAndStoreCsv(response.body!!.byteStream(), url, tabName)
                }
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed downstream connection. Please verify the URL and tab name, and ensure the Sheet sharing setting is set to public."
            Log.e("FinanceRepository", "Google Sheet sync error", e)
            
            syncSettingsDao.insertOrUpdateSettings(
                currentSettings.copy(
                    spreadsheetUrl = url,
                    tabName = tabName,
                    connectionStatus = ConnectionStatus.ERROR,
                    errorMessage = errorMsg
                )
            )
            return@withContext Result.failure(e)
        }
    }

    private suspend fun parseAndStoreCsv(
        inputStream: java.io.InputStream,
        originalUrl: String,
        tabName: String
    ): Result<Unit> {
        val tXS = mutableListOf<Transaction>()
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            val headerLine = reader.readLine() ?: return Result.failure(Exception("The spreadsheet is empty."))
            
            // Parse headers
            val headers = splitCsvLine(headerLine).map { it.trim().lowercase() }
            
            val dateIndex = headers.indexOfFirst { it.contains("date") }
            val nameIndex = headers.indexOfFirst { it.contains("name") || it.contains("desc") || it.contains("payee") || it.contains("title") }
            val categoryIndex = headers.indexOfFirst { it.contains("cat") || it.contains("group") || it.contains("tag") }
            val amountIndex = headers.indexOfFirst { it.contains("amount") || it.contains("cost") || it.contains("price") || it.contains("value") }
            val typeIndex = headers.indexOfFirst { it.contains("type") || it.contains("is_expense") || it.contains("isexpense") }

            // Validate that we found at least a Name and an Amount
            if (nameIndex == -1 || amountIndex == -1) {
                return Result.failure(Exception("Header mapping failure. Ensure your spreadsheet contains column headers like 'Name' and 'Amount' (case-insensitive). Found headers: $headers"))
            }

            var line: String? = reader.readLine()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val alternativeFormats = listOf(
                SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            )

            while (line != null) {
                val row = splitCsvLine(line)
                if (row.size > nameIndex && row.size > amountIndex) {
                    val rawName = row[nameIndex].trim().replace("\"", "")
                    val rawAmountStr = row[amountIndex].trim().replace("\"", "").replace("$", "").replace(",", "")
                    val rawAmount = rawAmountStr.toDoubleOrNull() ?: 0.0

                    if (rawName.isNotBlank() && rawAmount != 0.0) {
                        // Category Parsing
                        val category = if (categoryIndex != -1 && categoryIndex < row.size) {
                            row[categoryIndex].trim().replace("\"", "")
                        } else ""
                        val finalCategory = if (category.isBlank()) "Other" else category

                        // Date Parsing
                        var dateString = dateFormat.format(Date()) // default
                        if (dateIndex != -1 && dateIndex < row.size) {
                            val rawDate = row[dateIndex].trim().replace("\"", "")
                            if (rawDate.isNotBlank()) {
                                var parsedDate: Date? = null
                                for (fmt in alternativeFormats) {
                                    try {
                                        parsedDate = fmt.parse(rawDate)
                                        if (parsedDate != null) break
                                    } catch (e: Exception) {
                                        // continue
                                    }
                                }
                                if (parsedDate != null) {
                                    dateString = dateFormat.format(parsedDate)
                                } else {
                                    // Fallback to text matching or leaving it as is if it looks correct
                                    if (rawDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                                        dateString = rawDate
                                    }
                                }
                            }
                        }

                        // Expense detection
                        var isExpense = true
                        if (typeIndex != -1 && typeIndex < row.size) {
                            val rawType = row[typeIndex].trim().lowercase()
                            if (rawType.contains("income") || rawType.contains("deposit") || rawType == "false") {
                                isExpense = false
                            }
                        } else {
                            // Smart logic: if amount is positive and contains positive markers, or if it is negative prefix
                            if (rawAmountStr.startsWith("-")) {
                                isExpense = true
                            } else if (finalCategory.lowercase() == "salary" || finalCategory.lowercase() == "income") {
                                isExpense = false
                            }
                        }

                        tXS.add(
                            Transaction(
                                name = rawName,
                                category = finalCategory,
                                amount = Math.abs(rawAmount),
                                isExpense = isExpense,
                                date = dateString,
                                timestamp = try { dateFormat.parse(dateString)?.time ?: System.currentTimeMillis() } catch (e: Exception) { System.currentTimeMillis() }
                            )
                        )
                    }
                }
                line = reader.readLine()
            }
        }

        if (tXS.isEmpty()) {
            return Result.failure(Exception("Found 0 valid transaction rows under parsed headers. Ensure rows contain both titles and amount figures."))
        }

        // Transactions found! Overwrite Database fully (since Google Sheet act as the single source of truth!)
        transactionDao.clearAllTransactions()
        transactionDao.insertTransactions(tXS)

        val updatedSettings = SyncSettings(
            spreadsheetUrl = originalUrl,
            tabName = tabName,
            lastSyncedTime = System.currentTimeMillis(),
            connectionStatus = ConnectionStatus.CONNECTED,
            errorMessage = null
        )
        syncSettingsDao.insertOrUpdateSettings(updatedSettings)

        Log.d("FinanceRepository", "Sheet Sync Success! Stored ${tXS.size} transactions.")
        return Result.success(Unit)
    }

    /**
     * Splits a CSV line correctly handling quotes
     */
    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '\"') {
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        curVal.append('\"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true
                } else if (ch == ',') {
                    result.add(curVal.toString())
                    curVal = StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
            i++
        }
        result.add(curVal.toString())
        return result
    }
}
