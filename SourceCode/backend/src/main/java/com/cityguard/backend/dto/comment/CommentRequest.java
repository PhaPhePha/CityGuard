package com.cityguard.backend.dto.comment;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu gửi bình luận cho báo cáo sự cố (chuẩn hóa theo NFR-MAINT-01).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank(message = "Nội dung bình luận không được để trống")
    @Size(max = 2000, message = "Bình luận tối đa 2000 ký tự")
    @JsonAlias({"content", "text"})
    private String body;

    @JsonAlias({"requestId", "request_id", "client_request_id"})
    private String clientRequestId;
}
