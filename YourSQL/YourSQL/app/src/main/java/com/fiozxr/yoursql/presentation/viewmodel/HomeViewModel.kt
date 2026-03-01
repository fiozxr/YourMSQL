package com.fiozxr.yoursql.presentation.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiozxr.yoursql.data.repository.*
import com.fiozxr.yoursql.server.engine.ServerService
import com.fiozxr.yoursql.server.middleware.RequestLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HomeUiState(
    val isServerRunning: Boolean = false,
    val serverAddress: String? = null,
    val serverPort: Int = 5432,
    val uptime: Long = 0,
    val requestCount: Long = 0,
    val databaseCount: Int = 0,
    val tableCount: Int = 0,
    val storageUsed: Long = 0,
    val storageQuota: Long = 1L * 1024 * 1024 * 1024,
    val bucketCount: Int = 0,
    val tunnelUrl: String? = null,
    val showCreateDatabaseDialog: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepositoryImpl,
    private val tableRepository: TableRepositoryImpl,
    private val storageRepository: StorageRepositoryImpl,
    private val requestLogger: RequestLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Collect database count
            databaseRepository.getAllDatabases()
                .map { it.size }
                .collect { count ->
                    _uiState.update { it.copy(databaseCount = count) }
                }
        }

        viewModelScope.launch {
            // Collect storage info
            val used = storageRepository.getTotalStorageUsed()
            val buckets = storageRepository.getAllBucketsSync()
            _uiState.update {
                it.copy(
                    storageUsed = used,
                    bucketCount = buckets.size
                )
            }
        }
    }

    fun toggleServer(context: Context) {
        if (_uiState.value.isServerRunning) {
            ServerService.stopService(context)
            _uiState.update { it.copy(isServerRunning = false) }
        } else {
            ServerService.startService(context, _uiState.value.serverPort)
            _uiState.update {
                it.copy(
                    isServerRunning = true,
                    serverAddress = getLocalIpAddress(context)
                )
            }
        }
    }

    fun createDatabase(name: String) {
        viewModelScope.launch {
            databaseRepository.createDatabase(name)
                .onSuccess {
                    Timber.d("Created database: $name")
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to create database")
                }
        }
    }

    fun showCreateDatabaseDialog() {
        _uiState.update { it.copy(showCreateDatabaseDialog = true) }
    }

    fun hideCreateDatabaseDialog() {
        _uiState.update { it.copy(showCreateDatabaseDialog = false) }
    }

    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Tunnel URL", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun getLocalIpAddress(context: Context): String? {
        return try {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val ipInt = wifiManager.connectionInfo.ipAddress
            String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
        } catch (e: Exception) {
            null
        }
    }
}
