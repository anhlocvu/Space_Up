package com.example.spaceup.ui.duplicate

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
import androidx.compose.material.icons.filled.Warning
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
fun DuplicateFilesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DuplicateFilesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Tự động quét khi mở màn hình
    LaunchedEffect(Unit) {
        if (uiState is DuplicateFilesUiState.Idle) {
            viewModel.scanDuplicates()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dọn Tệp Trùng Lặp", color = SpaceTextPrimary, fontWeight = FontWeight.Bold) },
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
                label = "DuplicatesTransition"
            ) { state ->
                when (state) {
                    is DuplicateFilesUiState.Idle, is DuplicateFilesUiState.Scanning -> {
                        DuplicateScanningView()
                    }
                    is DuplicateFilesUiState.Success -> {
                        DuplicateListView(
                            duplicateGroups = state.duplicateGroups,
                            selectedFiles = state.selectedFiles,
                            onToggleSelect = { file, key -> viewModel.toggleFileSelection(file, key) },
                            onSelectAllDuplicates = { viewModel.selectAllDuplicatesOnly() },
                            onDeselectAll = { viewModel.deselectAll() },
                            onDeleteClick = { viewModel.deleteSelectedFiles() }
                        )
                    }
                    is DuplicateFilesUiState.Deleting -> {
                        DuplicateDeletingView(progress = state.progress)
                    }
                    is DuplicateFilesUiState.DeleteSuccess -> {
                        DuplicateDeleteSuccessView(
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
fun DuplicateScanningView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(72.dp),
            color = SpacePrimary,
            strokeWidth = 6.dp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Đang tìm kiếm tệp trùng lặp...",
            color = SpaceTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hệ thống đang đối chiếu dung lượng và so sánh mã băm MD5 của từng file...",
            color = SpaceTextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun DuplicateListView(
    duplicateGroups: Map<String, List<File>>,
    selectedFiles: Set<File>,
    onToggleSelect: (File, String) -> Unit,
    onSelectAllDuplicates: () -> Unit,
    onDeselectAll: () -> Unit,
    onDeleteClick: () -> Unit
) {
    if (duplicateGroups.isEmpty()) {
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
                text = "Bộ nhớ sạch sẽ!",
                color = SpaceTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Không phát hiện tệp tin trùng lặp nào trên thiết bị của bạn.",
                color = SpaceTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        val totalSelectedBytes = selectedFiles.sumOf { it.length() }
        val formattedSize = formatBytes(totalSelectedBytes)
        
        // Tổng số file trùng lặp (không tính file gốc)
        val duplicateCount = duplicateGroups.values.sumOf { it.size - 1 }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Nút chức năng
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Có $duplicateCount tệp trùng lặp",
                    color = SpaceTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onSelectAllDuplicates,
                        modifier = Modifier.semantics {
                            contentDescription = "Chọn lại tất cả các bản sao trùng lặp, giữ lại tệp gốc"
                        }
                    ) {
                        Text("Khuyên dùng", color = SpaceAccent, fontWeight = FontWeight.Bold)
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

            // Danh sách các nhóm trùng lặp
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                duplicateGroups.forEach { (hashKey, files) ->
                    if (files.size >= 2) {
                        item {
                            val fileSize = files[0].length()
                            val formattedFileSize = formatBytes(fileSize)
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SpaceCardBackground.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                    .padding(12.dp)
                            ) {
                                // Tiêu đề nhóm trùng lặp
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${files[0].name}",
                                        color = SpaceTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$formattedFileSize (Trùng ${files.size} bản)",
                                        color = SpaceWarning,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                HorizontalDivider(color = SpaceBackground.copy(alpha = 0.5f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                                
                                // Danh sách các file trong nhóm
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    files.forEachIndexed { index, file ->
                                        val isOriginal = index == 0
                                        val isSelected = selectedFiles.contains(file)
                                        
                                        DuplicateFileItem(
                                            file = file,
                                            isOriginal = isOriginal,
                                            isSelected = isSelected,
                                            onItemClick = {
                                                if (!isOriginal) {
                                                    onToggleSelect(file, hashKey)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nút dọn dẹp các bản sao đã chọn
            Button(
                onClick = onDeleteClick,
                enabled = selectedFiles.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpacePrimary,
                    disabledContainerColor = SpaceCardBackground
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics {
                        contentDescription = if (selectedFiles.isNotEmpty()) {
                            "Nhấn đúp để xóa ${selectedFiles.size} bản sao trùng lặp đã chọn, giải phóng $formattedSize dung lượng bộ nhớ thật"
                        } else {
                            "Chưa chọn tệp trùng lặp nào để dọn dẹp"
                        }
                    }
            ) {
                Text(
                    text = if (selectedFiles.isNotEmpty()) "Dọn bản sao đã chọn ($formattedSize)" else "Chọn bản sao để dọn",
                    color = if (selectedFiles.isNotEmpty()) SpaceTextPrimary else SpaceTextSecondary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DuplicateFileItem(
    file: File,
    isOriginal: Boolean,
    isSelected: Boolean,
    onItemClick: () -> Unit
) {
    val fileLabel = if (isOriginal) "Bản gốc (Giữ lại)" else "Bản sao (Xóa)"
    val fileColor = if (isOriginal) SpaceSuccess else SpaceTextSecondary
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) SpaceWarning.copy(alpha = 0.05f) else SpaceCardBackground)
            .clickable(enabled = !isOriginal, onClick = onItemClick)
            .padding(12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = if (isOriginal) {
                    "Bản gốc tệp tin ${file.name}, nằm trong thư mục ${file.parent}. Đây là bản gốc sẽ được bảo vệ giữ lại, không cho phép xóa."
                } else {
                    "Bản sao tệp tin ${file.name}, nằm trong thư mục ${file.parent}. Trạng thái " + 
                            if (isSelected) "Đã chọn để xóa. Nhấn đúp để giữ lại" else "Chưa chọn để xóa. Nhấn đúp để chọn xóa"
                }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isOriginal) {
            // Hiển thị vòng tròn Success nhỏ thay vì Checkbox để báo bản gốc an toàn
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpaceSuccess.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SpaceSuccess,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onItemClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SpaceWarning,
                    uncheckedColor = SpaceTextSecondary
                ),
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = fileLabel,
                color = fileColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = file.parent ?: "",
                color = SpaceTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DuplicateDeletingView(progress: Int) {
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
                color = SpacePrimary,
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
            text = "Đang dọn sạch các bản sao trùng lặp...",
            color = SpaceTextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
}

@Composable
fun DuplicateDeleteSuccessView(deletedBytes: Long, onDoneClick: () -> Unit) {
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
                text = "Đã Dọn Bản Sao Thành Công!",
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
                text = "Các tệp tin bản sao trùng lặp đã được xóa vĩnh viễn và giữ lại duy nhất 1 bản gốc an toàn.",
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
