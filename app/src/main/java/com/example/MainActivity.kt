package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.BudgetAnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.PulseFinanceTheme
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()

            PulseFinanceTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route ?: "dashboard"

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            // 1. Draw solid background
                            val bgColor = if (isDark) Color(0xFF0A0A0B) else Color(0xFFF4F7FC)
                            drawRect(color = bgColor)

                            // 2. Draw colorful ambient blurred glows in dark mode
                            if (isDark) {
                                val brushTeal = Brush.radialGradient(
                                    colors = listOf(Color(0xFF2DD4BF).copy(alpha = 0.10f), Color.Transparent),
                                    center = Offset(0f, 0f),
                                    radius = size.width * 0.9f
                                )
                                drawCircle(
                                    brush = brushTeal,
                                    center = Offset(0f, 0f),
                                    radius = size.width * 0.9f
                                )

                                val brushPurple = Brush.radialGradient(
                                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.10f), Color.Transparent),
                                    center = Offset(size.width, size.height * 0.8f),
                                    radius = size.width * 1.1f
                                )
                                drawCircle(
                                    brush = brushPurple,
                                    center = Offset(size.width, size.height * 0.8f),
                                    radius = size.width * 1.1f
                                )
                            }

                            // 3. Draw cyber technical mesh grid lines
                            val gridColor = if (isDark) Color(0xFF808080).copy(alpha = 0.07f) else Color(0xFF006874).copy(alpha = 0.02f)
                            val gridSize = 45f // grid box size in pixels

                            var x = 0f
                            while (x < size.width) {
                                drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                                x += gridSize
                            }

                            var y = 0f
                            while (y < size.height) {
                                drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                                y += gridSize
                            }
                        },
                    containerColor = Color.Transparent
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "dashboard",
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable("dashboard") {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToTransactions = { navController.navigate("transactions") },
                                    onNavigateToAnalytics = { navController.navigate("analytics") },
                                    modifier = Modifier.padding(bottom = 60.dp)
                                )
                            }
                            composable("transactions") {
                                TransactionsScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(bottom = 60.dp)
                                )
                            }
                            composable("analytics") {
                                BudgetAnalyticsScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(bottom = 60.dp)
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.padding(bottom = 60.dp)
                                )
                            }
                        }

                        // Floating custom frosted glass navigation deck at the bottom!
                        FrostedNavigationDeck(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo("dashboard") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FrostedNavigationDeck(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        modifier = modifier
            .height(64.dp)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .testTag("frosted_nav_deck")
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                NavItem("dashboard", "Terminal", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
                NavItem("transactions", "Ledger", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
                NavItem("analytics", "Forecast", Icons.Filled.Leaderboard, Icons.Outlined.Leaderboard),
                NavItem("settings", "Sync", Icons.Filled.CloudSync, Icons.Outlined.CloudSync)
            )

            navItems.forEach { item ->
                val active = currentRoute == item.route
                val icon = if (active) item.activeIcon else item.inactiveIcon
                val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigate(item.route) }
                        .testTag("nav_tab_${item.route}")
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.label,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = tint,
                        fontWeight = if (active) FontWeight.Black else FontWeight.Bold
                    )
                }
            }
        }
    }
}

data class NavItem(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)
