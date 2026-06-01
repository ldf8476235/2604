package com.deltatrade.platform.modules.admin.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdminSchemaUpgrade implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSchemaUpgrade.class);
    private final JdbcTemplate jdbcTemplate;

    public AdminSchemaUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureAuthUserColumn("account_status", "ALTER TABLE auth_user ADD COLUMN account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE'");
        ensureAuthUserColumn("ban_reason", "ALTER TABLE auth_user ADD COLUMN ban_reason VARCHAR(255) NULL");
        ensureTable("system_configs", "CREATE TABLE IF NOT EXISTS system_configs (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "config_key VARCHAR(64) NOT NULL UNIQUE," +
            "config_value TEXT NOT NULL," +
            "config_group_name VARCHAR(64) NOT NULL," +
            "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");
        ensureTable("operation_banner", "CREATE TABLE IF NOT EXISTS operation_banner (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "banner_no VARCHAR(32) NOT NULL UNIQUE," +
            "title VARCHAR(100) NOT NULL," +
            "image_key VARCHAR(255) NOT NULL," +
            "link_url VARCHAR(255)," +
            "sort_no INT NOT NULL DEFAULT 0," +
            "status VARCHAR(32) NOT NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("operation_shortcut", "CREATE TABLE IF NOT EXISTS operation_shortcut (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "shortcut_no VARCHAR(32) NOT NULL UNIQUE," +
            "name VARCHAR(64) NOT NULL," +
            "icon_key VARCHAR(255)," +
            "link_url VARCHAR(255) NOT NULL," +
            "sort_no INT NOT NULL DEFAULT 0," +
            "status VARCHAR(32) NOT NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("operation_announcement", "CREATE TABLE IF NOT EXISTS operation_announcement (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "announcement_no VARCHAR(32) NOT NULL UNIQUE," +
            "title VARCHAR(120) NOT NULL," +
            "content TEXT NOT NULL," +
            "category VARCHAR(32) NOT NULL," +
            "pinned BOOLEAN NOT NULL DEFAULT FALSE," +
            "status VARCHAR(32) NOT NULL," +
            "publish_at TIMESTAMP NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("admin_role", "CREATE TABLE IF NOT EXISTS admin_role (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "role_code VARCHAR(32) NOT NULL UNIQUE," +
            "role_name VARCHAR(64) NOT NULL," +
            "description VARCHAR(255)," +
            "permissions_json TEXT NOT NULL," +
            "status VARCHAR(32) NOT NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL)");
        ensureTable("admin_role_member", "CREATE TABLE IF NOT EXISTS admin_role_member (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "role_id BIGINT NOT NULL," +
            "user_id BIGINT NOT NULL," +
            "created_at TIMESTAMP NOT NULL," +
            "updated_at TIMESTAMP NOT NULL," +
            "UNIQUE KEY uk_admin_role_member (role_id, user_id))");
        seedDefaultOperations();
        repairCorruptedOperations();
        repairCorruptedAdminRoles();
    }

    private void ensureAuthUserColumn(String columnName, String ddl) {
        long startAt = System.currentTimeMillis();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SHOW COLUMNS FROM auth_user LIKE ?", columnName);
        if (!rows.isEmpty()) {
            return;
        }
        jdbcTemplate.execute(ddl);
        log.info("mysql ddl success target=auth_user_add_column costMs={} column={}", System.currentTimeMillis() - startAt, columnName);
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

    private void seedDefaultOperations() {
        upsertBannerSeed(
            "BN202604180001",
            "春季账号租赁节",
            "delta-trade/demo/banner-1.png",
            "/home",
            10,
            "ACTIVE"
        );
        upsertBannerSeed(
            "BN202604180002",
            "代肝服务保障公告",
            "delta-trade/demo/banner-2.png",
            "/boosting",
            20,
            "ACTIVE"
        );
        upsertShortcutSeed(
            "SC202604180001",
            "热门账号",
            "delta-trade/demo/shortcut-hot.png",
            "/home",
            10,
            "ACTIVE"
        );
        upsertShortcutSeed(
            "SC202604180002",
            "代肝服务",
            "delta-trade/demo/shortcut-boosting.png",
            "/boosting",
            20,
            "ACTIVE"
        );
        upsertAnnouncementSeed(
            "AN202604180001",
            "平台公告：账号发布审核规则",
            "普通个人用户发布账号默认进入人工审核，工作室是否免审由平台单独配置。",
            "SYSTEM",
            true,
            "PUBLISHED"
        );
        upsertAnnouncementSeed(
            "AN202604180002",
            "活动公告：春季代肝满减",
            "哈夫币代肝与安全箱代肝服务已开放组合优惠，具体以订单页展示为准。",
            "ACTIVITY",
            false,
            "PUBLISHED"
        );
    }

    private void repairCorruptedOperations() {
        repairBannerSeed("BN202604180001", "春季账号租赁节");
        repairBannerSeed("BN202604180002", "代肝服务保障公告");
        repairShortcutSeed("SC202604180001", "热门账号");
        repairShortcutSeed("SC202604180002", "代肝服务");
        repairAnnouncementSeed(
            "AN202604180001",
            "平台公告：账号发布审核规则",
            "普通个人用户发布账号默认进入人工审核，工作室是否免审由平台单独配置。"
        );
        repairAnnouncementSeed(
            "AN202604180002",
            "活动公告：春季代肝满减",
            "哈夫币代肝与安全箱代肝服务已开放组合优惠，具体以订单页展示为准。"
        );
    }

    private void repairCorruptedAdminRoles() {
        repairRoleSeed("SUPER_ADMIN", "超级管理员", "拥有平台全部后台能力");
        repairRoleSeed("OPS_ADMIN", "运营管理员", "负责运营配置、账号审核与公告管理");
        repairRoleSeed("SERVICE_ADMIN", "客服管理员", "负责消息、IM 与售后协同");
        repairRoleSeed("FINANCE_ADMIN", "财务管理员", "负责提现审核、工作室分润结算");
        ensureRolePermission("SUPER_ADMIN", "support");
        ensureRolePermission("SERVICE_ADMIN", "support");
        ensureRolePermission("SERVICE_ADMIN", "order");
    }

    private void upsertBannerSeed(String bannerNo, String title, String imageKey, String linkUrl, int sortNo, String status) {
        jdbcTemplate.update(
            "INSERT INTO operation_banner (banner_no, title, image_key, link_url, sort_no, status, created_at, updated_at) " +
                "SELECT ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM DUAL " +
                "WHERE NOT EXISTS (SELECT 1 FROM operation_banner WHERE banner_no = ?)",
            bannerNo, title, imageKey, linkUrl, Integer.valueOf(sortNo), status, bannerNo
        );
    }

    private void upsertShortcutSeed(String shortcutNo, String name, String iconKey, String linkUrl, int sortNo, String status) {
        jdbcTemplate.update(
            "INSERT INTO operation_shortcut (shortcut_no, name, icon_key, link_url, sort_no, status, created_at, updated_at) " +
                "SELECT ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM DUAL " +
                "WHERE NOT EXISTS (SELECT 1 FROM operation_shortcut WHERE shortcut_no = ?)",
            shortcutNo, name, iconKey, linkUrl, Integer.valueOf(sortNo), status, shortcutNo
        );
    }

    private void upsertAnnouncementSeed(String announcementNo, String title, String content, String category, boolean pinned, String status) {
        jdbcTemplate.update(
            "INSERT INTO operation_announcement (announcement_no, title, content, category, pinned, status, publish_at, created_at, updated_at) " +
                "SELECT ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM DUAL " +
                "WHERE NOT EXISTS (SELECT 1 FROM operation_announcement WHERE announcement_no = ?)",
            announcementNo, title, content, category, Boolean.valueOf(pinned), status, announcementNo
        );
    }

    private void repairBannerSeed(String bannerNo, String expectedTitle) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, title FROM operation_banner WHERE banner_no = ? LIMIT 1",
            bannerNo
        );
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> row = rows.get(0);
        String title = stringValue(row.get("title"));
        if (!looksCorrupted(title)) {
            return;
        }
        jdbcTemplate.update(
            "UPDATE operation_banner SET title = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            expectedTitle,
            row.get("id")
        );
        log.info("operation banner text repaired bannerNo={}", bannerNo);
    }

    private void repairShortcutSeed(String shortcutNo, String expectedName) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, name FROM operation_shortcut WHERE shortcut_no = ? LIMIT 1",
            shortcutNo
        );
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> row = rows.get(0);
        String name = stringValue(row.get("name"));
        if (!looksCorrupted(name)) {
            return;
        }
        jdbcTemplate.update(
            "UPDATE operation_shortcut SET name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            expectedName,
            row.get("id")
        );
        log.info("operation shortcut text repaired shortcutNo={}", shortcutNo);
    }

    private void repairAnnouncementSeed(String announcementNo, String expectedTitle, String expectedContent) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, title, content FROM operation_announcement WHERE announcement_no = ? LIMIT 1",
            announcementNo
        );
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> row = rows.get(0);
        String title = stringValue(row.get("title"));
        String content = stringValue(row.get("content"));
        if (!looksCorrupted(title) && !looksCorrupted(content)) {
            return;
        }
        jdbcTemplate.update(
            "UPDATE operation_announcement SET title = ?, content = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            expectedTitle,
            expectedContent,
            row.get("id")
        );
        log.info("operation announcement text repaired announcementNo={}", announcementNo);
    }

    private void repairRoleSeed(String roleCode, String expectedRoleName, String expectedDescription) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, role_name, description FROM admin_role WHERE role_code = ? LIMIT 1",
            roleCode
        );
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> row = rows.get(0);
        String roleName = stringValue(row.get("role_name"));
        String description = stringValue(row.get("description"));
        if (!looksCorrupted(roleName) && !looksCorrupted(description)) {
            return;
        }
        jdbcTemplate.update(
            "UPDATE admin_role SET role_name = ?, description = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            expectedRoleName,
            expectedDescription,
            row.get("id")
        );
        log.info("admin role text repaired roleCode={}", roleCode);
    }

    private void ensureRolePermission(String roleCode, String permission) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id, permissions_json FROM admin_role WHERE role_code = ? LIMIT 1",
            roleCode
        );
        if (rows.isEmpty()) {
            return;
        }
        Map<String, Object> row = rows.get(0);
        String permissionsJson = stringValue(row.get("permissions_json"));
        if (permissionsJson.contains("\"" + permission + "\"")) {
            return;
        }
        String nextPermissionsJson;
        String trimmed = permissionsJson.trim();
        if (trimmed.length() > 1 && trimmed.startsWith("[") && trimmed.endsWith("]")) {
            nextPermissionsJson = trimmed.substring(0, trimmed.length() - 1) + ",\"" + permission + "\"]";
        } else {
            nextPermissionsJson = "[\"" + permission + "\"]";
        }
        jdbcTemplate.update(
            "UPDATE admin_role SET permissions_json = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
            nextPermissionsJson,
            row.get("id")
        );
        log.info("admin role permission repaired roleCode={} permission={}", roleCode, permission);
    }

    private boolean looksCorrupted(String text) {
        if (text == null || text.trim().isEmpty()) {
            return true;
        }
        if (text.indexOf('\uFFFD') >= 0) {
            return true;
        }
        int questionCount = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '?') {
                questionCount += 1;
            }
        }
        return questionCount >= 3;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
