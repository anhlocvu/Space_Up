package com.example.spaceup.ui.junk

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
import kotlin.random.Random

sealed interface JunkUiState {
    object Idle : JunkUiState
    data class Scanning(val progress: Float, val currentFile: String) : JunkUiState
    data class ScanCompleted(
        val junkFiles: List<File>,
        val duplicateGroups: Map<String, List<File>>,
        val selectedFiles: Set<File>
    ) : JunkUiState
    data class Cleaning(val progress: Int) : JunkUiState
    data class CleanCompleted(val fileCount: Int, val junkBytes: Long) : JunkUiState
}

class JunkViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<JunkUiState>(JunkUiState.Idle)
    val uiState: StateFlow<JunkUiState> = _uiState.asStateFlow()

    private var _junkFiles = listOf<File>()
    private var _duplicateGroups = mapOf<String, List<File>>()
    private val _selectedFiles = MutableStateFlow<Set<File>>(emptySet())

    private val sampleJunkPaths = listOf(
        "/storage/emulated/0/Android/data/com.facebook.katana/cache/temp_icon_01.png",
        "/storage/emulated/0/Android/data/com.ss.android.ugc.trill/cache/video_cache_902.mp4",
        "/storage/emulated/0/Android/data/com.google.android.youtube/cache/thumb_preview.bin",
        "/storage/emulated/0/Download/.tmp_pending_3081.tmp",
        "/storage/emulated/0/Android/data/com.android.chrome/cache/CacheDir/index.db",
        "/storage/emulated/0/DCIM/.thumbnails/thumb_0921.jpg",
        "/storage/emulated/0/System/Logs/boot_debug.log",
        "/storage/emulated/0/Pictures/.temp_cache_image.png"
    )

    fun startScanning(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.value = JunkUiState.Scanning(0f, "Khởi động quét bộ nhớ...")
            delay(200)
            
            // 1. Quét tệp cache và tệp trùng lặp thực tế ngầm trên IO Thread
            val scanResult = withContext(Dispatchers.IO) {
                val junkFiles = DefaultDataRepository.getStorageJunkFiles()
                val duplicateGroups = DefaultDataRepository.getDuplicateFiles()
                Pair(junkFiles, duplicateGroups)
            }
            
            _junkFiles = scanResult.first
            _duplicateGroups = scanResult.second

            // Chọn mặc định: toàn bộ file rác và các bản sao của tệp trùng lặp (chừa lại file đầu tiên làm gốc)
            val preSelected = mutableSetOf<File>()
            preSelected.addAll(_junkFiles)
            for ((_, files) in _duplicateGroups) {
                if (files.size > 1) {
                    for (i in 1 until files.size) {
                        preSelected.add(files[i])
                    }
                }
            }
            _selectedFiles.value = preSelected

            // Trình diễn tiến trình quét chạy trên Main Thread để có hoạt ảnh sinh động
            val totalSteps = 10
            val allScanned = _junkFiles + _duplicateGroups.values.flatten()
            for (i in 1..totalSteps) {
                val progress = i.toFloat() / totalSteps
                
                val scanningFile = if (allScanned.isNotEmpty() && i <= allScanned.size) {
                    allScanned[i - 1].absolutePath
                } else {
                    sampleJunkPaths[Random.nextInt(sampleJunkPaths.size)]
                }
                
                _uiState.value = JunkUiState.Scanning(progress, scanningFile)
                delay(120)
            }

            _uiState.value = JunkUiState.ScanCompleted(
                junkFiles = _junkFiles,
                duplicateGroups = _duplicateGroups,
                selectedFiles = _selectedFiles.value
            )
        }
    }

    fun toggleFileSelection(file: File) {
        val currentState = _uiState.value
        if (currentState is JunkUiState.ScanCompleted) {
            val currentSelected = _selectedFiles.value.toMutableSet()
            if (currentSelected.contains(file)) {
                currentSelected.remove(file)
            } else {
                // Bảo vệ bản sao trùng lặp: không cho phép chọn toàn bộ file trong cùng một nhóm
                var isAllowed = true
                for ((_, files) in _duplicateGroups) {
                    if (files.contains(file)) {
                        val selectedInGroup = files.filter { currentSelected.contains(it) || it == file }
                        if (selectedInGroup.size >= files.size) {
                            isAllowed = false // Không cho phép chọn file cuối cùng để giữ lại làm bản gốc
                        }
                        break
                    }
                }
                if (isAllowed) {
                    currentSelected.add(file)
                }
            }
            _selectedFiles.value = currentSelected
            _uiState.value = JunkUiState.ScanCompleted(
                junkFiles = _junkFiles,
                duplicateGroups = _duplicateGroups,
                selectedFiles = currentSelected
            )
        }
    }

    fun selectAll() {
        val currentState = _uiState.value
        if (currentState is JunkUiState.ScanCompleted) {
            val allDeletable = mutableSetOf<File>()
            allDeletable.addAll(_junkFiles)
            for ((_, files) in _duplicateGroups) {
                if (files.size > 1) {
                    for (i in 1 until files.size) {
                        allDeletable.add(files[i])
                    }
                }
            }
            _selectedFiles.value = allDeletable
            _uiState.value = JunkUiState.ScanCompleted(
                junkFiles = _junkFiles,
                duplicateGroups = _duplicateGroups,
                selectedFiles = allDeletable
            )
        }
    }

    fun deselectAll() {
        val currentState = _uiState.value
        if (currentState is JunkUiState.ScanCompleted) {
            _selectedFiles.value = emptySet()
            _uiState.value = JunkUiState.ScanCompleted(
                junkFiles = _junkFiles,
                duplicateGroups = _duplicateGroups,
                selectedFiles = emptySet()
            )
        }
    }

    fun startCleaning(context: android.content.Context) {
        val currentState = _uiState.value
        if (currentState is JunkUiState.ScanCompleted) {
            val filesToDelete = currentState.selectedFiles.toList()
            val totalCount = filesToDelete.size
            
            viewModelScope.launch {
                _uiState.value = JunkUiState.Cleaning(0)
                delay(200)

                for (progress in 10..100 step 10) {
                    _uiState.value = JunkUiState.Cleaning(progress)
                    delay(50)
                }

                // Thực hiện xóa thật trên IO Thread
                val deletedBytes = withContext(Dispatchers.IO) {
                    DefaultDataRepository.deleteJunkFiles(filesToDelete)
                }

                // Đồng bộ và làm mới dung lượng hệ thống thực tế
                DefaultDataRepository.releaseStorage(deletedBytes)

                _uiState.value = JunkUiState.CleanCompleted(totalCount, deletedBytes)
            }
        }
    }

    fun reset() {
        _uiState.value = JunkUiState.Idle
        _junkFiles = emptyList()
        _duplicateGroups = emptyMap()
        _selectedFiles.value = emptySet()
    }
}
