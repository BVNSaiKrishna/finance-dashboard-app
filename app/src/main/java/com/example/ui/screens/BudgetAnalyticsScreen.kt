package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryBudget
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetAnalyticsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val activeMonth by viewModel.activeMonth.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val categoryBudgets by viewModel.categoryBudgets.collectAsState()

    var showAdjustOverallDialog by remember { mutableStateOf(false) }
    var showAddCategoryBudgetDialog by remember { mutableStateOf(false) }

    // Constants for dates
    val calendar = Calendar.getInstance()
    val totalDaysInMonth = remember(activeMonth) {
        val parts = activeMonth.split("-")
        val yr = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val mn = parts.getOrNull(1)?.toIntOrNull() ?: 5
        val queryCal = Calendar.getInstance()
        queryCal.set(yr, mn - 1, 1)
        queryCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val elapsedDays = remember(activeMonth) {
        val crtFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        if (activeMonth == crtFormat) {
            calendar.get(Calendar.DAY_OF_MONTH)
        } else {
            totalDaysInMonth
        }
    }

    val remainingDays = (totalDaysInMonth - elapsedDays).coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core header
        Column {
            Text(
                text = "Budget & Predictions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Predictive analysis driven by live daily ledger velocity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Interactive Overviews Card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OVERALL MONTH MAXIMUM LIMIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit overall limit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { showAdjustOverallDialog = true }
                            .testTag("adjust_budget_button")
                    )
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$${String.format("%,.0f", monthlyBudget.limit)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "allocated for $activeMonth",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }

        // 2. Predictive Velocity Console
        Text(
            text = "PREDICTIVE VELOCITY CONSOLE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Speed Column
            MetricsTelemetryCard(
                title = "DAILY VELOCITY",
                value = "$${String.format("%.2f", metrics.spendingVelocity)}",
                subtitle = "Expense burn/day",
                color = MaterialTheme.colorScheme.secondary,
                icon = Icons.Outlined.Speed,
                modifier = Modifier.weight(1f)
            )

            // Projected Burn Column
            MetricsTelemetryCard(
                title = "PROJECTED BURN",
                value = "$${String.format("%,.2f", metrics.predictedExpenses)}",
                subtitle = "Forecasted full month",
                color = MaterialTheme.colorScheme.tertiary,
                icon = Icons.Outlined.TrendingUp,
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Trajectory Warning/Safe Card
        val isOverProjected = metrics.predictedExpenses > monthlyBudget.limit
        val advisoryGradient = if (isOverProjected) {
            listOf(FinanceRed.copy(alpha = 0.12f), Color.Transparent)
        } else {
            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), Color.Transparent)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(colors = advisoryGradient))
                .border(
                    width = 1.dp,
                    color = if (isOverProjected) FinanceRed.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isOverProjected) Icons.Default.WarningAmber else Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isOverProjected) FinanceRed else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = if (isOverProjected) "LEDGER TRAJECTORY WARNING" else "TRAJECTORY ADVISORY: SAFE",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverProjected) FinanceRed else MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = if (isOverProjected) {
                    "Pace represents overspending! Your forecasted expense exceeds target limit by $${String.format("%,.2f", metrics.predictedExpenses - monthlyBudget.limit)}. To stabilize back beneath budget target, cap your daily pace to $${String.format("%.2f", (metrics.remainingBudget / remainingDays).coerceAtLeast(0.0))}/day over the final $remainingDays days remaining in this tracking month."
                } else {
                    "Stable burn telemetry detected! Your daily speed falls nicely within boundaries, finishing under budget by e.g. $${String.format("%,.2f", monthlyBudget.limit - metrics.predictedExpenses)}. You can comfortably utilize up to $${String.format("%.2f", (metrics.remainingBudget / remainingDays).coerceAtLeast(0.0))}/day in discretionary allowances."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 22.sp
            )
        }

        // 4. Category Budgets breakdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CATEGORY TARGET MATRIX",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { showAddCategoryBudgetDialog = true },
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .testTag("add_category_limit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Limit",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (categoryBudgets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No category specific budget thresholds defined.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            categoryBudgets.forEach { limit ->
                CategoryLimitRow(
                    limit = limit,
                    spentAmount = metrics.categoryDistribution[limit.category] ?: 0.0,
                    onDelete = { viewModel.deleteCategoryLimit(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(70.dp)) // Nav buffer
    }

    // Adjust Overall Monthly Budget Dialog
    if (showAdjustOverallDialog) {
        AdjustBudgetDialog(
            currentLimit = monthlyBudget.limit,
            onDismiss = { showAdjustOverallDialog = false },
            onSave = {
                viewModel.setOverallMonthlyBudget(it)
                showAdjustOverallDialog = false
            }
        )
    }

    // Add Category Budget Dialog
    if (showAddCategoryBudgetDialog) {
        AddCategoryLimitDialog(
            existingCategories = categoryBudgets.map { it.category },
            onDismiss = { showAddCategoryBudgetDialog = false },
            onSave = { category, limit ->
                viewModel.saveCategoryLimit(category, limit)
                showAddCategoryBudgetDialog = false
            }
        )
    }
}

@Composable
fun MetricsTelemetryCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CategoryLimitRow(
    limit: CategoryBudget,
    spentAmount: Double,
    onDelete: (Int) -> Unit
) {
    val ratio = if (limit.limit > 0) (spentAmount / limit.limit).toFloat() else 0f
    val isOver = spentAmount > limit.limit

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(limit.category),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = limit.category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(
                    onClick = { onDelete(limit.id) },
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = FinanceRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // progress track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio.coerceAtMost(1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if (isOver) FinanceRed else MaterialTheme.colorScheme.primary)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent: $${String.format("%.1f", spentAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOver) FinanceRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Target: $${String.format("%.0f", limit.limit)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun AdjustBudgetDialog(
    currentLimit: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var limitStr by remember { mutableStateOf(currentLimit.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Overall Monthly Limit", fontWeight = FontWeight.Bold) },
        text = {
            TextField(
                value = limitStr,
                onValueChange = { limitStr = it },
                label = { Text("Budget Limit ($)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val limit = limitStr.toDoubleOrNull() ?: 3500.0
                    onSave(limit)
                }
            ) {
                Text("SAVE BUDGET", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = CyberSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddCategoryLimitDialog(
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var limitStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food & Dining") }

    val categoriesList = listOf(
        "Food & Dining",
        "Rent & Bills",
        "Shopping",
        "Entertainment",
        "Transport",
        "Other"
    ).filter { !existingCategories.contains(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Category Budget", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (categoriesList.isEmpty()) {
                    Text(
                        text = "All primary categories already have custom thresholds set.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Select Category:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

                    TextField(
                        value = limitStr,
                        onValueChange = { limitStr = it },
                        label = { Text("Category Threshold ($)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (categoriesList.isNotEmpty()) {
                TextButton(
                    onClick = {
                        val limit = limitStr.toDoubleOrNull() ?: 500.0
                        onSave(category, limit)
                    }
                ) {
                    Text("SAVE LIMIT", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = CyberSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
