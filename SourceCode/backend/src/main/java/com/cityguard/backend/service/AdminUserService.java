package com.cityguard.backend.service;

import com.cityguard.backend.dto.admin.common.AdminPageResponse;
import com.cityguard.backend.dto.admin.user.AdminUpdateUserStatusRequest;
import com.cityguard.backend.dto.admin.user.AdminUserDetailResponse;
import com.cityguard.backend.dto.admin.user.AdminUserListItemResponse;

public interface AdminUserService {

    AdminPageResponse<AdminUserListItemResponse> getUsers(
            String search,
            String role,
            String status,
            int page, int size
    );

    AdminUserDetailResponse getUserDetail(Long id);
    AdminUserListItemResponse updateUserStatus(
            Long id,
            AdminUpdateUserStatusRequest request
    );

}
