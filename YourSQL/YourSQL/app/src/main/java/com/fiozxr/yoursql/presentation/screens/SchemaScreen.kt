package com.fiozxr.yoursql.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fiozxr.yoursql.presentation.viewmodel.SchemaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemaScreen(
    viewModel: SchemaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schema Manager") },
                actions = {
                    IconButton(onClick = { viewModel.refreshSchema() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateTableDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Create Table")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.tables.isEmpty()) {
                EmptyState(
                    message = "No tables",
                    action = "Create a table to get started"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.tables) { table ->
                        SchemaTableCard(
                            tableName = table.name,
                            columns = table.columns.map { "${it.name} (${it.type.sqlName})" },
                            onClick = { viewModel.showTableDetails(table) },
                            onDelete = { viewModel.deleteTable(table.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SchemaTableCard(
    tableName: String,
    columns: List<String>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tableName,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }

            Text(
                text = columns.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
