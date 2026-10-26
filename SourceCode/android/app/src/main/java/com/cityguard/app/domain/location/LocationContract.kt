
package com.cityguard.app.domain.location

data class DeviceLocation(
    val latitude: Double,
    val longitude: Double
)
// mô tả các kết quả mà việc lấy vị trí có thể trả về. Nó giúp xử lý rõ từng tình huống thay vì chỉ dùng null.
sealed interface LocationResult {
    data class Success(
        val point: DeviceLocation,
        val approximate: Boolean,
        val fromCache: Boolean
    ) : LocationResult

    data object PermissionDenied : LocationResult
    data object ServicesDisabled : LocationResult
    data object Stale : LocationResult
    data object Unavailable : LocationResult

    data class Failure(
        val message: String
    ) : LocationResult
}
// đóng vai trò hợp đồng. Lớp nào triển khai interface này phải cung cấp hàm:
interface LocationProvider {
    suspend fun getCurrentLocation(): LocationResult
}
