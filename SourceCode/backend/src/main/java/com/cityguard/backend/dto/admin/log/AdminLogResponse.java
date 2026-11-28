package com.cityguard.backend.dto.admin.log;

import com.cityguard.backend.entity.AdminLog;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@AllArgsConstructor
public class AdminLogResponse {
    private final Long id;
    private final Long actorId;
    private final String actorUsername;
    private final String actorDisplayName;
    private final String action;
    private final String targetType;
    private final Long targetId;
    private final String reason;
    private final String createdAt;

    public static AdminLogResponse from(AdminLog log){
        var actor = log.getActor();
        return new AdminLogResponse(
                log.getId(),
                actor.getId(),
                actor.getUsername(),
                actor.getDisplayName(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getReason(),
                DateTimeFormatter.ISO_INSTANT.format(log.getCreatedAt().toInstant())
        );
    }

}
