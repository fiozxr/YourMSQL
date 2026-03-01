package com.fiozxr.yoursql.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.fiozxr.yoursql.data.repository.ApiKeyRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val apiKeyRepository: ApiKeyRepositoryImpl
) : ViewModel() {

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    init {
        initializeDefaultApiKey()
    }

    private fun initializeDefaultApiKey() {
        // The default API key will be created lazily when first requested
        _isInitialized.value = true
    }
}
