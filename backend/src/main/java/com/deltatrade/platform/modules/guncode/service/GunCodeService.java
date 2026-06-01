package com.deltatrade.platform.modules.guncode.service;

import com.deltatrade.platform.common.auth.AuthPrincipal;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GunCodeService {

    private final JdbcTemplate jdbcTemplate;

    public GunCodeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public GunCodePayload loadGunCodes() {
        List<GunCodeGroup> groups = loadGroups();
        return new GunCodePayload(
            collectCategories(groups),
            collectTags(groups),
            groups
        );
    }

    public AdminGunCodeCenter loadAdminCenter() {
        List<AdminGunCodeGroupRow> rows = jdbcTemplate.query(
            "SELECT g.group_key, g.creator, g.source, g.badges_text, g.sort_no, g.updated_at, COUNT(e.id) AS entry_count " +
                "FROM gun_code_group g LEFT JOIN gun_code_entry e ON e.group_key = g.group_key " +
                "GROUP BY g.group_key, g.creator, g.source, g.badges_text, g.sort_no, g.updated_at " +
                "ORDER BY g.sort_no ASC, g.id ASC",
            (rs, rowNum) -> new AdminGunCodeGroupRow(
                rs.getString("group_key"),
                rs.getString("creator"),
                rs.getString("source"),
                splitText(rs.getString("badges_text")),
                rs.getInt("entry_count"),
                rs.getInt("sort_no"),
                formatTimestamp(rs.getTimestamp("updated_at"))
            )
        );
        GunCodePayload payload = loadGunCodes();
        return new AdminGunCodeCenter(
            mapOf(
                "groupCount", rows.size(),
                "entryCount", countEntries(payload.getGroups()),
                "categoryCount", payload.getCategories().size(),
                "tagCount", Math.max(0, payload.getTags().size() - 1)
            ),
            rows
        );
    }

    @Transactional
    public AdminGunCodeImportResult importRows(List<GunCodeImportRow> rows, boolean replaceExisting) {
        List<NormalizedGunCodeRow> normalizedRows = normalizeImportRows(rows);
        if (normalizedRows.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先上传至少一条改枪码数据");
        }

        Map<String, NormalizedGunCodeGroup> groups = new LinkedHashMap<String, NormalizedGunCodeGroup>();
        int insertedGroups = 0;
        int updatedGroups = 0;
        int insertedEntries = 0;
        int updatedEntries = 0;
        LocalDateTime now = LocalDateTime.now();

        for (NormalizedGunCodeRow row : normalizedRows) {
            NormalizedGunCodeGroup group = groups.get(row.getGroupKey());
            if (group == null) {
                group = new NormalizedGunCodeGroup(
                    row.getGroupKey(),
                    row.getCreator(),
                    row.getSource(),
                    row.getBadges(),
                    row.getGroupSortNo()
                );
                groups.put(group.getGroupKey(), group);
            }
            group.getEntries().add(row);
        }

        for (NormalizedGunCodeGroup group : groups.values()) {
            if (hasGroup(group.getGroupKey())) {
                jdbcTemplate.update(
                    "UPDATE gun_code_group SET creator = ?, source = ?, badges_text = ?, sort_no = ?, updated_at = ? WHERE group_key = ?",
                    group.getCreator(),
                    nullableText(group.getSource()),
                    joinText(group.getBadges()),
                    group.getSortNo(),
                    Timestamp.valueOf(now),
                    group.getGroupKey()
                );
                updatedGroups = updatedGroups + 1;
            } else {
                jdbcTemplate.update(
                    "INSERT INTO gun_code_group (group_key, creator, source, badges_text, sort_no, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    group.getGroupKey(),
                    group.getCreator(),
                    nullableText(group.getSource()),
                    joinText(group.getBadges()),
                    group.getSortNo(),
                    Timestamp.valueOf(now),
                    Timestamp.valueOf(now)
                );
                insertedGroups = insertedGroups + 1;
            }

            for (NormalizedGunCodeRow row : group.getEntries()) {
                if (hasEntry(row.getEntryCode())) {
                    jdbcTemplate.update(
                        "UPDATE gun_code_entry SET group_key = ?, title = ?, category = ?, tags_text = ?, sort_no = ?, updated_at = ? WHERE entry_code = ?",
                        row.getGroupKey(),
                        row.getTitle(),
                        row.getCategory(),
                        joinText(row.getTags()),
                        row.getEntrySortNo(),
                        Timestamp.valueOf(now),
                        row.getEntryCode()
                    );
                    updatedEntries = updatedEntries + 1;
                } else {
                    jdbcTemplate.update(
                        "INSERT INTO gun_code_entry (entry_code, group_key, title, category, likes_count, dislikes_count, tags_text, sort_no, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        row.getEntryCode(),
                        row.getGroupKey(),
                        row.getTitle(),
                        row.getCategory(),
                        0,
                        0,
                        joinText(row.getTags()),
                        row.getEntrySortNo(),
                        Timestamp.valueOf(now),
                        Timestamp.valueOf(now)
                    );
                    insertedEntries = insertedEntries + 1;
                }
            }
        }

        if (replaceExisting) {
            List<String> incomingEntryCodes = new ArrayList<String>();
            List<String> incomingGroupKeys = new ArrayList<String>(groups.keySet());
            for (NormalizedGunCodeRow row : normalizedRows) {
                incomingEntryCodes.add(row.getEntryCode());
            }
            deleteEntriesOutsideImport(incomingEntryCodes);
            deleteVotesOutsideImport(incomingEntryCodes);
            deleteGroupsOutsideImport(incomingGroupKeys);
        }

        return new AdminGunCodeImportResult(
            replaceExisting,
            normalizedRows.size(),
            insertedGroups,
            updatedGroups,
            insertedEntries,
            updatedEntries,
            loadAdminCenter()
        );
    }

    public Map<String, String> loadUserVotes(AuthPrincipal principal, List<String> entryCodes) {
        if (principal == null || principal.getUserId() == null || entryCodes == null || entryCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> normalizedCodes = normalizeEntryCodes(entryCodes);
        if (normalizedCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = String.join(",", Collections.nCopies(normalizedCodes.size(), "?"));
        List<Object> params = new ArrayList<Object>();
        params.add(principal.getUserId());
        params.addAll(normalizedCodes);
        Map<String, String> result = new LinkedHashMap<String, String>();
        List<String[]> votes = jdbcTemplate.query(
            "SELECT entry_code, vote_type FROM gun_code_vote WHERE user_id = ? AND entry_code IN (" + placeholders + ")",
            params.toArray(),
            (rs, rowNum) -> new String[]{rs.getString("entry_code"), rs.getString("vote_type")}
        );
        for (String[] vote : votes) {
            result.put(vote[0], vote[1]);
        }
        return result;
    }

    @Transactional
    public VoteResult vote(AuthPrincipal principal, String entryCode, String voteType) {
        if (principal == null || principal.getUserId() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        String normalizedEntryCode = normalizeEntryCode(entryCode);
        String normalizedVoteType = normalizeVoteType(voteType);
        VoteCountSnapshot snapshot = findEntrySnapshot(normalizedEntryCode);
        ExistingVote existing = findExistingVote(principal.getUserId(), normalizedEntryCode);
        LocalDateTime now = LocalDateTime.now();
        int likes = snapshot.getLikes();
        int dislikes = snapshot.getDislikes();
        String currentVote = normalizedVoteType;

        if (existing == null) {
            insertVote(principal.getUserId(), normalizedEntryCode, normalizedVoteType, now);
            if (VoteType.LIKE.name().equals(normalizedVoteType)) {
                likes = likes + 1;
            } else {
                dislikes = dislikes + 1;
            }
        } else if (normalizedVoteType.equals(existing.getVoteType())) {
            deleteVote(existing.getId());
            if (VoteType.LIKE.name().equals(normalizedVoteType)) {
                likes = Math.max(0, likes - 1);
            } else {
                dislikes = Math.max(0, dislikes - 1);
            }
            currentVote = null;
        } else {
            updateVote(existing.getId(), normalizedVoteType, now);
            if (VoteType.LIKE.name().equals(normalizedVoteType)) {
                likes = likes + 1;
                dislikes = Math.max(0, dislikes - 1);
            } else {
                dislikes = dislikes + 1;
                likes = Math.max(0, likes - 1);
            }
        }

        jdbcTemplate.update(
            "UPDATE gun_code_entry SET likes_count = ?, dislikes_count = ? WHERE entry_code = ?",
            likes,
            dislikes,
            normalizedEntryCode
        );

        return new VoteResult(normalizedEntryCode, likes, dislikes, currentVote);
    }

    private List<GunCodeGroup> loadGroups() {
        Map<String, GunCodeGroup> groups = new LinkedHashMap<String, GunCodeGroup>();
        List<GunCodeGroup> groupRows = jdbcTemplate.query(
            "SELECT group_key, creator, source, badges_text FROM gun_code_group ORDER BY sort_no ASC, id ASC",
            (rs, rowNum) -> new GunCodeGroup(
                rs.getString("group_key"),
                rs.getString("creator"),
                rs.getString("source"),
                splitText(rs.getString("badges_text")),
                new ArrayList<GunCodeEntry>()
            )
        );
        for (GunCodeGroup group : groupRows) {
            groups.put(group.getId(), group);
        }

        List<GunCodeEntryRow> entryRows = jdbcTemplate.query(
            "SELECT entry_code, group_key, title, category, likes_count, dislikes_count, tags_text " +
                "FROM gun_code_entry ORDER BY group_key ASC, sort_no ASC, id ASC",
            (rs, rowNum) -> new GunCodeEntryRow(
                rs.getString("group_key"),
                new GunCodeEntry(
                    rs.getString("entry_code"),
                    rs.getString("title"),
                    rs.getString("category"),
                    rs.getInt("likes_count"),
                    rs.getInt("dislikes_count"),
                    splitText(rs.getString("tags_text"))
                )
            )
        );
        for (GunCodeEntryRow entryRow : entryRows) {
            GunCodeGroup group = groups.get(entryRow.getGroupKey());
            if (group != null) {
                group.getEntries().add(entryRow.getEntry());
            }
        }

        return new ArrayList<GunCodeGroup>(groups.values());
    }

    private List<String> collectCategories(List<GunCodeGroup> groups) {
        Set<String> categories = new LinkedHashSet<String>();
        for (GunCodeGroup group : groups) {
            for (GunCodeEntry entry : group.getEntries()) {
                if (StringUtils.hasText(entry.getCategory())) {
                    categories.add(entry.getCategory());
                }
            }
        }
        return new ArrayList<String>(categories);
    }

    private List<String> collectTags(List<GunCodeGroup> groups) {
        Set<String> tags = new LinkedHashSet<String>();
        tags.add("全部标签");
        for (GunCodeGroup group : groups) {
            tags.addAll(group.getBadges());
            for (GunCodeEntry entry : group.getEntries()) {
                tags.addAll(entry.getTags());
            }
        }
        return new ArrayList<String>(tags);
    }

    private VoteCountSnapshot findEntrySnapshot(String entryCode) {
        List<VoteCountSnapshot> rows = jdbcTemplate.query(
            "SELECT entry_code, likes_count, dislikes_count FROM gun_code_entry WHERE entry_code = ? LIMIT 1",
            new Object[]{entryCode},
            (rs, rowNum) -> new VoteCountSnapshot(
                rs.getString("entry_code"),
                rs.getInt("likes_count"),
                rs.getInt("dislikes_count")
            )
        );
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "枪码不存在或已下架");
        }
        return rows.get(0);
    }

    private ExistingVote findExistingVote(Long userId, String entryCode) {
        List<ExistingVote> rows = jdbcTemplate.query(
            "SELECT id, vote_type FROM gun_code_vote WHERE user_id = ? AND entry_code = ? LIMIT 1",
            new Object[]{userId, entryCode},
            (rs, rowNum) -> new ExistingVote(rs.getLong("id"), rs.getString("vote_type"))
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void insertVote(Long userId, String entryCode, String voteType, LocalDateTime now) {
        jdbcTemplate.update(
            "INSERT INTO gun_code_vote (entry_code, user_id, vote_type, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
            entryCode,
            userId,
            voteType,
            Timestamp.valueOf(now),
            Timestamp.valueOf(now)
        );
    }

    private void updateVote(Long voteId, String voteType, LocalDateTime now) {
        jdbcTemplate.update(
            "UPDATE gun_code_vote SET vote_type = ?, updated_at = ? WHERE id = ?",
            voteType,
            Timestamp.valueOf(now),
            voteId
        );
    }

    private void deleteVote(Long voteId) {
        jdbcTemplate.update("DELETE FROM gun_code_vote WHERE id = ?", voteId);
    }

    private List<String> normalizeEntryCodes(List<String> entryCodes) {
        List<String> normalized = new ArrayList<String>();
        for (String entryCode : entryCodes) {
            if (StringUtils.hasText(entryCode)) {
                normalized.add(entryCode.trim());
            }
        }
        return normalized;
    }

    private String normalizeEntryCode(String entryCode) {
        String normalized = entryCode == null ? "" : entryCode.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "枪码不能为空");
        }
        return normalized;
    }

    private String normalizeVoteType(String voteType) {
        String normalized = voteType == null ? "" : voteType.trim().toUpperCase();
        if (!VoteType.LIKE.name().equals(normalized) && !VoteType.DISLIKE.name().equals(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "投票类型不合法");
        }
        return normalized;
    }

    private List<String> splitText(String value) {
        if (!StringUtils.hasText(value)) {
            return new ArrayList<String>();
        }
        String[] parts = value.split(",");
        List<String> result = new ArrayList<String>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                result.add(part.trim());
            }
        }
        return result;
    }

    private List<NormalizedGunCodeRow> normalizeImportRows(List<GunCodeImportRow> rows) {
        List<NormalizedGunCodeRow> result = new ArrayList<NormalizedGunCodeRow>();
        Set<String> seenEntryCodes = new LinkedHashSet<String>();
        int groupSortSeed = 10;
        Map<String, Integer> entrySortCounters = new LinkedHashMap<String, Integer>();
        for (int index = 0; index < rows.size(); index++) {
            GunCodeImportRow row = rows.get(index);
            if (row == null) {
                continue;
            }
            String creator = normalizeRequired(row.getCreator(), "第 " + (index + 1) + " 行创作者不能为空");
            String source = normalizeOptional(row.getSource(), 32);
            List<String> badges = normalizeTags(row.getBadges());
            String title = normalizeRequired(row.getTitle(), "第 " + (index + 1) + " 行标题不能为空");
            String category = normalizeRequired(row.getCategory(), "第 " + (index + 1) + " 行分类不能为空");
            String entryCode = normalizeRequired(row.getEntryCode(), "第 " + (index + 1) + " 行枪码不能为空").toUpperCase();
            List<String> tags = normalizeTags(row.getTags());
            if (!seenEntryCodes.add(entryCode)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "导入数据里存在重复枪码: " + entryCode);
            }
            String groupKey = resolveGroupKey(creator, source, badges);
            Integer counter = entrySortCounters.get(groupKey);
            int nextEntrySort = row.getEntrySortNo() == null ? (counter == null ? 10 : counter + 10) : row.getEntrySortNo().intValue();
            entrySortCounters.put(groupKey, nextEntrySort);
            int nextGroupSort = row.getGroupSortNo() == null ? groupSortSeed : row.getGroupSortNo().intValue();
            groupSortSeed = Math.max(groupSortSeed + 10, nextGroupSort + 10);
            result.add(new NormalizedGunCodeRow(groupKey, creator, source, badges, nextGroupSort, title, category, entryCode, tags, nextEntrySort));
        }
        return result;
    }

    private List<String> normalizeTags(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<String>();
        }
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            for (String part : value.split("[,|，/]")) {
                if (StringUtils.hasText(part)) {
                    result.add(part.trim());
                }
            }
        }
        return new ArrayList<String>(result);
    }

    private String normalizeRequired(String value, String errorMessage) {
        String normalized = value == null ? "" : value.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, errorMessage);
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private boolean hasGroup(String groupKey) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM gun_code_group WHERE group_key = ?",
            Long.class,
            groupKey
        );
        return count != null && count > 0;
    }

    private boolean hasEntry(String entryCode) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM gun_code_entry WHERE entry_code = ?",
            Long.class,
            entryCode
        );
        return count != null && count > 0;
    }

    private void deleteEntriesOutsideImport(List<String> entryCodes) {
        if (entryCodes.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(entryCodes.size(), "?"));
        jdbcTemplate.update(
            "DELETE FROM gun_code_entry WHERE entry_code NOT IN (" + placeholders + ")",
            entryCodes.toArray()
        );
    }

    private void deleteVotesOutsideImport(List<String> entryCodes) {
        if (entryCodes.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(entryCodes.size(), "?"));
        jdbcTemplate.update(
            "DELETE FROM gun_code_vote WHERE entry_code NOT IN (" + placeholders + ")",
            entryCodes.toArray()
        );
    }

    private void deleteGroupsOutsideImport(List<String> groupKeys) {
        if (groupKeys.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", Collections.nCopies(groupKeys.size(), "?"));
        jdbcTemplate.update(
            "DELETE FROM gun_code_group WHERE group_key NOT IN (" + placeholders + ")",
            groupKeys.toArray()
        );
    }

    private String buildGroupKey(String creator, String source, List<String> badges) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest((creator + "|" + source + "|" + joinText(badges)).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder("gc_");
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.substring(0, 27);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "改枪码分组生成失败");
        }
    }

    private String resolveGroupKey(String creator, String source, List<String> badges) {
        String existing = findExistingGroupKey(creator, source, badges);
        return StringUtils.hasText(existing) ? existing : buildGroupKey(creator, source, badges);
    }

    private String findExistingGroupKey(String creator, String source, List<String> badges) {
        List<String> rows = jdbcTemplate.query(
            "SELECT group_key FROM gun_code_group WHERE creator = ? AND COALESCE(source, '') = ? AND COALESCE(badges_text, '') = ? ORDER BY id ASC LIMIT 1",
            new Object[] { creator, source, joinText(badges) },
            (rs, rowNum) -> rs.getString("group_key")
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String joinText(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(",", values);
    }

    private int countEntries(List<GunCodeGroup> groups) {
        int count = 0;
        for (GunCodeGroup group : groups) {
            count = count + group.getEntries().size();
        }
        return count;
    }

    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return timestamp.toLocalDateTime().toString().replace('T', ' ');
    }

    private String nullableText(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }

    public static class GunCodePayload {
        private final List<String> categories;
        private final List<String> tags;
        private final List<GunCodeGroup> groups;

        public GunCodePayload(List<String> categories, List<String> tags, List<GunCodeGroup> groups) {
            this.categories = categories;
            this.tags = tags;
            this.groups = groups;
        }

        public List<String> getCategories() {
            return categories;
        }

        public List<String> getTags() {
            return tags;
        }

        public List<GunCodeGroup> getGroups() {
            return groups;
        }
    }

    public static class VoteResult {
        private final String entryCode;
        private final int likes;
        private final int dislikes;
        private final String currentVote;

        public VoteResult(String entryCode, int likes, int dislikes, String currentVote) {
            this.entryCode = entryCode;
            this.likes = likes;
            this.dislikes = dislikes;
            this.currentVote = currentVote;
        }

        public String getEntryCode() {
            return entryCode;
        }

        public int getLikes() {
            return likes;
        }

        public int getDislikes() {
            return dislikes;
        }

        public String getCurrentVote() {
            return currentVote;
        }
    }

    public static class AdminGunCodeCenter {
        private final Map<String, Object> summary;
        private final List<AdminGunCodeGroupRow> rows;

        public AdminGunCodeCenter(Map<String, Object> summary, List<AdminGunCodeGroupRow> rows) {
            this.summary = summary;
            this.rows = rows;
        }

        public Map<String, Object> getSummary() {
            return summary;
        }

        public List<AdminGunCodeGroupRow> getRows() {
            return rows;
        }
    }

    public static class AdminGunCodeGroupRow {
        private final String groupKey;
        private final String creator;
        private final String source;
        private final List<String> badges;
        private final int entryCount;
        private final int sortNo;
        private final String updatedAt;

        public AdminGunCodeGroupRow(String groupKey, String creator, String source, List<String> badges, int entryCount, int sortNo, String updatedAt) {
            this.groupKey = groupKey;
            this.creator = creator;
            this.source = source;
            this.badges = badges;
            this.entryCount = entryCount;
            this.sortNo = sortNo;
            this.updatedAt = updatedAt;
        }

        public String getGroupKey() { return groupKey; }
        public String getCreator() { return creator; }
        public String getSource() { return source; }
        public List<String> getBadges() { return badges; }
        public int getEntryCount() { return entryCount; }
        public int getSortNo() { return sortNo; }
        public String getUpdatedAt() { return updatedAt; }
    }

    public static class GunCodeImportRow {
        private String creator;
        private String source;
        private List<String> badges;
        private String title;
        private String category;
        private String entryCode;
        private List<String> tags;
        private Integer groupSortNo;
        private Integer entrySortNo;

        public String getCreator() { return creator; }
        public void setCreator(String creator) { this.creator = creator; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public List<String> getBadges() { return badges; }
        public void setBadges(List<String> badges) { this.badges = badges; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getEntryCode() { return entryCode; }
        public void setEntryCode(String entryCode) { this.entryCode = entryCode; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public Integer getGroupSortNo() { return groupSortNo; }
        public void setGroupSortNo(Integer groupSortNo) { this.groupSortNo = groupSortNo; }
        public Integer getEntrySortNo() { return entrySortNo; }
        public void setEntrySortNo(Integer entrySortNo) { this.entrySortNo = entrySortNo; }
    }

    public static class AdminGunCodeImportResult {
        private final boolean replaceExisting;
        private final int importedRowCount;
        private final int insertedGroups;
        private final int updatedGroups;
        private final int insertedEntries;
        private final int updatedEntries;
        private final AdminGunCodeCenter center;

        public AdminGunCodeImportResult(
            boolean replaceExisting,
            int importedRowCount,
            int insertedGroups,
            int updatedGroups,
            int insertedEntries,
            int updatedEntries,
            AdminGunCodeCenter center
        ) {
            this.replaceExisting = replaceExisting;
            this.importedRowCount = importedRowCount;
            this.insertedGroups = insertedGroups;
            this.updatedGroups = updatedGroups;
            this.insertedEntries = insertedEntries;
            this.updatedEntries = updatedEntries;
            this.center = center;
        }

        public boolean isReplaceExisting() { return replaceExisting; }
        public int getImportedRowCount() { return importedRowCount; }
        public int getInsertedGroups() { return insertedGroups; }
        public int getUpdatedGroups() { return updatedGroups; }
        public int getInsertedEntries() { return insertedEntries; }
        public int getUpdatedEntries() { return updatedEntries; }
        public AdminGunCodeCenter getCenter() { return center; }
    }

    public static class GunCodeGroup {
        private final String id;
        private final String creator;
        private final String source;
        private final List<String> badges;
        private final List<GunCodeEntry> entries;

        public GunCodeGroup(String id, String creator, String source, List<String> badges, List<GunCodeEntry> entries) {
            this.id = id;
            this.creator = creator;
            this.source = source;
            this.badges = badges;
            this.entries = entries;
        }

        public String getId() {
            return id;
        }

        public String getCreator() {
            return creator;
        }

        public String getSource() {
            return source;
        }

        public List<String> getBadges() {
            return badges;
        }

        public List<GunCodeEntry> getEntries() {
            return entries;
        }
    }

    public static class GunCodeEntry {
        private final String code;
        private final String title;
        private final String category;
        private final int likes;
        private final int dislikes;
        private final List<String> tags;

        public GunCodeEntry(String code, String title, String category, int likes, int dislikes, List<String> tags) {
            this.code = code;
            this.title = title;
            this.category = category;
            this.likes = likes;
            this.dislikes = dislikes;
            this.tags = tags;
        }

        public String getCode() {
            return code;
        }

        public String getTitle() {
            return title;
        }

        public String getCategory() {
            return category;
        }

        public int getLikes() {
            return likes;
        }

        public int getDislikes() {
            return dislikes;
        }

        public List<String> getTags() {
            return tags;
        }
    }

    private static class VoteCountSnapshot {
        private final String entryCode;
        private final int likes;
        private final int dislikes;

        private VoteCountSnapshot(String entryCode, int likes, int dislikes) {
            this.entryCode = entryCode;
            this.likes = likes;
            this.dislikes = dislikes;
        }

        public String getEntryCode() {
            return entryCode;
        }

        public int getLikes() {
            return likes;
        }

        public int getDislikes() {
            return dislikes;
        }
    }

    private static class ExistingVote {
        private final Long id;
        private final String voteType;

        private ExistingVote(Long id, String voteType) {
            this.id = id;
            this.voteType = voteType;
        }

        public Long getId() {
            return id;
        }

        public String getVoteType() {
            return voteType;
        }
    }

    private static class GunCodeEntryRow {
        private final String groupKey;
        private final GunCodeEntry entry;

        private GunCodeEntryRow(String groupKey, GunCodeEntry entry) {
            this.groupKey = groupKey;
            this.entry = entry;
        }

        public String getGroupKey() {
            return groupKey;
        }

        public GunCodeEntry getEntry() {
            return entry;
        }
    }

    private static class NormalizedGunCodeGroup {
        private final String groupKey;
        private final String creator;
        private final String source;
        private final List<String> badges;
        private final int sortNo;
        private final List<NormalizedGunCodeRow> entries = new ArrayList<NormalizedGunCodeRow>();

        private NormalizedGunCodeGroup(String groupKey, String creator, String source, List<String> badges, int sortNo) {
            this.groupKey = groupKey;
            this.creator = creator;
            this.source = source;
            this.badges = badges;
            this.sortNo = sortNo;
        }

        public String getGroupKey() { return groupKey; }
        public String getCreator() { return creator; }
        public String getSource() { return source; }
        public List<String> getBadges() { return badges; }
        public int getSortNo() { return sortNo; }
        public List<NormalizedGunCodeRow> getEntries() { return entries; }
    }

    private static class NormalizedGunCodeRow {
        private final String groupKey;
        private final String creator;
        private final String source;
        private final List<String> badges;
        private final int groupSortNo;
        private final String title;
        private final String category;
        private final String entryCode;
        private final List<String> tags;
        private final int entrySortNo;

        private NormalizedGunCodeRow(
            String groupKey,
            String creator,
            String source,
            List<String> badges,
            int groupSortNo,
            String title,
            String category,
            String entryCode,
            List<String> tags,
            int entrySortNo
        ) {
            this.groupKey = groupKey;
            this.creator = creator;
            this.source = source;
            this.badges = badges;
            this.groupSortNo = groupSortNo;
            this.title = title;
            this.category = category;
            this.entryCode = entryCode;
            this.tags = tags;
            this.entrySortNo = entrySortNo;
        }

        public String getGroupKey() { return groupKey; }
        public String getCreator() { return creator; }
        public String getSource() { return source; }
        public List<String> getBadges() { return badges; }
        public int getGroupSortNo() { return groupSortNo; }
        public String getTitle() { return title; }
        public String getCategory() { return category; }
        public String getEntryCode() { return entryCode; }
        public List<String> getTags() { return tags; }
        public int getEntrySortNo() { return entrySortNo; }
    }

    private enum VoteType {
        LIKE,
        DISLIKE
    }
}
