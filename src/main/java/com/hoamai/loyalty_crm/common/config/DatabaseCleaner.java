package com.hoamai.loyalty_crm.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseCleaner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            int updatedRows = jdbcTemplate.update("UPDATE customers SET email = NULL WHERE email = '' OR TRIM(email) = ''");
            if (updatedRows > 0) {
                log.info("Cleaned up {} customer record(s) with empty string emails to NULL", updatedRows);
            }
        } catch (Exception e) {
            log.warn("Could not clean up empty emails in database: {}", e.getMessage());
        }
    }
}
