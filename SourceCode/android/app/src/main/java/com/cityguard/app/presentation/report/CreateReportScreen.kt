package com.cityguard.app.presentation.report

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
import com.cityguard.app.domain.model.IncidentCategory
import com.cityguard.app.ui.theme.CityGuardTheme

// "Route": nối ViewModel với màn hình. Chỗ duy nhất biết ViewModel tồn tại.
@Composable
fun CreateReportRoute(
    onCreated: (String) -> Unit,   // gọi khi gửi thành công, kèm id report mới
    onBack: () -> Unit,
    viewModel: CreateReportViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
@Preview(showBackground = true, name = "Trống")
@Composable
private fun CreateReportEmptyPreview() {
    CityGuardTheme {
        CreateReportScreen(CreateReportUiState(), {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Đang gửi")
@Composable
private fun CreateReportSubmittingPreview() {
    CityGuardTheme {
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

@Preview(showBackground = true, name = "Lỗi")
@Composable
private fun CreateReportErrorPreview() {
    CityGuardTheme {
        CreateReportScreen(
            state = CreateReportUiState(errorMessage = "Vui lòng chọn loại sự cố"),
            onCategorySelected = {}, onDescriptionChanged = {}, onAddressChanged = {},
            onSubmit = {}, onBack = {}
        )
    }
}