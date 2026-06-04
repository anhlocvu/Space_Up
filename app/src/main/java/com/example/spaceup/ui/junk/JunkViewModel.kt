package com.example.spaceup.ui.junk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spaceup.data.DefaultDataRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface JunkUiState {
    object Idle : JunkUiState
    data class Scanning(val progress: Float, val currentFile: String) : JunkUiState
    data class ScanCompleted(val fileCount: Int, val junkBytes: Long) : JunkUiState
    data class Cleaning(val progress: Int) : JunkUiState
    data class CleanCompleted(val fileCount: Int, val junkBytes: Long) : JunkUiState
}

class JunkViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<JunkUiState>(JunkUiState.Idle)
    val uiState: StateFlow<JunkUiState> = _uiState.asStateFlow()

    private val sampleJunkPaths = listOf(
        "/storage/emulated/0/Android/data/com.facebook.katana/cache",
        "/storage/emulated/0/Android/data/com.ss.android.ugc.trill/cache",
        "/storage/emulated/0/Android/data/com.google.android.youtube/cache",
        "/storage/emulated/0/Download/.tmp_pending_3081",
        "/storage/emulated/0/Android/data/com.android.chrome/cache/CacheDir",
        "/storage/emulated/0/DCIM/.thumbnails/thumb_0921.jpg",
        "/storage/emulated/0/System/Logs/boot_debug.log",
        "/storage/emulated/0/Pictures/.temp_cache_image.png"
    )

    fun startScanning() {
        viewModelScope.launch {
            _uiState.value = JunkUiState.Scanning(0f, "Khởi động quét bộ nhớ...")
            delay(400)
            
            val totalSteps = 10
            for (i in 1..totalSteps) {
                val progress = i.toFloat() / totalSteps
                val randomFile = sampleJunkPaths[Random.nextInt(sampleJunkPaths.size)] + "/file_" + Random.nextInt(100, 999) + ".tmp"
                _uiState.value = JunkUiState.Scanning(progress, randomFile)
                delay(250)
            }

            val fileCount = Random.nextInt(1500, 4800)
            val junkBytes = Random.nextLong(450, 3200) * 1024 * 1024
            _uiState.value = JunkUiState.ScanCompleted(fileCount, junkBytes)
        }
    }

    fun startCleaning(fileCount: Int, junkBytes: Long) {
        viewModelScope.launch {
            _uiState.value = JunkUiState.Cleaning(0)
            delay(300)

            for (progress in 5..100 step 5) {
                _uiState.value = JunkUiState.Cleaning(progress)
                delay(120)
            }

            // Đồng bộ dữ liệu dung lượng lưu trữ vào DefaultDataRepository dùng chung
            DefaultDataRepository.releaseStorage(junkBytes)

            _uiState.value = JunkUiState.CleanCompleted(fileCount, junkBytes)
        }
    }

    fun reset() {
        _uiState.value = JunkUiState.Idle
    }
}
