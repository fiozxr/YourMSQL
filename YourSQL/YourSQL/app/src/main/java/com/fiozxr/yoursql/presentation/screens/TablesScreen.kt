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
import com.fiozxr.yoursql.domain.model.TableInfo
import com.fiozxr.yoursql.presentation.viewmodel.TablesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesScreen(
    viewModel: TablesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tables") },
                actions = {
                    IconButton(onClick = { viewModel.refreshTables() }) {
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
            // Database selector
            if (uiState.databases.size > 1) {
                DatabaseSelector(
                    databases = uiState.databases,
                    selectedDatabase = uiState.selectedDatabase,
                    onDatabaseSelected = { viewModel.selectDatabase(it) }
                )
            }

            // Tables list
            if (uiState.tables.isEmpty()) {
                EmptyState(
                    message = "No tables yet",
                    action = "Create your first table"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.tables) { table ->
                        TableCard(
                            table = table,
                            onClick = { viewModel.selectTable(table) },
                            onDelete = { viewModel.deleteTable(table.name) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableCard(
    table: TableInfo,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = table.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${table.rowCount} rows · ${table.columns.size} columns",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
fun DatabaseSelector(
    databases: List<String>,
    selectedDatabase: String,
    onDatabaseSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(16.dp)) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selectedDatabase)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            databases.forEach { db ->
                DropdownMenuItem(
                    text = { Text(db) },
                    onClick = {
                        onDatabaseSelected(db)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyState(message: String, action: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = action,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
