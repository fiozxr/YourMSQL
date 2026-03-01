package com.fiozxr.yoursql.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiozxr.yoursql.data.repository.DatabaseRepositoryImpl
import com.fiozxr.yoursql.data.repository.TableRepositoryImpl
import com.fiozxr.yoursql.domain.model.TableInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SchemaUiState(
    val tables: List<TableInfo> = emptyList(),
    val selectedTable: TableInfo? = null,
    val showCreateTableDialog: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class SchemaViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepositoryImpl,
    private val tableRepository: TableRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchemaUiState())
    val uiState: StateFlow<SchemaUiState> = _uiState.asStateFlow()

    init {
        loadTables()
    }

    private fun loadTables() {
        viewModelScope.launch {
            val databaseName = databaseRepository.getActiveDatabase()?.name ?: "main"
            tableRepository.getTables(databaseName)
                .collect { tables ->
                    _uiState.update { it.copy(tables = tables) }
                }
        }
    }

    fun refreshSchema() {
        viewModelScope.launch {
            val databaseName = databaseRepository.getActiveDatabase()?.name ?: "main"
            tableRepository.syncSchemaFromDatabase(databaseName)
        }
    }

    fun showCreateTableDialog() {
        _uiState.update { it.copy(showCreateTableDialog = true) }
    }

    fun hideCreateTableDialog() {
        _uiState.update { it.copy(showCreateTableDialog = false) }
    }

    fun showTableDetails(table: TableInfo) {
        _uiState.update { it.copy(selectedTable = table) }
    }

    fun deleteTable(tableName: String) {
        viewModelScope.launch {
            val databaseName = databaseRepository.getActiveDatabase()?.name ?: "main"
            tableRepository.dropTable(databaseName, tableName)
        }
    }
}
