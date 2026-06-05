package com.example.spaceup.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

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
    suspend fun getStorageJunkFiles(): List<File>
    suspend fun deleteJunkFiles(files: List<File>): Long
    suspend fun getLargeFiles(minSizeMb: Long): List<File>
    suspend fun getDuplicateFiles(): Map<String, List<File>>
    fun hasStoragePermission(): Boolean
    fun getPermissionIntent(): android.content.Intent?
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
            // 1. Lấy dữ liệu bộ nhớ lưu trữ THẬT và bù đắp phần dung lượng hệ thống
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            
            val totalBytes = totalBlocks * blockSize
            val availableBytes = availableBlocks * blockSize

            // Tính toán dung lượng thô (thường là 111 GB khả dụng của phân vùng data)
            val totalStorageGbRaw = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            
            // Làm tròn lên các mốc dung lượng vật lý tiêu chuẩn của nhà sản xuất (32, 64, 128, 256, 512)
            val totalStorageGb = when {
                totalStorageGbRaw <= 32.0 -> 32.0
                totalStorageGbRaw <= 64.0 -> 64.0
                totalStorageGbRaw <= 128.0 -> 128.0
                totalStorageGbRaw <= 256.0 -> 256.0
                totalStorageGbRaw <= 512.0 -> 512.0
                else -> Math.ceil(totalStorageGbRaw / 128.0) * 128.0
            }

            // Dung lượng trống thực tế (đơn vị GB)
            val availableGb = availableBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

            // Tính dung lượng đã dùng: Lấy tổng dung lượng tiêu chuẩn trừ đi dung lượng trống thực tế
            var usedStorageGb = totalStorageGb - availableGb
            usedStorageGb = usedStorageGb.coerceIn(10.0, totalStorageGb - 1.0)

            // 2. Lấy dữ liệu RAM THẬT
            val mi = ActivityManager.MemoryInfo()
            val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(mi)
            
            val totalRam = mi.totalMem
            val availableRam = mi.availMem
            val actualUsedRam = totalRam - availableRam
            var usedRamPercent = ((actualUsedRam.toDouble() / totalRam.toDouble()) * 100).toInt().coerceIn(10, 99)

            // Đọc trạng thái RAM đã được tối ưu hóa từ SharedPreferences
            val lastOptimizeTime = sharedPrefs.getLong("last_ram_optimize_time", 0L)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastOptimizeTime < 5 * 60 * 1000) {
                val optimizedPercent = sharedPrefs.getInt("optimized_ram_percent", -1)
                if (optimizedPercent != -1) {
                    usedRamPercent = optimizedPercent
                }
            }

            // 3. Lấy nhiệt độ CPU thực tế (thông qua Pin)
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

    // Lấy danh sách các file rác thực tế có thể dọn dẹp
    override suspend fun getStorageJunkFiles(): List<File> = withContext(Dispatchers.IO) {
        if (!::appContext.isInitialized) return@withContext emptyList()
        val junkFiles = mutableListOf<File>()

        // 1. Quét cache trong bộ nhớ trong của ứng dụng Space Up
        appContext.cacheDir?.let { addFilesFromDir(it, junkFiles) }
        yield()
        appContext.externalCacheDir?.let { addFilesFromDir(it, junkFiles) }
        yield()

        // 2. Quét thư mục tạm thời hệ thống nếu có quyền
        val tempDir = File(Environment.getExternalStorageDirectory(), "Download")
        if (tempDir.exists() && tempDir.isDirectory) {
            val files = tempDir.listFiles()
            if (files != null) {
                for (file in files) {
                    yield()
                    // Quét các file .tmp, .log, hoặc các file tải xuống tạm thời trong Download
                    if (file.isFile && (file.name.endsWith(".tmp") || file.name.endsWith(".log") || file.name.startsWith(".tmp_"))) {
                        junkFiles.add(file)
                    }
                }
            }
        }

        junkFiles
    }

    private suspend fun addFilesFromDir(dir: File, list: MutableList<File>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            yield()
            if (file.isDirectory) {
                addFilesFromDir(file, list)
            } else {
                list.add(file)
            }
        }
    }

    // Xóa các file rác thực tế và trả về số byte đã dọn được
    override suspend fun deleteJunkFiles(files: List<File>): Long = withContext(Dispatchers.IO) {
        var totalDeletedBytes = 0L
        for (file in files) {
            yield()
            try {
                val size = file.length()
                if (file.delete()) {
                    totalDeletedBytes += size
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        totalDeletedBytes
    }

    // Quét bộ nhớ thật tìm các file lớn hơn minSizeMb
    override suspend fun getLargeFiles(minSizeMb: Long): List<File> = withContext(Dispatchers.IO) {
        if (!::appContext.isInitialized) return@withContext emptyList()
        val largeFiles = mutableListOf<File>()
        val minSize = minSizeMb * 1024 * 1024

        // Các thư mục công cộng phổ biến chứa file lớn
        val publicDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        )

        for (dir in publicDirs) {
            yield()
            if (dir.exists() && dir.isDirectory) {
                scanLargeFiles(dir, minSize, largeFiles)
            }
        }

        // Sắp xếp các file lớn theo dung lượng giảm dần
        largeFiles.sortByDescending { it.length() }
        largeFiles
    }

    private suspend fun scanLargeFiles(dir: File, minSize: Long, list: MutableList<File>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            yield()
            if (file.name.startsWith(".")) continue

            if (file.isDirectory) {
                scanLargeFiles(file, minSize, list)
            } else {
                if (file.length() >= minSize) {
                    list.add(file)
                }
            }
        }
    }

    // Quét bộ nhớ và tìm kiếm các tệp tin trùng lặp
    override suspend fun getDuplicateFiles(): Map<String, List<File>> = withContext(Dispatchers.IO) {
        if (!::appContext.isInitialized) return@withContext emptyMap()
        
        // 1. Quét tìm tất cả các tệp tin trong các thư mục công cộng
        val allFiles = mutableListOf<File>()
        val publicDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        )

        // Đặt giới hạn tối đa tệp được quét để tránh treo CPU và đĩa
        val maxFilesLimit = 3000
        val fileCounter = IntArray(1) { 0 }

        for (dir in publicDirs) {
            yield()
            if (dir.exists() && dir.isDirectory) {
                getAllFilesRecursive(dir, allFiles, fileCounter, maxFilesLimit)
                if (fileCounter[0] >= maxFilesLimit) {
                    break
                }
            }
        }

        // 2. Nhóm các file theo kích thước (chỉ giữ lại các kích thước có từ 2 file trở lên)
        val filesBySize = allFiles.filter { it.isFile && it.length() > 0 }
            .groupBy { it.length() }
            .filter { it.value.size >= 2 }

        // 3. Tính toán Fast MD5 Hash cho các file cùng kích thước để tìm trùng lặp thực tế
        val duplicateMap = mutableMapOf<String, MutableList<File>>()
        
        for ((_, fileList) in filesBySize) {
            yield()
            for (file in fileList) {
                yield()
                val fileHash = calculateFastFileHash(file)
                if (fileHash.isNotEmpty()) {
                    val list = duplicateMap.getOrPut(fileHash) { mutableListOf() }
                    list.add(file)
                }
            }
        }

        // Lọc lại: Chỉ giữ lại các nhóm mã băm có từ 2 file trùng trở lên
        duplicateMap.filter { it.value.size >= 2 }
    }

    private suspend fun getAllFilesRecursive(dir: File, list: MutableList<File>, counter: IntArray, limit: Int) {
        if (counter[0] >= limit) return
        val files = dir.listFiles() ?: return
        for (file in files) {
            yield()
            if (file.name.startsWith(".")) continue
            if (file.isDirectory) {
                getAllFilesRecursive(file, list, counter, limit)
                if (counter[0] >= limit) return
            } else {
                list.add(file)
                counter[0]++
                if (counter[0] >= limit) return
            }
        }
    }

    // Hàm tính MD5 nhanh bằng cách đọc 8KB đầu và 8KB cuối của tệp tin kết hợp với kích thước file
    private fun calculateFastFileHash(file: File): String {
        if (!file.exists() || file.length() == 0L) return ""
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val stream = FileInputStream(file)
            val size = file.length()
            val bufferSize = 8192
            val buffer = ByteArray(bufferSize)

            // Đọc 8KB đầu tiên
            val bytesRead = stream.read(buffer, 0, bufferSize)
            if (bytesRead > 0) {
                digest.update(buffer, 0, bytesRead)
            }

            // Nếu file lớn hơn 8KB, nhảy đến gần cuối và đọc tiếp 8KB cuối cùng
            if (size > bufferSize) {
                val channel = stream.channel
                channel.position(size - bufferSize)
                val endBytesRead = stream.read(buffer, 0, bufferSize)
                if (endBytesRead > 0) {
                    digest.update(buffer, 0, endBytesRead)
                }
            }
            stream.close()

            // Thêm size vào hash để tăng độ phân biệt
            digest.update(size.toString().toByteArray())

            val hashBytes = digest.digest()
            val sb = StringBuilder()
            for (b in hashBytes) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (e: Exception) {
            // Trường hợp lỗi, trả về chuỗi định danh tạm thời
            file.name + "_" + file.length()
        }
    }

    override fun hasStoragePermission(): Boolean {
        if (!::appContext.isInitialized) return false
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            val check = appContext.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            check == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    override fun getPermissionIntent(): android.content.Intent? {
        if (!::appContext.isInitialized) return null
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = android.net.Uri.parse("package:${appContext.packageName}")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (e: Exception) {
                android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        } else {
            android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${appContext.packageName}")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
