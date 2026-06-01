package com.deltatrade.platform.modules.profile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProfileSchemaUpgrade implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(ProfileSchemaUpgrade.class);

    private final JdbcTemplate jdbcTemplate;

    public ProfileSchemaUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        ensureTable("user_wallet", "CREATE TABLE IF NOT EXISTS user_wallet (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "user_id BIGINT NOT NULL UNIQUE," +
            "available_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00," +
            "frozen_balance DECIMAL(12,2) NOT NULL DEFAULT 0.00," +
            "total_commission DECIMAL(12,2) NOT NULL DEFAULT 0.00," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("wallet_transaction", "CREATE TABLE IF NOT EXISTS wallet_transaction (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "transaction_no VARCHAR(32) NOT NULL UNIQUE," +
            "user_id BIGINT NOT NULL," +
            "biz_type VARCHAR(32) NOT NULL," +
            "title VARCHAR(100) NOT NULL," +
            "amount DECIMAL(12,2) NOT NULL," +
            "direction VARCHAR(16) NOT NULL," +
            "channel VARCHAR(32)," +
            "status VARCHAR(32) NOT NULL," +
            "related_no VARCHAR(32)," +
            "remark VARCHAR(255)," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("wallet_recharge_order", "CREATE TABLE IF NOT EXISTS wallet_recharge_order (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "recharge_no VARCHAR(32) NOT NULL UNIQUE," +
            "user_id BIGINT NOT NULL," +
            "amount DECIMAL(12,2) NOT NULL," +
            "status VARCHAR(32) NOT NULL," +
            "payment_method VARCHAR(32) NOT NULL," +
            "payment_trade_type VARCHAR(32)," +
            "payment_prepay_id VARCHAR(128)," +
            "payment_code_url VARCHAR(1024)," +
            "payment_transaction_id VARCHAR(64)," +
            "payment_expire_at TIMESTAMP NULL," +
            "payment_notified_at TIMESTAMP NULL," +
            "paid_at TIMESTAMP NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("withdraw_account", "CREATE TABLE IF NOT EXISTS withdraw_account (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "user_id BIGINT NOT NULL UNIQUE," +
            "channel VARCHAR(32) NOT NULL," +
            "account_name VARCHAR(64) NOT NULL," +
            "account_no VARCHAR(128) NOT NULL," +
            "qr_code_key VARCHAR(255)," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("withdraw_application", "CREATE TABLE IF NOT EXISTS withdraw_application (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "application_no VARCHAR(32) NOT NULL UNIQUE," +
            "user_id BIGINT NOT NULL," +
            "amount DECIMAL(12,2) NOT NULL," +
            "channel VARCHAR(32) NOT NULL," +
            "account_name VARCHAR(64) NOT NULL," +
            "account_no VARCHAR(128) NOT NULL," +
            "qr_code_key VARCHAR(255)," +
            "status VARCHAR(32) NOT NULL," +
            "reject_reason VARCHAR(255)," +
            "reviewed_at TIMESTAMP NULL," +
            "paid_at TIMESTAMP NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("user_message", "CREATE TABLE IF NOT EXISTS user_message (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "user_id BIGINT NOT NULL," +
            "category VARCHAR(32) NOT NULL," +
            "title VARCHAR(120) NOT NULL," +
            "content VARCHAR(500) NOT NULL," +
            "read_flag BOOLEAN NOT NULL DEFAULT FALSE," +
            "deleted_flag BOOLEAN NOT NULL DEFAULT FALSE," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("user_coupon", "CREATE TABLE IF NOT EXISTS user_coupon (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "coupon_no VARCHAR(32) NOT NULL UNIQUE," +
            "user_id BIGINT NOT NULL," +
            "name VARCHAR(120) NOT NULL," +
            "amount DECIMAL(12,2) NOT NULL," +
            "condition_text VARCHAR(255) NOT NULL," +
            "scope_text VARCHAR(255) NOT NULL," +
            "status VARCHAR(32) NOT NULL," +
            "related_order_no VARCHAR(32)," +
            "expire_at TIMESTAMP NOT NULL," +
            "used_at TIMESTAMP NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("distribution_order", "CREATE TABLE IF NOT EXISTS distribution_order (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "distribution_no VARCHAR(32) NOT NULL UNIQUE," +
            "promoter_user_id BIGINT NOT NULL," +
            "referred_user_id BIGINT NULL," +
            "buyer_nickname VARCHAR(64) NOT NULL," +
            "source_order_no VARCHAR(32)," +
            "source_order_type VARCHAR(32)," +
            "source_order_status VARCHAR(32)," +
            "order_amount DECIMAL(12,2) NOT NULL," +
            "commission_rate DECIMAL(10,4) NOT NULL DEFAULT 0.0000," +
            "commission_amount DECIMAL(12,2) NOT NULL," +
            "status VARCHAR(32) NOT NULL," +
            "settled_at TIMESTAMP NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("distribution_invite_link", "CREATE TABLE IF NOT EXISTS distribution_invite_link (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "promoter_user_id BIGINT NOT NULL," +
            "invite_code VARCHAR(120) NOT NULL UNIQUE," +
            "invite_path VARCHAR(255) NOT NULL," +
            "poster_key VARCHAR(255)," +
            "active BOOLEAN NOT NULL DEFAULT TRUE," +
            "invalidated_at TIMESTAMP NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("distribution_referral", "CREATE TABLE IF NOT EXISTS distribution_referral (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "promoter_user_id BIGINT NOT NULL," +
            "referred_user_id BIGINT NOT NULL UNIQUE," +
            "invite_code VARCHAR(120) NOT NULL," +
            "status VARCHAR(32) NOT NULL," +
            "source_channel VARCHAR(32)," +
            "first_paid_order_no VARCHAR(32)," +
            "first_paid_order_type VARCHAR(32)," +
            "registered_at TIMESTAMP NOT NULL," +
            "effective_at TIMESTAMP NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureColumn("auth_user", "distribution_enabled",
            "ALTER TABLE auth_user ADD COLUMN distribution_enabled BOOLEAN NOT NULL DEFAULT FALSE");
        ensureColumn("auth_user", "distribution_opened_at",
            "ALTER TABLE auth_user ADD COLUMN distribution_opened_at TIMESTAMP NULL");
        ensureColumn("account_listing", "distribution_commission_rate",
            "ALTER TABLE account_listing ADD COLUMN distribution_commission_rate DECIMAL(10,4) NULL");
        ensureColumn("boosting_service", "distribution_commission_rate",
            "ALTER TABLE boosting_service ADD COLUMN distribution_commission_rate DECIMAL(10,4) NULL");
        ensureColumn("studio_profile", "distribution_commission_rate",
            "ALTER TABLE studio_profile ADD COLUMN distribution_commission_rate DECIMAL(10,4) NULL");
        ensureColumn("distribution_order", "source_order_type",
            "ALTER TABLE distribution_order ADD COLUMN source_order_type VARCHAR(32) NULL");
        ensureColumn("distribution_order", "source_order_status",
            "ALTER TABLE distribution_order ADD COLUMN source_order_status VARCHAR(32) NULL");
        ensureColumn("distribution_order", "commission_rate",
            "ALTER TABLE distribution_order ADD COLUMN commission_rate DECIMAL(10,4) NOT NULL DEFAULT 0.0000");
        ensureColumn("distribution_order", "settled_at",
            "ALTER TABLE distribution_order ADD COLUMN settled_at TIMESTAMP NULL");
        ensureColumn("withdraw_account", "qr_code_key",
            "ALTER TABLE withdraw_account ADD COLUMN qr_code_key VARCHAR(255) NULL");
        ensureColumn("withdraw_application", "qr_code_key",
            "ALTER TABLE withdraw_application ADD COLUMN qr_code_key VARCHAR(255) NULL");
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
