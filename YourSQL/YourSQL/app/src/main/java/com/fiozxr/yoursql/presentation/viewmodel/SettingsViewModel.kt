package com.fiozxr.yoursql.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiozxr.yoursql.data.SettingsDataStore
import com.fiozxr.yoursql.data.repository.ApiKeyRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val serverPort: Int = 5432,
    val httpsEnabled: Boolean = false,
    val apiKeyCount: Int = 0,
    val ipAllowlist: List<String> = emptyList(),
    val backupEnabled: Boolean = false,
    val backupFrequency: String = "daily",
    val theme: String = "system",
    val isLoading: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val apiKeyRepository: ApiKeyRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsDataStore.serverPort,
                settingsDataStore.httpsEnabled,
                settingsDataStore.ipAllowlist,
                settingsDataStore.backupEnabled,
                settingsDataStore.backupFrequency,
                settingsDataStore.theme
            ) { port, https, ips, backup, freq, theme ->
                SettingsUiState(
                    serverPort = port,
                    httpsEnabled = https,
                    ipAllowlist = ips,
                    backupEnabled = backup,
                    backupFrequency = freq,
                    theme = theme
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        viewModelScope.launch {
            val keys = apiKeyRepository.getAllApiKeysSync()
            _uiState.update { it.copy(apiKeyCount = keys.size) }
        }
    }

    fun setServerPort(port: Int) {
        viewModelScope.launch {
            settingsDataStore.setServerPort(port)
        }
    }

    fun toggleHttps() {
        viewModelScope.launch {
            settingsDataStore.setHttpsEnabled(!_uiState.value.httpsEnabled)
        }
    }

    fun toggleBackup() {
        viewModelScope.launch {
            settingsDataStore.setBackupEnabled(!_uiState.value.backupEnabled)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsDataStore.setTheme(theme)
        }
    }
}
