package com.cityguard.backend.dto.admin.category;

import com.cityguard.backend.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi danh mục sự cố cho Admin Web
 * (theo ADMIN_API_CONTRACT.md mục 3.3 & Admin_web/src/types/category.types.ts).
 * Lưu ý: Entity Category dùng trường displayName (cột display_name),
 * còn React Admin Web yêu cầu trường JSON tên là "name".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCategoryResponse {

    private Long id;
    private String code;
    private String name;
    private String color;

    public static AdminCategoryResponse from(Category category) {
        if (category == null) {
            return null;
        }
        return AdminCategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getDisplayName())
                .color(category.getColor())
                .build();
    }
}
