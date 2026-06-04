package com.example.spaceup.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spaceup.data.DataRepository
import com.example.spaceup.data.SystemStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface MainScreenUiState {
    object Loading : MainScreenUiState
    data class Error(val throwable: Throwable) : MainScreenUiState
    data class Success(val status: SystemStatus) : MainScreenUiState
}

class MainScreenViewModel(dataRepository: DataRepository) : ViewModel() {
    val uiState: StateFlow<MainScreenUiState> =
        dataRepository.systemStatus
            .map<SystemStatus, MainScreenUiState> { MainScreenUiState.Success(it) }
            .catch { emit(MainScreenUiState.Error(it)) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                MainScreenUiState.Loading
            )
}
