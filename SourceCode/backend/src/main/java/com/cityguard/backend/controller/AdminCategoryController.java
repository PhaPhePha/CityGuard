package com.cityguard.backend.controller;

import com.cityguard.backend.dto.admin.category.AdminCategoryResponse;
import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.entity.Category;
import com.cityguard.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final CategoryRepository categoryRepository;
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminCategoryResponse>>> getCategories(){
        List<Category> categories = categoryRepository.findAll(
                Sort.by(Sort.Direction.ASC,"id")
        );
        List<AdminCategoryResponse> response = categories.stream()
                .map(AdminCategoryResponse::from)
                .toList();

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }
}
