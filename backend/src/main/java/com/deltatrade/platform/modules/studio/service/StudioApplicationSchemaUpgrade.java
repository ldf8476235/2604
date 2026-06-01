package com.deltatrade.platform.modules.studio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StudioApplicationSchemaUpgrade implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StudioApplicationSchemaUpgrade.class);
    private final JdbcTemplate jdbcTemplate;

    public StudioApplicationSchemaUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureTable("studio_application", "CREATE TABLE IF NOT EXISTS studio_application (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "application_no VARCHAR(32) NOT NULL UNIQUE," +
            "applicant_user_id BIGINT NOT NULL," +
            "studio_id BIGINT NULL," +
            "studio_name VARCHAR(100) NOT NULL," +
            "qualification_code VARCHAR(64) NOT NULL," +
            "qualification_note VARCHAR(255) NULL," +
            "contact_name VARCHAR(64) NOT NULL," +
            "contact_phone VARCHAR(32) NOT NULL," +
            "qualification_material_key VARCHAR(255) NOT NULL," +
            "status VARCHAR(32) NOT NULL," +
            "reject_reason VARCHAR(255) NULL," +
            "reviewed_by_user_id BIGINT NULL," +
            "reviewed_at TIMESTAMP NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
    }

    private void ensureTable(String tableName, String sql) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.TABLES WHERE UPPER(TABLE_NAME) = UPPER(?)",
            Long.class,
            tableName
        );
        if (count != null && count > 0) {
            return;
        }
        long startAt = System.currentTimeMillis();
        jdbcTemplate.execute(sql);
        log.info("schema upgrade success table={} costMs={}", tableName, System.currentTimeMillis() - startAt);
    }
}
