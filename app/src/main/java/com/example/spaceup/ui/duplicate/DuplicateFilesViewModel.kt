package com.example.spaceup.ui.duplicate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spaceup.data.DefaultDataRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface DuplicateFilesUiState {
    object Idle : DuplicateFilesUiState
    object Scanning : DuplicateFilesUiState
    data class Success(
        val duplicateGroups: Map<String, List<File>>,
        val selectedFiles: Set<File>
    ) : DuplicateFilesUiState
    data class Deleting(val progress: Int) : DuplicateFilesUiState
    data class DeleteSuccess(val deletedBytes: Long) : DuplicateFilesUiState
}

class DuplicateFilesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<DuplicateFilesUiState>(DuplicateFilesUiState.Idle)
    val uiState: StateFlow<DuplicateFilesUiState> = _uiState.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<Map<String, List<File>>>(emptyMap())
    private val _selectedFiles = MutableStateFlow<Set<File>>(emptySet())

    fun scanDuplicates() {
        viewModelScope.launch {
            _uiState.value = DuplicateFilesUiState.Scanning
            delay(1000) // Trễ hoạt ảnh quét mượt mà
            
            val foundDuplicates = DefaultDataRepository.getDuplicateFiles()
            _duplicateGroups.value = foundDuplicates
            
            // Tự động tích chọn tất cả các tệp bản sao trùng lặp (chỉ giữ lại 1 tệp gốc duy nhất trong mỗi nhóm)
            val preSelected = mutableSetOf<File>()
            for ((_, files) in foundDuplicates) {
                if (files.size > 1) {
                    // Giữ lại file đầu tiên làm gốc (files[0]), tích chọn các file còn lại (files[1] đến files[n-1]) để xóa
                    for (i in 1 until files.size) {
                        preSelected.add(files[i])
                    }
                }
            }
            _selectedFiles.value = preSelected
            
            _uiState.value = DuplicateFilesUiState.Success(foundDuplicates, preSelected)
        }
    }

    fun toggleFileSelection(file: File, groupKey: String) {
        val currentSelected = _selectedFiles.value.toMutableSet()
        val groupFiles = _duplicateGroups.value[groupKey] ?: emptyList()
        
        if (currentSelected.contains(file)) {
            // Kiểm tra xem nếu bỏ chọn file này, có đảm bảo giữ lại ít nhất 1 file gốc không?
            // (Thực tế bỏ chọn thì file đó được giữ lại, nên luôn an toàn).
            currentSelected.remove(file)
        } else {
            // Nếu muốn chọn file này để xóa:
            // Cảnh báo: Không cho phép chọn TOÀN BỘ file trong nhóm (phải giữ lại ít nhất 1 file gốc).
            val selectedInGroup = groupFiles.filter { currentSelected.contains(it) || it == file }
            if (selectedInGroup.size >= groupFiles.size) {
                // Đã chọn hết file trong nhóm -> không cho chọn file cuối cùng này để giữ lại làm file gốc
                return 
            }
            currentSelected.add(file)
        }
        
        _selectedFiles.value = currentSelected
        _uiState.value = DuplicateFilesUiState.Success(_duplicateGroups.value, currentSelected)
    }

    fun deselectAll() {
        _selectedFiles.value = emptySet()
        _uiState.value = DuplicateFilesUiState.Success(_duplicateGroups.value, emptySet())
    }

    fun selectAllDuplicatesOnly() {
        // Tích chọn lại toàn bộ bản sao, chừa lại đúng 1 bản gốc mỗi nhóm
        val duplicatesOnly = mutableSetOf<File>()
        for ((_, files) in _duplicateGroups.value) {
            if (files.size > 1) {
                for (i in 1 until files.size) {
                    duplicatesOnly.add(files[i])
                }
            }
        }
        _selectedFiles.value = duplicatesOnly
        _uiState.value = DuplicateFilesUiState.Success(_duplicateGroups.value, duplicatesOnly)
    }

    fun deleteSelectedFiles() {
        viewModelScope.launch {
            val filesToDelete = _selectedFiles.value.toList()
            if (filesToDelete.isEmpty()) return@launch

            _uiState.value = DuplicateFilesUiState.Deleting(0)
            delay(300)

            for (progress in 20..100 step 20) {
                _uiState.value = DuplicateFilesUiState.Deleting(progress)
                delay(200)
            }

            // Thực hiện xóa tệp thật từ ổ cứng
            val deletedBytes = DefaultDataRepository.deleteJunkFiles(filesToDelete)

            // Cộng dồn dung lượng giải phóng thực tế vào bộ nhớ hệ thống dùng chung
            DefaultDataRepository.releaseStorage(deletedBytes)

            _uiState.value = DuplicateFilesUiState.DeleteSuccess(deletedBytes)
        }
    }

    fun reset() {
        _uiState.value = DuplicateFilesUiState.Idle
        _duplicateGroups.value = emptyMap()
        _selectedFiles.value = emptySet()
    }
}
