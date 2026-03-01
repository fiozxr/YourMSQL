package com.fiozxr.yoursql.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fiozxr.yoursql.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server Settings
            SettingsSection(title = "Server") {
                SettingsItem(
                    icon = Icons.Default.Router,
                    title = "Port",
                    subtitle = uiState.serverPort.toString(),
                    onClick = { /* Show port dialog */ }
                )
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "HTTPS",
                    subtitle = if (uiState.httpsEnabled) "Enabled" else "Disabled",
                    onClick = { viewModel.toggleHttps() }
                )
            }

            // Security Settings
            SettingsSection(title = "Security") {
                SettingsItem(
                    icon = Icons.Default.VpnKey,
                    title = "API Keys",
                    subtitle = "${uiState.apiKeyCount} keys configured",
                    onClick = { /* Navigate to API keys */ }
                )
                SettingsItem(
                    icon = Icons.Default.Block,
                    title = "IP Allowlist",
                    subtitle = if (uiState.ipAllowlist.isEmpty()) "Not configured" else "${uiState.ipAllowlist.size} IPs",
                    onClick = { /* Show IP allowlist dialog */ }
                )
            }

            // Backup Settings
            SettingsSection(title = "Backup") {
                SettingsItem(
                    icon = Icons.Default.Backup,
                    title = "Automatic Backup",
                    subtitle = if (uiState.backupEnabled) "${uiState.backupFrequency}" else "Disabled",
                    onClick = { viewModel.toggleBackup() }
                )
            }

            // Appearance
            SettingsSection(title = "Appearance") {
                SettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Theme",
                    subtitle = uiState.theme,
                    onClick = { /* Show theme selector */ }
                )
            }

            // About
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.0",
                    onClick = { }
                )
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
