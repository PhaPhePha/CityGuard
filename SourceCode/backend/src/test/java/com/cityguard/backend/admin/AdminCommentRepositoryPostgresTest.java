package com.cityguard.backend.admin;

import com.cityguard.backend.entity.Comment;
import com.cityguard.backend.repository.CommentRepository;
import com.github.f4b6a3.ulid.UlidCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "CITYGUARD_PHASE5_TEST_PG_PORT", matches = "[0-9]{1,5}")
@SpringBootTest(properties = {
        "cityguard.demo.seed-data=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "jwt.secret=CityGuardPhase5TestSecretKeyWithAtLeast32Bytes!"
})
@Transactional
class AdminCommentRepositoryPostgresTest {

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://127.0.0.1:"
                + System.getenv("CITYGUARD_PHASE5_TEST_PG_PORT") + "/cityguard_phase5_test");
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private CommentRepository repository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void filtersCountsAndOrdersCommentsAndLoadsRowForUpdate() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("cityguard_phase5_test");
        String username = "phase5_" + UUID.randomUUID().toString().replace("-", "");
        Long userId = jdbc.queryForObject("""
                insert into users (username, password_hash, display_name)
                values (?, ?, 'Phase 5 member') returning id
                """, Long.class, username, passwordEncoder.encode("phase5-test-only"));
        Long categoryId = jdbc.queryForObject("""
                insert into categories (code, display_name, icon, color, default_ttl_seconds)
                values ('PHASE5_TEST', 'Phase 5 test', 'test', '#123456', 3600) returning id
                """, Long.class);
        String publicId = "rpt_" + UlidCreator.getUlid();
        Long reportId = createReport(publicId, userId, categoryId);
        Long otherReportId = createReport("rpt_" + UlidCreator.getUlid(), userId, categoryId);
        Long olderId = createComment(reportId, userId, false);
        Long newerId = createComment(reportId, userId, false);
        Long hiddenId = createComment(reportId, userId, true);
        createComment(otherReportId, userId, false);

        var firstPage = repository.findAdminComments(publicId, false, PageRequest.of(0, 1));
        assertThat(firstPage.getContent()).extracting(Comment::getId).containsExactly(newerId);
        assertThat(firstPage.getTotalElements()).isEqualTo(2);
        assertThat(repository.findAdminComments(publicId, false, PageRequest.of(1, 1)).getContent())
                .extracting(Comment::getId).containsExactly(olderId);
        assertThat(repository.findAdminComments(publicId, true, PageRequest.of(0, 10)).getContent())
                .extracting(Comment::getId).containsExactly(hiddenId);
        assertThat(repository.findAdminComments(publicId, null, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(3);
        assertThat(repository.findAdminComments(null, null, PageRequest.of(0, 10)).getContent())
                .extracting(Comment::getId).contains(olderId, newerId, hiddenId);
        assertThat(repository.findByIdForUpdate(hiddenId)).hasValueSatisfying(comment ->
                assertThat(comment.getIsHidden()).isTrue());
    }

    private Long createReport(String publicId, Long userId, Long categoryId) {
        return jdbc.queryForObject("""
                insert into reports (public_id, user_id, category_id, client_request_id, latitude, longitude, expires_at)
                values (?, ?, ?, ?, 10.8, 106.7, now() + interval '1 hour') returning id
                """, Long.class, publicId, userId, categoryId, UUID.randomUUID());
    }

    private Long createComment(Long reportId, Long userId, boolean hidden) {
        return jdbc.queryForObject("""
                insert into comments (report_id, user_id, client_request_id, body, is_hidden, created_at)
                values (?, ?, ?, 'Test comment', ?, transaction_timestamp()) returning id
                """, Long.class, reportId, userId, UUID.randomUUID(), hidden);
    }
}
