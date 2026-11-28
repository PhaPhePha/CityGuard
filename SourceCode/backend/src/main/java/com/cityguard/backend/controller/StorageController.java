package com.cityguard.backend.controller;

import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.exception.AppException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Controller xử lý tải lên tệp hình ảnh cục bộ cho báo cáo sự cố.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/storage")
@Tag(name = "Storage", description = "Các API tải lên và lưu trữ tệp hình ảnh")
public class StorageController {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Path UPLOAD_DIR = Paths.get("uploads").toAbsolutePath().normalize();
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif", ".heic", ".heif");

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải ảnh lên máy chủ", description = "Nhận MultipartFile ảnh (< 5MB), lưu cục bộ và trả về URL truy cập")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Vui lòng chọn tệp hình ảnh để tải lên");
        }

        if (file.getSize() >= MAX_FILE_SIZE_BYTES) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Kích thước hình ảnh phải nhỏ hơn 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Định dạng tệp không hợp lệ, chỉ hỗ trợ tệp hình ảnh");
        }

        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String fileName = UUID.randomUUID() + extension;

        try {
            Files.createDirectories(UPLOAD_DIR);
            Path targetPath = UPLOAD_DIR.resolve(fileName).normalize();
            if (!targetPath.startsWith(UPLOAD_DIR)) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Tên tệp không hợp lệ");
            }
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Không thể lưu tệp ảnh tải lên", ex);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lưu tệp hình ảnh, vui lòng thử lại");
        }

        String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(fileName)
                .toUriString();
        return ResponseEntity.ok(ApiResponse.success("Tải ảnh lên thành công", Map.of(
                "url", fileUrl,
                "imageUrl", fileUrl
        )));
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            if (ALLOWED_EXTENSIONS.contains(ext)) {
                return ext;
            }
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/heic" -> ".heic";
            case "image/heif" -> ".heif";
            default -> ".jpg";
        };
    }
}

