package com.example.spaceup.ui.junk

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spaceup.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanJunkScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JunkViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Tự động quét khi màn hình được hiển thị lần đầu
    LaunchedEffect(Unit) {
        if (uiState is JunkUiState.Idle) {
            viewModel.startScanning()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dọn Rác Hệ Thống", color = SpaceTextPrimary, fontWeight = FontWeight.Bold) },
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
                targetState = uiState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "JunkTransition"
            ) { state ->
                when (state) {
                    is JunkUiState.Idle, is JunkUiState.Scanning -> {
                        val progress = if (state is JunkUiState.Scanning) state.progress else 0f
                        val currentFile = if (state is JunkUiState.Scanning) state.currentFile else ""
                        ScanningView(progress = progress, currentFile = currentFile)
                    }
                    is JunkUiState.ScanCompleted -> {
                        ScanCompletedView(
                            fileCount = state.fileCount,
                            junkBytes = state.junkBytes,
                            onCleanClick = {
                                viewModel.startCleaning(state.fileCount, state.junkBytes)
                            }
                        )
                    }
                    is JunkUiState.Cleaning -> {
                        CleaningView(progress = state.progress)
                    }
                    is JunkUiState.CleanCompleted -> {
                        CleanCompletedView(
                            fileCount = state.fileCount,
                            junkBytes = state.junkBytes,
                            onDoneClick = {
                                viewModel.reset()
                                onBack()
                            }
                        )
                    }
                }
            }
        }
    }
}

// 1. Giao diện đang quét rác
@Composable
fun ScanningView(progress: Float, currentFile: String) {
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
                progress = { progress },
                modifier = Modifier.size(160.dp),
                color = SpacePrimary,
                strokeWidth = 10.dp,
                trackColor = SpaceCardBackground
            )
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = SpaceAccent,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Đang quét bộ nhớ...",
            color = SpaceTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        Text(
            text = "${(progress * 100).toInt()}%",
            color = SpaceAccent,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = SpaceCardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Đang kiểm tra tệp tin:",
                    color = SpaceTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = currentFile,
                    color = SpaceTextPrimary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

// 2. Giao diện quét xong, báo kết quả quét
@Composable
fun ScanCompletedView(fileCount: Int, junkBytes: Long, onCleanClick: () -> Unit) {
    val formattedSize = formatBytes(junkBytes)
    
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
                    .background(SpaceWarning.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = SpaceWarning,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Quét Rác Hoàn Tất",
                color = SpaceTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Text thông báo tổng dung lượng rác và số lượng file rác
            Text(
                text = "Phát hiện $fileCount tệp tin rác",
                color = SpaceTextSecondary,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Có thể giải phóng: $formattedSize",
                color = SpaceWarning,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Ứng dụng sẽ xóa toàn bộ các tệp tin cache ứng dụng dư thừa, tệp tạm thời, nhật ký hệ thống và tài nguyên thừa trong máy.",
                color = SpaceTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Button(
            onClick = onCleanClick,
            colors = ButtonDefaults.buttonColors(containerColor = SpacePrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics {
                    contentDescription = "Bắt đầu dọn dẹp $formattedSize dung lượng rác"
                }
        ) {
            Text(
                text = "Bắt đầu dọn rác",
                color = SpaceTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 3. Giao diện đang dọn dẹp (vòng xoay % dọn dẹp)
@Composable
fun CleaningView(progress: Int) {
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
            text = "Đang dọn dẹp các tệp rác & bộ nhớ đệm...",
            color = SpaceTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

// 4. Giao diện hoàn thành dọn dẹp & hiển thị thống kê
@Composable
fun CleanCompletedView(fileCount: Int, junkBytes: Long, onDoneClick: () -> Unit) {
    val formattedSize = formatBytes(junkBytes)
    
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
                text = "Dọn Dẹp Thành Công!",
                color = SpaceSuccess,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // Bảng thống kê chi tiết
            Card(
                colors = CardDefaults.cardColors(containerColor = SpaceCardBackground),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "THỐNG KÊ KẾT QUẢ",
                        color = SpaceTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = SpaceBackground.copy(alpha = 0.5f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Đã giải phóng:", color = SpaceTextSecondary, fontSize = 16.sp)
                        Text(formattedSize, color = SpaceSuccess, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Số tệp đã xóa:", color = SpaceTextSecondary, fontSize = 16.sp)
                        Text("$fileCount tệp", color = SpaceTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bộ nhớ đệm dọn:", color = SpaceTextSecondary, fontSize = 16.sp)
                        Text("Tất cả (100%)", color = SpaceAccent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Button(
            onClick = onDoneClick,
            colors = ButtonDefaults.buttonColors(containerColor = SpacePrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics {
                    contentDescription = "Hoàn tất dọn dẹp, quay lại màn hình chính"
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

// Hàm format bytes tiện lợi
fun formatBytes(bytes: Long): String {
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb > 999.0) {
        val gb = mb / 1024.0
        String.format(Locale.US, "%.2f GB", gb)
    } else {
        String.format(Locale.US, "%.0f MB", mb)
    }
}
