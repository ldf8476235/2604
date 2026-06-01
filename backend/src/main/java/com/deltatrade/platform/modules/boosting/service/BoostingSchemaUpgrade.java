package com.deltatrade.platform.modules.boosting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BoostingSchemaUpgrade implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BoostingSchemaUpgrade.class);

    private final JdbcTemplate jdbcTemplate;

    public BoostingSchemaUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureTable("boosting_service", "CREATE TABLE IF NOT EXISTS boosting_service (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "service_no VARCHAR(32) NOT NULL UNIQUE," +
            "category_code VARCHAR(32) NOT NULL," +
            "category_label VARCHAR(64) NOT NULL," +
            "name VARCHAR(100) NOT NULL," +
            "description VARCHAR(255) NOT NULL," +
            "price DECIMAL(10,2) NOT NULL," +
            "cycle_code VARCHAR(32) NOT NULL," +
            "cycle_label VARCHAR(64) NOT NULL," +
            "guarantee_note VARCHAR(255) NOT NULL," +
            "provider_type VARCHAR(32) NOT NULL," +
            "provider_name VARCHAR(100) NOT NULL," +
            "sales_count INT NOT NULL DEFAULT 0," +
            "status VARCHAR(32) NOT NULL," +
            "sort_no INT NOT NULL DEFAULT 0," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL" +
            ")");
        ensureTable("boosting_order", "CREATE TABLE IF NOT EXISTS boosting_order (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "order_no VARCHAR(32) NOT NULL UNIQUE," +
            "service_no VARCHAR(32) NOT NULL," +
            "service_name VARCHAR(100) NOT NULL," +
            "service_category VARCHAR(64) NOT NULL," +
            "service_description VARCHAR(255) NOT NULL," +
            "price DECIMAL(10,2) NOT NULL," +
            "cycle_label VARCHAR(64) NOT NULL," +
            "guarantee_note VARCHAR(255) NOT NULL," +
            "provider_type VARCHAR(32) NOT NULL," +
            "provider_name VARCHAR(100) NOT NULL," +
            "buyer_user_id BIGINT NOT NULL," +
            "buyer_nickname VARCHAR(64) NOT NULL," +
            "game_region VARCHAR(100) NOT NULL," +
            "account_name VARCHAR(100) NOT NULL," +
            "account_password_cipher TEXT NOT NULL," +
            "character_name VARCHAR(100) NOT NULL," +
            "special_requirement TEXT," +
            "status VARCHAR(32) NOT NULL," +
            "payment_method VARCHAR(32)," +
            "payment_trade_type VARCHAR(32)," +
            "payment_prepay_id VARCHAR(128)," +
            "payment_code_url VARCHAR(1024)," +
            "payment_transaction_id VARCHAR(64)," +
            "payment_expire_at TIMESTAMP NULL," +
            "payment_notified_at TIMESTAMP NULL," +
            "progress_percent INT NOT NULL DEFAULT 0," +
            "progress_summary VARCHAR(255)," +
            "chat_group_no VARCHAR(32)," +
            "after_sale_reason TEXT," +
            "after_sale_proof_key VARCHAR(255)," +
            "created_at TIMESTAMP NOT NULL," +
            "paid_at TIMESTAMP NULL," +
            "started_at TIMESTAMP NULL," +
            "completed_at TIMESTAMP NULL," +
            "canceled_at TIMESTAMP NULL," +
            "after_sale_at TIMESTAMP NULL," +
            "updated_at TIMESTAMP NOT NULL" +
            ")");
        ensureTable("boosting_progress_log", "CREATE TABLE IF NOT EXISTS boosting_progress_log (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "order_no VARCHAR(32) NOT NULL," +
            "progress_percent INT NOT NULL," +
            "title VARCHAR(100) NOT NULL," +
            "content VARCHAR(255) NOT NULL," +
            "created_by VARCHAR(64) NOT NULL," +
            "created_at TIMESTAMP NOT NULL" +
            ")");
        ensureColumn("boosting_order", "payment_trade_type", "ALTER TABLE boosting_order ADD COLUMN payment_trade_type VARCHAR(32) NULL");
        ensureColumn("boosting_order", "payment_prepay_id", "ALTER TABLE boosting_order ADD COLUMN payment_prepay_id VARCHAR(128) NULL");
        ensureColumn("boosting_order", "payment_code_url", "ALTER TABLE boosting_order ADD COLUMN payment_code_url VARCHAR(1024) NULL");
        ensureColumn("boosting_order", "payment_transaction_id", "ALTER TABLE boosting_order ADD COLUMN payment_transaction_id VARCHAR(64) NULL");
        ensureColumn("boosting_order", "payment_expire_at", "ALTER TABLE boosting_order ADD COLUMN payment_expire_at TIMESTAMP NULL");
        ensureColumn("boosting_order", "payment_notified_at", "ALTER TABLE boosting_order ADD COLUMN payment_notified_at TIMESTAMP NULL");
        seedDefaultServices();
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

    private void seedDefaultServices() {
        seedService(
            "BS202604200001",
            "RANK",
            "段位冲分",
            "王牌段位冲分包",
            "适合已具备稳定战绩的账号，承接星级冲分与赛季保段。",
            "168.00",
            "D1",
            "24 小时内",
            "代肝过程全程可追踪，异常战绩波动支持售后复核。",
            "STUDIO",
            "速通互联工作室",
            18,
            "ACTIVE",
            10
        );
        seedService(
            "BS202604200002",
            "CURRENCY",
            "哈夫币代刷",
            "哈夫币速刷服务",
            "适合日常搬砖与活动冲刺，按单结算，支持分段交付进度。",
            "128.00",
            "D2",
            "48 小时内",
            "平台客服跟进进度，如未按约定周期完成可申请补偿。",
            "STUDIO",
            "速通互联工作室",
            11,
            "ACTIVE",
            20
        );
        seedService(
            "BS202604200003",
            "SAFEBOX",
            "安全箱护送",
            "安全箱护送保分单",
            "针对安全箱相关玩法提供稳妥护送与保底服务，适合高价值账号。",
            "218.00",
            "D3",
            "72 小时内",
            "支持平台介入核查，若因服务方原因失败可发起售后。",
            "PLATFORM",
            "平台直营",
            7,
            "ACTIVE",
            30
        );
    }

    private void seedService(
        String serviceNo,
        String categoryCode,
        String categoryLabel,
        String name,
        String description,
        String price,
        String cycleCode,
        String cycleLabel,
        String guaranteeNote,
        String providerType,
        String providerName,
        int salesCount,
        String status,
        int sortNo
    ) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM boosting_service WHERE service_no = ?",
            Integer.class,
            serviceNo
        );
        if (count != null && count.intValue() > 0) {
            return;
        }
        jdbcTemplate.update(
            "INSERT INTO boosting_service (" +
                "service_no, category_code, category_label, name, description, price, cycle_code, cycle_label, " +
                "guarantee_note, provider_type, provider_name, sales_count, status, sort_no, created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
            serviceNo,
            categoryCode,
            categoryLabel,
            name,
            description,
            price,
            cycleCode,
            cycleLabel,
            guaranteeNote,
            providerType,
            providerName,
            Integer.valueOf(salesCount),
            status,
            Integer.valueOf(sortNo)
        );
        log.info("boosting seed service inserted serviceNo={} providerName={}", serviceNo, providerName);
    }
}
