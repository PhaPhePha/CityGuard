package com.cityguard.backend.repository;

import com.cityguard.backend.entity.Report;
import com.cityguard.backend.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
}

