package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.request.CommentRequest;
import com.cityguard.backend.dto.request.CreateReportRequest;
import com.cityguard.backend.dto.request.VoteRequest;
import com.cityguard.backend.dto.response.CommentResponse;
import com.cityguard.backend.dto.response.ReportResponse;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.enums.ReportType;
import com.cityguard.backend.enums.TrustLabel;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.service.ReportService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** In-memory implementation used until the PostgreSQL repository is delivered. */
@Service
public class ReportServiceImpl implements ReportService {
    private final ConcurrentHashMap<String, ReportRecord> reports = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> requestIds = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(4);

    public ReportServiceImpl() {
        seed("rpt_001", "demo", ReportType.TRAFFIC_JAM, "Kẹt xe kéo dài tại Ngã tư Hàng Xanh", "Ngã tư Hàng Xanh, Bình Thạnh, TP.HCM", 10.8012, 106.7145, 9, 1);
        seed("rpt_002", "demo", ReportType.FLOOD, "Ngập nước sâu đoạn qua trường Đại học Hutech", "Đường Ung Văn Khiêm, Bình Thạnh, TP.HCM", 10.8045, 106.7180, 4, 1);
        seed("rpt_003", "demo", ReportType.FALLEN_TREE, "Cây xanh gãy nhánh chắn nửa làn xe máy", "Đường Điện Biên Phủ, Bình Thạnh, TP.HCM", 10.7980, 106.7050, 1, 0);
    }

    private void seed(String id, String creatorId, ReportType type, String description, String address,
                      double lat, double lng, int confirms, int noLonger) {
        ReportRecord r = new ReportRecord(id, creatorId, type, description, address, lat, lng, null,
                Instant.now().minus(15, ChronoUnit.MINUTES), Instant.now().plus(2, ChronoUnit.HOURS));
        r.confirmCount = confirms; r.noLongerCount = noLonger; reports.put(id, r);
    }

    @Override public List<ReportResponse> getReports(String bbox, ReportType type, ReportStatus status) {
        Bbox bounds = Bbox.parse(bbox);
        return reports.values().stream().filter(r -> type == null || r.type == type)
                .filter(r -> status == null || effectiveStatus(r) == status)
                .filter(r -> bounds == null || bounds.contains(r.latitude, r.longitude))
                .map(r -> toResponse(r, null)).toList();
    }

    @Override public ReportResponse getReportById(String id) { return toResponse(getRecord(id), null); }

    @Override public synchronized ReportResponse create(String userId, CreateReportRequest q) {
        String existing = requestIds.get(q.requestId());
        if (existing != null) return toResponse(reports.get(existing), userId);
        String id = "rpt_" + String.format("%03d", sequence.getAndIncrement());
        ReportRecord r = new ReportRecord(id, userId, q.type(), q.description().trim(), q.addressSnapshot().trim(),
                q.latitude(), q.longitude(), q.imageUrl(), Instant.now(), Instant.now().plus(2, ChronoUnit.HOURS));
        reports.put(id, r); requestIds.put(q.requestId(), id); return toResponse(r, userId);
    }

    @Override public synchronized ReportResponse vote(String userId, String id, VoteRequest q) {
        ReportRecord r = getRecord(id); String next = q.type().toUpperCase();
        if (!next.equals("CONFIRMED") && !next.equals("NO_LONGER")) throw new IllegalArgumentException("Invalid vote type");
        String previous = r.votes.put(userId, next);
        if ("CONFIRMED".equals(previous)) r.confirmCount--;
        if ("NO_LONGER".equals(previous)) r.noLongerCount--;
        if ("CONFIRMED".equals(next)) r.confirmCount++;
        if ("NO_LONGER".equals(next)) r.noLongerCount++;
        return toResponse(r, userId);
    }

    @Override public synchronized ReportResponse comment(String userId, String id, CommentRequest q) {
        ReportRecord r = getRecord(id);
        r.comments.add(new CommentResponse(UUID.randomUUID().toString(), userId, q.content().trim(), Instant.now().toString()));
        return toResponse(r, userId);
    }

    @Override public List<ReportResponse> getMyReports(String userId) {
        return reports.values().stream().filter(r -> r.creatorId.equals(userId)).map(r -> toResponse(r, userId)).toList();
    }

    private ReportRecord getRecord(String id) { ReportRecord r = reports.get(id); if (r == null) throw new AppException(ErrorCode.REPORT_NOT_FOUND); return r; }

    private ReportResponse toResponse(ReportRecord r, String userId) {
        int score = Math.max(0, Math.min(100, 50 + (r.confirmCount - r.noLongerCount) * 5));
        return ReportResponse.builder().id(r.id).type(r.type).status(effectiveStatus(r)).trustScore(score)
                .trustLabel(score >= 70 ? TrustLabel.HIGH : score >= 40 ? TrustLabel.MEDIUM : TrustLabel.PENDING_VERIFY)
                .confirmCount(r.confirmCount).noLongerCount(r.noLongerCount).userVote(userId == null ? null : r.votes.get(userId))
                .latitude(r.latitude).longitude(r.longitude).addressSnapshot(r.address).description(r.description).imageUrl(r.imageUrl)
                .createdAt(r.createdAt.toString()).expiresAt(r.expiresAt.toString()).creatorId(r.creatorId)
                .comments(new ArrayList<>(r.comments)).build();
    }

    private ReportStatus effectiveStatus(ReportRecord r) { return Instant.now().isAfter(r.expiresAt) ? ReportStatus.EXPIRED : ReportStatus.ACTIVE; }

    private static final class ReportRecord {
        final String id, creatorId, description, address, imageUrl; final ReportType type; final double latitude, longitude; final Instant createdAt, expiresAt;
        int confirmCount, noLongerCount; final ConcurrentHashMap<String, String> votes = new ConcurrentHashMap<>(); final List<CommentResponse> comments = new ArrayList<>();
        ReportRecord(String id, String creatorId, ReportType type, String description, String address, double latitude, double longitude, String imageUrl, Instant createdAt, Instant expiresAt) {
            this.id=id; this.creatorId=creatorId; this.type=type; this.description=description; this.address=address; this.latitude=latitude; this.longitude=longitude; this.imageUrl=imageUrl; this.createdAt=createdAt; this.expiresAt=expiresAt;
        }
    }

    private record Bbox(double minLng, double minLat, double maxLng, double maxLat) {
        static Bbox parse(String value) { if (value == null || value.isBlank()) return null; try { String[] p=value.split(","); if(p.length!=4)return null; return new Bbox(Double.parseDouble(p[0]),Double.parseDouble(p[1]),Double.parseDouble(p[2]),Double.parseDouble(p[3])); } catch(RuntimeException e){return null;} }
        boolean contains(double lat, double lng) { return lng>=minLng && lng<=maxLng && lat>=minLat && lat<=maxLat; }
    }
}
