package com.example.spaceup

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object ScanJunk : NavKey
@Serializable data class CleanProgress(val fileCount: Int, val junkBytes: Long) : NavKey
@Serializable data class CleanResult(val fileCount: Int, val junkBytes: Long) : NavKey
@Serializable data object RamBoost : NavKey
@Serializable data object CpuCool : NavKey
@Serializable data object About : NavKey
@Serializable data object LargeFiles : NavKey
@Serializable data object DuplicateFiles : NavKey

