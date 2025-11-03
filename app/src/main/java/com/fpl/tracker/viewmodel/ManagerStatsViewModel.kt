package com.fpl.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fpl.tracker.data.api.RetrofitInstance
import com.fpl.tracker.data.models.*
import com.fpl.tracker.data.repository.FPLRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ManagerStatsUiState(
    val isLoading: Boolean = false,
    val managerData: ManagerData? = null,
    val managerHistory: ManagerHistory? = null,
    val bootstrapData: BootstrapData? = null,
    val error: String? = null
)

class ManagerStatsViewModel : ViewModel() {
    private val repository = FPLRepository(RetrofitInstance.api)
    
    private val _uiState = MutableStateFlow(ManagerStatsUiState())
    val uiState: StateFlow<ManagerStatsUiState> = _uiState.asStateFlow()

    fun loadManagerData(managerId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // Load bootstrap data
            val bootstrapResult = repository.getBootstrapData()
            
            // Load manager data
            val managerResult = repository.getManagerData(managerId)
            
            // Load manager history
            val historyResult = repository.getManagerHistory(managerId)
            
            if (managerResult.isFailure || historyResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = managerResult.exceptionOrNull()?.message ?: 
                           historyResult.exceptionOrNull()?.message ?: "Unknown error"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    managerData = managerResult.getOrNull(),
                    managerHistory = historyResult.getOrNull(),
                    bootstrapData = bootstrapResult.getOrNull()
                )
            }
        }
    }
}

