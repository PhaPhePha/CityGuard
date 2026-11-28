package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.admin.common.AdminPageResponse;
import com.cityguard.backend.dto.admin.log.AdminLogResponse;
import com.cityguard.backend.entity.AdminLog;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.repository.AdminLogRepository;
import com.cityguard.backend.service.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminLogServiceImpl implements AdminLogService {
    private final AdminLogRepository adminLogRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminPageResponse<AdminLogResponse> getLogs(
            Long actorId,
            String action,
            String targetType,
            int page,
            int size
    ){
        if(page <0 || size<1 ||size>100){
            throw new AppException(ErrorCode.INVALID_INPUT,"Thông số phân trang không hợp lệ");
        }

        var logs = adminLogRepository.findAdminLogs(
                actorId,
                normalizeFilter(action),
                normalizeFilter(targetType),
                PageRequest.of(page, size)
        );
        return AdminPageResponse.of(logs.map(AdminLogResponse::from));
    }

    private static String normalizeFilter(String value){
        return value ==null || value.isBlank() ? null : value.strip();
    }

}
