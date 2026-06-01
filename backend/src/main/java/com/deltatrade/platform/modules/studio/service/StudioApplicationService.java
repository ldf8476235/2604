package com.deltatrade.platform.modules.studio.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.auth.mapper.AuthUserMapper;
import com.deltatrade.platform.modules.auth.model.AuthUserDO;
import com.deltatrade.platform.modules.listing.mapper.StudioProfileMapper;
import com.deltatrade.platform.modules.listing.model.StudioProfileDO;
import com.deltatrade.platform.modules.oss.service.OssStorageService;
import com.deltatrade.platform.modules.profile.mapper.UserMessageMapper;
import com.deltatrade.platform.modules.profile.model.UserMessageDO;
import com.deltatrade.platform.modules.studio.mapper.StudioApplicationMapper;
import com.deltatrade.platform.modules.studio.model.StudioApplicationDO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StudioApplicationService {

    private static final Logger log = LoggerFactory.getLogger(StudioApplicationService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BigDecimal DEFAULT_SHARE_RATIO = new BigDecimal("0.7000");

    private final StudioApplicationMapper studioApplicationMapper;
    private final StudioProfileMapper studioProfileMapper;
    private final AuthUserMapper authUserMapper;
    private final UserMessageMapper userMessageMapper;
    private final OssStorageService ossStorageService;

    public StudioApplicationService(
        StudioApplicationMapper studioApplicationMapper,
        StudioProfileMapper studioProfileMapper,
        AuthUserMapper authUserMapper,
        UserMessageMapper userMessageMapper,
        OssStorageService ossStorageService
    ) {
        this.studioApplicationMapper = studioApplicationMapper;
        this.studioProfileMapper = studioProfileMapper;
        this.authUserMapper = authUserMapper;
        this.userMessageMapper = userMessageMapper;
        this.ossStorageService = ossStorageService;
    }

    @Transactional
    public StudioApplicationView loadMyApplication(Long userId) {
        AuthUserDO user = requireUser(userId);
        StudioProfileDO studio = findStudioProfile(userId);
        StudioApplicationDO latest = findLatestApplication(userId);
        if (latest == null && studio != null) {
            return buildApprovedViewFromStudio(user, studio);
        }
        if (latest == null) {
            return StudioApplicationView.empty(user);
        }
        StudioProfileDO linkedStudio = latest.getStudioId() == null ? studio : studioProfileMapper.selectById(latest.getStudioId());
        return toUserView(latest, user, linkedStudio);
    }

    @Transactional
    public StudioApplicationView submitApplication(
        Long userId,
        String studioName,
        String qualificationCode,
        String qualificationNote,
        String contactName,
        String contactPhone,
        String qualificationMaterialKey
    ) {
        AuthUserDO user = requireUser(userId);
        if (findStudioProfile(userId) != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号已经开通工作室身份，无需重复申请");
        }
        StudioApplicationDO pending = studioApplicationMapper.selectOne(
            Wrappers.<StudioApplicationDO>lambdaQuery()
                .eq(StudioApplicationDO::getApplicantUserId, userId)
                .eq(StudioApplicationDO::getStatus, "PENDING")
                .orderByDesc(StudioApplicationDO::getCreatedAt)
                .last("LIMIT 1")
        );
        if (pending != null) {
            log.warn("studio application duplicate pending userId={} applicationNo={}", userId, pending.getApplicationNo());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "你已有待审核的工作室申请，请等待管理员处理");
        }
        LocalDateTime now = LocalDateTime.now();
        StudioApplicationDO row = new StudioApplicationDO();
        row.setApplicationNo(buildSerial("SA"));
        row.setApplicantUserId(userId);
        row.setStudioName(normalizeRequired(studioName, "工作室名称不能为空"));
        row.setQualificationCode(normalizeRequired(qualificationCode, "统一社会信用代码或主体说明不能为空"));
        row.setQualificationNote(StringUtils.hasText(qualificationNote) ? qualificationNote.trim() : null);
        row.setContactName(normalizeRequired(contactName, "联系人不能为空"));
        row.setContactPhone(normalizeRequired(contactPhone, "联系电话不能为空"));
        row.setQualificationMaterialKey(normalizeRequired(qualificationMaterialKey, "请上传营业执照或证明材料"));
        row.setStatus("PENDING");
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        insertApplication(row);
        createMessage(userId, "SYSTEM", "工作室申请已提交", "你的工作室入驻申请 " + row.getApplicationNo() + " 已提交，等待运营或超管审核。");
        log.info("studio application submitted userId={} applicationNo={} studioName={} contactPhone={}",
            userId, row.getApplicationNo(), row.getStudioName(), maskPhone(row.getContactPhone()));
        return toUserView(row, user, null);
    }

    @Transactional
    public Map<String, Object> loadAdminApplications(String status, String keyword) {
        long startAt = System.currentTimeMillis();
        List<StudioApplicationDO> rows = studioApplicationMapper.selectList(
            Wrappers.<StudioApplicationDO>lambdaQuery()
                .eq(StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status), StudioApplicationDO::getStatus, status.trim().toUpperCase(Locale.ROOT))
                .orderByDesc(StudioApplicationDO::getCreatedAt)
                .last("LIMIT 120")
        );
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        int pending = 0;
        int approved = 0;
        int rejected = 0;
        for (StudioApplicationDO row : rows) {
            if ("PENDING".equals(row.getStatus())) {
                pending++;
            } else if ("APPROVED".equals(row.getStatus())) {
                approved++;
            } else if ("REJECTED".equals(row.getStatus())) {
                rejected++;
            }
            AuthUserDO applicant = authUserMapper.selectById(row.getApplicantUserId());
            if (normalizedKeyword != null) {
                String haystack = (
                    defaultText(row.getApplicationNo(), "") + "|" +
                    defaultText(row.getStudioName(), "") + "|" +
                    defaultText(applicant == null ? null : applicant.getNickname(), "") + "|" +
                    defaultText(row.getContactName(), "") + "|" +
                    defaultText(row.getContactPhone(), "")
                ).toLowerCase(Locale.ROOT);
                if (!haystack.contains(normalizedKeyword)) {
                    continue;
                }
            }
            items.add(mapOf(
                "applicationNo", row.getApplicationNo(),
                "status", row.getStatus(),
                "statusText", renderStatus(row.getStatus()),
                "studioId", row.getStudioId(),
                "studioName", row.getStudioName(),
                "applicantUserId", row.getApplicantUserId(),
                "applicantNickname", applicant == null ? "-" : defaultText(applicant.getNickname(), "-"),
                "applicantPhone", applicant == null ? "-" : maskPhone(applicant.getPhone()),
                "applicantPhoneRaw", applicant == null ? "" : defaultText(applicant.getPhone(), ""),
                "contactName", defaultText(row.getContactName(), "-"),
                "contactPhone", defaultText(row.getContactPhone(), "-"),
                "qualificationCode", defaultText(row.getQualificationCode(), "-"),
                "qualificationNote", defaultText(row.getQualificationNote(), ""),
                "qualificationMaterialKey", defaultText(row.getQualificationMaterialKey(), ""),
                "qualificationMaterialUrl", previewNullable(row.getQualificationMaterialKey()),
                "rejectReason", defaultText(row.getRejectReason(), ""),
                "createdAt", formatTime(row.getCreatedAt()),
                "reviewedAt", formatTime(row.getReviewedAt())
            ));
        }
        log.info("admin studio applications loaded status={} keyword={} count={} costMs={}",
            status, maskKeyword(keyword), items.size(), System.currentTimeMillis() - startAt);
        return mapOf("rows", items, "summary", mapOf("pending", pending, "approved", approved, "rejected", rejected));
    }

    @Transactional
    public Map<String, Object> reviewApplication(String applicationNo, String action, String reason, Long reviewerUserId) {
        StudioApplicationDO application = requireApplication(applicationNo);
        if (!"PENDING".equals(application.getStatus())) {
            log.warn("studio application review skipped applicationNo={} status={} reviewerUserId={}",
                applicationNo, application.getStatus(), reviewerUserId);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅待审核申请支持审核操作");
        }
        AuthUserDO applicant = requireUser(application.getApplicantUserId());
        String normalizedAction = normalizeAction(action);
        LocalDateTime now = LocalDateTime.now();
        application.setReviewedByUserId(reviewerUserId);
        application.setReviewedAt(now);
        application.setUpdatedAt(now);
        if ("REJECT".equals(normalizedAction)) {
            application.setStatus("REJECTED");
            application.setRejectReason(normalizeRequired(reason, "驳回原因不能为空"));
            updateApplication(application);
            createMessage(applicant.getId(), "SYSTEM", "工作室申请已驳回",
                "你的工作室入驻申请 " + application.getApplicationNo() + " 已被驳回，原因：" + application.getRejectReason());
            log.info("studio application rejected applicationNo={} reviewerUserId={} applicantUserId={} reason={}",
                applicationNo, reviewerUserId, applicant.getId(), application.getRejectReason());
            return mapOf("applicationNo", applicationNo, "status", "REJECTED", "message", "申请已驳回");
        }

        StudioProfileDO studio = findStudioProfile(applicant.getId());
        if (studio == null) {
            studio = new StudioProfileDO();
            studio.setOwnerUserId(applicant.getId());
            studio.setCreatedAt(now);
        }
        studio.setStudioName(application.getStudioName());
        studio.setDescription(defaultText(application.getQualificationNote(), null));
        studio.setContactPhone(application.getContactPhone());
        studio.setContactName(application.getContactName());
        studio.setQualificationCode(application.getQualificationCode());
        studio.setQualificationMaterialKey(application.getQualificationMaterialKey());
        studio.setQualificationNote(application.getQualificationNote());
        studio.setReviewStrategy(defaultText(studio.getReviewStrategy(), "REVIEW_REQUIRED"));
        studio.setShareRatio(studio.getShareRatio() == null ? DEFAULT_SHARE_RATIO : studio.getShareRatio());
        studio.setActive(Boolean.TRUE);
        studio.setCooperationStatus("ACTIVE");
        studio.setUpdatedAt(now);
        saveStudioProfile(studio);

        application.setStudioId(studio.getId());
        application.setStatus("APPROVED");
        application.setRejectReason(null);
        updateApplication(application);
        createMessage(applicant.getId(), "SYSTEM", "工作室申请审核通过",
            "你的工作室入驻申请 " + application.getApplicationNo() + " 已通过审核，工作室后台权限已开通。");
        log.info("studio application approved applicationNo={} reviewerUserId={} applicantUserId={} studioId={}",
            applicationNo, reviewerUserId, applicant.getId(), studio.getId());
        return mapOf("applicationNo", applicationNo, "status", "APPROVED", "studioId", studio.getId(), "message", "申请已通过并开通工作室权限");
    }

    private StudioApplicationView toUserView(StudioApplicationDO row, AuthUserDO user, StudioProfileDO studio) {
        return new StudioApplicationView(
            row.getApplicationNo(),
            row.getStatus(),
            renderStatus(row.getStatus()),
            defaultText(row.getRejectReason(), ""),
            row.getApplicantUserId(),
            defaultText(user.getNickname(), "-"),
            defaultText(user.getPhone(), ""),
            row.getStudioId(),
            row.getStudioName(),
            row.getQualificationCode(),
            defaultText(row.getQualificationNote(), ""),
            row.getContactName(),
            row.getContactPhone(),
            defaultText(row.getQualificationMaterialKey(), ""),
            previewNullable(row.getQualificationMaterialKey()),
            formatTime(row.getCreatedAt()),
            formatTime(row.getReviewedAt()),
            studio != null,
            studio == null ? "" : defaultText(studio.getReviewStrategy(), "REVIEW_REQUIRED"),
            studio == null ? "" : defaultText(studio.getCooperationStatus(), "ACTIVE")
        );
    }

    private StudioApplicationView buildApprovedViewFromStudio(AuthUserDO user, StudioProfileDO studio) {
        return new StudioApplicationView(
            "DIRECT-STUDIO-" + studio.getId(),
            "APPROVED",
            renderStatus("APPROVED"),
            "",
            user.getId(),
            defaultText(user.getNickname(), "-"),
            defaultText(user.getPhone(), ""),
            studio.getId(),
            defaultText(studio.getStudioName(), "-"),
            defaultText(studio.getQualificationCode(), ""),
            defaultText(studio.getQualificationNote(), ""),
            defaultText(studio.getContactName(), ""),
            defaultText(studio.getContactPhone(), ""),
            defaultText(studio.getQualificationMaterialKey(), ""),
            previewNullable(studio.getQualificationMaterialKey()),
            formatTime(studio.getCreatedAt()),
            formatTime(studio.getUpdatedAt()),
            true,
            defaultText(studio.getReviewStrategy(), "REVIEW_REQUIRED"),
            defaultText(studio.getCooperationStatus(), "ACTIVE")
        );
    }

    private StudioApplicationDO findLatestApplication(Long userId) {
        long startAt = System.currentTimeMillis();
        StudioApplicationDO row = studioApplicationMapper.selectOne(
            Wrappers.<StudioApplicationDO>lambdaQuery()
                .eq(StudioApplicationDO::getApplicantUserId, userId)
                .orderByDesc(StudioApplicationDO::getCreatedAt)
                .last("LIMIT 1")
        );
        log.info("mysql query success target=studio_application_latest_by_user_id costMs={} hit={} userId={}",
            System.currentTimeMillis() - startAt, row != null, userId);
        return row;
    }

    private StudioProfileDO findStudioProfile(Long userId) {
        long startAt = System.currentTimeMillis();
        StudioProfileDO studio = studioProfileMapper.selectOne(
            Wrappers.<StudioProfileDO>lambdaQuery().eq(StudioProfileDO::getOwnerUserId, userId).last("LIMIT 1")
        );
        log.info("mysql query success target=studio_profile_by_owner_user_id costMs={} hit={} userId={}",
            System.currentTimeMillis() - startAt, studio != null, userId);
        return studio;
    }

    private StudioApplicationDO requireApplication(String applicationNo) {
        long startAt = System.currentTimeMillis();
        StudioApplicationDO row = studioApplicationMapper.selectOne(
            Wrappers.<StudioApplicationDO>lambdaQuery().eq(StudioApplicationDO::getApplicationNo, applicationNo).last("LIMIT 1")
        );
        log.info("mysql query success target=studio_application_by_application_no costMs={} hit={} applicationNo={}",
            System.currentTimeMillis() - startAt, row != null, applicationNo);
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "工作室申请不存在");
        }
        return row;
    }

    private AuthUserDO requireUser(Long userId) {
        long startAt = System.currentTimeMillis();
        AuthUserDO user = authUserMapper.selectById(userId);
        log.info("mysql query success target=auth_user_by_id costMs={} hit={} userId={}",
            System.currentTimeMillis() - startAt, user != null, userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前用户不存在");
        }
        return user;
    }

    private void saveStudioProfile(StudioProfileDO studio) {
        long startAt = System.currentTimeMillis();
        if (studio.getId() == null) {
            int rows = studioProfileMapper.insert(studio);
            log.info("mysql insert success target=studio_profile costMs={} rows={} ownerUserId={} studioId={}",
                System.currentTimeMillis() - startAt, rows, studio.getOwnerUserId(), studio.getId());
            return;
        }
        int rows = studioProfileMapper.updateById(studio);
        log.info("mysql update success target=studio_profile costMs={} rows={} ownerUserId={} studioId={}",
            System.currentTimeMillis() - startAt, rows, studio.getOwnerUserId(), studio.getId());
    }

    private void insertApplication(StudioApplicationDO application) {
        long startAt = System.currentTimeMillis();
        int rows = studioApplicationMapper.insert(application);
        log.info("mysql insert success target=studio_application costMs={} rows={} applicationNo={} applicantUserId={}",
            System.currentTimeMillis() - startAt, rows, application.getApplicationNo(), application.getApplicantUserId());
    }

    private void updateApplication(StudioApplicationDO application) {
        long startAt = System.currentTimeMillis();
        int rows = studioApplicationMapper.updateById(application);
        log.info("mysql update success target=studio_application costMs={} rows={} applicationNo={} status={}",
            System.currentTimeMillis() - startAt, rows, application.getApplicationNo(), application.getStatus());
    }

    private void createMessage(Long userId, String category, String title, String content) {
        UserMessageDO message = new UserMessageDO();
        LocalDateTime now = LocalDateTime.now();
        message.setUserId(userId);
        message.setCategory(category);
        message.setTitle(title);
        message.setContent(content);
        message.setReadFlag(false);
        message.setDeletedFlag(false);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        long startAt = System.currentTimeMillis();
        int rows = userMessageMapper.insert(message);
        log.info("mysql insert success target=user_message costMs={} rows={} userId={} category={}",
            System.currentTimeMillis() - startAt, rows, userId, category);
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
        return value.trim();
    }

    private String normalizeAction(String action) {
        String normalized = normalizeRequired(action, "审核动作不能为空").toUpperCase(Locale.ROOT);
        if (!"APPROVE".equals(normalized) && !"REJECT".equals(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不支持的审核动作");
        }
        return normalized;
    }

    private String renderStatus(String status) {
        if ("APPROVED".equals(status)) {
            return "已通过";
        }
        if ("REJECTED".equals(status)) {
            return "已驳回";
        }
        if ("NONE".equals(status)) {
            return "未提交";
        }
        return "待审核";
    }

    private String previewNullable(String objectKey) {
        return StringUtils.hasText(objectKey) ? ossStorageService.previewUrl(objectKey.trim()) : null;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMATTER);
    }

    private String buildSerial(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
            UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase(Locale.ROOT);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String maskKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        String text = keyword.trim();
        if (text.length() <= 2) {
            return "**";
        }
        return text.substring(0, 1) + "**" + text.substring(text.length() - 1);
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.trim().length() < 7) {
            return defaultText(phone, "");
        }
        String text = phone.trim();
        return text.substring(0, 3) + "****" + text.substring(text.length() - 4);
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < pairs.length; index += 2) {
            result.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return result;
    }

    public static class StudioApplicationView {
        private final String applicationNo;
        private final String status;
        private final String statusText;
        private final String rejectReason;
        private final Long applicantUserId;
        private final String applicantNickname;
        private final String applicantPhone;
        private final Long studioId;
        private final String studioName;
        private final String qualificationCode;
        private final String qualificationNote;
        private final String contactName;
        private final String contactPhone;
        private final String qualificationMaterialKey;
        private final String qualificationMaterialUrl;
        private final String createdAt;
        private final String reviewedAt;
        private final boolean hasStudioAccess;
        private final String reviewStrategy;
        private final String cooperationStatus;

        public StudioApplicationView(
            String applicationNo,
            String status,
            String statusText,
            String rejectReason,
            Long applicantUserId,
            String applicantNickname,
            String applicantPhone,
            Long studioId,
            String studioName,
            String qualificationCode,
            String qualificationNote,
            String contactName,
            String contactPhone,
            String qualificationMaterialKey,
            String qualificationMaterialUrl,
            String createdAt,
            String reviewedAt,
            boolean hasStudioAccess,
            String reviewStrategy,
            String cooperationStatus
        ) {
            this.applicationNo = applicationNo;
            this.status = status;
            this.statusText = statusText;
            this.rejectReason = rejectReason;
            this.applicantUserId = applicantUserId;
            this.applicantNickname = applicantNickname;
            this.applicantPhone = applicantPhone;
            this.studioId = studioId;
            this.studioName = studioName;
            this.qualificationCode = qualificationCode;
            this.qualificationNote = qualificationNote;
            this.contactName = contactName;
            this.contactPhone = contactPhone;
            this.qualificationMaterialKey = qualificationMaterialKey;
            this.qualificationMaterialUrl = qualificationMaterialUrl;
            this.createdAt = createdAt;
            this.reviewedAt = reviewedAt;
            this.hasStudioAccess = hasStudioAccess;
            this.reviewStrategy = reviewStrategy;
            this.cooperationStatus = cooperationStatus;
        }

        public static StudioApplicationView empty(AuthUserDO user) {
            return new StudioApplicationView("", "NONE", "未提交", "", user.getId(), user.getNickname(), user.getPhone(), null, "", "", "", "", "", "", null, "", "", false, "", "");
        }

        public String getApplicationNo() { return applicationNo; }
        public String getStatus() { return status; }
        public String getStatusText() { return statusText; }
        public String getRejectReason() { return rejectReason; }
        public Long getApplicantUserId() { return applicantUserId; }
        public String getApplicantNickname() { return applicantNickname; }
        public String getApplicantPhone() { return applicantPhone; }
        public Long getStudioId() { return studioId; }
        public String getStudioName() { return studioName; }
        public String getQualificationCode() { return qualificationCode; }
        public String getQualificationNote() { return qualificationNote; }
        public String getContactName() { return contactName; }
        public String getContactPhone() { return contactPhone; }
        public String getQualificationMaterialKey() { return qualificationMaterialKey; }
        public String getQualificationMaterialUrl() { return qualificationMaterialUrl; }
        public String getCreatedAt() { return createdAt; }
        public String getReviewedAt() { return reviewedAt; }
        public boolean isHasStudioAccess() { return hasStudioAccess; }
        public String getReviewStrategy() { return reviewStrategy; }
        public String getCooperationStatus() { return cooperationStatus; }
    }
}
