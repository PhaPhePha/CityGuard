package com.cityguard.backend.dto.admin.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateCommentStatusRequest {

    @NotNull(message = "Trạng thái ẩn bình luận là bắt buộc")
    private Boolean isHidden;

    @NotBlank(message  = "Lý do kiểm duyệt không được để trống")
    @Size(max=1000, message = "Lý do không được vượt quá 1000 ký tự")
    private String reason;
}
