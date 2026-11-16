package com.hoangpha.cityguard.presentation.report

import android.net.Uri

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import coil.compose.AsyncImage

import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.presentation.map.components.markerIconRes
import com.hoangpha.cityguard.ui.theme.CityGuardAppTheme
import com.hoangpha.cityguard.ui.theme.CityGuardYellow
// "Route": nối ViewModel với màn hình. Chỗ duy nhất biết ViewModel tồn tại.
@Composable
fun CreateReportRoute(
    onCreated: (String) -> Unit,   // gọi khi gửi thành công, kèm id report mới
    onBack: () -> Unit,
    viewModel: CreateReportViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Mở bộ chọn ảnh của hệ thống; trả về địa chỉ (Uri) của ảnh đã chọn
    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.onImageChosen(uri.toString())
    }
    val context = LocalContext.current

    // Nhớ địa chỉ file ảnh đang chụp. rememberSaveable giữ lại khi xoay màn hình hoặc khi
    // hệ thống tạm tắt app lúc camera đang mở; bản dùng remember sẽ làm mất ảnh vừa chụp.
    var pendingPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val pending = pendingPhotoUri
        if (success) {
            pending?.let { viewModel.onImageChosen(it) }
        } else if (pending != null) {
            // Hủy chụp: xóa file ảnh trống đã tạo trước đó
            try {
                context.contentResolver.delete(android.net.Uri.parse(pending), null, null)
            } catch (e: Exception) {
                // Không xóa được thì bỏ qua, không ảnh hưởng người dùng
            }
        }
        pendingPhotoUri = null
    }
    // Chạy đúng một lần mỗi khi createdReportId đổi từ null sang có giá trị
    LaunchedEffect(state.createdReportId) {
        state.createdReportId?.let(onCreated)
    }

    CreateReportScreen(
        state = state,
        onCategorySelected = viewModel::onCategorySelected,
        onDescriptionChanged = viewModel::onDescriptionChanged,
        onAddressChanged = viewModel::onAddressChanged,
        onSubmit = viewModel::submit,
        onPickImage = {
            pickLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onTakePhoto = {
            try {
                val uri = createCameraImageUri(context)
                pendingPhotoUri = uri.toString()
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                pendingPhotoUri = null
                viewModel.onCameraUnavailable()
            }
        },
        onRemoveImage = { viewModel.onImageChosen(null) },
        onBack = onBack
    )
}

// "Screen": chỉ vẽ theo state, không biết ViewModel. Nhờ vậy xem được bằng @Preview.
private val SheetBackground = Color(0xFFFFF8E1)

@Composable
fun CreateReportScreen(
    state: CreateReportUiState,
    onCategorySelected: (IncidentCategory) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onPickImage: () -> Unit = {},
    onTakePhoto: () -> Unit = {},
    onRemoveImage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(CityGuardYellow)) {
        // Phần đầu nền vàng: nút quay lại và tiêu đề
        Row(
            modifier = Modifier.statusBarsPadding().padding(start = 8.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = !state.isSubmitting) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại",
                    modifier = Modifier.size(28.dp),
                    tint = Color.Black
                )
            }
            Text(
                "Tạo báo cáo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
        }
        // Tờ nội dung kem, bo tròn hai góc trên
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SheetBackground, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ô "Thêm ảnh" lớn: bấm vào để chụp ảnh. Đã có ảnh thì hiện ảnh trong ô.
            val imageUri = state.imageUri
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFE8E2D0))
                    .border(1.dp, Color(0xFFD6CFB8), RoundedCornerShape(16.dp))
                    .clickable(enabled = !state.isSubmitting, onClick = onTakePhoto),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Ảnh sự cố",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Thêm ảnh", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onPickImage,
                    enabled = !state.isSubmitting,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                ) { Text("Chọn từ thư viện") }
                if (imageUri != null) {
                    TextButton(
                        onClick = onRemoveImage,
                        enabled = !state.isSubmitting,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)
                    ) { Text("Xóa ảnh") }
                }
            }

            Text("Loại sự cố", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            CategoryGrid(
                selected = state.category,
                enabled = !state.isSubmitting,
                onSelect = onCategorySelected
            )

            Text("Mô tả", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChanged,
                placeholder = { Text("Mô tả về sự cố...") },
                supportingText = {
                    Text(
                        "${state.description.length}/${CreateReportUiState.MAX_DESCRIPTION_LENGTH}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                },
                enabled = !state.isSubmitting,
                minLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White
                )
            )

            Text("Vị trí hiện tại", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = state.address,
                onValueChange = onAddressChanged,
                placeholder = { Text("Nhập vị trí sự cố") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                singleLine = true,
                enabled = !state.isSubmitting,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White
                )
            )

            state.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = onSubmit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CityGuardYellow,
                    contentColor = Color.Black
                )
            ) {
                Text(if (state.isSubmitting) "Đang gửi..." else "Gửi báo cáo", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(80.dp))   // chừa chỗ cho thanh điều hướng dưới
        }
    }
}
//2x2 các loại sự cố, mỗi ô có icon và tên, giống prototype
@Composable
private fun CategoryGrid(
    selected: IncidentCategory?,
    enabled: Boolean,
    onSelect: (IncidentCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        IncidentCategory.entries.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { category ->
                    val isSelected = selected == category
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = enabled) { onSelect(category) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) CityGuardYellow else Color(0xFFDDDDDD)
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = category.markerIconRes()),
                                contentDescription = category.displayName,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                category.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
// ----- Xem thử trong Android Studio (không cần chạy emulator) -----
@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Trống")
@Composable
private fun CreateReportEmptyPreview() {
    CityGuardAppTheme {
        CreateReportScreen(CreateReportUiState(), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Đang gửi")
@Composable
private fun CreateReportSubmittingPreview() {
    CityGuardAppTheme {
        CreateReportScreen(
            state = CreateReportUiState(
                category = IncidentCategory.TRAFFIC_JAM,
                description = "Kẹt xe kéo dài.",
                address = "Võ Oanh, Bình Thạnh",
                isSubmitting = true
            ),
            onCategorySelected = {}, onDescriptionChanged = {}, onAddressChanged = {},
            onSubmit = {}, onBack = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780, name = "Lỗi")
@Composable
private fun CreateReportErrorPreview() {
    CityGuardAppTheme {
        CreateReportScreen(
            state = CreateReportUiState(errorMessage = "Vui lòng chọn loại sự cố"),
            onCategorySelected = {}, onDescriptionChanged = {}, onAddressChanged = {},
            onSubmit = {}, onBack = {}
        )
    }
}