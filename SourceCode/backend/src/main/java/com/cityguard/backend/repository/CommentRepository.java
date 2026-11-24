package com.cityguard.backend.repository;

import com.cityguard.backend.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByReportIdAndIsHiddenFalseOrderByCreatedAtDesc(Long reportId);

    @Query(value = "SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.report.id = :reportId AND c.isHidden = false AND c.deletedAt IS NULL ORDER BY c.createdAt DESC, c.id DESC",
           countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.report.id = :reportId AND c.isHidden = false AND c.deletedAt IS NULL")
    Page<Comment> findActiveCommentsByReportId(@Param("reportId") Long reportId, Pageable pageable);

    Optional<Comment> findByClientRequestId(UUID clientRequestId);
}
