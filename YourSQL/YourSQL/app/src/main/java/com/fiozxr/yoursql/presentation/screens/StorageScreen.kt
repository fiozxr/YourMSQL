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
import com.fiozxr.yoursql.presentation.viewmodel.StorageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    viewModel: StorageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateBucketDialog() }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "Create Bucket")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Storage usage
            StorageUsageBar(
                used = uiState.totalUsed,
                total = uiState.quota,
                modifier = Modifier.padding(16.dp)
            )

            // Buckets list
            if (uiState.buckets.isEmpty()) {
                EmptyState(
                    message = "No buckets",
                    action = "Create a bucket to store files"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.buckets) { bucket ->
                        BucketCard(
                            name = bucket.name,
                            fileCount = bucket.fileCount,
                            size = bucket.formattedTotalSize,
                            isPublic = bucket.isPublic,
                            onClick = { viewModel.selectBucket(bucket) },
                            onDelete = { viewModel.deleteBucket(bucket.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StorageUsageBar(
    used: Long,
    total: Long,
    modifier: Modifier = Modifier
) {
    val percent = if (total > 0) (used.toFloat() / total * 100) else 0f

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Storage Usage",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = String.format("%.1f%%", percent),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatFileSize(used),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = formatFileSize(total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BucketCard(
    name: String,
    fileCount: Int,
    size: String,
    isPublic: Boolean,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Column {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$fileCount files · $size",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                if (isPublic) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Public",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
        size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0))
    }
}
