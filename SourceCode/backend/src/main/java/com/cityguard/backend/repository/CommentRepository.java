package com.cityguard.backend.repository;

import com.cityguard.backend.entity.Comment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    void deleteByReportId(Long reportId);
    List<Comment> findByReportIdAndIsHiddenFalseOrderByCreatedAtDesc(Long reportId);

    @Query("SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.report.id = :reportId AND c.isHidden = false AND c.deletedAt IS NULL ORDER BY c.createdAt DESC, c.id DESC")
    List<Comment> findActiveCommentsListByReportId(@Param("reportId") Long reportId);

    @Query(value = "SELECT c FROM Comment c LEFT JOIN FETCH c.user WHERE c.report.id = :reportId AND c.isHidden = false AND c.deletedAt IS NULL ORDER BY c.createdAt DESC, c.id DESC",
           countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.report.id = :reportId AND c.isHidden = false AND c.deletedAt IS NULL")
    Page<Comment> findActiveCommentsByReportId(@Param("reportId") Long reportId, Pageable pageable);

    Optional<Comment> findByClientRequestId(UUID clientRequestId);

    @Query(
            value = """
    select c from Comment c
    join fetch c.report r
    join fetch c.user
    where r.publicId = coalesce(:reportPublicId, r.publicId)
    and c.isHidden = coalesce(:isHidden, c.isHidden)
    order by c.createdAt desc, c.id desc
""",
            countQuery = """
                    select count(c) from Comment c
                    join c.report r
                    where r.publicId = coalesce(:reportPublicId, r.publicId)
                    and c.isHidden = coalesce(:isHidden, c.isHidden)
                    """
    )
    Page<Comment> findAdminComments(
            @Param("reportPublicId") String reportPublicId,
            @Param("isHidden") Boolean isHidden,
            Pageable pageable
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comment c where c.id = :id")
    Optional<Comment> findByIdForUpdate(@Param("id") Long id);
}
