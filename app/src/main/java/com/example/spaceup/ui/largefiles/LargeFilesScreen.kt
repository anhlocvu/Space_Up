package com.example.spaceup.ui.largefiles

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.spaceup.theme.*
import com.example.spaceup.ui.junk.formatBytes
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeFilesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LargeFilesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Tự động quét các tệp lớn khi vào màn hình (tối thiểu 10MB)
    LaunchedEffect(Unit) {
        if (uiState is LargeFilesUiState.Idle) {
            viewModel.scanFiles(minSizeMb = 10L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dọn Tệp Dung Lượng Lớn", color = SpaceTextPrimary, fontWeight = FontWeight.Bold) },
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
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "LargeFilesTransition"
            ) { state ->
                when (state) {
                    is LargeFilesUiState.Idle, is LargeFilesUiState.Scanning -> {
                        LargeFilesScanningView()
                    }
                    is LargeFilesUiState.Success -> {
                        LargeFilesListView(
                            files = state.files,
                            selectedFiles = state.selectedFiles,
                            onToggleSelect = { viewModel.toggleFileSelection(it) },
                            onSelectAll = { viewModel.selectAllFiles() },
                            onDeselectAll = { viewModel.deselectAllFiles() },
                            onDeleteClick = { viewModel.deleteSelectedFiles() }
                        )
                    }
                    is LargeFilesUiState.Deleting -> {
                        LargeFilesDeletingView(progress = state.progress)
                    }
                    is LargeFilesUiState.DeleteSuccess -> {
                        LargeFilesDeleteSuccessView(
                            deletedBytes = state.deletedBytes,
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

@Composable
fun LargeFilesScanningView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(72.dp),
            color = SpaceAccent,
            strokeWidth = 6.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Đang quét các tệp dung lượng lớn (>10MB)...",
            color = SpaceTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ứng dụng đang duyệt thư mục Download, DCIM, Documents, Movies...",
            color = SpaceTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun LargeFilesListView(
    files: List<File>,
    selectedFiles: Set<File>,
    onToggleSelect: (File) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDeleteClick: () -> Unit
) {
    if (files.isEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SpaceSuccess,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Không phát hiện tệp lớn nào",
                color = SpaceTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bộ nhớ của bạn không có tệp nào lớn hơn 10MB trong các thư mục công cộng.",
                color = SpaceTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    } else {
        val totalSelectedBytes = selectedFiles.sumOf { it.length() }
        val formattedSelectedSize = formatBytes(totalSelectedBytes)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Nút chức năng Chọn tất cả / Bỏ chọn tất cả
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Phát hiện ${files.size} tệp lớn",
                    color = SpaceTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onSelectAll,
                        modifier = Modifier.semantics {
                            contentDescription = "Chọn tất cả các tệp quét được"
                        }
                    ) {
                        Text("Chọn tất cả", color = SpaceAccent, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = onDeselectAll,
                        modifier = Modifier.semantics {
                            contentDescription = "Bỏ chọn tất cả các tệp"
                        }
                    ) {
                        Text("Bỏ chọn", color = SpaceTextSecondary)
                    }
                }
            }

            // Danh sách tệp lớn
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(files) { file ->
                    val isSelected = selectedFiles.contains(file)
                    val fileSizeFormatted = formatBytes(file.length())
                    
                    LargeFileItem(
                        file = file,
                        fileSizeFormatted = fileSizeFormatted,
                        isSelected = isSelected,
                        onItemClick = { onToggleSelect(file) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút xóa các tệp đã chọn
            Button(
                onClick = onDeleteClick,
                enabled = selectedFiles.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpaceWarning,
                    disabledContainerColor = SpaceCardBackground
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics {
                        contentDescription = if (selectedFiles.isNotEmpty()) {
                            "Nhấn đúp để xóa ${selectedFiles.size} tệp tin đã chọn, giải phóng $formattedSelectedSize dung lượng thực tế"
                        } else {
                            "Chưa chọn tệp tin nào để xóa"
                        }
                    }
            ) {
                Text(
                    text = if (selectedFiles.isNotEmpty()) "Xóa đã chọn ($formattedSelectedSize)" else "Chọn tệp để xóa",
                    color = if (selectedFiles.isNotEmpty()) SpaceTextPrimary else SpaceTextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LargeFileItem(
    file: File,
    fileSizeFormatted: String,
    isSelected: Boolean,
    onItemClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpaceCardBackground),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "Tệp tin ${file.name}, dung lượng $fileSizeFormatted, đường dẫn ${file.parent}, trạng thái " + 
                        if (isSelected) "Đã được chọn. Nhấn đúp để bỏ chọn" else "Chưa chọn. Nhấn đúp để chọn"
            }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onItemClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SpaceAccent,
                    uncheckedColor = SpaceTextSecondary
                ),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    color = SpaceTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = file.parent ?: "",
                    color = SpaceTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = fileSizeFormatted,
                color = SpaceWarning,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun LargeFilesDeletingView(progress: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress.toFloat() / 100f },
                modifier = Modifier.size(150.dp),
                color = SpaceWarning,
                strokeWidth = 10.dp,
                trackColor = SpaceCardBackground
            )
            Text(
                text = "$progress%",
                color = SpaceTextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Đang xóa các tệp tin đã chọn...",
            color = SpaceTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

@Composable
fun LargeFilesDeleteSuccessView(deletedBytes: Long, onDoneClick: () -> Unit) {
    val formattedSize = formatBytes(deletedBytes)
    
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
                    .size(100.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(SpaceSuccess.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SpaceSuccess,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Đã Dọn Tệp Lớn Thành Công!",
                color = SpaceSuccess,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Đã giải phóng thực tế: $formattedSize",
                color = SpaceTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Dung lượng bộ nhớ đã được giải phóng thật sự khỏi thiết bị.",
                color = SpaceTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
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
