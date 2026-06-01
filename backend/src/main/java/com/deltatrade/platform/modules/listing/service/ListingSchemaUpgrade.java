package com.deltatrade.platform.modules.listing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ListingSchemaUpgrade implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ListingSchemaUpgrade.class);

    private final JdbcTemplate jdbcTemplate;
    private final ListingPublishService listingPublishService;

    public ListingSchemaUpgrade(JdbcTemplate jdbcTemplate, ListingPublishService listingPublishService) {
        this.jdbcTemplate = jdbcTemplate;
        this.listingPublishService = listingPublishService;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        ensureTable("listing_favorite", "CREATE TABLE IF NOT EXISTS listing_favorite (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "listing_no VARCHAR(32) NOT NULL," +
            "user_id BIGINT NOT NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL," +
            "UNIQUE KEY uk_listing_favorite (listing_no, user_id))");
        ensureColumn("province_code", "ALTER TABLE account_listing ADD COLUMN province_code VARCHAR(16) NULL");
        ensureColumn("province_name", "ALTER TABLE account_listing ADD COLUMN province_name VARCHAR(64) NULL");
        ensureColumn("city_code", "ALTER TABLE account_listing ADD COLUMN city_code VARCHAR(16) NULL");
        ensureColumn("city_name", "ALTER TABLE account_listing ADD COLUMN city_name VARCHAR(64) NULL");
        ensureColumn("delivery_method", "ALTER TABLE account_listing ADD COLUMN delivery_method VARCHAR(32) NULL");
        ensureColumn("publish_attributes_json", "ALTER TABLE account_listing ADD COLUMN publish_attributes_json LONGTEXT NULL");
        ensureColumn("view_count", "ALTER TABLE account_listing ADD COLUMN view_count INT NOT NULL DEFAULT 0");
        ensureColumn("favorite_count", "ALTER TABLE account_listing ADD COLUMN favorite_count INT NOT NULL DEFAULT 0");
        ensureColumn("sales_count", "ALTER TABLE account_listing ADD COLUMN sales_count INT NOT NULL DEFAULT 0");
        ensureColumnType("title", "ALTER TABLE account_listing MODIFY COLUMN title VARCHAR(512) NOT NULL");
        repairLegacyRankNames();
        listingPublishService.repairHistoricalRentalPricing();
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

    private void ensureColumn(String columnName, String sql) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                "WHERE UPPER(TABLE_NAME) = 'ACCOUNT_LISTING' AND UPPER(COLUMN_NAME) = UPPER(?)",
            Long.class,
            columnName
        );
        if (count != null && count > 0) {
            return;
        }
        long startAt = System.currentTimeMillis();
        jdbcTemplate.execute(sql);
        log.info("schema upgrade success table=account_listing column={} costMs={}", columnName, System.currentTimeMillis() - startAt);
    }

    private void ensureColumnType(String columnName, String sql) {
        Long length = jdbcTemplate.queryForObject(
            "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS " +
                "WHERE UPPER(TABLE_NAME) = 'ACCOUNT_LISTING' AND UPPER(COLUMN_NAME) = UPPER(?)",
            Long.class,
            columnName
        );
        if (length != null && length >= 512L) {
            return;
        }
        long startAt = System.currentTimeMillis();
        jdbcTemplate.execute(sql);
        log.info("schema upgrade success table=account_listing column={} type=wide_text costMs={}", columnName, System.currentTimeMillis() - startAt);
    }

    private void repairLegacyRankNames() {
        long startAt = System.currentTimeMillis();
        int rows = jdbcTemplate.update(
            "UPDATE account_listing " +
                "SET rank_name = CASE " +
                "WHEN LOWER(rank_name) = 'legend' THEN 'summit' " +
                "WHEN LOWER(rank_name) = 'master' THEN 'blackhawk' " +
                "ELSE rank_name END " +
                "WHERE LOWER(rank_name) IN ('legend', 'master')"
        );
        if (rows > 0) {
            log.info("listing rank repair success rows={} costMs={}", rows, System.currentTimeMillis() - startAt);
        }
    }
}
