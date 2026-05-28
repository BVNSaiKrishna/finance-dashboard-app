package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeMonthTransactions by viewModel.activeMonthTransactions.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val selectedFilterCategory by viewModel.selectedCategoryFilter.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    // Dynamic list of categories from active month transactions
    val categories = remember(activeMonthTransactions) {
        val list = activeMonthTransactions.map { it.category }.distinct().sorted()
        listOf("All") + list
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen title
            Text(
                text = "Transaction Ledger",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Search Terminal
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search transactions...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .testTag("ledger_search_box"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )

            // Category Horizontal Filter row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = (cat == "All" && selectedFilterCategory == null) || (cat == selectedFilterCategory)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                viewModel.selectCategoryFilter(if (cat == "All") null else cat)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("category_pill_$cat")
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Expandable List
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterListOff,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "No recorded transactions match.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { transaction ->
                        TransactionItemRow(
                            transaction = transaction,
                            onDelete = { viewModel.deleteTransaction(it) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(70.dp)) // Floating FAB Buffer
                    }
                }
            }
        }

        // Add Transaction floating FAB action
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 8.dp)
                .testTag("add_transaction_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.Black
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Transaction")
        }
    }

    // Modal input Dialog structure
    if (showAddDialog) {
        AddTransactionModal(
            onDismiss = { showAddDialog = false },
            onSave = { name, cat, amt, isExp, dt ->
                viewModel.addTransaction(name, cat, amt, isExp, dt)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TransactionItemRow(
    transaction: Transaction,
    onDelete: (Transaction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val borderColor = if (transaction.isExpense) {
        FinanceRed.copy(alpha = 0.25f)
    } else {
        FinanceGreen.copy(alpha = 0.25f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { expanded = !expanded }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon + Title
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (transaction.isExpense) FinanceRed.copy(alpha = 0.1f)
                            else FinanceGreen.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(transaction.category),
                        contentDescription = transaction.category,
                        tint = if (transaction.isExpense) FinanceRed else FinanceGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = transaction.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Amount and Sign
            val sign = if (transaction.isExpense) "-" else "+"
            val textColor = if (transaction.isExpense) FinanceRed else FinanceGreen

            Text(
                text = "$sign$${String.format("%,.2f", transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = textColor
            )
        }

        // Expandable Subpanel details
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = spring()),
            exit = shrinkVertically(animationSpec = spring())
        ) {
            Divider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "CALENDAR:",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = transaction.date,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "FLOW TYPE:",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (transaction.isExpense) "DEBIT DR" else "CREDIT CR",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = if (transaction.isExpense) FinanceRed else FinanceGreen
                        )
                    }
                }

                // Delete Button
                Button(
                    onClick = { onDelete(transaction) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FinanceRed.copy(alpha = 0.15f),
                        contentColor = FinanceRed
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "WIPE",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AddTransactionModal(
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food & Dining") }
    var amountStr by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var dateStr by remember { mutableStateOf("") }

    val categoriesList = listOf(
        "Food & Dining",
        "Rent & Bills",
        "Shopping",
        "Entertainment",
        "Transport",
        "Salary",
        "Capital Gain",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Ledger Transaction",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Name
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Title / Payee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Amount
                TextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date
                val todayString = remember {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                }
                if (dateStr.isEmpty()) {
                    dateStr = todayString
                }

                TextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Expense Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isExpense = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpense) FinanceRed else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isExpense) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("DEBIT/EXP")
                    }
                    Button(
                        onClick = { isExpense = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isExpense) FinanceGreen else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (!isExpense) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CREDIT/INC")
                    }
                }

                // Category List Drop-down selector or pills
                Text(
                    text = "SELECT CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoriesList) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalAmt = amountStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && finalAmt > 0) {
                        onSave(name, category, finalAmt, isExpense, dateStr)
                    }
                }
            ) {
                Text("SAVE TRANSACTION", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = CyberSurface
    )
}

// Help utility for getting specific Category vectors
fun getCategoryIcon(category: String): ImageVector {
    val clean = category.lowercase().trim()
    return when {
        clean.contains("food") || clean.contains("dine") || clean.contains("eat") || clean.contains("grocer") -> Icons.Default.Restaurant
        clean.contains("rent") || clean.contains("bill") || clean.contains("housing") || clean.contains("electro") || clean.contains("util") -> Icons.Default.Home
        clean.contains("shop") || clean.contains("apparel") || clean.contains("cloth") || clean.contains("gear") -> Icons.Default.ShoppingBag
        clean.contains("entertain") || clean.contains("netfl") || clean.contains("game") || clean.contains("mov") || clean.contains("music") -> Icons.Default.Casino
        clean.contains("transport") || clean.contains("uber") || clean.contains("scoot") || clean.contains("bus") || clean.contains("subway") -> Icons.Default.DirectionsBus
        clean.contains("salary") || clean.contains("payc") || clean.contains("income") || clean.contains("gain") || clean.contains("freelance") -> Icons.Default.AccountBalanceWallet
        else -> Icons.Default.Payments
    }
}
