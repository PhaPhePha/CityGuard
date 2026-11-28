package com.cityguard.backend.dto.admin.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * DTO phân trang dành cho React Admin Web (theo ADMIN_API_CONTRACT.md mục 3.4.A & common.types.ts).
 * Sử dụng đúng tên trường JSON: items, page, size, totalItems, totalPages, isLast.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPageResponse<T> {

    private List<T> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;

    @JsonProperty("isLast")
    private boolean isLast;

    @JsonProperty("isLast")
    public boolean isLast() {
        return isLast;
    }

    public static <T> AdminPageResponse<T> of(Page<T> springPage) {
        return AdminPageResponse.<T>builder()
                .items(springPage.getContent())
                .page(springPage.getNumber())
                .size(springPage.getSize())
                .totalItems(springPage.getTotalElements())
                .totalPages(springPage.getTotalPages())
                .isLast(springPage.isLast())
                .build();
    }

    public static <E, T> AdminPageResponse<T> of(Page<E> springPage, List<T> mappedItems) {
        return AdminPageResponse.<T>builder()
                .items(mappedItems)
                .page(springPage.getNumber())
                .size(springPage.getSize())
                .totalItems(springPage.getTotalElements())
                .totalPages(springPage.getTotalPages())
                .isLast(springPage.isLast())
                .build();
    }
}
