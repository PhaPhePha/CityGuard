package com.hoangpha.cityguard.presentation.report
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.ui.theme.CityGuardAppTheme

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
        onRemoveImage = { viewModel.onImageChosen(null) },
        onBack = onBack
    )
}

// "Screen": chỉ vẽ theo state, không biết ViewModel. Nhờ vậy xem được bằng @Preview.
@Composable
fun CreateReportScreen(
    state: CreateReportUiState,
    onCategorySelected: (IncidentCategory) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onPickImage: () -> Unit = {},
    onRemoveImage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack, enabled = !state.isSubmitting) {
            Text("← Quay lại")
        }
        Text("Tạo báo cáo", style = MaterialTheme.typography.headlineSmall)
        Text("Ảnh sự cố", style = MaterialTheme.typography.titleMedium)
        state.imageUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "Ảnh sự cố",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            TextButton(onClick = onRemoveImage, enabled = !state.isSubmitting) {
                Text("Xóa ảnh")
            }
        }
        OutlinedButton(
            onClick = onPickImage,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.imageUri == null) "Chọn ảnh từ thư viện" else "Đổi ảnh")
        }

        Text("Loại sự cố", style = MaterialTheme.typography.titleMedium)
        IncidentCategory.entries.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { category ->
                    FilterChip(
                        selected = state.category == category,
                        onClick = { onCategorySelected(category) },
                        label = { Text(category.displayName) },
                        enabled = !state.isSubmitting,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChanged,
            label = { Text("Mô tả về sự cố...") },
            supportingText = {
                Text("${state.description.length}/${CreateReportUiState.MAX_DESCRIPTION_LENGTH}")
            },
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.address,
            onValueChange = onAddressChanged,
            label = { Text("Vị trí") },
            singleLine = true,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )

        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (state.createdReportId != null) {
            Text("Đã gửi báo cáo thành công (${state.createdReportId})")
        }

        Button(
            onClick = onSubmit,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isSubmitting) "Đang gửi..." else "Gửi báo cáo")
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