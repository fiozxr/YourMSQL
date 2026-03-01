package com.fiozxr.yoursql.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiozxr.yoursql.server.middleware.RequestLogEntry
import com.fiozxr.yoursql.server.middleware.RequestLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogsUiState(
    val logs: List<RequestLogEntry> = emptyList(),
    val filter: String = "",
    val filteredLogs: List<RequestLogEntry> = emptyList()
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val requestLogger: RequestLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            requestLogger.logs.collect { logs ->
                _uiState.update {
                    it.copy(
                        logs = logs,
                        filteredLogs = applyFilter(logs, it.filter)
                    )
                }
            }
        }
    }

    fun updateFilter(filter: String) {
        _uiState.update {
            it.copy(
                filter = filter,
                filteredLogs = applyFilter(it.logs, filter)
            )
        }
    }

    fun clearLogs() {
        requestLogger.clearLogs()
    }

    fun refresh() {
        // Logs are automatically updated via flow
    }

    private fun applyFilter(logs: List<RequestLogEntry>, filter: String): List<RequestLogEntry> {
        if (filter.isBlank()) return logs
        val lowerFilter = filter.lowercase()
        return logs.filter { log ->
            log.method.lowercase().contains(lowerFilter) ||
                    log.path.lowercase().contains(lowerFilter) ||
                    log.statusCode.toString().contains(lowerFilter) ||
                    log.clientIp.contains(lowerFilter)
        }
    }
}
