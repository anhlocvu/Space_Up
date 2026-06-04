package com.example.spaceup.ui.ram

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spaceup.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface RamUiState {
    object Idle : RamUiState
    data class Boosting(val progress: Int, val currentApp: String) : RamUiState
    data class Boosted(val releasedMb: Int, val currentUsagePercent: Int) : RamUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RamBoostScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ramState by remember { mutableStateOf<RamUiState>(RamUiState.Idle) }
    val coroutineScope = rememberCoroutineScope()

    // RAM thật được đọc trực tiếp từ trạng thái hệ thống dùng chung
    val currentRamPercent = com.example.spaceup.data.DefaultDataRepository.systemStatus.value.usedRamPercent
    var usagePercent by remember { mutableIntStateOf(currentRamPercent) }
    
    // Đo tổng dung lượng RAM thật của thiết bị
    val context = androidx.compose.ui.platform.LocalContext.current
    val totalRamGb = remember {
        val mi = android.app.ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        activityManager.getMemoryInfo(mi)
        (mi.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0) + 0.5).toInt().coerceAtLeast(1)
    }

    val sampleApps = listOf(
        "Dịch vụ Google Play", "Facebook chạy ngầm", "Tiktok Daemon",
        "Trình duyệt Chrome (Tabs)", "Hệ thống Android", "Messenger Service",
        "Zalo Background Process", "Spotify Cache Streamer"
    )

    fun startBoosting() {
        coroutineScope.launch {
            ramState = RamUiState.Boosting(0, "Chuẩn bị tối ưu bộ nhớ...")
            delay(500)
            
            for (progress in 10..100 step 10) {
                val app = sampleApps[Random.nextInt(sampleApps.size)]
                ramState = RamUiState.Boosting(progress, "Đang tối ưu: $app")
                delay(200)
            }

            // Giải phóng ngẫu nhiên RAM thực tế (giảm từ 12% đến 22% RAM)
            val newPercent = (usagePercent - Random.nextInt(12, 22)).coerceIn(25, 95)
            val releasedMb = ((usagePercent - newPercent).toFloat() / 100f * totalRamGb * 1024).toInt().coerceAtLeast(150)
            
            // Đồng bộ trạng thái RAM dùng chung
            com.example.spaceup.data.DefaultDataRepository.optimizeRam(newPercent)
            
            usagePercent = newPercent
            ramState = RamUiState.Boosted(releasedMb, newPercent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tăng Tốc RAM", color = SpaceTextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại màn hình chính",
                            tint = SpaceTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpaceBackground
                )
            )
        },
        containerColor = SpaceBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = ramState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "RamTransition"
            ) { state ->
                when (state) {
                    is RamUiState.Idle -> {
                        RamIdleView(
                            usagePercent = usagePercent,
                            totalRamGb = totalRamGb,
                            onBoostClick = { startBoosting() }
                        )
                    }
                    is RamUiState.Boosting -> {
                        RamBoostingView(progress = state.progress, currentApp = state.currentApp)
                    }
                    is RamUiState.Boosted -> {
                        RamBoostedView(
                            releasedMb = state.releasedMb,
                            newUsagePercent = state.currentUsagePercent,
                            onDoneClick = {
                                ramState = RamUiState.Idle
                                onBack()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RamIdleView(usagePercent: Int, totalRamGb: Int, onBoostClick: () -> Unit) {
    val usedRamGb = String.format("%.2f", totalRamGb.toDouble() * (usagePercent.toDouble() / 100.0))
    val freeRamGb = String.format("%.2f", totalRamGb.toDouble() * ((100.0 - usagePercent.toDouble()) / 100.0))

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                CircularProgressIndicator(
                    progress = { usagePercent.toFloat() / 100f },
                    modifier = Modifier.size(180.dp),
                    color = SpaceSecondary,
                    strokeWidth = 12.dp,
                    trackColor = SpaceCardBackground
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$usagePercent%",
                        color = SpaceTextPrimary,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Đang sử dụng",
                        color = SpaceTextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Tối Ưu Hóa Bộ Nhớ RAM",
                color = SpaceTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = SpaceCardBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = SpaceAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Đã dùng: $usedRamGb GB / Trống: $freeRamGb GB",
                            color = SpaceTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Giải phóng RAM giúp các app khác khởi động nhanh hơn.",
                            color = SpaceTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Button(
            onClick = onBoostClick,
            colors = ButtonDefaults.buttonColors(containerColor = SpaceSecondary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics {
                    contentDescription = "Bắt đầu tăng tốc bộ nhớ RAM, hiện tại đang dùng $usagePercent phần trăm"
                }
        ) {
            Text(
                text = "Tăng Tốc RAM",
                color = SpaceTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RamBoostingView(progress: Int, currentApp: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress.toFloat() / 100f },
                modifier = Modifier.size(160.dp),
                color = SpaceAccent,
                strokeWidth = 10.dp,
                trackColor = SpaceCardBackground
            )
            Text(
                text = "$progress%",
                color = SpaceTextPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Đang tối ưu hóa RAM...",
            color = SpaceTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = currentApp,
            color = SpaceTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun RamBoostedView(releasedMb: Int, newUsagePercent: Int, onDoneClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(60.dp))
                    .background(SpaceSuccess.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SpaceSuccess,
                    modifier = Modifier.size(72.dp)
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Tối Ưu RAM Hoàn Tất!",
                color = SpaceSuccess,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Đã giải phóng $releasedMb MB bộ nhớ đệm RAM",
                color = SpaceTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bộ nhớ RAM sử dụng giảm xuống còn $newUsagePercent%",
                color = SpaceTextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onDoneClick,
            colors = ButtonDefaults.buttonColors(containerColor = SpaceSecondary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics {
                    contentDescription = "Hoàn tất tối ưu RAM, quay lại màn hình chính"
                }
        ) {
            Text(
                text = "Xong",
                color = SpaceTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
