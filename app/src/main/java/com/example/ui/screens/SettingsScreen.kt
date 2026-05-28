package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val syncSettings by viewModel.syncSettings.collectAsState()
    val isSyncing by viewModel.syncingState.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()

    var urlInput by remember { mutableStateOf("") }
    var tabInput by remember { mutableStateOf("") }

    // Synchronize inputs with saved database states initially
    LaunchedEffect(syncSettings) {
        if (urlInput.isBlank() && syncSettings.spreadsheetUrl.isNotBlank()) {
            urlInput = syncSettings.spreadsheetUrl
        }
        if (tabInput.isBlank()) {
            tabInput = syncSettings.tabName
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncResult) {
        syncResult?.let { res ->
            if (res.isSuccess) {
                snackbarHostState.showSnackbar("Synchronization completed! Recalculated index.")
            } else {
                snackbarHostState.showSnackbar("Synchronization failed: ${res.exceptionOrNull()?.message}")
            }
            viewModel.resetSyncResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header
            Column {
                Text(
                    text = "Sync & Integrations",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Link your mobile ledger to Google Sheets for spreadsheet sync.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. Google Sheets sync panel
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "GOOGLE SHEETS CONNECTION DIRECTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    TextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        placeholder = { Text("https://docs.google.com/spreadsheets/d/.../edit") },
                        label = { Text("Spreadsheet Browser Link") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("spreadsheet_url_field"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )

                    TextField(
                        value = tabInput,
                        onValueChange = { tabInput = it },
                        placeholder = { Text("Sheet1") },
                        label = { Text("Specific Workspace Tab Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tab_name_field"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Tab, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    )

                    Button(
                        onClick = { viewModel.performGoogleSheetSync(urlInput, tabInput) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sync_submit_button"),
                        enabled = !isSyncing && urlInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SYNCHRONIZING SECURE TUNNEL...", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CONNECT & SYNC NOW", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Telemetry Connection Status Card
            TelemetryStatusCard(
                status = syncSettings.connectionStatus,
                lastSynctime = syncSettings.lastSyncedTime,
                errorMsg = syncSettings.errorMessage
            )

            // 3. User documentation and guidance board
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ONBOARDING REFERENCE PROCEDURES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    BulletPoint("1. Open any tracking spreadsheet in Google Sheets.")
                    BulletPoint("2. Ensure column headers contain 'Name', 'Category', 'Amount', and optionally 'Date' and 'Type'.")
                    BulletPoint("3. Click 'File' > 'Share' > 'Share with others' and set general access to 'Anyone with link can view'.")
                    BulletPoint("4. Copy the URL from your browser address bar and paste here, then tap sync.")
                }
            }

            // 4. Fallback simulator panel
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "DEVELOPMENT LAB & TESTING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "No public sheet handy? Reset the mobile database with a high-fidelity mock spreadsheet sync dataset to explore the full dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { viewModel.populateRealisticMockForEmptyUrl() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .testTag("simulate_sync_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIMULATE GOOGLE SHEET SYNC LOCAL", fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.height(70.dp)) // Nav buffer
        }
    }
}

@Composable
fun TelemetryStatusCard(
    status: ConnectionStatus,
    lastSynctime: Long,
    errorMsg: String?
) {
    val statusColor = when (status) {
        ConnectionStatus.CONNECTED -> FinanceGreen
        ConnectionStatus.SYNCING -> ElectricYellow
        ConnectionStatus.ERROR -> FinanceRed
        ConnectionStatus.NOT_CONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val displayStatus = when (status) {
        ConnectionStatus.CONNECTED -> "CONNECTED - SINGLE SOURCE SYNC ACTIVE"
        ConnectionStatus.SYNCING -> "SYNCHRONIZING TERMINAL RECOGNITION..."
        ConnectionStatus.ERROR -> "CONNECTION TELEMETRY DISRUPTED / OFFLINE"
        ConnectionStatus.NOT_CONNECTED -> "DISCONNECTED - LOCAL OFFLINE RECORD"
    }

    val outlineVariant = Brush.verticalGradient(
        colors = listOf(statusColor.copy(alpha = 0.3f), Color.Transparent)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(width = 1.dp, brush = outlineVariant, shape = RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "TELEMETRY & SYNC STATUS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                text = displayStatus,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }

        val displayTime = if (lastSynctime > 0) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(Date(lastSynctime))
        } else {
            "Never synced (holding local sample record)"
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "LAST SYNC STAMP:",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = displayTime,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (status == ConnectionStatus.ERROR && errorMsg != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(FinanceRed.copy(alpha = 0.1f))
                    .border(width = 0.5.dp, color = FinanceRed.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "DECRYPT CRITICAL ERROR:",
                    style = MaterialTheme.typography.labelSmall,
                    color = FinanceRed,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 18.sp
    )
}
