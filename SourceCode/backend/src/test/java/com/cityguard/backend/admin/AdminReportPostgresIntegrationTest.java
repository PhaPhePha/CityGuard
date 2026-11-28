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
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@EnabledIfEnvironmentVariable(named = "CITYGUARD_PHASE3_TEST_PG_PORT", matches = "[0-9]{1,5}")
@SpringBootTest(properties = {
        "cityguard.demo.seed-data=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "jwt.secret=CityGuardPhase3TestSecretKeyWithAtLeast32Bytes!"
})
class AdminReportPostgresIntegrationTest {

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> "jdbc:postgresql://127.0.0.1:"
                + System.getenv("CITYGUARD_PHASE3_TEST_PG_PORT")
                + "/cityguard_phase3_test?ApplicationName=cityguard-phase3-it");
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "");
    }

    @Autowired private WebApplicationContext context;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtService jwt;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mvc;
    private long actorId;
    private String authorization;

    @BeforeEach
    void prepare() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("cityguard_phase3_test");
        mvc = webAppContextSetup(context)
                .addFilters(new DelegatingFilterProxy("springSecurityFilterChain", context)).build();
        String username = "phase3_" + UUID.randomUUID().toString().replace("-", "");
        actorId = jdbc.queryForObject("""
                insert into users (username, password_hash, display_name, role)
                values (?, ?, 'Phase 3 Moderator', 'MODERATOR') returning id
                """, Long.class, username, passwordEncoder.encode("phase3-test-password"));
        authorization = "Bearer " + jwt.generateAccessToken(User.builder()
                .id(actorId).username(username).role("MODERATOR").status("ACTIVE").build());
    }

    @Test
    void commitsReportAndAuditLogAndRejectsRepeat() throws Exception {
        String publicId = createReport();

        hide(publicId, "Hide report").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));
        assertThat(reportStatus(publicId)).isEqualTo("HIDDEN");
        assertThat(jdbc.queryForMap("""
                select actor_id, action, target_type, reason from admin_logs
                where target_id = (select id from reports where public_id = ?)
                """, publicId)).containsEntry("actor_id", actorId)
                .containsEntry("action", "HIDE_REPORT").containsEntry("target_type", "REPORT")
                .containsEntry("reason", "Hide report");

        hide(publicId, "Duplicate request").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        assertThat(reportStatus(publicId)).isEqualTo("HIDDEN");
        assertThat(auditCount(publicId)).isEqualTo(1);
    }

    @Test
    void rollsBackReportAndAuditLogWhenAuditFailsAtCommit() throws Exception {
        String publicId = createReport();
        jdbc.execute("""
                create or replace function phase3_reject_audit() returns trigger language plpgsql as $$
                begin
                    if new.reason = 'phase3-forced-audit-failure' then
                        if (select status from reports where id = new.target_id) <> 'HIDDEN' then
                            raise exception 'Report update was not flushed before audit failure';
                        end if;
                        raise exception 'Phase 3 injected audit failure';
                    end if;
                    return new;
                end;
                $$
                """);
        jdbc.execute("""
                create constraint trigger phase3_reject_audit_insert after insert on admin_logs
                deferrable initially deferred for each row execute function phase3_reject_audit()
                """);
        try {
            var response = hide(publicId, "phase3-forced-audit-failure")
                    .andExpect(status().isInternalServerError()).andReturn();
            assertThat(response.getResolvedException()).hasStackTraceContaining("Phase 3 injected audit failure");
            assertThat(reportStatus(publicId)).isEqualTo("ACTIVE");
            assertThat(auditCount(publicId)).isZero();
        } finally {
            jdbc.execute("drop trigger phase3_reject_audit_insert on admin_logs");
            jdbc.execute("drop function phase3_reject_audit()");
        }
    }

    @Test
    void concurrentHideRequestsYieldOneSuccessfulTransitionAndOneAuditLog() throws Exception {
        String publicId = createReport();
        var executor = Executors.newFixedThreadPool(2);
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var lock = connection.prepareStatement("select id from reports where public_id = ? for update")) {
                lock.setString(1, publicId);
                lock.executeQuery().close();
            }

            var first = executor.submit(() -> hide(publicId, "First moderator request").andReturn());
            var second = executor.submit(() -> hide(publicId, "Second moderator request").andReturn());
            try {
                awaitBlockedRequests();
            } finally {
                connection.rollback();
            }

            var responses = List.of(first.get(10, TimeUnit.SECONDS).getResponse().getStatus(),
                    second.get(10, TimeUnit.SECONDS).getResponse().getStatus());
            var auditCount = auditCount(publicId);
            assertThat(reportStatus(publicId)).isEqualTo("HIDDEN");
            assertThat(responses).as("HTTP statuses for concurrent hides, with %s audit log(s)", auditCount)
                    .containsExactlyInAnyOrder(200, 400);
            assertThat(auditCount).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private void awaitBlockedRequests() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            Integer blocked = jdbc.queryForObject("""
                    select count(*) from pg_stat_activity
                    where application_name = 'cityguard-phase3-it'
                      and wait_event_type = 'Lock' and cardinality(pg_blocking_pids(pid)) > 0
                    """, Integer.class);
            if (blocked == 2) {
                return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("Both PATCH requests must be waiting on PostgreSQL locks");
    }

    private String createReport() {
        jdbc.update("""
                insert into categories (code, display_name, icon, color, default_ttl_seconds)
                values ('PHASE3_TEST', 'Phase 3 test', 'test', '#123456', 3600) on conflict (code) do nothing
                """);
        String publicId = "rpt_" + UlidCreator.getUlid();
        jdbc.update("""
                insert into reports (public_id, user_id, category_id, client_request_id, latitude, longitude, status, expires_at)
                select ?, ?, id, ?, 10.8, 106.7, 'ACTIVE', now() + interval '1 hour'
                from categories where code = 'PHASE3_TEST'
                """, publicId, actorId, UUID.randomUUID());
        return publicId;
    }

    private org.springframework.test.web.servlet.ResultActions hide(String publicId, String reason) throws Exception {
        return mvc.perform(patch("/api/v1/admin/reports/{publicId}/status", publicId)
                .header("Authorization", authorization).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"HIDDEN\",\"reason\":\"" + reason + "\"}"));
    }

    private String reportStatus(String publicId) {
        return jdbc.queryForObject("select status from reports where public_id = ?", String.class, publicId);
    }

    private int auditCount(String publicId) {
        return jdbc.queryForObject("""
                select count(*) from admin_logs where target_type = 'REPORT'
                and target_id = (select id from reports where public_id = ?)
                """, Integer.class, publicId);
    }
}
