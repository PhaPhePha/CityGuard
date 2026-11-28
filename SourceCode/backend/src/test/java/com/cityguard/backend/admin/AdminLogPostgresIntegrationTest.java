package com.cityguard.backend.admin;

import com.cityguard.backend.entity.User;
import com.cityguard.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@EnabledIfEnvironmentVariable(named = "CITYGUARD_PHASE6_TEST_PG_PORT", matches = "[0-9]{1,5}")
@SpringBootTest(properties = {
        "cityguard.demo.seed-data=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "jwt.secret=CityGuardPhase6TestSecretKeyWithAtLeast32Bytes!"
})
class AdminLogPostgresIntegrationTest {

    private static final String PATH = "/api/v1/admin/logs";

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        AdminLogRepositoryPostgresTest.isolatedDatabase(registry);
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtService jwt;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private WebApplicationContext context;

    private MockMvc mvc;
    private User admin;
    private String hash;

    @BeforeEach
    void prepare() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("cityguard_phase6_test");
        mvc = webAppContextSetup(context)
                .addFilters(new DelegatingFilterProxy("springSecurityFilterChain", context)).build();
        hash = passwordEncoder.encode("phase6-api-test-only");
        admin = createUser("ADMIN");
    }

    @Test
    void requiresAdminAndUsesDefaultPagination() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("Authorization", bearer(createUser("USER"))))
                .andExpect(status().isForbidden());
        mvc.perform(get(PATH).header("Authorization", bearer(createUser("MODERATOR"))))
                .andExpect(status().isForbidden());
        mvc.perform(get(PATH).header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    void filtersPagesAndReturnsReactFieldsWithoutWritingAuditLogs() throws Exception {
        User actor = createUser("MODERATOR");
        OffsetDateTime time = OffsetDateTime.parse("2026-11-28T14:00:00+07:00");
        insertLog(actor, "HIDE_REPORT", "REPORT", time.minusMinutes(1));
        Long firstInTie = insertLog(actor, "HIDE_REPORT", "REPORT", time);
        Long newest = insertLog(actor, "HIDE_REPORT", "REPORT", time);
        insertLog(actor, "RESTORE_REPORT", "REPORT", time);
        insertLog(actor, "HIDE_COMMENT", "COMMENT", time);
        insertLog(admin, "HIDE_REPORT", "REPORT", time);
        Long countBefore = jdbc.queryForObject("select count(*) from admin_logs", Long.class);

        mvc.perform(get(PATH).header("Authorization", bearer(admin))
                        .param("actorId", actor.getId().toString()).param("action", "HIDE_REPORT")
                        .param("targetType", "REPORT").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalItems").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.isLast").value(false))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(newest))
                .andExpect(jsonPath("$.data.items[0].actorId").value(actor.getId()))
                .andExpect(jsonPath("$.data.items[0].actorUsername").value(actor.getUsername()))
                .andExpect(jsonPath("$.data.items[0].actorDisplayName").value("Phase 6 test actor"))
                .andExpect(jsonPath("$.data.items[0].action").value("HIDE_REPORT"))
                .andExpect(jsonPath("$.data.items[0].targetType").value("REPORT"))
                .andExpect(jsonPath("$.data.items[0].targetId").value(42))
                .andExpect(jsonPath("$.data.items[0].reason").value("Moderation test"))
                .andExpect(jsonPath("$.data.items[0].createdAt").value("2026-11-28T07:00:00Z"))
                .andExpect(jsonPath("$.data.items[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].passwordHash").doesNotExist());
        mvc.perform(get(PATH).header("Authorization", bearer(admin))
                        .param("actorId", actor.getId().toString()).param("action", "HIDE_REPORT")
                        .param("targetType", "REPORT").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(firstInTie));

        assertThat(jdbc.queryForObject("select count(*) from admin_logs", Long.class)).isEqualTo(countBefore);
        assertThat(jdbc.queryForObject("select action from admin_logs where id = ?", String.class, newest))
                .isEqualTo("HIDE_REPORT");
    }

    @Test
    void returnsEmptyPageAndRejectsInvalidPaginationOrActorId() throws Exception {
        mvc.perform(get(PATH).header("Authorization", bearer(admin))
                        .param("actorId", Long.toString(Long.MAX_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalItems").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.isLast").value(true));
        mvc.perform(get(PATH).header("Authorization", bearer(admin)).param("actorId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        mvc.perform(get(PATH).header("Authorization", bearer(admin)).param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    private User createUser(String role) {
        String username = "phase6_" + UUID.randomUUID().toString().replace("-", "");
        Long id = jdbc.queryForObject("""
                insert into users (username, password_hash, display_name, role)
                values (?, ?, 'Phase 6 test actor', ?) returning id
                """, Long.class, username, hash, role);
        return User.builder().id(id).username(username).status("ACTIVE").role(role).build();
    }

    private String bearer(User user) {
        return "Bearer " + jwt.generateAccessToken(user);
    }

    private Long insertLog(User actor, String action, String targetType, OffsetDateTime time) {
        return jdbc.queryForObject("""
                insert into admin_logs (actor_id, action, target_type, target_id, reason, created_at)
                values (?, ?, ?, 42, 'Moderation test', ?) returning id
                """, Long.class, actor.getId(), action, targetType, time);
    }
}
