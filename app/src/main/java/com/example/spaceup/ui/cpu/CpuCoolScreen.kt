package com.example.spaceup.ui.cpu

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

sealed interface CpuUiState {
    object Idle : CpuUiState
    data class Cooling(val progress: Int, val currentAction: String) : CpuUiState
    data class Cooled(val beforeTemp: Float, val afterTemp: Float) : CpuUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuCoolScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var cpuState by remember { mutableStateOf<CpuUiState>(CpuUiState.Idle) }
    val coroutineScope = rememberCoroutineScope()

    var currentTemp by remember { mutableFloatStateOf(43.8f) } // Nhiệt độ ban đầu nóng

    val sampleActions = listOf(
        "Đang quét nhiệt độ các nhân CPU...",
        "Phát hiện luồng xử lý dư thừa...",
        "Tạm dừng tiến trình chạy ngầm sinh nhiệt...",
        "Đang giải phóng tài nguyên CPU...",
        "Đang hạ nhiệt hệ thống..."
    )

    fun startCooling() {
        coroutineScope.launch {
            cpuState = CpuUiState.Cooling(0, "Đang chuẩn bị hạ nhiệt CPU...")
            delay(500)

            for (progress in 20..100 step 20) {
                val action = sampleActions[(progress / 20 - 1).coerceIn(0, sampleActions.size - 1)]
                cpuState = CpuUiState.Cooling(progress, action)
                delay(400)
            }

            // Nhiệt độ sau khi hạ nhiệt dao động từ 34.0°C đến 36.5°C
            val finalTemp = Random.nextFloat() * 2.5f + 34.0f
            val originalTemp = currentTemp
            currentTemp = finalTemp
            
            // Đồng bộ trạng thái CPU dùng chung
            com.example.spaceup.data.DefaultDataRepository.coolCpu(finalTemp)
            
            cpuState = CpuUiState.Cooled(originalTemp, finalTemp)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tối Ưu CPU", color = SpaceTextPrimary, fontWeight = FontWeight.Bold) },
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
                targetState = cpuState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "CpuTransition"
            ) { state ->
                when (state) {
                    is CpuUiState.Idle -> {
                        CpuIdleView(
                            currentTemp = currentTemp,
                            onCoolClick = { startCooling() }
                        )
                    }
                    is CpuUiState.Cooling -> {
                        CpuCoolingView(progress = state.progress, currentAction = state.currentAction)
                    }
                    is CpuUiState.Cooled -> {
                        CpuCooledView(
                            beforeTemp = state.beforeTemp,
                            afterTemp = state.afterTemp,
                            onDoneClick = {
                                cpuState = CpuUiState.Idle
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
fun CpuIdleView(currentTemp: Float, onCoolClick: () -> Unit) {
    val isHot = currentTemp > 38.0f
    val tempColor = if (isHot) SpaceWarning else SpaceSuccess
    val tempStatus = if (isHot) "Nhiệt độ cao (Nóng)" else "Nhiệt độ tối ưu (Mát)"

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
                    progress = { currentTemp / 60f }, // Giả lập tỷ lệ dựa trên 60 độ max
                    modifier = Modifier.size(180.dp),
                    color = tempColor,
                    strokeWidth = 12.dp,
                    trackColor = SpaceCardBackground
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format("%.1f°C", currentTemp),
                        color = SpaceTextPrimary,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = tempStatus,
                        color = tempColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Tối Ưu & Hạ Nhiệt CPU",
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
                            text = "Trạng thái CPU: " + if (isHot) "Quá tải nhẹ" else "Bình thường",
                            color = SpaceTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Đóng các tiến trình nền để giảm tải lượng tính toán cho chip xử lý.",
                            color = SpaceTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Button(
            onClick = onCoolClick,
            colors = ButtonDefaults.buttonColors(containerColor = SpaceAccent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics {
                    contentDescription = "Bắt đầu dọn dẹp và làm mát CPU, nhiệt độ hiện tại là ${String.format("%.1f", currentTemp)} độ C"
                }
        ) {
            Text(
                text = "Hạ Nhiệt CPU",
                color = SpaceTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CpuCoolingView(progress: Int, currentAction: String) {
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
            text = "Đang tối ưu hạ nhiệt CPU...",
            color = SpaceTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = currentAction,
            color = SpaceTextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun CpuCooledView(beforeTemp: Float, afterTemp: Float, onDoneClick: () -> Unit) {
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
                text = "CPU Đã Được Hạ Nhiệt!",
                color = SpaceSuccess,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = String.format("Nhiệt độ giảm từ %.1f°C xuống %.1f°C", beforeTemp, afterTemp),
                color = SpaceTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hệ thống hoạt động mát mẻ, ổn định và mượt mà.",
                color = SpaceTextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Button(
            onClick = onDoneClick,
            colors = ButtonDefaults.buttonColors(containerColor = SpaceAccent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics {
                    contentDescription = "Hoàn tất tối ưu CPU, quay lại màn hình chính"
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
