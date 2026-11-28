package com.cityguard.backend.repository;

import com.cityguard.backend.entity.Report;
import com.cityguard.backend.enums.ReportStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.category c LEFT JOIN FETCH r.reportMetrics m WHERE r.publicId = :publicId")
    Optional<Report> findByPublicIdWithDetails(@Param("publicId") String publicId);

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.category c LEFT JOIN FETCH r.reportMetrics m WHERE r.id = :id")
    Optional<Report> findByIdWithDetails(@Param("id") Long id);

    Optional<Report> findByClientRequestId(UUID clientRequestId);

    long countByUserId(Long userId);
    long countByStatus(ReportStatus status);

    @Query(value = "SELECT r FROM Report r LEFT JOIN FETCH r.category c LEFT JOIN FETCH r.reportMetrics m WHERE r.user.id = :userId ORDER BY r.createdAt DESC",
           countQuery = "SELECT COUNT(r) FROM Report r WHERE r.user.id = :userId")
    Page<Report> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = "SELECT r FROM Report r LEFT JOIN FETCH r.category c LEFT JOIN FETCH r.reportMetrics m WHERE r.user.id = :userId AND r.status = :status ORDER BY r.createdAt DESC",
           countQuery = "SELECT COUNT(r) FROM Report r WHERE r.user.id = :userId AND r.status = :status")
    Page<Report> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ReportStatus status, Pageable pageable);
    @Query("""
    SELECT r FROM Report r
    LEFT JOIN FETCH r.category
    LEFT JOIN FETCH r.reportMetrics
    LEFT JOIN FETCH r.user
    WHERE r.publicId = :publicId
""")
    Optional<Report> findByPublicIdForAdmin(
            @Param("publicId") String publicId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Report r where r.publicId = :publicId")
    Optional<Report> findByPublicIdForUpdate(@Param("publicId") String publicId);

    @Query(
            value = """
        SELECT r FROM Report r
        LEFT JOIN FETCH r.category c
        LEFT JOIN FETCH r.reportMetrics m
        WHERE (:status IS NULL OR r.status = :status)
          AND (:categoryId IS NULL OR c.id = :categoryId)
          AND (
              :keywordPattern IS NULL
              OR LOWER(r.publicId) LIKE :keywordPattern
              OR LOWER(COALESCE(r.description, '')) LIKE :keywordPattern
              OR LOWER(COALESCE(r.addressSnapshot, '')) LIKE :keywordPattern
          )
    """,
            countQuery = """
        SELECT COUNT(r) FROM Report r
        WHERE (:status IS NULL OR r.status = :status)
          AND (:categoryId IS NULL OR r.category.id = :categoryId)
          AND (
              :keywordPattern IS NULL
              OR LOWER(r.publicId) LIKE :keywordPattern
              OR LOWER(COALESCE(r.description, '')) LIKE :keywordPattern
              OR LOWER(COALESCE(r.addressSnapshot, '')) LIKE :keywordPattern
          )
    """
    )
    Page<Report> findAdminReports(
            @Param("keywordPattern") String keywordPattern,
            @Param("categoryId") Long categoryId,
            @Param("status") ReportStatus status,
            Pageable pageable
    );

}
