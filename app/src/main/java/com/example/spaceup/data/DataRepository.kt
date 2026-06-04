package com.example.spaceup.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SystemStatus(
    val usedStorageGb: Double,
    val totalStorageGb: Double = 128.0,
    val usedRamPercent: Int,
    val cpuTempCc: Float
)

interface DataRepository {
    val systemStatus: StateFlow<SystemStatus>
    fun releaseStorage(bytes: Long)
    fun optimizeRam(newPercent: Int)
    fun coolCpu(newTemp: Float)
}

object DefaultDataRepository : DataRepository {
    private val _systemStatus = MutableStateFlow(
        SystemStatus(
            usedStorageGb = 84.5,
            totalStorageGb = 128.0,
            usedRamPercent = 73,
            cpuTempCc = 43.8f
        )
    )
    override val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()

    override fun releaseStorage(bytes: Long) {
        val releasedGb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val current = _systemStatus.value
        val newUsed = (current.usedStorageGb - releasedGb).coerceAtLeast(15.0)
        _systemStatus.value = current.copy(usedStorageGb = newUsed)
    }

    override fun optimizeRam(newPercent: Int) {
        val current = _systemStatus.value
        _systemStatus.value = current.copy(usedRamPercent = newPercent)
    }

    override fun coolCpu(newTemp: Float) {
        val current = _systemStatus.value
        _systemStatus.value = current.copy(cpuTempCc = newTemp)
    }
}
