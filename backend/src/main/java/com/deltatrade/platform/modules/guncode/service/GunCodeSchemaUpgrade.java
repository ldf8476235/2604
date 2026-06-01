package com.deltatrade.platform.modules.guncode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Component
public class GunCodeSchemaUpgrade implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GunCodeSchemaUpgrade.class);

    private final JdbcTemplate jdbcTemplate;

    public GunCodeSchemaUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureTable("gun_code_group", "CREATE TABLE IF NOT EXISTS gun_code_group (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "group_key VARCHAR(64) NOT NULL UNIQUE," +
            "creator VARCHAR(64) NOT NULL," +
            "source VARCHAR(32)," +
            "badges_text VARCHAR(255) NOT NULL," +
            "sort_no INT NOT NULL DEFAULT 0," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("gun_code_entry", "CREATE TABLE IF NOT EXISTS gun_code_entry (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "entry_code VARCHAR(64) NOT NULL UNIQUE," +
            "group_key VARCHAR(64) NOT NULL," +
            "title VARCHAR(120) NOT NULL," +
            "category VARCHAR(32) NOT NULL," +
            "likes_count INT NOT NULL DEFAULT 0," +
            "dislikes_count INT NOT NULL DEFAULT 0," +
            "tags_text VARCHAR(255)," +
            "sort_no INT NOT NULL DEFAULT 0," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("gun_code_vote", "CREATE TABLE IF NOT EXISTS gun_code_vote (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "entry_code VARCHAR(64) NOT NULL," +
            "user_id BIGINT NOT NULL," +
            "vote_type VARCHAR(16) NOT NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL," +
            "UNIQUE KEY uk_gun_code_vote_user_entry (user_id, entry_code))");
        seedIfEmpty();
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

    private void seedIfEmpty() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM gun_code_group", Long.class);
        if (count != null && count > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        insertGroup("bosh", "Bosh", "抖音", "主播顶护魔王同款", 10, now);
        insertEntry("6J4F3B001T61VNNP5VK98", "bosh", "M14射手步枪", "精确射手步枪", 6, 57, "", 10, now);
        insertEntry("6J1V9N80BVIRVL16CV1AB", "bosh", "M700狙击步枪", "狙击步枪", 5, 27, "", 20, now);
        insertEntry("6JEIQE800M7RFBSRGE5GE", "bosh", "M14射手步枪", "精确射手步枪", 4, 31, "", 30, now);
        insertEntry("613FQ880CVVHEAFPPG8CH", "bosh", "M250通用机枪", "轻机枪", 3, 27, "", 40, now);
        insertEntry("6J8KCPC02D5DHK6SCDPOP", "bosh", "SR-25射手步枪", "精确射手步枪", 2, 29, "", 50, now);

        insertGroup("chena", "辰阿", "抖音", "主播顶护魔王同款,主播同款", 20, now);
        insertEntry("6JIHRKC0473LGNSN240H5", "chena", "AS Val突击步枪", "步枪", 6, 35, "主播同款", 10, now);
        insertEntry("6JIHRQG0473LGNSN240H5", "chena", "AS Val突击步枪 斜握腰射61", "步枪", 3, 22, "主播同款", 20, now);
        insertEntry("6JIHS4S0473LGNSN240H5", "chena", "M7战斗步枪 3.5倍", "步枪", 3, 16, "", 30, now);
        insertEntry("6JIHSAK0473LGNSN240H5", "chena", "M7战斗步枪 红点", "步枪", 2, 17, "", 40, now);
        insertEntry("6JIHRUO0473LGNSN240H5", "chena", "SR-25射手步枪", "精确射手步枪", 1, 18, "", 50, now);

        insertGroup("hebeilong", "河北龙", "抖音", "主播顶护魔王同款,主播同款", 30, now);
        insertEntry("6JBT1S804RKG5PEP9BL67", "hebeilong", "AS Val突击步枪", "步枪", 2, 12, "主播同款", 10, now);
        insertEntry("6JGLRIO094JVITT8VSU6I", "hebeilong", "AS Val突击步枪", "步枪", 0, 11, "主播同款", 20, now);
        insertEntry("6JCVNS807MAMKEF8I6OO5", "hebeilong", "AS Val突击步枪", "步枪", 0, 11, "", 30, now);
        insertEntry("6JCB5I009S4O9TFSL4USO", "hebeilong", "AS Val突击步枪", "步枪", 0, 11, "", 40, now);
        insertEntry("6J2296O05VO2TE43IMV7H", "hebeilong", "M14射手步枪", "精确射手步枪", 0, 10, "", 50, now);

        insertGroup("shu", "赎", "抖音", "主播顶护魔王同款,主播同款", 40, now);
        insertEntry("6JIEOS404A5GIV02K4A4U", "shu", "M7战斗步枪", "步枪", 3, 17, "主播同款", 10, now);
        insertEntry("6JIEO1C04A5GIV02K4A4U", "shu", "AS Val突击步枪", "步枪", 1, 18, "", 20, now);
        insertEntry("6JIEO5C04A5GIV02K4A4U", "shu", "AWM狙击步枪", "狙击步枪", 1, 16, "", 30, now);
        insertEntry("6JIEOBK04A5GIV02K4A4U", "shu", "M14射手步枪", "精确射手步枪", 1, 16, "", 40, now);

        insertGroup("hk1ng", "Hk1ng", "抖音", "主播顶护魔王同款,主播同款", 50, now);
        insertEntry("6JA0LF804LEDIEE22K4SR", "hk1ng", "M14射手步枪", "精确射手步枪", 2, 13, "", 10, now);
        insertEntry("6JA0LHO04LEDIEE22K4SR", "hk1ng", "M7战斗步枪", "步枪", 1, 11, "", 20, now);
        insertEntry("6JA0LJC04LEDIEE22K4SR", "hk1ng", "M700狙击步枪", "狙击步枪", 1, 12, "", 30, now);
        insertEntry("6JA0MCO04LEDIEE22K4SR", "hk1ng", "SR-3M紧凑突击步枪", "步枪", 1, 10, "", 40, now);
        insertEntry("6JA0MU004LEDIEE22K4SR", "hk1ng", "腾龙突击步枪", "步枪", 1, 10, "", 50, now);

        insertGroup("wukai", "悟凯", "抖音", "主播顶护魔王同款,主播同款", 60, now);
        insertEntry("6JALDNS096GBR33JHF31J", "wukai", "MK47突击步枪", "步枪", 0, 12, "主播同款", 10, now);
        insertEntry("6JALDS0096GBR33JHF31J", "wukai", "AS Val突击步枪", "步枪", 0, 9, "", 20, now);
        insertEntry("6JALE08096GBR33JHF31J", "wukai", "M14射手步枪", "精确射手步枪", 0, 9, "", 30, now);
        insertEntry("6JALEBS096GBR33JHF31J", "wukai", "M7战斗步枪", "步枪", 0, 9, "", 40, now);
        insertEntry("6JALEGS096GBR33JHF31J", "wukai", "M7战斗步枪", "步枪", 0, 9, "", 50, now);

        insertGroup("s12k", "S12K", "", "霰弹枪", 70, now);
        insertEntry("6JDANNK0B1H3TI624U64G", "s12k", "14w丐版", "霰弹枪", 4, 8, "", 10, now);
        insertEntry("6JDANPO0B1H3TI624U64G", "s12k", "23w满改", "霰弹枪", 2, 7, "", 20, now);

        insertGroup("fs12", "FS-12霰弹枪", "", "霰弹枪,修脚,版本强势T0", 80, now);
        insertEntry("6JFFIU804U3TN9SQ0V71K", "fs12", "29w手枪版", "霰弹枪", 4, 6, "修脚,版本强势T0", 10, now);
        insertEntry("6JFFIV804U3TN9SQ0V71K", "fs12", "37W主枪版", "霰弹枪", 2, 7, "修脚", 20, now);

        insertGroup("shotgun725", "725双管霰弹枪", "", "霰弹枪", 90, now);
        insertEntry("6JCBPEG01OMBBV761REOP", "shotgun725", "25W莽侠满改", "霰弹枪", 1, 4, "", 10, now);
    }

    private void insertGroup(String groupKey, String creator, String source, String badgesText, int sortNo, LocalDateTime now) {
        jdbcTemplate.update(
            "INSERT INTO gun_code_group (group_key, creator, source, badges_text, sort_no, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
            groupKey,
            creator,
            source,
            badgesText,
            sortNo,
            Timestamp.valueOf(now),
            Timestamp.valueOf(now)
        );
    }

    private void insertEntry(String entryCode, String groupKey, String title, String category, int likes, int dislikes, String tagsText, int sortNo, LocalDateTime now) {
        jdbcTemplate.update(
            "INSERT INTO gun_code_entry (entry_code, group_key, title, category, likes_count, dislikes_count, tags_text, sort_no, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            entryCode,
            groupKey,
            title,
            category,
            likes,
            dislikes,
            tagsText,
            sortNo,
            Timestamp.valueOf(now),
            Timestamp.valueOf(now)
        );
    }
}
