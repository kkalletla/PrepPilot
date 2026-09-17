package com.preppilot.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.preppilot.subscription.UsageCounter;
import com.preppilot.subscription.UsageCounterRepository;
import com.preppilot.user.User;
import com.preppilot.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Flyway must create every table from SPEC.md and Hibernate must validate the entities against them. */
@SpringBootTest
@Transactional
class SchemaMigrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired UsageCounterRepository counters;

    @Test
    void allCoreTablesExist() {
        List<String> tables = jdbc.queryForList(
                "SELECT LOWER(table_name) FROM information_schema.tables WHERE LOWER(table_schema) = 'public'",
                String.class);
        assertThat(tables).contains(
                "users", "subscriptions", "problems", "problem_progress",
                "design_questions", "design_sessions", "usage_counters");
    }

    @Test
    void usageCounterIsUniquePerUserPerDay() {
        User u = users.save(new User("schema@test.dev", "hash"));
        UsageCounter c = counters.save(new UsageCounter(u.getId(), LocalDate.of(2026, 9, 17)));
        c.incrementDsa();
        c.incrementDsa();
        counters.saveAndFlush(c);

        UsageCounter reloaded = counters.findByUserIdAndUsageDate(u.getId(), LocalDate.of(2026, 9, 17)).orElseThrow();
        assertThat(reloaded.getDsaSessionsUsed()).isEqualTo(2);
        assertThat(reloaded.getDesignSessionsUsed()).isZero();
    }
}
