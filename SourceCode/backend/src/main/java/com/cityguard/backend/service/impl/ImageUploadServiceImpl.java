
package com.cityguard.backend.service.impl;

import com.cityguard.backend.config.FileStorageConfig;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.service.ImageUploadService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadServiceImpl implements ImageUploadService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final FileStorageConfig fileStorageConfig;

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Vui lòng chọn ảnh để tải lên");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Ảnh không được vượt quá 5 MB");
        }

        String extension = ALLOWED_TYPES.get(file.getContentType());

        if (extension == null) {
            throw new AppException(
                    ErrorCode.INVALID_INPUT,
                    "Chỉ chấp nhận ảnh JPG, PNG hoặc WebP"
            );
        }

        String fileName = UUID.randomUUID() + extension;

        try {
            Path uploadPath = fileStorageConfig.getUploadPath();
            Files.createDirectories(uploadPath);

            Path targetPath = uploadPath.resolve(fileName).normalize();

            if (!targetPath.startsWith(uploadPath)) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Đường dẫn ảnh không hợp lệ");
            }

            file.transferTo(targetPath);

            log.info("Đã lưu ảnh upload: {}", fileName);

            return "/uploads/" + fileName;

        } catch (IOException e) {
            log.error("Không thể lưu ảnh upload", e);
            throw new AppException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Không thể lưu ảnh, vui lòng thử lại"
            );
        }
    }
}
