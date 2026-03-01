package com.fiozxr.yoursql.presentation.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiozxr.yoursql.data.repository.ApiKeyRepositoryImpl
import com.fiozxr.yoursql.data.repository.UserRepositoryImpl
import com.fiozxr.yoursql.domain.model.ApiKey
import com.fiozxr.yoursql.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AuthUiState(
    val apiKeys: List<ApiKey> = emptyList(),
    val users: List<User> = emptyList(),
    val showCreateKeyDialog: Boolean = false,
    val showCreateUserDialog: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepositoryImpl,
    private val userRepository: UserRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            apiKeyRepository.getAllApiKeys()
                .collect { keys ->
                    _uiState.update { it.copy(apiKeys = keys) }
                }
        }

        viewModelScope.launch {
            userRepository.getAllUsers()
                .collect { users ->
                    _uiState.update { it.copy(users = users) }
                }
        }
    }

    fun showCreateKeyDialog() {
        _uiState.update { it.copy(showCreateKeyDialog = true) }
    }

    fun hideCreateKeyDialog() {
        _uiState.update { it.copy(showCreateKeyDialog = false) }
    }

    fun createApiKey(name: String, scope: com.fiozxr.yoursql.domain.model.ApiKeyScope) {
        viewModelScope.launch {
            apiKeyRepository.createApiKey(name, scope)
            hideCreateKeyDialog()
        }
    }

    fun revokeApiKey(key: String) {
        viewModelScope.launch {
            apiKeyRepository.revokeApiKey(key)
        }
    }

    fun copyKeyToClipboard(context: Context, key: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("API Key", key)
        clipboard.setPrimaryClip(clip)
    }

    fun showCreateUserDialog() {
        _uiState.update { it.copy(showCreateUserDialog = true) }
    }

    fun hideCreateUserDialog() {
        _uiState.update { it.copy(showCreateUserDialog = false) }
    }

    fun createUser(email: String, password: String) {
        viewModelScope.launch {
            userRepository.createUser(email, password)
            hideCreateUserDialog()
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            userRepository.deleteUser(userId)
        }
    }
}
