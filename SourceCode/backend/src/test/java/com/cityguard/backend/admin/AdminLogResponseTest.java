package com.cityguard.backend.admin;

import com.cityguard.backend.dto.admin.log.AdminLogResponse;
import com.cityguard.backend.entity.AdminLog;
import com.cityguard.backend.entity.User;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AdminLogResponseTest {

    @Test
    void serializesOnlyAuditLogFieldsExpectedByReact() {
        var actor = User.builder().id(2L).username("moderator").displayName("Moderator")
                .email("moderator@example.test").passwordHash("fixture-only").build();
        var log = AdminLog.builder().id(1L).actor(actor)
                .action("HIDE_REPORT").targetType("REPORT").targetId(3L)
                .createdAt(OffsetDateTime.parse("2026-11-28T14:00:00+07:00")).build();

        var json = JsonMapper.builder().build().valueToTree(AdminLogResponse.from(log));

        assertThat(json.size()).isEqualTo(9);
        assertThat(json.path("id").asLong()).isEqualTo(1);
        assertThat(json.path("actorId").asLong()).isEqualTo(2);
        assertThat(json.path("actorUsername").asString()).isEqualTo("moderator");
        assertThat(json.path("actorDisplayName").asString()).isEqualTo("Moderator");
        assertThat(json.path("action").asString()).isEqualTo("HIDE_REPORT");
        assertThat(json.path("targetType").asString()).isEqualTo("REPORT");
        assertThat(json.path("targetId").isNumber()).isTrue();
        assertThat(json.path("targetId").asLong()).isEqualTo(3);
        assertThat(json.path("reason").isNull()).isTrue();
        assertThat(json.path("createdAt").asString()).isEqualTo("2026-11-28T07:00:00Z");
    }
}
