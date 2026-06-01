package com.deltatrade.platform.modules.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderSchemaUpgrade implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderSchemaUpgrade.class);

    private final JdbcTemplate jdbcTemplate;

    public OrderSchemaUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        ensureTable("trade_order", "CREATE TABLE IF NOT EXISTS trade_order (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "order_no VARCHAR(32) NOT NULL UNIQUE," +
            "listing_no VARCHAR(32)," +
            "listing_title VARCHAR(512) NOT NULL," +
            "listing_summary VARCHAR(255)," +
            "listing_cover_key VARCHAR(255)," +
            "buyer_user_id BIGINT NOT NULL," +
            "buyer_nickname VARCHAR(64) NOT NULL," +
            "seller_user_id BIGINT NOT NULL," +
            "seller_nickname VARCHAR(64) NOT NULL," +
            "seller_type VARCHAR(16) NOT NULL," +
            "seller_display_name VARCHAR(100) NOT NULL," +
            "status VARCHAR(32) NOT NULL," +
            "payment_method VARCHAR(32)," +
            "payment_trade_type VARCHAR(32)," +
            "payment_prepay_id VARCHAR(128)," +
            "payment_code_url VARCHAR(1024)," +
            "payment_transaction_id VARCHAR(64)," +
            "payment_expire_at TIMESTAMP NULL," +
            "payment_notified_at TIMESTAMP NULL," +
            "item_amount DECIMAL(10,2) NOT NULL," +
            "service_fee DECIMAL(10,2) NOT NULL," +
            "total_amount DECIMAL(10,2) NOT NULL," +
            "deposit_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00," +
            "buyer_confirmed_at TIMESTAMP NULL," +
            "seller_confirmed_at TIMESTAMP NULL," +
            "extra_items_included TINYINT(1) NOT NULL DEFAULT 0," +
            "extra_items_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00," +
            "extra_items_snapshot_json LONGTEXT NULL," +
            "chat_group_no VARCHAR(32)," +
            "created_at TIMESTAMP NOT NULL," +
            "paid_at TIMESTAMP NULL," +
            "trade_started_at TIMESTAMP NULL," +
            "completed_at TIMESTAMP NULL," +
            "closed_at TIMESTAMP NULL," +
            "after_sale_at TIMESTAMP NULL," +
            "after_sale_note VARCHAR(500)," +
            "after_sale_proof_key VARCHAR(255)," +
            "after_sale_handled_at TIMESTAMP NULL," +
            "refund_requested_at TIMESTAMP NULL," +
            "refund_reviewed_at TIMESTAMP NULL," +
            "refunded_at TIMESTAMP NULL," +
            "refund_amount DECIMAL(10,2) NULL," +
            "refund_reason VARCHAR(500)," +
            "refund_review_note VARCHAR(500)," +
            "refund_operator_user_id BIGINT NULL," +
            "refund_operator_role VARCHAR(32)," +
            "buyer_deleted_at TIMESTAMP NULL," +
            "seller_deleted_at TIMESTAMP NULL," +
            "updated_at TIMESTAMP NOT NULL" +
            ")");
        ensureColumn("trade_order", "after_sale_note", "ALTER TABLE trade_order ADD COLUMN after_sale_note VARCHAR(500) NULL");
        ensureColumn("trade_order", "after_sale_proof_key", "ALTER TABLE trade_order ADD COLUMN after_sale_proof_key VARCHAR(255) NULL");
        ensureColumn("trade_order", "after_sale_handled_at", "ALTER TABLE trade_order ADD COLUMN after_sale_handled_at TIMESTAMP NULL");
        ensureColumn("trade_order", "refund_requested_at", "ALTER TABLE trade_order ADD COLUMN refund_requested_at TIMESTAMP NULL");
        ensureColumn("trade_order", "refund_reviewed_at", "ALTER TABLE trade_order ADD COLUMN refund_reviewed_at TIMESTAMP NULL");
        ensureColumn("trade_order", "refunded_at", "ALTER TABLE trade_order ADD COLUMN refunded_at TIMESTAMP NULL");
        ensureColumn("trade_order", "refund_amount", "ALTER TABLE trade_order ADD COLUMN refund_amount DECIMAL(10,2) NULL");
        ensureColumn("trade_order", "refund_reason", "ALTER TABLE trade_order ADD COLUMN refund_reason VARCHAR(500) NULL");
        ensureColumn("trade_order", "refund_review_note", "ALTER TABLE trade_order ADD COLUMN refund_review_note VARCHAR(500) NULL");
        ensureColumn("trade_order", "refund_operator_user_id", "ALTER TABLE trade_order ADD COLUMN refund_operator_user_id BIGINT NULL");
        ensureColumn("trade_order", "refund_operator_role", "ALTER TABLE trade_order ADD COLUMN refund_operator_role VARCHAR(32) NULL");
        ensureColumn("trade_order", "buyer_deleted_at", "ALTER TABLE trade_order ADD COLUMN buyer_deleted_at TIMESTAMP NULL");
        ensureColumn("trade_order", "seller_deleted_at", "ALTER TABLE trade_order ADD COLUMN seller_deleted_at TIMESTAMP NULL");
        ensureColumn("trade_order", "payment_trade_type", "ALTER TABLE trade_order ADD COLUMN payment_trade_type VARCHAR(32) NULL");
        ensureColumn("trade_order", "payment_prepay_id", "ALTER TABLE trade_order ADD COLUMN payment_prepay_id VARCHAR(128) NULL");
        ensureColumn("trade_order", "payment_code_url", "ALTER TABLE trade_order ADD COLUMN payment_code_url VARCHAR(1024) NULL");
        ensureColumn("trade_order", "payment_transaction_id", "ALTER TABLE trade_order ADD COLUMN payment_transaction_id VARCHAR(64) NULL");
        ensureColumn("trade_order", "payment_expire_at", "ALTER TABLE trade_order ADD COLUMN payment_expire_at TIMESTAMP NULL");
        ensureColumn("trade_order", "payment_notified_at", "ALTER TABLE trade_order ADD COLUMN payment_notified_at TIMESTAMP NULL");
        ensureColumn("trade_order", "deposit_amount", "ALTER TABLE trade_order ADD COLUMN deposit_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00");
        ensureColumn("trade_order", "buyer_confirmed_at", "ALTER TABLE trade_order ADD COLUMN buyer_confirmed_at TIMESTAMP NULL");
        ensureColumn("trade_order", "seller_confirmed_at", "ALTER TABLE trade_order ADD COLUMN seller_confirmed_at TIMESTAMP NULL");
        ensureColumn("trade_order", "extra_items_included", "ALTER TABLE trade_order ADD COLUMN extra_items_included TINYINT(1) NOT NULL DEFAULT 0");
        ensureColumn("trade_order", "extra_items_amount", "ALTER TABLE trade_order ADD COLUMN extra_items_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00");
        ensureColumn("trade_order", "extra_items_snapshot_json", "ALTER TABLE trade_order ADD COLUMN extra_items_snapshot_json LONGTEXT NULL");
        ensureColumnType("trade_order", "listing_title", "ALTER TABLE trade_order MODIFY COLUMN listing_title VARCHAR(512) NOT NULL");
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

    private void ensureColumnType(String tableName, String columnName, String sql) {
        Long length = jdbcTemplate.queryForObject(
            "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS WHERE UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)",
            Long.class,
            tableName,
            columnName
        );
        if (length != null && length >= 512L) {
            return;
        }
        long startAt = System.currentTimeMillis();
        jdbcTemplate.execute(sql);
        log.info("schema upgrade success table={} column={} type=wide_text costMs={}", tableName, columnName, System.currentTimeMillis() - startAt);
    }
}
