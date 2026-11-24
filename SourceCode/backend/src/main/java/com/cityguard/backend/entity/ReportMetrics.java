package com.cityguard.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "report_metrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportMetrics {

    @Id
    @Column(name = "report_id")
    private Long reportId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "report_id")
    private Report report;

    @Column(name = "confirm_count", nullable = false)
    @Builder.Default
    private Long confirmCount = 0L;

    @Column(name = "no_longer_count", nullable = false)
    @Builder.Default
    private Long noLongerCount = 0L;

    @Column(name = "trust_score", precision = 5, scale = 2)
    private BigDecimal trustScore;

    @Column(name = "trust_label", length = 32)
    private String trustLabel;

    @Column(name = "computed_at")
    private OffsetDateTime computedAt;
}

