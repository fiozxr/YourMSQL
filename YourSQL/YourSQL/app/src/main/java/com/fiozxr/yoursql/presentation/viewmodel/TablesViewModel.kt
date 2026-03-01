package com.fiozxr.yoursql.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiozxr.yoursql.data.repository.DatabaseRepositoryImpl
import com.fiozxr.yoursql.data.repository.TableRepositoryImpl
import com.fiozxr.yoursql.domain.model.TableInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class TablesUiState(
    val databases: List<String> = emptyList(),
    val selectedDatabase: String = "",
    val tables: List<TableInfo> = emptyList(),
    val isLoading: Boolean = false,
    val showCreateTableDialog: Boolean = false,
    val selectedTable: TableInfo? = null
)

@HiltViewModel
class TablesViewModel @Inject constructor(
    private val databaseRepository: DatabaseRepositoryImpl,
    private val tableRepository: TableRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(TablesUiState())
    val uiState: StateFlow<TablesUiState> = _uiState.asStateFlow()

    init {
        loadDatabases()
    }

    private fun loadDatabases() {
        viewModelScope.launch {
            databaseRepository.getAllDatabases()
                .collect { databases ->
                    val dbNames = databases.map { it.name }
                    val activeDb = databases.find { it.isActive }?.name ?: dbNames.firstOrNull() ?: ""

                    _uiState.update {
                        it.copy(
                            databases = dbNames,
                            selectedDatabase = activeDb
                        )
                    }

                    if (activeDb.isNotEmpty()) {
                        loadTables(activeDb)
                    }
                }
        }
    }

    private fun loadTables(databaseName: String) {
        viewModelScope.launch {
            tableRepository.getTables(databaseName)
                .collect { tables ->
                    _uiState.update { it.copy(tables = tables) }
                }
        }
    }

    fun selectDatabase(databaseName: String) {
        _uiState.update { it.copy(selectedDatabase = databaseName) }
        loadTables(databaseName)
    }

    fun refreshTables() {
        val dbName = _uiState.value.selectedDatabase
        if (dbName.isNotEmpty()) {
            viewModelScope.launch {
                tableRepository.syncSchemaFromDatabase(dbName)
            }
        }
    }

    fun selectTable(table: TableInfo) {
        _uiState.update { it.copy(selectedTable = table) }
    }

    fun deleteTable(tableName: String) {
        viewModelScope.launch {
            val dbName = _uiState.value.selectedDatabase
            tableRepository.dropTable(dbName, tableName)
                .onSuccess {
                    Timber.d("Deleted table: $tableName")
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to delete table")
                }
        }
    }

    fun showCreateTableDialog() {
        _uiState.update { it.copy(showCreateTableDialog = true) }
    }

    fun hideCreateTableDialog() {
        _uiState.update { it.copy(showCreateTableDialog = false) }
    }
}
