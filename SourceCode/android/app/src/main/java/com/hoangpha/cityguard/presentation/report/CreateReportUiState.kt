package com.hoangpha.cityguard.presentation.report

import com.hoangpha.cityguard.domain.model.IncidentCategory

// Mọi thứ màn hình cần để vẽ nằm trong một đối tượng duy nhất
data class CreateReportUiState(
    val category: IncidentCategory? = null,   // null = chưa chọn loại
    val description: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationApproximate: Boolean = false,
    val locationFromCache: Boolean = false,
    val imageUri: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val createdReportId: String? = null       // có giá trị = đã gửi xong, màn hình có thể chuyển đi
) {
    companion object {
        const val MAX_DESCRIPTION_LENGTH = 150
    }
}
