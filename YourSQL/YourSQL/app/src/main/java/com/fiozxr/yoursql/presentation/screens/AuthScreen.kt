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
import com.fiozxr.yoursql.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authentication") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("API Keys") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Users") }
                )
            }

            when (selectedTab) {
                0 -> ApiKeysTab(
                    apiKeys = uiState.apiKeys,
                    onCreateKey = { viewModel.showCreateKeyDialog() },
                    onRevokeKey = { viewModel.revokeApiKey(it) },
                    onCopyKey = { viewModel.copyKeyToClipboard(it) }
                )
                1 -> UsersTab(
                    users = uiState.users,
                    onCreateUser = { viewModel.showCreateUserDialog() },
                    onDeleteUser = { viewModel.deleteUser(it) }
                )
            }
        }
    }
}

@Composable
fun ApiKeysTab(
    apiKeys: List<com.fiozxr.yoursql.domain.model.ApiKey>,
    onCreateKey: () -> Unit,
    onRevokeKey: (String) -> Unit,
    onCopyKey: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onCreateKey,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create API Key")
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apiKeys) { key ->
                ApiKeyCard(
                    apiKey = key,
                    onRevoke = { onRevokeKey(key.key) },
                    onCopy = { onCopyKey(key.key) }
                )
            }
        }
    }
}

@Composable
fun ApiKeyCard(
    apiKey: com.fiozxr.yoursql.domain.model.ApiKey,
    onRevoke: () -> Unit,
    onCopy: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                Column {
                    Text(
                        text = apiKey.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = apiKey.scopeDisplayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                    if (!apiKey.isDefault) {
                        IconButton(onClick = onRevoke) {
                            Icon(Icons.Default.Delete, contentDescription = "Revoke")
                        }
                    }
                }
            }

            Text(
                text = "Created: ${apiKey.formattedCreatedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Requests: ${apiKey.requestCount}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UsersTab(
    users: List<com.fiozxr.yoursql.domain.model.User>,
    onCreateUser: () -> Unit,
    onDeleteUser: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onCreateUser,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create User")
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users) { user ->
                UserCard(
                    user = user,
                    onDelete = { onDeleteUser(user.id) }
                )
            }
        }
    }
}

@Composable
fun UserCard(
    user: com.fiozxr.yoursql.domain.model.User,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Role: ${user.role}",
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
