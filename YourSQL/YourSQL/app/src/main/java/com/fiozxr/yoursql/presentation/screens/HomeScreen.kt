package com.fiozxr.yoursql.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fiozxr.yoursql.presentation.theme.SuccessGreen
import com.fiozxr.yoursql.presentation.viewmodel.HomeViewModel
import com.fiozxr.yoursql.server.engine.ServerService
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YourSQL") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server Status Card
            ServerStatusCard(
                isRunning = uiState.isServerRunning,
                address = uiState.serverAddress,
                port = uiState.serverPort,
                uptime = uiState.uptime,
                requestCount = uiState.requestCount,
                onToggleServer = { viewModel.toggleServer(context) }
            )

            // Statistics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    title = "Databases",
                    value = uiState.databaseCount.toString(),
                    icon = Icons.Default.Storage,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Tables",
                    value = uiState.tableCount.toString(),
                    icon = Icons.Default.TableChart,
                    modifier = Modifier.weight(1f)
                )
            }

            // Storage Card
            StorageCard(
                usedBytes = uiState.storageUsed,
                totalBytes = uiState.storageQuota,
                bucketCount = uiState.bucketCount
            )

            // Quick Actions
            QuickActionsCard(
                onCreateDatabase = { viewModel.showCreateDatabaseDialog() },
                onOpenQueryEditor = { /* Navigate to query */ },
                onViewLogs = { /* Navigate to logs */ }
            )

            // Tunnel Status (if active)
            if (uiState.tunnelUrl != null) {
                TunnelCard(
                    tunnelUrl = uiState.tunnelUrl,
                    onCopyUrl = { viewModel.copyToClipboard(context, uiState.tunnelUrl) }
                )
            }
        }
    }

    // Create Database Dialog
    if (uiState.showCreateDatabaseDialog) {
        CreateDatabaseDialog(
            onDismiss = { viewModel.hideCreateDatabaseDialog() },
            onCreate = { name ->
                viewModel.createDatabase(name)
                viewModel.hideCreateDatabaseDialog()
            }
        )
    }
}

@Composable
fun ServerStatusCard(
    isRunning: Boolean,
    address: String?,
    port: Int,
    uptime: Long,
    requestCount: Long,
    onToggleServer: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) SuccessGreen.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) SuccessGreen else MaterialTheme.colorScheme.error)
                    )
                    Text(
                        text = if (isRunning) "Server Running" else "Server Stopped",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onToggleServer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.error
                        else SuccessGreen
                    )
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRunning) "Stop" else "Start")
                }
            }

            if (isRunning) {
                AnimatedVisibility(visible = isRunning) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow("Address", address ?: "Unknown")
                        InfoRow("Port", port.toString())
                        InfoRow("Uptime", formatUptime(uptime))
                        InfoRow("Requests", requestCount.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StorageCard(
    usedBytes: Long,
    totalBytes: Long,
    bucketCount: Int
) {
    val usagePercent = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes * 100) else 0f

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Storage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$bucketCount buckets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { usagePercent / 100f },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatFileSize(usedBytes),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatFileSize(totalBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    onCreateDatabase: () -> Unit,
    onOpenQueryEditor: () -> Unit,
    onViewLogs: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCreateDatabase,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New DB")
                }
                OutlinedButton(
                    onClick = onOpenQueryEditor,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Query")
                }
                OutlinedButton(
                    onClick = onViewLogs,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.List, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Logs")
                }
            }
        }
    }
}

@Composable
fun TunnelCard(
    tunnelUrl: String,
    onCopyUrl: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Public Tunnel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onCopyUrl) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL")
                }
            }

            Text(
                text = tunnelUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            Text(
                text = "Warning: Your server is publicly accessible. Ensure API key authentication is enabled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CreateDatabaseDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var databaseName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Database") },
        text = {
            OutlinedTextField(
                value = databaseName,
                onValueChange = { databaseName = it },
                label = { Text("Database Name") },
                placeholder = { Text("my_database") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(databaseName) },
                enabled = databaseName.isNotBlank() && databaseName.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatUptime(uptimeMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(uptimeMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(uptimeMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(uptimeMs) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
        size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}
