package com.cityguard.backend.admin;

import com.cityguard.backend.dto.admin.comment.AdminCommentResponse;
import com.cityguard.backend.entity.Comment;
import com.cityguard.backend.entity.Report;
import com.cityguard.backend.entity.User;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AdminCommentResponseTest {

    @Test
    void serializesReactFieldsWithoutInternalReportIdOrPasswordHash() {
        var comment = Comment.builder()
                .id(10L)
                .report(Report.builder().id(20L).publicId("rpt_01M6P4YPXFBG21T29EZJ31RR0Q").build())
                .user(User.builder().id(30L).username("member").displayName("Member").passwordHash("fixture").build())
                .body("Sample comment")
                .isHidden(false)
                .createdAt(OffsetDateTime.parse("2026-11-27T08:00:00+07:00"))
                .build();
        var json = JsonMapper.builder().build().valueToTree(AdminCommentResponse.from(comment));

        assertThat(json.path("reportPublicId").asText()).isEqualTo(comment.getReport().getPublicId());
        assertThat(json.path("isHidden").isBoolean()).isTrue();
        assertThat(json.path("isHidden").asBoolean()).isFalse();
        assertThat(json.path("createdAt").asText()).isEqualTo("2026-11-27T01:00:00Z");
        assertThat(json.path("deletedAt").isNull()).isTrue();
        assertThat(json.has("hidden")).isFalse();
        assertThat(json.has("reportId")).isFalse();
        assertThat(json.has("passwordHash")).isFalse();
    }
}
