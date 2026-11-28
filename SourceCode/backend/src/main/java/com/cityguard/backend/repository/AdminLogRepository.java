package com.cityguard.backend.repository;

import com.cityguard.backend.entity.AdminLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {
    @Query(
            value = """
    select log from AdminLog log
    join fetch log.actor actor
    where actor.id = coalesce(:actorId, actor.id)
    and log.action = coalesce(:action, log.action)
    and log.targetType = coalesce(:targetType, log.targetType)
    order by log.createdAt desc, log.id desc
""",
            countQuery = """
                    select count(log) from AdminLog log
                    join log.actor actor
                    where actor.id = coalesce(:actorId, actor.id)
                    and log.action = coalesce(:action, log.action)
                    and log.targetType = coalesce(:targetType, log.targetType)
                    """
    )
    Page<AdminLog> findAdminLogs(
            @Param("actorId") Long actorId,
            @Param("action")String action,
            @Param("targetType")String targetType,
            Pageable pageable
    );
}

