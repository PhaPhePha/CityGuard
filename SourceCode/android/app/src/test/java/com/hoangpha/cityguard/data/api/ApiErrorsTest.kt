package com.hoangpha.cityguard.data.api

// TV2 - Trần Hữu Thịnh: phân biệt lỗi kết nối với lỗi timeout, không hiển thị kết luận sai về tốc độ xử lý backend.
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.*
import org.junit.Test

class ApiErrorsTest {
    @Test fun timeoutDoesNotClaimServerIsSlow() {
        val message = SocketTimeoutException("timeout").userMessage()
        assertTrue(message.contains("quá thời gian chờ"))
        assertFalse(message.contains("phản hồi chậm"))
    }
    @Test fun refusedConnectionHasClearMessage() {
        assertTrue(ConnectException("refused").userMessage().contains("Không kết nối được máy chủ"))
    }
    @Test fun hostnameFailureHasClearMessage() {
        assertTrue(UnknownHostException("host").userMessage().contains("Không tìm thấy máy chủ"))
    }
    @Test fun serverMessageIsPreserved() {
        assertEquals("Email đã được sử dụng", ApiException(409, "Email đã được sử dụng").userMessage())
    }
}
