package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.comment.CommentResponse;
import com.cityguard.backend.dto.report.CreateReportRequest;
import com.cityguard.backend.dto.report.ReportResponse;
import com.cityguard.backend.dto.report.VoteRequest;
import com.cityguard.backend.entity.Category;
import com.cityguard.backend.entity.Comment;
import com.cityguard.backend.entity.Report;
import com.cityguard.backend.entity.ReportMetrics;
import com.cityguard.backend.entity.ReportVote;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.enums.ReportType;
import com.cityguard.backend.enums.TrustLabel;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.repository.CategoryRepository;
import com.cityguard.backend.repository.CommentRepository;
import com.cityguard.backend.repository.ReportMetricsRepository;
import com.cityguard.backend.repository.ReportRepository;
import com.cityguard.backend.repository.ReportVoteRepository;
import com.cityguard.backend.security.SecurityUtils;
import com.cityguard.backend.service.ReportService;
import com.github.f4b6a3.ulid.UlidCreator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Triển khai ReportService kết nối cơ sở dữ liệu PostgreSQL thực tế (theo NFR-MAINT-01 & WBS Task 4.3.1).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final ReportRepository reportRepository;
    private final ReportMetricsRepository reportMetricsRepository;
    private final ReportVoteRepository reportVoteRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getReports(
            String bbox,
            ReportType type,
            ReportStatus status,
            String sort,
            Double userLat,
            Double userLng
    ) {
        List<ReportStatus> statuses;
        if (status != null) {
            if (status == ReportStatus.HIDDEN && !isCurrentUserAdminOrModerator()) {
                throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xem các báo cáo đã bị ẩn");
            }
            statuses = List.of(status);
        } else {
            // Mặc định lọc các trạng thái còn hiển thị trên bản đồ (theo DacTaKyThuat Trang 4 Mục 2.2)
            statuses = List.of(ReportStatus.PENDING_VERIFY, ReportStatus.ACTIVE);
        }

        String categoryCode = type != null ? type.name() : null;

        Double minLng = null, minLat = null, maxLng = null, maxLat = null;
        if (bbox != null && !bbox.isBlank()) {
            String[] parts = bbox.split(",");
            if (parts.length == 4) {
                try {
                    double p0 = Double.parseDouble(parts[0].trim());
                    double p1 = Double.parseDouble(parts[1].trim());
                    double p2 = Double.parseDouble(parts[2].trim());
                    double p3 = Double.parseDouble(parts[3].trim());

                    if (p0 > p1) {
                        minLng = Math.min(p0, p2);
                        maxLng = Math.max(p0, p2);
                        minLat = Math.min(p1, p3);
                        maxLat = Math.max(p1, p3);
                    } else {
                        minLat = Math.min(p0, p2);
                        maxLat = Math.max(p0, p2);
                        minLng = Math.min(p1, p3);
                        maxLng = Math.max(p1, p3);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Không thể phân tích tham số bounding box: {}", bbox);
                }
            }
        }

        List<Report> reports = reportRepository.findReports(
                statuses,
                categoryCode,
                OffsetDateTime.now(),
                minLat,
                maxLat,
                minLng,
                maxLng
        );

        if ("nearby".equalsIgnoreCase(sort) && userLat != null && userLng != null) {
            reports = reports.stream()
                    .sorted(Comparator.comparingDouble(r ->
                            calculateHaversineDistanceKm(userLat, userLng, r.getLatitude(), r.getLongitude())))
                    .toList();
        } else {
            reports = reports.stream()
                    .sorted(Comparator.comparing(Report::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        Map<Long, String> userVotes = Collections.emptyMap();
        if (currentUserId != null && !reports.isEmpty()) {
            List<Long> reportIds = reports.stream().map(Report::getId).toList();
            userVotes = reportVoteRepository.findByUserIdAndReportIdIn(currentUserId, reportIds).stream()
                    .collect(Collectors.toMap(v -> v.getReport().getId(), ReportVote::getVoteType, (v1, v2) -> v1));
        }

        final Map<Long, String> votesMap = userVotes;
        return reports.stream()
                .map(r -> mapToReportResponse(r, votesMap.get(r.getId()), false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportById(String id) {
        Report report = findReportByIdOrPublicId(id);

        if (report.getStatus() == ReportStatus.HIDDEN && !isCurrentUserAdminOrModerator()) {
            throw new AppException(ErrorCode.REPORT_NOT_FOUND);
        }

        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        String userVote = null;
        if (currentUserId != null) {
            userVote = reportVoteRepository.findByReportIdAndUserId(report.getId(), currentUserId)
                    .map(ReportVote::getVoteType)
                    .orElse(null);
        }

        return mapToReportResponse(report, userVote, true);
    }

    @Override
    @Transactional
    public ReportResponse create(CreateReportRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để tạo báo cáo sự cố"));

        return processCreateReport(currentUser, request);
    }

    private ReportResponse processCreateReport(User currentUser, CreateReportRequest request) {
        String categoryCode = request.getType() != null ? request.getType().name() : null;
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Vui lòng chọn loại sự cố hợp lệ");
        }

        Category category = categoryRepository.findByCodeIgnoreCase(categoryCode)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND,
                        "Không tìm thấy danh mục sự cố: " + categoryCode));

        UUID clientRequestId = parseOrGenerateUuid(request.getClientRequestId());

        // Kiểm tra Idempotency theo client_request_id (BanGiaoDB_V2 Mục 7.1)
        Optional<Report> existingOpt = reportRepository.findByClientRequestId(clientRequestId);
        if (existingOpt.isPresent()) {
            Report existing = existingOpt.get();
            if (existing.getUser().getId().equals(currentUser.getId())) {
                boolean sameCategory = existing.getCategory().getId().equals(category.getId());
                boolean sameLat = Math.abs(existing.getLatitude() - request.getLatitude()) < 0.0001;
                boolean sameLng = Math.abs(existing.getLongitude() - request.getLongitude()) < 0.0001;
                if (sameCategory && sameLat && sameLng) {
                    return mapToReportResponse(existing, null, false);
                } else {
                    throw new AppException(ErrorCode.RESOURCE_CONFLICT,
                            "Yêu cầu bị trùng client_request_id với nội dung khác");
                }
            } else {
                throw new AppException(ErrorCode.RESOURCE_CONFLICT,
                        "client_request_id đã được sử dụng bởi người dùng khác");
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        int ttlSeconds = category.getDefaultTtlSeconds() != null && category.getDefaultTtlSeconds() > 0
                ? category.getDefaultTtlSeconds()
                : 7200;
        OffsetDateTime expiresAt = now.plusSeconds(ttlSeconds);

        // Sinh ULID định dạng rpt_<26-char Crockford Base32 ULID> khớp Schema V3
        String publicId = "rpt_" + UlidCreator.getUlid().toString();

        Report report = Report.builder()
                .publicId(publicId)
                .user(currentUser)
                .category(category)
                .clientRequestId(clientRequestId)
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .addressSnapshot(request.getAddressSnapshot())
                .status(ReportStatus.PENDING_VERIFY)
                .createdAt(now)
                .updatedAt(now)
                .expiresAt(expiresAt)
                .build();

        report = reportRepository.save(report);

        // Khởi tạo ReportMetrics ban đầu (confirmCount = 0, noLongerCount = 0, trustLabel = PENDING_VERIFY)
        ReportMetrics metrics = ReportMetrics.builder()
                .report(report)
                .confirmCount(0L)
                .noLongerCount(0L)
                .trustScore(null)
                .trustLabel(TrustLabel.PENDING_VERIFY.name())
                .computedAt(now)
                .build();
        reportMetricsRepository.save(metrics);
        report.setReportMetrics(metrics);

        return mapToReportResponse(report, null, false);
    }

    @Override
    @Transactional
    public ReportResponse vote(String id, VoteRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để xác nhận sự cố"));

        return processVote(currentUser, id, request);
    }

    private ReportResponse processVote(User currentUser, String id, VoteRequest request) {
        Report report = findReportByIdOrPublicId(id);

        if (report.getStatus() == ReportStatus.HIDDEN) {
            throw new AppException(ErrorCode.FORBIDDEN, "Báo cáo này đã bị ẩn bởi quản trị viên");
        }

        String voteType = request.getVoteType();
        if (!"CONFIRMED".equalsIgnoreCase(voteType) && !"NO_LONGER".equalsIgnoreCase(voteType)) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Loại vote phải là CONFIRMED hoặc NO_LONGER");
        }
        voteType = voteType.toUpperCase();

        // Kiểm tra vote của user trên report này (hỗ trợ Toggle / Hủy vote nếu bấm lại cùng loại)
        String effectiveUserVote = voteType;
        Optional<ReportVote> existingVoteOpt = reportVoteRepository.findByReportIdAndUserId(
                report.getId(), currentUser.getId());

        if (existingVoteOpt.isPresent()) {
            ReportVote existingVote = existingVoteOpt.get();
            if (existingVote.getVoteType().equalsIgnoreCase(voteType)) {
                // Hủy vote khi người dùng bấm lại đúng loại vote hiện tại
                reportVoteRepository.delete(existingVote);
                reportVoteRepository.flush();
                effectiveUserVote = null;
            } else {
                existingVote.setVoteType(voteType);
                existingVote.setUpdatedAt(OffsetDateTime.now());
                reportVoteRepository.save(existingVote);
            }
        } else {
            ReportVote newVote = ReportVote.builder()
                    .report(report)
                    .user(currentUser)
                    .voteType(voteType)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            reportVoteRepository.save(newVote);
        }

        // Đếm lại confirm_count và no_longer_count từ report_votes
        long confirmCount = reportVoteRepository.countByReportIdAndVoteType(report.getId(), "CONFIRMED");
        long noLongerCount = reportVoteRepository.countByReportIdAndVoteType(report.getId(), "NO_LONGER");
        double confirm = (double) confirmCount;
        double noLonger = (double) noLongerCount;
        double totalVotes = confirm + noLonger;

        // Công thức Trust Score theo DacTaKyThuat Trang 7 Mục 5.2
        double supportRatio = (confirm + 1.0) / (confirm + noLonger + 2.0);
        double evidenceFactor = Math.min((confirm + noLonger) / 10.0, 1.0);
        double ageHours = Math.max(0.0,
                (double) Duration.between(report.getCreatedAt(), OffsetDateTime.now()).toMinutes() / 60.0);
        double recencyFactor = Math.exp(-ageHours / 48.0);
        long trustScore = Math.round(100.0 * (0.70 * supportRatio + 0.30 * evidenceFactor) * recencyFactor);

        // Gán TrustLabel
        TrustLabel trustLabel;
        if (totalVotes == 0) {
            trustLabel = TrustLabel.PENDING_VERIFY;
        } else if (trustScore >= 70 && totalVotes >= 5) {
            trustLabel = TrustLabel.HIGH;
        } else if (trustScore >= 40) {
            trustLabel = TrustLabel.MEDIUM;
        } else {
            trustLabel = TrustLabel.LOW;
        }

        // Cập nhật ReportMetrics
        ReportMetrics metrics = report.getReportMetrics();
        if (metrics == null) {
            metrics = ReportMetrics.builder().report(report).build();
        }
        metrics.setConfirmCount(confirmCount);
        metrics.setNoLongerCount(noLongerCount);
        metrics.setTrustScore(totalVotes == 0 ? null : BigDecimal.valueOf(trustScore));
        metrics.setTrustLabel(trustLabel.name());
        metrics.setComputedAt(OffsetDateTime.now());
        reportMetricsRepository.save(metrics);
        report.setReportMetrics(metrics);

        // Nếu Report đang PENDING_VERIFY và đã có vote -> chuyển ACTIVE (DacTaKyThuat Trang 4 Mục 2.2)
        if (report.getStatus() == ReportStatus.PENDING_VERIFY && totalVotes > 0) {
            report.setStatus(ReportStatus.ACTIVE);
            report.setUpdatedAt(OffsetDateTime.now());
            reportRepository.save(report);
        }

        return mapToReportResponse(report, effectiveUserVote, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getMyReports() {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED,
                        "Vui lòng đăng nhập để xem danh sách báo cáo của bạn"));

        List<Report> reports = reportRepository
                .findByUserId(currentUser.getId(), Pageable.unpaged()).getContent();
        return reports.stream()
                .map(r -> mapToReportResponse(r, null, false))
                .toList();
    }

    private Report findReportByIdOrPublicId(String id) {
        if (id == null || id.isBlank()) {
            throw new AppException(ErrorCode.REPORT_NOT_FOUND);
        }
        String trimmed = id.trim();
        if (trimmed.matches("^\\d+$")) {
            Long numericId = Long.parseLong(trimmed);
            return reportRepository.findByIdWithDetails(numericId)
                    .or(() -> reportRepository.findByPublicIdWithDetails(trimmed))
                    .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        }
        return reportRepository.findByPublicIdWithDetails(trimmed)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
    }

    private boolean isCurrentUserAdminOrModerator() {
        return SecurityUtils.getCurrentUser()
                .map(User::getRole)
                .map(role -> "ADMIN".equalsIgnoreCase(role) || "MODERATOR".equalsIgnoreCase(role))
                .orElse(false);
    }

    private double calculateHaversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private ReportResponse mapToReportResponse(Report report, String userVote, boolean includeComments) {
        ReportType type = null;
        if (report.getCategory() != null && report.getCategory().getCode() != null) {
            try {
                type = ReportType.valueOf(report.getCategory().getCode());
            } catch (IllegalArgumentException ignored) {
            }
        }

        ReportMetrics metrics = report.getReportMetrics();
        Integer trustScore = null;
        TrustLabel trustLabel = TrustLabel.PENDING_VERIFY;
        int confirmCount = 0;
        int noLongerCount = 0;

        if (metrics != null) {
            if (metrics.getConfirmCount() != null) {
                confirmCount = metrics.getConfirmCount().intValue();
            }
            if (metrics.getNoLongerCount() != null) {
                noLongerCount = metrics.getNoLongerCount().intValue();
            }
            if (metrics.getTrustScore() != null) {
                trustScore = metrics.getTrustScore().intValue();
            }
            if (metrics.getTrustLabel() != null) {
                try {
                    trustLabel = TrustLabel.valueOf(metrics.getTrustLabel());
                } catch (IllegalArgumentException ignored) {
                }
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

        List<CommentResponse> comments = List.of();
        if (includeComments) {
            Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
            comments = commentRepository.findActiveCommentsListByReportId(report.getId())
                    .stream()
                    .map(c -> mapToCommentResponse(c, currentUserId))
                    .toList();
        }

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
                .comments(comments)
                .build();
    }

    private UUID parseOrGenerateUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return UUID.randomUUID();
        }
        String trimmed = raw.trim();
        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException ex) {
            return UUID.nameUUIDFromBytes(trimmed.getBytes(StandardCharsets.UTF_8));
        }
    }

    private CommentResponse mapToCommentResponse(Comment comment, Long currentUserId) {
        String createdAtStr = comment.getCreatedAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(comment.getCreatedAt().toInstant())
                : null;

        boolean isOwner = currentUserId != null
                && comment.getUser() != null
                && currentUserId.equals(comment.getUser().getId());

        return CommentResponse.builder()
                .id(String.valueOf(comment.getId()))
                .authorId(comment.getUser() != null ? String.valueOf(comment.getUser().getId()) : null)
                .authorName(comment.getUser() != null ? comment.getUser().getDisplayName() : null)
                .body(comment.getBody())
                .content(comment.getBody())
                .createdAt(createdAtStr)
                .isOwner(isOwner)
                .build();
    }
}
