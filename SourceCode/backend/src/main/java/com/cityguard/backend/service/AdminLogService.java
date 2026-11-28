package com.cityguard.backend.service;

import com.cityguard.backend.dto.admin.common.AdminPageResponse;
import com.cityguard.backend.dto.admin.log.AdminLogResponse;

public interface AdminLogService {

    AdminPageResponse<AdminLogResponse> getLogs(
            Long actorId,
            String action,
            String targetType,
            int page,
            int size
    );
}
