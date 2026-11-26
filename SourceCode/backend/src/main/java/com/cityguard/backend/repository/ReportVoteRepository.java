package com.cityguard.backend.repository;

import com.cityguard.backend.entity.ReportVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportVoteRepository extends JpaRepository<ReportVote, Long> {
    Optional<ReportVote> findByReportIdAndUserId(Long reportId, Long userId);
    long countByReportIdAndVoteType(Long reportId, String voteType);
    long countByUserIdAndVoteType(Long userId, String voteType);
    List<ReportVote> findByUserIdAndReportIdIn(Long userId, List<Long> reportIds);
}
