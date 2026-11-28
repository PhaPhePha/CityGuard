package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.admin.common.AdminPageResponse;
import com.cityguard.backend.dto.admin.report.AdminReportDetailResponse;
import com.cityguard.backend.dto.admin.report.AdminReportSummaryResponse;
import com.cityguard.backend.dto.admin.report.AdminUpdateReportStatusRequest;
import com.cityguard.backend.entity.AdminLog;
import com.cityguard.backend.entity.Report;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.repository.AdminLogRepository;
import com.cityguard.backend.repository.ReportRepository;
import com.cityguard.backend.security.SecurityUtils;
import com.cityguard.backend.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final ReportRepository reportRepository;
    private final AdminLogRepository adminLogRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminReportDetailResponse getReportDetail(String publicId){
        Report report = reportRepository
                .findByPublicIdForAdmin(publicId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.REPORT_NOT_FOUND,
                        "Không tìm thấy báo cáo"
                ));
        return AdminReportDetailResponse.from(report);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPageResponse<AdminReportSummaryResponse>getReports(
            String keyword,
            Long categoryId,
            ReportStatus status,
            int page,
            int size,
            String sortBy,
            String direction
    ){
        if(page <0 || size<1 || size>100){
            throw new AppException(
                    ErrorCode.INVALID_INPUT,
                    "Thông số phân trang không hợp lệ"
            );
        }

        if(!"createdAt".equalsIgnoreCase(sortBy)){
            throw new AppException(
                    ErrorCode.INVALID_INPUT,
                    "Trường sắp xếp không hợp lệ"
            );
        }
        Sort.Direction sortDirection;
        try{
            sortDirection = Sort.Direction.fromString(direction);
        }catch(IllegalArgumentException ex){
            throw new AppException(
                    ErrorCode.INVALID_INPUT,
                    "Chiều sắp xếp không hợp lệ"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
                        .and(Sort.by(Sort.Direction.DESC,"id"))
        );

        String keywordPatttern =
                (keyword == null || keyword.isBlank())
                ? null
                        : "%" + keyword.trim().toLowerCase(
                                Locale.ROOT
                ) + "%";
        Page<Report> reports = reportRepository.findAdminReports(
                keywordPatttern,
                categoryId,
                status,
                pageable
        );
        List<AdminReportSummaryResponse> items =
                reports.getContent()
                        .stream()
                        .map(AdminReportSummaryResponse::from)
                        .toList();

        return AdminPageResponse.of(reports, items);
    }

    @Override
    @Transactional
    public AdminReportDetailResponse updateReportStatus(
            String publicId,
            AdminUpdateReportStatusRequest request
    ) {
        Report report = reportRepository.findByPublicIdForUpdate(publicId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        ReportStatus current = report.getStatus();
        ReportStatus target = request.getStatus();

        if (!isValidTransition(current, target)) {
            throw new AppException(
                    ErrorCode.INVALID_INPUT,
                    "Không thể chuyển trạng thái từ " + current + " sang " + target
            );
        }
        User actor = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        String action = switch (target) {
            case HIDDEN -> "HIDE_REPORT";
            case ACTIVE -> "RESTORE_REPORT";
            default -> "UPDATE_REPORT_STATUS";
        };
        report.setStatus(target);
        adminLogRepository.save(AdminLog.builder()
                .actor(actor)
                .action(action)
                .targetType("REPORT")
                .targetId(report.getId())
                .reason(request.getReason())
                .createdAt(OffsetDateTime.now())
                .build()
        );
        return AdminReportDetailResponse.from(report);
    }

    private boolean isValidTransition(
            ReportStatus current,
            ReportStatus target
    ){
        if(current == null || target == null || current == target){
            return false;
        }
        return switch(current){
            case ACTIVE, PENDING_VERIFY ->
                target == ReportStatus.HIDDEN
                        ||target == ReportStatus.RESOLVED;
            case HIDDEN ->
                target == ReportStatus.ACTIVE;
            case RESOLVED, EXPIRED ->
                target == ReportStatus.HIDDEN;
        };
    }
}
