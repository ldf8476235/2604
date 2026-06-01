package com.deltatrade.platform.modules.im.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ImSchemaUpgrade implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ImSchemaUpgrade.class);

    private final JdbcTemplate jdbcTemplate;

    public ImSchemaUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureTable("im_conversation", "CREATE TABLE IF NOT EXISTS im_conversation (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "conversation_no VARCHAR(32) NOT NULL UNIQUE," +
            "scene_code VARCHAR(32) NOT NULL," +
            "source_order_no VARCHAR(32) NOT NULL," +
            "title VARCHAR(120) NOT NULL," +
            "buyer_user_id BIGINT NOT NULL," +
            "seller_user_id BIGINT NULL," +
            "support_display_name VARCHAR(64) NOT NULL," +
            "last_message_excerpt VARCHAR(255)," +
            "last_message_at TIMESTAMP NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL" +
            ")");
        ensureTable("im_participant", "CREATE TABLE IF NOT EXISTS im_participant (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "conversation_no VARCHAR(32) NOT NULL," +
            "user_id BIGINT NOT NULL," +
            "participant_role VARCHAR(32) NOT NULL," +
            "last_read_message_id BIGINT NOT NULL DEFAULT 0," +
            "joined_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL," +
            "UNIQUE KEY uk_im_participant (conversation_no, user_id)" +
            ")");
        ensureTable("im_message", "CREATE TABLE IF NOT EXISTS im_message (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "conversation_no VARCHAR(32) NOT NULL," +
            "sender_role VARCHAR(32) NOT NULL," +
            "sender_user_id BIGINT NULL," +
            "sender_name VARCHAR(64) NOT NULL," +
            "message_type VARCHAR(16) NOT NULL," +
            "content_text TEXT," +
            "file_key VARCHAR(255)," +
            "file_name VARCHAR(255)," +
            "created_at TIMESTAMP NOT NULL" +
            ")");
        ensureTable("im_support_read_state", "CREATE TABLE IF NOT EXISTS im_support_read_state (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "conversation_no VARCHAR(32) NOT NULL UNIQUE," +
            "last_read_message_id BIGINT NOT NULL DEFAULT 0," +
            "updated_at TIMESTAMP NOT NULL" +
            ")");
        backfillSupportReadState();
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

    private void backfillSupportReadState() {
        long startAt = System.currentTimeMillis();
        int affected = jdbcTemplate.update(
            "INSERT INTO im_support_read_state (conversation_no, last_read_message_id, updated_at) " +
                "SELECT c.conversation_no, COALESCE(MAX(m.id), 0), NOW() " +
                "FROM im_conversation c " +
                "LEFT JOIN im_message m ON m.conversation_no = c.conversation_no " +
                "LEFT JOIN im_support_read_state s ON s.conversation_no = c.conversation_no " +
                "WHERE s.id IS NULL " +
                "GROUP BY c.conversation_no"
        );
        if (affected > 0) {
            log.info("schema upgrade support read state backfilled rows={} costMs={}", affected, System.currentTimeMillis() - startAt);
        }
    }
}
