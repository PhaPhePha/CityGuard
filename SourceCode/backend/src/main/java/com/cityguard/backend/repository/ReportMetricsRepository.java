package com.cityguard.backend.repository;

import com.cityguard.backend.entity.ReportMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportMetricsRepository extends JpaRepository<ReportMetrics, Long> {
}

