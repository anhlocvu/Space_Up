package com.example.spaceup.ui.largefiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spaceup.data.DefaultDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface LargeFilesUiState {
    object Idle : LargeFilesUiState
    object Scanning : LargeFilesUiState
    data class Success(val files: List<File>, val selectedFiles: Set<File>) : LargeFilesUiState
    data class Deleting(val progress: Int) : LargeFilesUiState
    data class DeleteSuccess(val deletedBytes: Long) : LargeFilesUiState
}

class LargeFilesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<LargeFilesUiState>(LargeFilesUiState.Idle)
    val uiState: StateFlow<LargeFilesUiState> = _uiState.asStateFlow()

    private val _files = MutableStateFlow<List<File>>(emptyList())
    private val _selectedFiles = MutableStateFlow<Set<File>>(emptySet())

    fun scanFiles(minSizeMb: Long = 10L) {
        viewModelScope.launch {
            _uiState.value = LargeFilesUiState.Scanning
            delay(300)
            
            // Chuyển sang IO Thread để quét file lớn
            val foundFiles = withContext(Dispatchers.IO) {
                DefaultDataRepository.getLargeFiles(minSizeMb)
            }
            _files.value = foundFiles
            _selectedFiles.value = emptySet()
            
            _uiState.value = LargeFilesUiState.Success(foundFiles, emptySet())
        }
    }

    fun toggleFileSelection(file: File) {
        val currentSelected = _selectedFiles.value.toMutableSet()
        if (currentSelected.contains(file)) {
            currentSelected.remove(file)
        } else {
            currentSelected.add(file)
        }
        _selectedFiles.value = currentSelected
        _uiState.value = LargeFilesUiState.Success(_files.value, currentSelected)
    }

    fun selectAllFiles() {
        val allFiles = _files.value.toSet()
        _selectedFiles.value = allFiles
        _uiState.value = LargeFilesUiState.Success(_files.value, allFiles)
    }

    fun deselectAllFiles() {
        _selectedFiles.value = emptySet()
        _uiState.value = LargeFilesUiState.Success(_files.value, emptySet())
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch {
            val filesToDelete = _selectedFiles.value.toList()
            if (filesToDelete.isEmpty()) return@launch

            _uiState.value = LargeFilesUiState.Deleting(0)
            delay(300)

            for (progress in 20..100 step 20) {
                _uiState.value = LargeFilesUiState.Deleting(progress)
                delay(150)
            }

            // Chuyển sang IO Thread để thực hiện xóa các tệp tin
            val deletedBytes = withContext(Dispatchers.IO) {
                DefaultDataRepository.deleteJunkFiles(filesToDelete)
            }

            // Cộng dồn dung lượng giải phóng
            DefaultDataRepository.releaseStorage(deletedBytes)

            _uiState.value = LargeFilesUiState.DeleteSuccess(deletedBytes)
        }
    }

    fun reset() {
        _uiState.value = LargeFilesUiState.Idle
        _files.value = emptyList()
        _selectedFiles.value = emptySet()
    }
}
