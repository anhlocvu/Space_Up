package com.example.spaceup.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.spaceup.*
import com.example.spaceup.data.DefaultDataRepository
import com.example.spaceup.data.SystemStatus
import com.example.spaceup.theme.*

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    when (state) {
        MainScreenUiState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize().background(SpaceBackground),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SpacePrimary)
            }
        }
        is MainScreenUiState.Success -> {
            MainScreen(
                status = (state as MainScreenUiState.Success).status,
                onItemClick = onItemClick,
                modifier = modifier
            )
        }
        is MainScreenUiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize().background(SpaceBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lỗi tải thông tin hệ thống: ${(state as MainScreenUiState.Error).throwable.message}",
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(
    status: SystemStatus,
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Space Up", 
                        color = SpaceTextPrimary, 
                        fontWeight = FontWeight.Black, 
                        letterSpacing = 1.sp,
                        fontSize = 24.sp
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceBackground)
            )
        },
        containerColor = SpaceBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Storage Status Card (Dung lượng bộ nhớ)
            StorageCard(status = status)

            // 2. RAM & CPU Quick Info (Bento Grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoBox(
                    title = "Bộ nhớ RAM",
                    value = "${status.usedRamPercent}%",
                    description = if (status.usedRamPercent > 70) "Cần tối ưu" else "Ổn định",
                    color = SpaceSecondary,
                    modifier = Modifier.weight(1f)
                )
                InfoBox(
                    title = "Nhiệt độ CPU",
                    value = String.format("%.1f°C", status.cpuTempCc),
                    description = if (status.cpuTempCc > 38.0f) "Hơi nóng" else "Mát mẻ",
                    color = SpaceAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Menu chức năng chính (Các nút lớn chuẩn Accessible)
            Text(
                text = "CÔNG CỤ TỐI ƯU HỆ THỐNG",
                color = SpaceTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MenuButton(
                    title = "Dọn Rác Hệ Thống",
                    subtitle = "Giải phóng bộ nhớ đệm, tệp dư thừa",
                    icon = Icons.Default.Delete,
                    color = SpacePrimary,
                    onClick = { onItemClick(ScanJunk) },
                    accessibilityLabel = "Dọn rác hệ thống, nhấn đúp để bắt đầu quét các tệp dư thừa"
                )
                
                MenuButton(
                    title = "Tăng Tốc RAM",
                    subtitle = "Giải phóng dung lượng RAM chạy nền",
                    icon = Icons.Default.PlayArrow,
                    color = SpaceSecondary,
                    onClick = { onItemClick(RamBoost) },
                    accessibilityLabel = "Tăng tốc RAM, nhấn đúp để tối ưu hóa bộ nhớ đệm RAM, hiện tại bộ nhớ đang sử dụng ${status.usedRamPercent} phần trăm"
                )

                MenuButton(
                    title = "Tối Ưu & Hạ Nhiệt CPU",
                    subtitle = "Tối ưu hóa các luồng xử lý chip",
                    icon = Icons.Default.Star,
                    color = SpaceAccent,
                    onClick = { onItemClick(CpuCool) },
                    accessibilityLabel = "Tối ưu và hạ nhiệt CPU, nhiệt độ hiện tại là ${String.format("%.1f", status.cpuTempCc)} độ C"
                )

                MenuButton(
                    title = "Giới Thiệu Ứng Dụng",
                    subtitle = "Thông tin nhà phát triển",
                    icon = Icons.Default.Info,
                    color = SpaceTextSecondary,
                    onClick = { onItemClick(About) },
                    accessibilityLabel = "Giới thiệu ứng dụng, nhấn đúp để xem thông tin về nhà phát triển technology entertainment"
                )
            }
        }
    }
}

@Composable
fun StorageCard(status: SystemStatus) {
    val usedGb = status.usedStorageGb
    val totalGb = status.totalStorageGb
    val freeGb = totalGb - usedGb
    val progress = (usedGb / totalGb).toFloat()
    
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Bộ nhớ thiết bị: Đã dùng ${String.format("%.1f", usedGb)} GB trên tổng số ${totalGb.toInt()} GB, còn trống ${String.format("%.1f", freeGb)} GB"
            }
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Bộ nhớ điện thoại",
                        color = SpaceTextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${String.format("%.1f", usedGb)} GB / ${totalGb.toInt()} GB",
                        color = SpaceTextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}% đã dùng",
                    color = SpacePrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Thanh tiến trình Gradient tuyệt đẹp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(SpaceBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(SpacePrimary, SpaceSecondary)
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Bộ nhớ trống: ${String.format("%.1f", freeGb)} GB",
                color = SpaceTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun InfoBox(
    title: String,
    value: String,
    description: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBackground),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = title, color = SpaceTextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = color, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, color = SpaceTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun MenuButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    accessibilityLabel: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = SpaceTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = SpaceTextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SpaceUpTheme {
        MainScreen(
            status = SystemStatus(usedStorageGb = 40.0, totalStorageGb = 128.0, usedRamPercent = 60, cpuTempCc = 36.5f),
            onItemClick = {}
        )
    }
}
