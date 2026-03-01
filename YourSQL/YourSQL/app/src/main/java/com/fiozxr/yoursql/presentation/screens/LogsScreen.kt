package com.fiozxr.yoursql.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fiozxr.yoursql.presentation.theme.ErrorRed
import com.fiozxr.yoursql.presentation.theme.SuccessGreen
import com.fiozxr.yoursql.presentation.theme.WarningYellow
import com.fiozxr.yoursql.presentation.viewmodel.LogsViewModel
import com.fiozxr.yoursql.server.middleware.StatusCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    viewModel: LogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Logs") },
                actions = {
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter bar
            OutlinedTextField(
                value = uiState.filter,
                onValueChange = { viewModel.updateFilter(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Filter by method, path, or status...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            // Logs list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.filteredLogs) { log ->
                    LogItem(log = log)
                }
            }
        }
    }
}

@Composable
fun LogItem(log: com.fiozxr.yoursql.server.middleware.RequestLogEntry) {
    val statusColor = when (log.statusCategory) {
        StatusCategory.SUCCESS -> SuccessGreen
        StatusCategory.REDIRECT -> WarningYellow
        StatusCategory.CLIENT_ERROR -> ErrorRed
        StatusCategory.SERVER_ERROR -> ErrorRed.copy(red = 0.8f)
        StatusCategory.UNKNOWN -> Color.Gray
    }

    val methodColor = when (log.method) {
        "GET" -> Color(0xFF4CAF50)
        "POST" -> Color(0xFF2196F3)
        "PUT", "PATCH" -> Color(0xFFFF9800)
        "DELETE" -> Color(0xFFF44336)
        else -> Color.Gray
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Method
            Surface(
                color = methodColor.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = log.method,
                    style = MaterialTheme.typography.labelSmall,
                    color = methodColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Status
            Surface(
                color = statusColor.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = log.statusCode.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Path
            Text(
                text = log.path,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )

            // Latency
            Text(
                text = "${log.latencyMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
