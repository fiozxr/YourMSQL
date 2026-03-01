package com.fiozxr.yoursql.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiozxr.yoursql.data.repository.DatabaseRepositoryImpl
import com.fiozxr.yoursql.domain.repository.QueryExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class QueryUiState(
    val query: String = "",
    val results: List<Map<String, Any?>> = emptyList(),
    val columns: List<String> = emptyList(),
    val executionTime: Long = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showHistory: Boolean = false,
    val queryHistory: List<String> = emptyList()
)

@HiltViewModel
class QueryViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepositoryImpl,
    private val queryExecutor: QueryExecutor
) : ViewModel() {

    private val _uiState = MutableStateFlow(QueryUiState())
    val uiState: StateFlow<QueryUiState> = _uiState.asStateFlow()

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun executeQuery() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val databaseName = databaseRepository.getActiveDatabase()?.name ?: "main"
                val result = queryExecutor.executeQuery(databaseName, query)

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            results = result.rows,
                            columns = result.columns,
                            executionTime = result.executionTimeMs,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            error = result.errorMessage,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Query execution failed")
                _uiState.update {
                    it.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearQuery() {
        _uiState.update {
            it.copy(
                query = "",
                results = emptyList(),
                columns = emptyList(),
                executionTime = 0,
                error = null
            )
        }
    }

    fun showHistory() {
        _uiState.update { it.copy(showHistory = true) }
    }

    fun hideHistory() {
        _uiState.update { it.copy(showHistory = false) }
    }
}
