package com.cityguard.backend.admin;

import com.cityguard.backend.entity.User;
import com.cityguard.backend.security.JwtService;
import com.github.f4b6a3.ulid.UlidCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@EnabledIfEnvironmentVariable(named = "CITYGUARD_PHASE5_TEST_PG_PORT", matches = "[0-9]{1,5}")
@SpringBootTest(properties = {
        "cityguard.demo.seed-data=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "jwt.secret=CityGuardPhase5TestSecretKeyWithAtLeast32Bytes!"
})
class AdminCommentPostgresIntegrationTest {

    private static final String PATH = "/api/v1/admin/comments";
    private static final String STATUS_PATH = PATH + "/{id}/status";

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        AdminCommentRepositoryPostgresTest.isolatedDatabase(registry);
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://127.0.0.1:"
                + System.getenv("CITYGUARD_PHASE5_TEST_PG_PORT")
                + "/cityguard_phase5_test?ApplicationName=cityguard-phase5-it");
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtService jwt;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private WebApplicationContext context;
    @Autowired private DataSource dataSource;

    private MockMvc mvc;
    private User moderator;
    private User member;
    private String reportPublicId;
    private long commentId;
    private String passwordHash;

    @BeforeEach
    void prepare() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("cityguard_phase5_test");
        mvc = webAppContextSetup(context)
                .addFilters(new DelegatingFilterProxy("springSecurityFilterChain", context)).build();
        passwordHash = passwordEncoder.encode("phase5-test-only");
        moderator = createUser("MODERATOR");
        member = createUser("USER");
        reportPublicId = "rpt_" + UlidCreator.getUlid();
        jdbc.update("""
                insert into categories (code, display_name, icon, color, default_ttl_seconds)
                values ('PHASE5_API_TEST', 'Phase 5 API test', 'test', '#123456', 3600)
                on conflict (code) do nothing
                """);
        Long reportId = jdbc.queryForObject("""
                insert into reports (public_id, user_id, category_id, client_request_id, latitude, longitude, expires_at)
                select ?, ?, id, ?, 10.8, 106.7, now() + interval '1 hour'
                from categories where code = 'PHASE5_API_TEST' returning id
                """, Long.class, reportPublicId, member.getId(), UUID.randomUUID());
        commentId = jdbc.queryForObject("""
                insert into comments (report_id, user_id, client_request_id, body)
                values (?, ?, ?, 'Test comment') returning id
                """, Long.class, reportId, member.getId(), UUID.randomUUID());
    }

    @Test
    void supportsDefaultPagination() throws Exception {
        mvc.perform(get(PATH).header("Authorization", bearer(moderator)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    void returnsFilteredPageInReactShape() throws Exception {
        mvc.perform(get(PATH).header("Authorization", bearer(createUser("ADMIN")))
                        .param("reportPublicId", reportPublicId).param("isHidden", "false")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(commentId))
                .andExpect(jsonPath("$.data.items[0].reportPublicId").value(reportPublicId))
                .andExpect(jsonPath("$.data.items[0].isHidden").value(false))
                .andExpect(jsonPath("$.data.items[0].passwordHash").doesNotExist());
    }

    @Test
    void checksAuthenticationAndAuthorization() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(patch(STATUS_PATH, commentId)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("Authorization", bearer(member))).andExpect(status().isForbidden());
        change(member, commentId, true, "Forbidden").andExpect(status().isForbidden());
        assertThat(auditActions()).isEmpty();
    }

    @Test
    void validatesInputsAndReturnsNotFound() throws Exception {
        mvc.perform(get(PATH).header("Authorization", bearer(moderator))
                        .param("page", "abc").param("size", "10"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(STATUS_PATH, commentId).header("Authorization", bearer(moderator))
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(STATUS_PATH, commentId).header("Authorization", bearer(moderator))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Missing status\"}"))
                .andExpect(status().isBadRequest());
        change(moderator, commentId, true, " ").andExpect(status().isBadRequest());
        change(moderator, Long.MAX_VALUE, true, "Missing comment").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_NOT_FOUND"));
        assertThat(isHidden()).isFalse();
        assertThat(auditActions()).isEmpty();
    }

    @Test
    void hidesAndRestoresWithAuditWhilePreservingDeletedAt() throws Exception {
        jdbc.update("update comments set deleted_at = now() where id = ?", commentId);
        var before = jdbc.queryForObject("select deleted_at::text from comments where id = ?", String.class, commentId);

        change(moderator, commentId, true, "Hide").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isHidden").value(true));
        assertThat(isHidden()).isTrue();
        assertThat(jdbc.queryForMap("""
                select actor_id, target_type, target_id, reason from admin_logs
                where target_type = 'COMMENT' and target_id = ?
                """, commentId)).containsEntry("actor_id", moderator.getId())
                .containsEntry("target_id", commentId).containsEntry("target_type", "COMMENT")
                .containsEntry("reason", "Hide");
        change(moderator, commentId, true, "Repeat").andExpect(status().isOk());
        assertThat(auditActions()).containsExactly("HIDE_COMMENT");
        change(createUser("ADMIN"), commentId, false, "Restore").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isHidden").value(false));
        assertThat(isHidden()).isFalse();
        assertThat(auditActions()).containsExactly("HIDE_COMMENT", "RESTORE_COMMENT");
        assertThat(jdbc.queryForObject("select deleted_at::text from comments where id = ?", String.class, commentId))
                .isEqualTo(before);
    }

    @Test
    void rollsBackCommentAndAuditIfCommitFails() throws Exception {
        jdbc.execute("""
                create function phase5_reject_audit() returns trigger language plpgsql as $$
                begin
                    if new.reason = 'phase5-forced-audit-failure' then
                        if (select is_hidden from comments where id = new.target_id) is distinct from true then
                            raise exception 'Comment update was not flushed';
                        end if;
                        raise exception 'Phase 5 injected audit failure';
                    end if;
                    return new;
                end;
                $$
                """);
        jdbc.execute("""
                create constraint trigger phase5_reject_audit_insert after insert on admin_logs
                deferrable initially deferred for each row execute function phase5_reject_audit()
                """);
        try {
            var result = change(moderator, commentId, true, "phase5-forced-audit-failure")
                    .andExpect(status().isInternalServerError()).andReturn();
            assertThat(result.getResolvedException()).hasStackTraceContaining("Phase 5 injected audit failure");
            assertThat(isHidden()).isFalse();
            assertThat(auditActions()).isEmpty();
        } finally {
            jdbc.execute("drop trigger phase5_reject_audit_insert on admin_logs");
            jdbc.execute("drop function phase5_reject_audit()");
        }
    }

    @Test
    void concurrentModeratorsHidingOneCommentWriteOneAuditLog() throws Exception {
        User secondModerator = createUser("MODERATOR");
        var executor = Executors.newFixedThreadPool(2);
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var lock = connection.prepareStatement("select id from comments where id = ? for update")) {
                lock.setLong(1, commentId);
                lock.executeQuery().close();
            }
            var first = executor.submit(() -> change(moderator, commentId, true, "First").andReturn());
            var second = executor.submit(() -> change(secondModerator, commentId, true, "Second").andReturn());
            try {
                await().atMost(Duration.ofSeconds(10)).until(() -> jdbc.queryForObject("""
                        select count(*) from pg_stat_activity
                        where application_name = 'cityguard-phase5-it'
                          and wait_event_type = 'Lock' and cardinality(pg_blocking_pids(pid)) > 0
                        """, Integer.class) == 2);
            } finally {
                connection.rollback();
            }
            assertThat(first.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);
            assertThat(second.get(10, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(200);
            assertThat(isHidden()).isTrue();
            assertThat(auditActions()).containsExactly("HIDE_COMMENT");
        } finally {
            executor.shutdownNow();
        }
    }

    private User createUser(String role) {
        String username = "phase5_" + UUID.randomUUID().toString().replace("-", "");
        Long id = jdbc.queryForObject("""
                insert into users (username, password_hash, display_name, role)
                values (?, ?, 'Phase 5 API test user', ?) returning id
                """, Long.class, username, passwordHash, role);
        return User.builder().id(id).username(username).status("ACTIVE").role(role).build();
    }

    private String bearer(User actor) {
        return "Bearer " + jwt.generateAccessToken(actor);
    }

    private ResultActions change(User actor, long id, boolean hidden, String reason) throws Exception {
        return mvc.perform(patch(STATUS_PATH, id).header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"isHidden":%s,"reason":"%s"}
                        """.formatted(hidden, reason)));
    }

    private Boolean isHidden() {
        return jdbc.queryForObject("select is_hidden from comments where id = ?", Boolean.class, commentId);
    }

    private List<String> auditActions() {
        return jdbc.queryForList("""
                select action from admin_logs where target_type = 'COMMENT' and target_id = ? order by id
                """, String.class, commentId);
    }
}
