package com.cityguard.backend.admin;

import com.cityguard.backend.entity.AdminLog;
import com.cityguard.backend.repository.AdminLogRepository;
import jakarta.persistence.EntityManagerFactory;
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

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "CITYGUARD_PHASE6_TEST_PG_PORT", matches = "[0-9]{1,5}")
@SpringBootTest(properties = {
        "cityguard.demo.seed-data=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "jwt.secret=CityGuardPhase6TestSecretKeyWithAtLeast32Bytes!"
})
@Transactional
class AdminLogRepositoryPostgresTest {

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://127.0.0.1:"
                + System.getenv("CITYGUARD_PHASE6_TEST_PG_PORT") + "/cityguard_phase6_test");
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
    }

    @Autowired private AdminLogRepository repository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @Test
    void filtersCountsOrdersAndFetchesActor() {
        assertThat(jdbc.queryForObject("select current_database()", String.class)).isEqualTo("cityguard_phase6_test");
        long existingMatches = jdbc.queryForObject("""
                select count(*) from admin_logs where action = 'HIDE_REPORT' and target_type = 'REPORT'
                """, Long.class);
        String hash = passwordEncoder.encode("phase6-test-only");
        Long actorId = createActor(hash);
        Long otherActorId = createActor(hash);
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-11-28T01:00:00Z");
        Long older = insertLog(actorId, "HIDE_REPORT", "REPORT", timestamp.minusMinutes(1));
        Long firstAtSameTime = insertLog(actorId, "HIDE_REPORT", "REPORT", timestamp);
        Long secondAtSameTime = insertLog(actorId, "HIDE_REPORT", "REPORT", timestamp);
        Long otherAction = insertLog(actorId, "RESTORE_REPORT", "REPORT", timestamp.plusMinutes(1));
        insertLog(actorId, "HIDE_COMMENT", "COMMENT", timestamp);
        insertLog(otherActorId, "HIDE_REPORT", "REPORT", timestamp);

        var firstPage = repository.findAdminLogs(actorId, "HIDE_REPORT", "REPORT", PageRequest.of(0, 1));
        assertThat(firstPage.getContent()).extracting(AdminLog::getId).containsExactly(secondAtSameTime);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        assertThat(entityManagerFactory.getPersistenceUnitUtil().isLoaded(firstPage.getContent().get(0), "actor"))
                .isTrue();
        assertThat(repository.findAdminLogs(actorId, "HIDE_REPORT", "REPORT", PageRequest.of(1, 2)).getContent())
                .extracting(AdminLog::getId).containsExactly(older);
        assertThat(repository.findAdminLogs(actorId, "HIDE_REPORT", "REPORT", PageRequest.of(0, 10)).getContent())
                .extracting(AdminLog::getId).containsExactly(secondAtSameTime, firstAtSameTime, older);
        assertThat(repository.findAdminLogs(actorId, null, null, PageRequest.of(0, 10)).getContent())
                .hasSize(5).first().extracting(AdminLog::getId).isEqualTo(otherAction);
        assertThat(repository.findAdminLogs(null, "HIDE_REPORT", "REPORT", PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(existingMatches + 4);
        assertThat(repository.findAdminLogs(actorId, "UNKNOWN_ACTION", null, PageRequest.of(0, 10)).getTotalElements())
                .isZero();
    }

    private Long createActor(String hash) {
        String username = "phase6_" + UUID.randomUUID().toString().replace("-", "");
        return jdbc.queryForObject("""
                insert into users (username, password_hash, display_name, role)
                values (?, ?, 'Phase 6 actor', 'ADMIN') returning id
                """, Long.class, username, hash);
    }

    private Long insertLog(Long actorId, String action, String targetType, OffsetDateTime createdAt) {
        return jdbc.queryForObject("""
                insert into admin_logs (actor_id, action, target_type, target_id, created_at)
                values (?, ?, ?, 42, ?) returning id
                """, Long.class, actorId, action, targetType, createdAt);
    }
}
