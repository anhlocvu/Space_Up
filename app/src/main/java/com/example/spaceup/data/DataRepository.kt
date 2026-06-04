package com.example.spaceup.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
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
    fun refreshStatus()
}

object DefaultDataRepository : DataRepository {
    private lateinit var appContext: Context
    private val sharedPrefs by lazy {
        appContext.getSharedPreferences("space_up_prefs", Context.MODE_PRIVATE)
    }

    private val _systemStatus = MutableStateFlow(
        SystemStatus(
            usedStorageGb = 0.0,
            totalStorageGb = 128.0,
            usedRamPercent = 50,
            cpuTempCc = 36.5f
        )
    )
    override val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        refreshStatus()
    }

    override fun refreshStatus() {
        if (!::appContext.isInitialized) return

        try {
            // 1. Lấy dữ liệu bộ nhớ lưu trữ THẬT
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            
            val totalBytes = totalBlocks * blockSize
            val availableBytes = availableBlocks * blockSize
            var usedBytes = totalBytes - availableBytes

            // Trừ đi lượng rác ảo/thật đã giải phóng lưu trong SharedPreferences để duy trì trạng thái
            val savedReleaseBytes = sharedPrefs.getLong("released_storage_bytes", 0L)
            usedBytes = (usedBytes - savedReleaseBytes).coerceAtLeast(0L)

            val totalStorageGb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val usedStorageGb = usedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

            // 2. Lấy dữ liệu RAM THẬT
            val mi = ActivityManager.MemoryInfo()
            val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(mi)
            
            val totalRam = mi.totalMem
            val availableRam = mi.availMem
            val actualUsedRam = totalRam - availableRam
            var usedRamPercent = ((actualUsedRam.toDouble() / totalRam.toDouble()) * 100).toInt().coerceIn(10, 99)

            // Đọc trạng thái RAM đã được tối ưu hóa từ SharedPreferences nếu còn hiệu lực (5 phút)
            val lastOptimizeTime = sharedPrefs.getLong("last_ram_optimize_time", 0L)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastOptimizeTime < 5 * 60 * 1000) {
                val optimizedPercent = sharedPrefs.getInt("optimized_ram_percent", -1)
                if (optimizedPercent != -1) {
                    usedRamPercent = optimizedPercent
                }
            }

            // 3. Lấy nhiệt độ CPU thực tế (dựa trên nhiệt độ Pin)
            val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val batteryTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            var cpuTemp = if (batteryTemp > 0) batteryTemp.toFloat() / 10f else 36.8f

            // Đọc nhiệt độ CPU đã được làm mát
            val lastCoolTime = sharedPrefs.getLong("last_cpu_cool_time", 0L)
            if (currentTime - lastCoolTime < 5 * 60 * 1000) {
                val cooledTemp = sharedPrefs.getFloat("cooled_cpu_temp", -1f)
                if (cooledTemp != -1f) {
                    cpuTemp = cooledTemp
                }
            }

            _systemStatus.value = SystemStatus(
                usedStorageGb = usedStorageGb,
                totalStorageGb = totalStorageGb,
                usedRamPercent = usedRamPercent,
                cpuTempCc = cpuTemp
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun releaseStorage(bytes: Long) {
        if (!::appContext.isInitialized) return
        val currentReleased = sharedPrefs.getLong("released_storage_bytes", 0L)
        sharedPrefs.edit().putLong("released_storage_bytes", currentReleased + bytes).apply()
        refreshStatus()
    }

    override fun optimizeRam(newPercent: Int) {
        if (!::appContext.isInitialized) return
        sharedPrefs.edit()
            .putLong("last_ram_optimize_time", System.currentTimeMillis())
            .putInt("optimized_ram_percent", newPercent)
            .apply()
        refreshStatus()
    }

    override fun coolCpu(newTemp: Float) {
        if (!::appContext.isInitialized) return
        sharedPrefs.edit()
            .putLong("last_cpu_cool_time", System.currentTimeMillis())
            .putFloat("cooled_cpu_temp", newTemp)
            .apply()
        refreshStatus()
    }
}
