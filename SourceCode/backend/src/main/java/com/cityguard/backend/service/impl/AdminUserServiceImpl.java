package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.admin.common.AdminPageResponse;
import com.cityguard.backend.dto.admin.report.AdminReportSummaryResponse;
import com.cityguard.backend.dto.admin.user.AdminUpdateUserStatusRequest;
import com.cityguard.backend.dto.admin.user.AdminUserDetailResponse;
import com.cityguard.backend.dto.admin.user.AdminUserListItemResponse;
import com.cityguard.backend.entity.AdminLog;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.repository.AdminLogRepository;
import com.cityguard.backend.repository.ReportRepository;
import com.cityguard.backend.repository.UserRepository;
import com.cityguard.backend.security.SecurityUtils;
import com.cityguard.backend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {
    private static final Set<String> ROLES = Set.of("USER", "MODERATOR","ADMIN");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "LOCKED");
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final AdminLogRepository adminLogRepository;
    @Override
    @Transactional(readOnly = true)
    public AdminPageResponse<AdminUserListItemResponse> getUsers(
            String search,
            String role,
            String status,
            int page,
            int size
    ){
        if(page < 0 || size < 1 || size > 100){
            throw new AppException(ErrorCode.INVALID_INPUT,"Thông số phân trang không hợp lệ");
        }
        if(role != null && !ROLES.contains(role)){
            throw new AppException(ErrorCode.INVALID_INPUT, "Vai trò không hợp lệ");
        }
        if(status != null && !STATUSES.contains(status)){
            throw new AppException(ErrorCode.INVALID_INPUT, "Trạng thái tài khoản không hợp lệ");
        }

        String searchTerm = search == null ? null : search.strip();
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"createdAt","id"));
        var users = userRepository.findAdminUsers(searchTerm, role, status, pageable);

        return AdminPageResponse.of(users.map(AdminUserListItemResponse::from));
    }

    @Override
    @Transactional
    public AdminUserDetailResponse getUserDetail(Long id){
        var user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Không tìm thấy người dùng")
                );
        var reports = reportRepository.findByUserId(
                id, PageRequest.of(0,5,Sort.by(Sort.Direction.DESC,"id"))
        );
        var recentReports = reports.map(AdminReportSummaryResponse::from).getContent();
        return AdminUserDetailResponse.from(user, reports.getTotalElements(), recentReports);
    }
    @Override
    @Transactional
    public AdminUserListItemResponse updateUserStatus(Long id, AdminUpdateUserStatusRequest request){
        var actor = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
        String target = request.getStatus();
        if(target == null || !STATUSES.contains(target)){
            throw new AppException(ErrorCode.INVALID_INPUT, "Trạng thái tài khoản không hợp lệ");
        }
        if("LOCKED".equals(target)&& id.equals(actor.getId())){
            throw new AppException(ErrorCode.INVALID_INPUT, "Bạn không thể tự khóa tài khoản");
        }

        var user = userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,"Không tìm thấy người dùng"
                ));
        if (target.equals(user.getStatus())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Tài khoản đã ở trạng thái " + target);
        }

        user.setStatus(target);
        adminLogRepository.save(AdminLog.builder()
                .actor(actor)
                .action("LOCKED".equals(target) ? "LOCK_USER" : "UNLOCK_USER")
                .targetType("USER")
                .targetId(user.getId())
                .reason(request.getReason())
                .createdAt(OffsetDateTime.now())
                .build()
        );

        return AdminUserListItemResponse.from(user);
    }
}
