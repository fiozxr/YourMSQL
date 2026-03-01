package com.fiozxr.yoursql.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiozxr.yoursql.data.repository.StorageRepositoryImpl
import com.fiozxr.yoursql.domain.model.Bucket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageUiState(
    val buckets: List<Bucket> = emptyList(),
    val totalUsed: Long = 0,
    val quota: Long = 1L * 1024 * 1024 * 1024,
    val selectedBucket: Bucket? = null,
    val showCreateBucketDialog: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val storageRepository: StorageRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            storageRepository.getAllBuckets()
                .collect { buckets ->
                    val used = storageRepository.getTotalStorageUsed()
                    _uiState.update {
                        it.copy(
                            buckets = buckets,
                            totalUsed = used
                        )
                    }
                }
        }
    }

    fun refresh() {
        loadData()
    }

    fun showCreateBucketDialog() {
        _uiState.update { it.copy(showCreateBucketDialog = true) }
    }

    fun hideCreateBucketDialog() {
        _uiState.update { it.copy(showCreateBucketDialog = false) }
    }

    fun createBucket(name: String, isPublic: Boolean) {
        viewModelScope.launch {
            storageRepository.createBucket(name, isPublic)
            hideCreateBucketDialog()
        }
    }

    fun selectBucket(bucket: Bucket) {
        _uiState.update { it.copy(selectedBucket = bucket) }
    }

    fun deleteBucket(name: String) {
        viewModelScope.launch {
            storageRepository.deleteBucket(name)
        }
    }
}
