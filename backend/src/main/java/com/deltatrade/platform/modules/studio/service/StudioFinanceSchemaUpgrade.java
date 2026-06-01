package com.deltatrade.platform.modules.studio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StudioFinanceSchemaUpgrade implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StudioFinanceSchemaUpgrade.class);
    private final JdbcTemplate jdbcTemplate;

    public StudioFinanceSchemaUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureTable("studio_withdraw_application", "CREATE TABLE IF NOT EXISTS studio_withdraw_application (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "application_no VARCHAR(32) NOT NULL UNIQUE," +
            "studio_id BIGINT NOT NULL," +
            "owner_user_id BIGINT NOT NULL," +
            "amount DECIMAL(12,2) NOT NULL," +
            "channel VARCHAR(32) NOT NULL," +
            "account_name VARCHAR(64) NOT NULL," +
            "account_no VARCHAR(128) NOT NULL," +
            "status VARCHAR(32) NOT NULL," +
            "reject_reason VARCHAR(255)," +
            "reviewed_at TIMESTAMP NULL," +
            "paid_at TIMESTAMP NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("studio_operator", "CREATE TABLE IF NOT EXISTS studio_operator (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "operator_no VARCHAR(32) NOT NULL UNIQUE," +
            "studio_id BIGINT NOT NULL," +
            "owner_user_id BIGINT NOT NULL," +
            "name VARCHAR(64) NOT NULL," +
            "phone VARCHAR(20) NOT NULL," +
            "password_hash VARCHAR(128) NOT NULL," +
            "permissions_json TEXT NOT NULL," +
            "status VARCHAR(32) NOT NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureColumn("studio_profile", "description", "ALTER TABLE studio_profile ADD COLUMN description VARCHAR(500) NULL");
        ensureColumn("studio_profile", "contact_phone", "ALTER TABLE studio_profile ADD COLUMN contact_phone VARCHAR(32) NULL");
        ensureColumn("studio_profile", "contact_name", "ALTER TABLE studio_profile ADD COLUMN contact_name VARCHAR(64) NULL");
        ensureColumn("studio_profile", "contact_wechat", "ALTER TABLE studio_profile ADD COLUMN contact_wechat VARCHAR(64) NULL");
        ensureColumn("studio_profile", "qualification_code", "ALTER TABLE studio_profile ADD COLUMN qualification_code VARCHAR(64) NULL");
        ensureColumn("studio_profile", "qualification_material_key", "ALTER TABLE studio_profile ADD COLUMN qualification_material_key VARCHAR(255) NULL");
        ensureColumn("studio_profile", "qualification_note", "ALTER TABLE studio_profile ADD COLUMN qualification_note VARCHAR(255) NULL");
        ensureColumn("studio_profile", "share_ratio", "ALTER TABLE studio_profile ADD COLUMN share_ratio DECIMAL(5,4) NOT NULL DEFAULT 0.7000");
        ensureColumn("studio_profile", "cooperation_status", "ALTER TABLE studio_profile ADD COLUMN cooperation_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'");
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

    private void ensureColumn(String tableName, String columnName, String sql) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)",
            Long.class,
            tableName,
            columnName
        );
        if (count != null && count > 0) {
            return;
        }
        long startAt = System.currentTimeMillis();
        jdbcTemplate.execute(sql);
        log.info("schema upgrade success table={} column={} costMs={}", tableName, columnName, System.currentTimeMillis() - startAt);
    }
}
