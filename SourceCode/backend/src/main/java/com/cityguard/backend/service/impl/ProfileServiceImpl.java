package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.common.PageResponse;
import com.cityguard.backend.dto.report.ReportResponse;
import com.cityguard.backend.dto.user.UserProfileSummaryResponse;
import com.cityguard.backend.entity.Report;
import com.cityguard.backend.entity.ReportMetrics;
import com.cityguard.backend.entity.ReportVote;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.enums.ReportType;
import com.cityguard.backend.enums.TrustLabel;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.repository.ReportRepository;
import com.cityguard.backend.repository.ReportVoteRepository;
import com.cityguard.backend.security.SecurityUtils;
import com.cityguard.backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileServiceImpl implements ProfileService {

    private final ReportRepository reportRepository;
    private final ReportVoteRepository reportVoteRepository;

    @Override
    public PageResponse<ReportResponse> getMyReports(ReportStatus status, int page, int size) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để xem báo cáo của bạn"));

        int pageSize = Math.min(Math.max(1, size), 50);
        int pageNumber = Math.max(0, page);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Report> reportPage;
        if (status != null) {
            reportPage = reportRepository.findByUserIdAndStatus(currentUser.getId(), status, pageable);
        } else {
            reportPage = reportRepository.findByUserId(currentUser.getId(), pageable);
        }

        // Tải các vote của user đối với các report này
        List<Long> reportIds = reportPage.getContent().stream().map(Report::getId).toList();
        Map<Long, String> userVotes = Collections.emptyMap();
        if (!reportIds.isEmpty()) {
            userVotes = reportVoteRepository.findByUserIdAndReportIdIn(currentUser.getId(), reportIds).stream()
                    .collect(Collectors.toMap(v -> v.getReport().getId(), ReportVote::getVoteType, (v1, v2) -> v1));
        }

        final Map<Long, String> votesMap = userVotes;
        Page<ReportResponse> responsePage = reportPage.map(r -> mapToReportResponse(r, votesMap.get(r.getId())));
        return PageResponse.of(responsePage);
    }

    @Override
    public UserProfileSummaryResponse getProfileSummary() {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để xem thông tin cá nhân"));

        long reportCount = reportRepository.countByUserId(currentUser.getId());
        long confirmedReports = reportVoteRepository.countByUserIdAndVoteType(currentUser.getId(), "CONFIRMED");

        return UserProfileSummaryResponse.builder()
                .id(currentUser.getId())
                .username(currentUser.getUsername())
                .email(currentUser.getEmail())
                .displayName(currentUser.getDisplayName())
                .avatarUrl(currentUser.getAvatarUrl())
                .role(currentUser.getRole())
                .reportCount(reportCount)
                .confirmedReports(confirmedReports)
                .build();
    }

    private ReportResponse mapToReportResponse(Report report, String userVote) {
        ReportType type = null;
        if (report.getCategory() != null && report.getCategory().getCode() != null) {
            try {
                type = ReportType.valueOf(report.getCategory().getCode());
            } catch (IllegalArgumentException ignored) {}
        }

        ReportMetrics metrics = report.getReportMetrics();
        Integer trustScore = null;
        TrustLabel trustLabel = TrustLabel.PENDING_VERIFY;
        int confirmCount = 0;
        int noLongerCount = 0;

        if (metrics != null) {
            if (metrics.getConfirmCount() != null) confirmCount = metrics.getConfirmCount().intValue();
            if (metrics.getNoLongerCount() != null) noLongerCount = metrics.getNoLongerCount().intValue();
            if (metrics.getTrustScore() != null) trustScore = metrics.getTrustScore().intValue();
            if (metrics.getTrustLabel() != null) {
                try {
                    trustLabel = TrustLabel.valueOf(metrics.getTrustLabel());
                } catch (IllegalArgumentException ignored) {}
            }
        }

        String createdAtStr = report.getCreatedAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(report.getCreatedAt().toInstant())
                : null;
        String expiresAtStr = report.getExpiresAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(report.getExpiresAt().toInstant())
                : null;
        String creatorId = report.getUser() != null
                ? String.valueOf(report.getUser().getId())
                : null;

        return ReportResponse.builder()
                .id(report.getPublicId())
                .type(type)
                .status(report.getStatus())
                .trustScore(trustScore)
                .trustLabel(trustLabel)
                .confirmCount(confirmCount)
                .noLongerCount(noLongerCount)
                .userVote(userVote)
                .latitude(report.getLatitude())
                .longitude(report.getLongitude())
                .addressSnapshot(report.getAddressSnapshot())
                .description(report.getDescription())
                .imageUrl(report.getImageUrl())
                .createdAt(createdAtStr)
                .expiresAt(expiresAtStr)
                .creatorId(creatorId)
                .comments(List.of())
                .build();
    }
}

