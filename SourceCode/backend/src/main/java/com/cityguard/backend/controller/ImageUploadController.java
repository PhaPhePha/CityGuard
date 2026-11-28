
package com.cityguard.backend.controller;

import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.dto.upload.ImageUploadResponse;
import com.cityguard.backend.service.ImageUploadService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    @PostMapping(
            value = "/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file
    ) {
        String imagePath = imageUploadService.uploadImage(file);

        String imageUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path(imagePath)
                .toUriString();

        ImageUploadResponse response = ImageUploadResponse.builder()
                .imageUrl(imageUrl)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tải ảnh lên thành công", response));
    }
}
