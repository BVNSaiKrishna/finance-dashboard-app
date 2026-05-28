package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.DashboardMetrics
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.MonthSummary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeMonth by viewModel.activeMonth.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val historicalList by viewModel.historicalMetrics.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val transactions by viewModel.activeMonthTransactions.collectAsState()

    val isDark by viewModel.isDarkMode.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top welcome bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DESKTOP SYNC",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Financial Terminal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Switch Theme",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Tappable Month comparison chart as top slider
        MonthSelectorTerminal(
            availableMonths = availableMonths,
            activeMonth = activeMonth,
            historicalList = historicalList,
            onMonthSelected = { viewModel.setActiveMonth(it) }
        )

        // Empty State Onboarding banner if no transactions
        if (transactions.isEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "No CSV loaded",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Terminal is Disconnected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your dashboard is currently showing offline mock states. Head to the 'Sync Settings' page to wire up your live Google Sheet CSV URL tracking.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Row of 3 key mini cards: Total Spend, Average Transaction, Top Category
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricsMiniCard(
                title = "TOTAL SPEND",
                value = "$${String.format("%,.2f", metrics.totalSpend)}",
                color = MaterialTheme.colorScheme.primary,
                icon = Icons.Outlined.LocalActivity,
                modifier = Modifier.weight(1f)
            )
            MetricsMiniCard(
                title = "AVG / TX",
                value = "$${String.format("%,.2f", metrics.avgTransactionAmount)}",
                color = MaterialTheme.colorScheme.secondary,
                icon = Icons.Outlined.TrendingUp,
                modifier = Modifier.weight(1f)
            )
            MetricsMiniCard(
                title = "TOP DRAIN",
                value = metrics.topCategory,
                color = MaterialTheme.colorScheme.tertiary,
                icon = Icons.Outlined.Category,
                modifier = Modifier.weight(1f)
            )
        }

        // Budget Progress Meter
        BudgetProgressMeter(
            metrics = metrics,
            budgetLimit = monthlyBudget.limit
        )

        // Graphs block: Category Donut and Daily Spend Bars
        CategoryAndDailyCharts(
            metrics = metrics,
            transactions = transactions
        )

        Spacer(modifier = Modifier.height(60.dp)) // Nav buffer
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderAlpha: Float = 0.15f,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)
    }
    val cardBackground = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(cardBackground)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(borderColor, Color.Transparent)
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        content = content
    )
}

@Composable
fun MetricsMiniCard(
    title: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun MonthSelectorTerminal(
    availableMonths: List<String>,
    activeMonth: String,
    historicalList: Map<String, MonthSummary>,
    onMonthSelected: (String) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "TAPPABLE INDEX (CHAMBER MONTH COMPARISON)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ensure at least Mar, Apr, May represent if empty
                val displayMonths = if (availableMonths.isEmpty()) listOf("2026-05", "2026-04", "2026-03") else availableMonths

                // Compute high water mark budget for visual height
                val maxSpend = historicalList.values.maxOfOrNull { it.expenses } ?: 3000.0

                displayMonths.take(3).reversed().forEach { mth ->
                    val isSelected = mth == activeMonth
                    val summary = historicalList[mth] ?: MonthSummary()
                    val rawSpend = summary.expenses
                    val rawIncome = summary.income

                    // height weight calculation
                    val ratio = if (maxSpend > 0) (rawSpend / maxSpend).toFloat().coerceIn(0.15f, 1f) else 0.5f

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onMonthSelected(mth) }
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Month name
                            val parsedMonthName = try {
                                val sFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                                val d = sFormat.parse(mth)
                                SimpleDateFormat("MMM", Locale.getDefault()).format(d!!).uppercase()
                            } catch (e: Exception) {
                                mth
                            }

                            Text(
                                text = parsedMonthName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Visual vertical bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .height(55.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(ratio)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }

                            // Spend label
                            Text(
                                text = "$${String.format("%.0f", rawSpend)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetProgressMeter(
    metrics: DashboardMetrics,
    budgetLimit: Double
) {
    val remaining = metrics.remainingBudget
    val usedPercent = metrics.percentageUsed
    val isOverBudget = remaining < 0

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
                Column {
                    Text(
                        text = "BUDGET VELOCITY METER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isOverBudget) "CRITICAL: Depleted" else "Allowance Active",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "${String.format("%.1f", usedPercent)}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            // Sleek Progress track with glowing linear bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val clampedRatio = (usedPercent / 100f).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clampedRatio)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = if (isOverBudget) listOf(MaterialTheme.colorScheme.error, Color(0xFFFF9494))
                                else listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        )
                )
            }

            // Limits bottom legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "REMAINING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%,.2f", remaining)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "LIMIT TARGET",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%,.2f", budgetLimit)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryAndDailyCharts(
    metrics: DashboardMetrics,
    transactions: List<Transaction>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Donut Chart
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "CATEGORY MIX",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (metrics.categoryDistribution.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No category data for active timeline.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Jetpack Canvas Donut representation
                        CategoryDonutChart(
                            categoryDistribution = metrics.categoryDistribution,
                            modifier = Modifier
                                .size(140.dp)
                                .padding(8.dp)
                        )

                        // Legend representation
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val activeColors = getPaletteList()
                            metrics.categoryDistribution.entries.sortedByDescending { it.value }.take(4).forEachIndexed { i, entry ->
                                val color = activeColors[i % activeColors.size]
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(
                                        text = entry.key,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "$${String.format("%.0f", entry.value)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Daily Spend Bar Chart representation
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DAILY NET VELOCITY SPEND",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                DailySpendBarChart(
                    transactions = transactions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    categoryDistribution: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val entries = categoryDistribution.entries.toList()
    val total = entries.sumOf { it.value }

    val colorsPalette = getPaletteList()

    Canvas(modifier = modifier) {
        var startAngle = 270f
        val strokeWidthPx = 14.dp.toPx()

        if (total == 0.0) {
            drawCircle(
                color = Color.LightGray.copy(alpha = 0.3f),
                style = Stroke(width = strokeWidthPx)
            )
        } else {
            entries.forEachIndexed { i, (category, amount) ->
                val sweepAngle = ((amount / total) * 360f).toFloat()
                val color = colorsPalette[i % colorsPalette.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun DailySpendBarChart(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    // Group active month transactions by day
    val expenses = transactions.filter { it.isExpense }
    val dayMap = expenses.groupBy {
        try {
            val simple = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date)
            calendar.time = simple!!
            calendar.get(Calendar.DAY_OF_MONTH)
        } catch (e: Exception) {
            1
        }
    }.mapValues { entry -> entry.value.sumOf { it.amount } }

    // Represent up to 31 days
    val points = (1..31).map { dayMap[it] ?: 0.0 }
    val maxPoint = points.maxOrNull() ?: 100.0

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEachIndexed { index, amount ->
            val day = index + 1
            // Compute percentage Height
            val percent = if (maxPoint > 0) (amount / maxPoint).toFloat().coerceIn(0.02f, 1f) else 0.02f
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.width(30.dp)
            ) {
                // Tooltip amount
                if (amount > 0) {
                    Text(
                        text = "$${amount.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Vertical capsules
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(percent)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                )
                            )
                    )
                }

                Text(
                    text = "$day",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Global Category Colors List
fun getPaletteList(): List<Color> {
    return listOf(
        NeonTeal,
        NeonPurple,
        CyberBlue,
        ElectricYellow,
        NeonYellow,
        Color(0xFFFF70A6),
        Color(0xFF70E4EF)
    )
}
