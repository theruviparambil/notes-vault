package com.bluestaq.notesvault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    /**
     * Truncates audit timestamps to microsecond precision. PostgreSQL
     * TIMESTAMPTZ stores microseconds while Instant.now() returns nanoseconds,
     * so without truncation an entity's createdAt does not equal the value
     * reloaded from the database.
     */
    @Bean
    DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(Instant.now().truncatedTo(ChronoUnit.MICROS));
    }
}
