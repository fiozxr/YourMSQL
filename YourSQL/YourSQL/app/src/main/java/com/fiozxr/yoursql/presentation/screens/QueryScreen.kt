package com.fiozxr.yoursql.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fiozxr.yoursql.presentation.theme.MonoStyle
import com.fiozxr.yoursql.presentation.viewmodel.QueryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueryScreen(
    viewModel: QueryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Query Editor") },
                actions = {
                    IconButton(onClick = { viewModel.clearQuery() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                    IconButton(onClick = { viewModel.showHistory() }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.executeQuery() },
                icon = { Icon(Icons.Default.PlayArrow, null) },
                text = { Text("Run") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // SQL Editor
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                BasicTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    textStyle = MonoStyle.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (uiState.query.isEmpty()) {
                            Text(
                                "Enter SQL query...",
                                style = MonoStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // Results
            if (uiState.results.isNotEmpty()) {
                ResultsTable(
                    columns = uiState.columns,
                    rows = uiState.results,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            // Status bar
            if (uiState.executionTime > 0) {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${uiState.results.size} rows",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${uiState.executionTime}ms",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultsTable(
    columns: List<String>,
    rows: List<Map<String, Any?>>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.padding(16.dp)) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                columns.forEach { col ->
                    Text(
                        text = col,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Divider()

            // Rows
            LazyColumn {
                items(rows) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        columns.forEach { col ->
                            Text(
                                text = row[col]?.toString() ?: "null",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
