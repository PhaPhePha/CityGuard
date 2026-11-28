package com.cityguard.backend.admin;

import com.cityguard.backend.entity.User;
import com.cityguard.backend.security.JwtService;
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

@EnabledIfEnvironmentVariable(named = "CITYGUARD_PHASE4_TEST_PG_PORT", matches = "[0-9]{1,5}")
@SpringBootTest(properties = {
        "cityguard.demo.seed-data=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "jwt.secret=CityGuardPhase4TestSecretKeyWithAtLeast32Bytes!"
})
class AdminUserPostgresIntegrationTest {

    private static final String USERS_PATH = "/api/v1/admin/users";

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://127.0.0.1:"
                + System.getenv("CITYGUARD_PHASE4_TEST_PG_PORT")
                + "/cityguard_phase4_test?ApplicationName=cityguard-phase4-it");
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired private WebApplicationContext context;
    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtService jwt;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mvc;
    private User admin;
    private String passwordHash;

    @BeforeEach
    void prepare() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("cityguard_phase4_test");
        mvc = webAppContextSetup(context)
                .addFilters(new DelegatingFilterProxy("springSecurityFilterChain", context)).build();
        passwordHash = passwordEncoder.encode("phase4-test-only");
        admin = createUser("ADMIN", "Administrator");
    }

    @Test
    void listsFilteredUsersAndReturnsSafeDetail() throws Exception {
        String marker = UUID.randomUUID().toString();
        createUser("USER", marker + " priority");
        User target = createUser("USER", marker + " priority");
        createUser("MODERATOR", marker + " priority");
        User locked = createUser("USER", marker + " priority");
        jdbc.update("update users set status = 'LOCKED' where id = ?", locked.getId());

        mvc.perform(get(USERS_PATH).header("Authorization", bearer(admin))
                        .param("search", marker.toUpperCase(java.util.Locale.ROOT))
                        .param("role", "USER").param("status", "ACTIVE").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(target.getId()))
                .andExpect(jsonPath("$.data.items[0].passwordHash").doesNotExist());
        mvc.perform(get(USERS_PATH).header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mvc.perform(get(USERS_PATH + "/{id}", target.getId()).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(target.getId()))
                .andExpect(jsonPath("$.data.totalReports").value(0))
                .andExpect(jsonPath("$.data.recentReports").isEmpty())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        mvc.perform(get(USERS_PATH + "/{id}", Long.MAX_VALUE).header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    @Test
    void restrictsAllRoutesToAdmin() throws Exception {
        User target = createUser("USER", "Target");
        for (User actor : List.of(target, createUser("MODERATOR", "Moderator"))) {
            mvc.perform(get(USERS_PATH).header("Authorization", bearer(actor))).andExpect(status().isForbidden());
            mvc.perform(get(USERS_PATH + "/{id}", target.getId()).header("Authorization", bearer(actor)))
                    .andExpect(status().isForbidden());
            change(actor, target, "LOCKED", "Unauthorized").andExpect(status().isForbidden());
        }
        mvc.perform(get(USERS_PATH)).andExpect(status().isUnauthorized());
        assertThat(auditActions(target)).isEmpty();
    }

    @Test
    void locksAndUnlocksWithAuditAndRejectsNoOpAndSelfLock() throws Exception {
        User target = createUser("USER", "Target");
        change(admin, target, "LOCKED", " ").andExpect(status().isBadRequest());
        change(admin, admin, "LOCKED", "Self lock").andExpect(status().isBadRequest());
        assertThat(auditActions(admin)).isEmpty();

        change(admin, target, "LOCKED", "Lock").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));
        assertThat(userStatus(target)).isEqualTo("LOCKED");
        change(admin, target, "LOCKED", "Duplicate").andExpect(status().isBadRequest());
        assertThat(auditActions(target)).containsExactly("LOCK_USER");
        change(admin, target, "ACTIVE", "Unlock").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        assertThat(userStatus(target)).isEqualTo("ACTIVE");
        assertThat(auditActions(target)).containsExactly("LOCK_USER", "UNLOCK_USER");
    }

    @Test
    void rollsBackStatusWhenAuditFailsAtCommit() throws Exception {
        User target = createUser("USER", "Target");
        jdbc.execute("""
                create function phase4_reject_audit() returns trigger language plpgsql as $$
                begin
                    if new.reason = 'phase4-forced-failure' then
                        if (select status from users where id = new.target_id) <> 'LOCKED' then
                            raise exception 'User update was not flushed before audit failure';
                        end if;
                        raise exception 'Phase 4 injected audit failure';
                    end if;
                    return new;
                end;
                $$
                """);
        jdbc.execute("""
                create constraint trigger phase4_reject_audit_insert after insert on admin_logs
                deferrable initially deferred for each row execute function phase4_reject_audit()
                """);
        try {
            var result = change(admin, target, "LOCKED", "phase4-forced-failure")
                    .andExpect(status().isInternalServerError()).andReturn();
            assertThat(result.getResolvedException()).hasStackTraceContaining("Phase 4 injected audit failure");
            assertThat(userStatus(target)).isEqualTo("ACTIVE");
            assertThat(auditActions(target)).isEmpty();
        } finally {
            jdbc.execute("drop trigger phase4_reject_audit_insert on admin_logs");
            jdbc.execute("drop function phase4_reject_audit()");
        }
    }

    @Test
    void concurrentLocksProduceOneTransitionAndOneAuditLog() throws Exception {
        User target = createUser("USER", "Target");
        User otherAdmin = createUser("ADMIN", "Other administrator");
        var executor = Executors.newFixedThreadPool(2);
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var lock = connection.prepareStatement("select id from users where id = ? for update")) {
                lock.setLong(1, target.getId());
                lock.executeQuery().close();
            }
            var first = executor.submit(() -> change(admin, target, "LOCKED", "First").andReturn());
            var second = executor.submit(() -> change(otherAdmin, target, "LOCKED", "Second").andReturn());
            try {
                await().atMost(java.time.Duration.ofSeconds(10)).until(() -> jdbc.queryForObject("""
                        select count(*) from pg_stat_activity
                        where application_name = 'cityguard-phase4-it'
                          and wait_event_type = 'Lock' and cardinality(pg_blocking_pids(pid)) > 0
                        """, Integer.class) == 2);
            } finally {
                connection.rollback();
            }
            assertThat(List.of(first.get(10, TimeUnit.SECONDS).getResponse().getStatus(),
                    second.get(10, TimeUnit.SECONDS).getResponse().getStatus())).containsExactlyInAnyOrder(200, 400);
            assertThat(userStatus(target)).isEqualTo("LOCKED");
            assertThat(auditActions(target)).containsExactly("LOCK_USER");
        } finally {
            executor.shutdownNow();
        }
    }

    private User createUser(String role, String displayName) {
        String username = "phase4_" + UUID.randomUUID().toString().replace("-", "");
        Long id = jdbc.queryForObject("""
                insert into users (username, password_hash, display_name, role)
                values (?, ?, ?, ?) returning id
                """, Long.class, username, passwordHash, displayName, role);
        return User.builder().id(id).username(username).status("ACTIVE").role(role).build();
    }

    private String bearer(User actor) {
        return "Bearer " + jwt.generateAccessToken(actor);
    }

    private ResultActions change(User actor, User target, String state, String reason) throws Exception {
        return mvc.perform(patch(USERS_PATH + "/{id}/status", target.getId())
                .header("Authorization", bearer(actor)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"%s","reason":"%s"}
                        """.formatted(state, reason)));
    }

    private String userStatus(User user) {
        return jdbc.queryForObject("select status from users where id = ?", String.class, user.getId());
    }

    private List<String> auditActions(User user) {
        return jdbc.queryForList("""
                select action from admin_logs where target_type = 'USER' and target_id = ? order by id
                """, String.class, user.getId());
    }
}
