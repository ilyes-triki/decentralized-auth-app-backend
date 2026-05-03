package com.auth.backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * After a failed migration, Flyway blocks startup until the failure is cleared.
 * {@code flyway.repair()} removes failed rows and realigns checksums so a fixed migration can run again.
 * <p>
 * Disabled for Postgres profile ({@code app.flyway.repair-before-migrate=false}); enable locally if needed.
 */
@Configuration
public class FlywayRepairConfig {

    @Bean
    @ConditionalOnProperty(name = "app.flyway.repair-before-migrate", havingValue = "true")
    public FlywayMigrationStrategy flywayRepairThenMigrate() {
        return (Flyway flyway) -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
