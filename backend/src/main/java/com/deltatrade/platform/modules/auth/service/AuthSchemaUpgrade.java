package com.deltatrade.platform.modules.auth.service;

import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AuthSchemaUpgrade {

    private static final Logger log = LoggerFactory.getLogger(AuthSchemaUpgrade.class);

    private final JdbcTemplate jdbcTemplate;

    public AuthSchemaUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void ensureAuthColumns() {
        ensurePhoneNullable();
        ensureFaceSessionTable();
        ensureColumn("avatar_key", "ALTER TABLE auth_user ADD COLUMN avatar_key VARCHAR(255) NULL");
        ensureColumn("real_name", "ALTER TABLE auth_user ADD COLUMN real_name VARCHAR(64) NULL");
        ensureColumn("real_name_phone", "ALTER TABLE auth_user ADD COLUMN real_name_phone VARCHAR(20) NULL");
        ensureColumn("id_card_no", "ALTER TABLE auth_user ADD COLUMN id_card_no VARCHAR(64) NULL");
        ensureColumn("real_name_status", "ALTER TABLE auth_user ADD COLUMN real_name_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED'");
        ensureColumn("real_name_reject_reason", "ALTER TABLE auth_user ADD COLUMN real_name_reject_reason VARCHAR(255) NULL");
        ensureColumn("real_name_front_key", "ALTER TABLE auth_user ADD COLUMN real_name_front_key VARCHAR(255) NULL");
        ensureColumn("real_name_back_key", "ALTER TABLE auth_user ADD COLUMN real_name_back_key VARCHAR(255) NULL");
        ensureColumn("login_alert_enabled", "ALTER TABLE auth_user ADD COLUMN login_alert_enabled BOOLEAN NOT NULL DEFAULT TRUE");
        ensureColumn("secondary_verify_enabled", "ALTER TABLE auth_user ADD COLUMN secondary_verify_enabled BOOLEAN NOT NULL DEFAULT FALSE");
    }

    private void ensureFaceSessionTable() {
        long startAt = System.currentTimeMillis();
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS real_name_face_session (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "user_id BIGINT NOT NULL," +
                "order_id VARCHAR(32) NOT NULL UNIQUE," +
                "jh_order_id VARCHAR(64) NULL UNIQUE," +
                "real_name VARCHAR(64) NOT NULL," +
                "id_card_no VARCHAR(64) NOT NULL," +
                "status VARCHAR(32) NOT NULL," +
                "fail_reason VARCHAR(255) NULL," +
                "provider VARCHAR(32) NOT NULL DEFAULT 'JUHE_FACE_H5'," +
                "raw_result TEXT NULL," +
                "created_at DATETIME NOT NULL," +
                "updated_at DATETIME NOT NULL," +
                "completed_at DATETIME NULL" +
                ")"
        );
        log.info("mysql ddl success target=real_name_face_session costMs={}", System.currentTimeMillis() - startAt);
    }

    private void ensurePhoneNullable() {
        long startAt = System.currentTimeMillis();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SHOW COLUMNS FROM auth_user LIKE 'phone'");
        if (rows.isEmpty()) {
            return;
        }
        Object nullable = rows.get(0).get("Null");
        if ("YES".equalsIgnoreCase(String.valueOf(nullable))) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE auth_user MODIFY COLUMN phone VARCHAR(20) NULL");
        log.info("mysql ddl success target=auth_user_modify_column costMs={} column=phone", System.currentTimeMillis() - startAt);
    }

    private void ensureColumn(String columnName, String ddl) {
        long startAt = System.currentTimeMillis();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SHOW COLUMNS FROM auth_user LIKE ?", columnName);
        if (!rows.isEmpty()) {
            return;
        }
        jdbcTemplate.execute(ddl);
        log.info("mysql ddl success target=auth_user_add_column costMs={} column={}", System.currentTimeMillis() - startAt, columnName);
    }
}
