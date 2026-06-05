package com.example.spaceup.ui.junk

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanJunkScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JunkViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Tự động quét khi màn hình được hiển thị lần đầu
    LaunchedEffect(Unit) {
        if (uiState is JunkUiState.Idle) {
            viewModel.startScanning(context)
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
                            junkFiles = state.junkFiles,
                            duplicateGroups = state.duplicateGroups,
                            selectedFiles = state.selectedFiles,
                            onToggleSelection = { file -> viewModel.toggleFileSelection(file) },
                            onSelectAll = { viewModel.selectAll() },
                            onDeselectAll = { viewModel.deselectAll() },
                            onCleanClick = {
                                viewModel.startCleaning(context)
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

// 2. Giao diện quét xong, báo kết quả quét kèm danh sách chọn file
@Composable
fun ScanCompletedView(
    junkFiles: List<File>,
    duplicateGroups: Map<String, List<File>>,
    selectedFiles: Set<File>,
    onToggleSelection: (File) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onCleanClick: () -> Unit
) {
    val totalJunkBytes = selectedFiles.sumOf { it.length() }
    val formattedSize = formatBytes(totalJunkBytes)
    val totalJunkCount = selectedFiles.size

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Phần tóm tắt dung lượng trên cùng (cố định)
        Card(
            colors = CardDefaults.cardColors(containerColor = SpaceCardBackground),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = "Quét hoàn tất. Phát hiện thấy tổng số tệp đã chọn để dọn dẹp là $totalJunkCount tệp. Dung lượng có thể giải phóng là $formattedSize."
                }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(SpaceWarning.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = SpaceWarning,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Quét Rác Hoàn Tất",
                            color = SpaceTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Đã chọn dọn dẹp: $totalJunkCount tệp",
                            color = SpaceTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = formattedSize,
                    color = SpaceWarning,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Thanh công cụ chọn nhanh (Chọn tất cả / Bỏ chọn) cho người dùng
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Danh sách tệp tin phát hiện",
                color = SpaceTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onSelectAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                ) {
                    Text("Chọn tất cả", color = SpacePrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onDeselectAll,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                ) {
                    Text("Bỏ chọn", color = SpaceTextSecondary, fontSize = 13.sp)
                }
            }
        }

        // Danh sách các file có thể cuộn được (Chiếm phần thân)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Phân mục tệp rác & tệp tạm
            if (junkFiles.isNotEmpty()) {
                item {
                    Text(
                        text = "TỆP RÁC & BỘ NHỚ TẠM THỜI (${junkFiles.size})",
                        color = SpaceTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                }
                items(junkFiles) { file ->
                    val isSelected = selectedFiles.contains(file)
                    FileJunkItem(
                        fileName = file.name,
                        filePath = file.parent ?: "",
                        fileSize = formatBytes(file.length()),
                        isSelected = isSelected,
                        onCheckedChange = { onToggleSelection(file) },
                        accessibilityLabel = "Tệp rác: ${file.name}, thư mục: ${file.parent ?: "không xác định"}, kích thước: ${formatBytes(file.length())}. ${if (isSelected) "Đã chọn để xóa" else "Chưa chọn để xóa"}"
                    )
                }
            }

            // 2. Phân mục tệp tin trùng lặp
            if (duplicateGroups.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "TỆP TRÙNG LẶP PHÁT HIỆN (${duplicateGroups.size} nhóm)",
                        color = SpaceTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )
                }
                
                duplicateGroups.forEach { (hash, files) ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SpaceCardBackground.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Nhóm trùng lặp (${files.size} tệp giống nhau):",
                                    color = SpacePrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                files.forEachIndexed { index, file ->
                                    if (index == 0) {
                                        // Bản gốc (không cho phép chọn xóa)
                                        DuplicateFileItem(
                                            fileName = file.name,
                                            filePath = file.parent ?: "",
                                            fileSize = formatBytes(file.length()),
                                            isOriginal = true,
                                            isSelected = false,
                                            onCheckedChange = {},
                                            accessibilityLabel = "Bản gốc: ${file.name}, thư mục: ${file.parent ?: "không xác định"}, kích thước: ${formatBytes(file.length())}. Trạng thái: Luôn giữ lại an toàn."
                                        )
                                    } else {
                                        // Bản sao (cho chọn xóa)
                                        val isSelected = selectedFiles.contains(file)
                                        DuplicateFileItem(
                                            fileName = file.name,
                                            filePath = file.parent ?: "",
                                            fileSize = formatBytes(file.length()),
                                            isOriginal = false,
                                            isSelected = isSelected,
                                            onCheckedChange = { onToggleSelection(file) },
                                            accessibilityLabel = "Bản sao trùng lặp: ${file.name}, thư mục: ${file.parent ?: "không xác định"}, kích thước: ${formatBytes(file.length())}. ${if (isSelected) "Đã chọn để xóa" else "Chưa chọn để xóa"}"
                                        )
                                    }
                                    if (index < files.size - 1) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Trường hợp không quét ra tệp nào (rác trống)
            if (junkFiles.isEmpty() && duplicateGroups.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bộ nhớ sạch sẽ! Không tìm thấy tệp rác hay tệp trùng lặp nào.",
                            color = SpaceTextSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nút hành động dọn dẹp ở dưới cùng (cố định)
        Button(
            onClick = onCleanClick,
            enabled = totalJunkCount > 0,
            colors = ButtonDefaults.buttonColors(containerColor = SpacePrimary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .semantics {
                    contentDescription = "Bắt đầu dọn dẹp $totalJunkCount tệp tin đã chọn, giải phóng $formattedSize dung lượng rác"
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

@Composable
fun FileJunkItem(
    fileName: String,
    filePath: String,
    fileSize: String,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accessibilityLabel: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SpaceCardBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = SpacePrimary,
                    uncheckedColor = SpaceTextSecondary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    color = SpaceTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = filePath,
                    color = SpaceTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = fileSize,
                color = SpaceAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DuplicateFileItem(
    fileName: String,
    filePath: String,
    fileSize: String,
    isOriginal: Boolean,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accessibilityLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            }
            .clip(RoundedCornerShape(8.dp))
            .background(if (isOriginal) Color.Transparent else SpaceBackground.copy(alpha = 0.2f))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isOriginal) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = false,
                    onCheckedChange = null,
                    enabled = false
                )
            }
        } else {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = SpacePrimary,
                    uncheckedColor = SpaceTextSecondary
                )
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                color = if (isOriginal) SpaceTextPrimary.copy(alpha = 0.8f) else SpaceTextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isOriginal) FontWeight.Normal else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isOriginal) "Bản gốc: $filePath" else "Bản sao: $filePath",
                color = SpaceTextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        if (isOriginal) {
            Text(
                text = "GIỮ LẠI",
                color = SpaceSuccess,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = fileSize,
                color = SpaceWarning,
                fontSize = 13.sp,
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
