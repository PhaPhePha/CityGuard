package com.hoangpha.cityguard.data.api

//Đọc lỗi chuẩn backend và phân biệt HTTP 400/401/403/409 với lỗi mạng; dùng chung cho các màn TV2.

import com.google.gson.Gson
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.ConnectException
import java.net.UnknownHostException
import java.io.InterruptedIOException
import retrofit2.Response

class ApiException(val status: Int, message: String) : RuntimeException(message)
private data class ErrorBody(val message: String?)
internal fun <T> requireApiData(response: Response<ApiResponse<T>>): T {
    // Lỗi mạng/timeout chỉ hiện lỗi và cho thử lại, không chuyển thành đăng nhập thành công.
    if (!response.isSuccessful) {
        val serverMessage = runCatching {
            Gson().fromJson(response.errorBody()?.string(), ErrorBody::class.java)?.message
        }.getOrNull()
        val fallback = when (response.code()) {
            400 -> "Thông tin chưa hợp lệ. Vui lòng kiểm tra lại."
            401 -> "Phiên đăng nhập đã hết hạn hoặc thông tin đăng nhập không đúng."
            403 -> "Bạn không có quyền thực hiện thao tác này."
            409 -> "Tên đăng nhập hoặc email đã được sử dụng."
            else -> "Máy chủ đang gặp lỗi. Vui lòng thử lại."
        }
        throw ApiException(response.code(), serverMessage?.takeIf { it.isNotBlank() } ?: fallback)
    }
    val envelope = response.body()
    if (envelope?.success != true || envelope.data == null) {
        throw ApiException(response.code(), "Máy chủ trả về dữ liệu không hợp lệ. Vui lòng thử lại.")
    }
    return envelope.data
}
internal fun Throwable.userMessage(): String = when (this) {
    is ApiException -> message ?: "Yêu cầu không thành công."
    is SocketTimeoutException -> "Kết nối quá thời gian chờ. Vui lòng thử lại."
    is UnknownHostException -> "Không tìm thấy máy chủ. Kiểm tra mạng rồi thử lại."
    is ConnectException -> "Không kết nối được máy chủ. Vui lòng thử lại sau."
    is InterruptedIOException -> "Yêu cầu quá thời gian chờ. Vui lòng thử lại."
    is IOException -> "Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại."
    else -> "Không tải được dữ liệu. Vui lòng thử lại."
}
