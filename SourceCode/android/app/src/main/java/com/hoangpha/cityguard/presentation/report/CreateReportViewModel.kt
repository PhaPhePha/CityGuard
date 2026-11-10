package com.hoangpha.cityguard.presentation.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoangpha.cityguard.data.mock.FakeReportRepository
import com.hoangpha.cityguard.domain.model.IncidentCategory
import com.hoangpha.cityguard.domain.repository.NewReport
import com.hoangpha.cityguard.domain.repository.ReportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class CreateReportViewModel(
    // Hôm nay là dữ liệu giả; khi có API chỉ đổi đối tượng này
    private val repository: ReportRepository = FakeReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateReportUiState())
    val uiState: StateFlow<CreateReportUiState> = _uiState.asStateFlow()

    // Mã chống trùng: tạo MỘT lần khi mở form. Gửi lại sau khi lỗi thì dùng lại đúng mã này.
    private val requestId: String = UUID.randomUUID().toString()

    fun onCategorySelected(category: IncidentCategory) {
        _uiState.update { it.copy(category = category, errorMessage = null) }
    }

    fun onDescriptionChanged(text: String) {
        if (text.length <= CreateReportUiState.MAX_DESCRIPTION_LENGTH) {
            _uiState.update { it.copy(description = text, errorMessage = null) }
        }
    }

    fun onAddressChanged(text: String) {
        _uiState.update { it.copy(address = text, errorMessage = null) }
    }

    fun onImageChosen(uri: String?) {
        _uiState.update { it.copy(imageUri = uri, errorMessage = null) }
    }

    fun submit() {
        val current = _uiState.value
        if (current.isSubmitting) return                     // chặn bấm gửi lần hai

        val category = current.category
        if (category == null) {
            _uiState.update { it.copy(errorMessage = "Vui lòng chọn loại sự cố") }
            return
        }
        if (current.description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập mô tả") }
            return
        }
        if (current.address.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Vui lòng nhập vị trí") }
            return
        }

        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val report = repository.createReport(
                    NewReport(
                        requestId = requestId,
                        category = category,
                        description = current.description.trim(),
                        address = current.address.trim(),
                        imageUri = current.imageUri
                    )
                )
                _uiState.update { it.copy(isSubmitting = false, createdReportId = report.id) }
            } catch (e: CancellationException) {
                throw e                                       // không nuốt tín hiệu hủy của coroutine
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSubmitting = false, errorMessage = "Gửi thất bại. Vui lòng thử lại.")
                }
            }
        }
    }
}
