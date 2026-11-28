package com.cityguard.backend.repository;

import com.cityguard.backend.entity.Report;
import com.cityguard.backend.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("""
        SELECT r FROM Report r
        LEFT JOIN FETCH r.category c
        LEFT JOIN FETCH r.reportMetrics m
        WHERE r.status IN :statuses
          AND (:categoryCode IS NULL OR c.code = :categoryCode)
          AND r.expiresAt > :now
          AND (:minLat IS NULL OR r.latitude >= :minLat)
          AND (:maxLat IS NULL OR r.latitude <= :maxLat)
          AND (:minLng IS NULL OR r.longitude >= :minLng)
          AND (:maxLng IS NULL OR r.longitude <= :maxLng)
        ORDER BY r.createdAt DESC
    """)
    List<Report> findReports(
            @Param("statuses") List<ReportStatus> statuses,
            @Param("categoryCode") String categoryCode,
            @Param("now") OffsetDateTime now,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng
    );

    @Query(
            value = """
        SELECT r FROM Report r
        LEFT JOIN FETCH r.category
        LEFT JOIN FETCH r.reportMetrics
        WHERE r.status IN :statuses
          AND r.expiresAt > :now
          AND (:recent = false OR r.createdAt >= :recentSince)
        ORDER BY r.createdAt DESC, r.id DESC
    """,
            countQuery = """
        SELECT COUNT(r) FROM Report r
        WHERE r.status IN :statuses
          AND r.expiresAt > :now
          AND (:recent = false OR r.createdAt >= :recentSince)
    """
    )
    Page<Report> findCommunityReports(
            @Param("statuses") List<ReportStatus> statuses,
            @Param("now") OffsetDateTime now,
            @Param("recent") boolean recent,
            @Param("recentSince") OffsetDateTime recentSince,
            Pageable pageable
    );

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.category c LEFT JOIN FETCH r.reportMetrics m WHERE r.publicId = :publicId")
    Optional<Report> findByPublicIdWithDetails(@Param("publicId") String publicId);

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.category c LEFT JOIN FETCH r.reportMetrics m WHERE r.id = :id")
    Optional<Report> findByIdWithDetails(@Param("id") Long id);

    Optional<Report> findByClientRequestId(UUID clientRequestId);

    long countByUserId(Long userId);

    @Query(value = "SELECT r FROM Report r LEFT JOIN FETCH r.category c LEFT JOIN FETCH r.reportMetrics m WHERE r.user.id = :userId ORDER BY r.createdAt DESC",
           countQuery = "SELECT COUNT(r) FROM Report r WHERE r.user.id = :userId")
    Page<Report> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT r FROM Report r LEFT JOIN FETCH r.category c LEFT JOIN FETCH r.reportMetrics m WHERE r.user.id = :userId AND r.status = :status ORDER BY r.createdAt DESC",
           countQuery = "SELECT COUNT(r) FROM Report r WHERE r.user.id = :userId AND r.status = :status")
    Page<Report> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ReportStatus status, Pageable pageable);
}
