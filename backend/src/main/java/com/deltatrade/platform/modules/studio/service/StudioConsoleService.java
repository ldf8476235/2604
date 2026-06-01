package com.deltatrade.platform.modules.studio.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.auth.mapper.AuthUserMapper;
import com.deltatrade.platform.modules.auth.model.AuthUserDO;
import com.deltatrade.platform.modules.admin.mapper.OperationAnnouncementMapper;
import com.deltatrade.platform.modules.admin.model.OperationAnnouncementDO;
import com.deltatrade.platform.modules.boosting.mapper.BoostingServiceMapper;
import com.deltatrade.platform.modules.boosting.model.BoostingServiceDO;
import com.deltatrade.platform.modules.listing.mapper.AccountListingMapper;
import com.deltatrade.platform.modules.listing.mapper.StudioProfileMapper;
import com.deltatrade.platform.modules.listing.model.AccountListingDO;
import com.deltatrade.platform.modules.listing.model.StudioProfileDO;
import com.deltatrade.platform.modules.listing.service.ListingPublishService;
import com.deltatrade.platform.modules.order.mapper.TradeOrderMapper;
import com.deltatrade.platform.modules.order.model.TradeOrderDO;
import com.deltatrade.platform.modules.order.service.OrderService;
import com.deltatrade.platform.modules.oss.service.OssStorageService;
import com.deltatrade.platform.modules.profile.service.DistributionService;
import com.deltatrade.platform.modules.profile.mapper.WithdrawAccountMapper;
import com.deltatrade.platform.modules.profile.mapper.UserMessageMapper;
import com.deltatrade.platform.modules.profile.model.UserMessageDO;
import com.deltatrade.platform.modules.profile.model.WithdrawAccountDO;
import com.deltatrade.platform.modules.studio.mapper.StudioOperatorMapper;
import com.deltatrade.platform.modules.studio.mapper.StudioWithdrawApplicationMapper;
import com.deltatrade.platform.modules.studio.model.StudioOperatorDO;
import com.deltatrade.platform.modules.studio.model.StudioWithdrawApplicationDO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class StudioConsoleService {

    private static final Logger log = LoggerFactory.getLogger(StudioConsoleService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final BigDecimal DEFAULT_SHARE_RATIO = new BigDecimal("0.7000");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };

    private final StudioProfileMapper studioProfileMapper;
    private final AccountListingMapper accountListingMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final BoostingServiceMapper boostingServiceMapper;
    private final AuthUserMapper authUserMapper;
    private final OperationAnnouncementMapper operationAnnouncementMapper;
    private final ListingPublishService listingPublishService;
    private final WithdrawAccountMapper withdrawAccountMapper;
    private final UserMessageMapper userMessageMapper;
    private final StudioWithdrawApplicationMapper studioWithdrawApplicationMapper;
    private final StudioOperatorMapper studioOperatorMapper;
    private final JdbcTemplate jdbcTemplate;
    private final OssStorageService ossStorageService;
    private final ObjectMapper objectMapper;
    private final DistributionService distributionService;
    private final OrderService orderService;

    public StudioConsoleService(
        StudioProfileMapper studioProfileMapper,
        AccountListingMapper accountListingMapper,
        TradeOrderMapper tradeOrderMapper,
        BoostingServiceMapper boostingServiceMapper,
        AuthUserMapper authUserMapper,
        OperationAnnouncementMapper operationAnnouncementMapper,
        ListingPublishService listingPublishService,
        WithdrawAccountMapper withdrawAccountMapper,
        UserMessageMapper userMessageMapper,
        StudioWithdrawApplicationMapper studioWithdrawApplicationMapper,
        StudioOperatorMapper studioOperatorMapper,
        JdbcTemplate jdbcTemplate,
        OssStorageService ossStorageService,
        ObjectMapper objectMapper,
        DistributionService distributionService,
        OrderService orderService
    ) {
        this.studioProfileMapper = studioProfileMapper;
        this.accountListingMapper = accountListingMapper;
        this.tradeOrderMapper = tradeOrderMapper;
        this.boostingServiceMapper = boostingServiceMapper;
        this.authUserMapper = authUserMapper;
        this.operationAnnouncementMapper = operationAnnouncementMapper;
        this.listingPublishService = listingPublishService;
        this.withdrawAccountMapper = withdrawAccountMapper;
        this.userMessageMapper = userMessageMapper;
        this.studioWithdrawApplicationMapper = studioWithdrawApplicationMapper;
        this.studioOperatorMapper = studioOperatorMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.ossStorageService = ossStorageService;
        this.objectMapper = objectMapper;
        this.distributionService = distributionService;
        this.orderService = orderService;
    }

    public Map<String, Object> loadDashboard(Long userId) {
        long startAt = System.currentTimeMillis();
        StudioProfileDO studio = requireStudioProfile(userId);
        int onSaleCount = countListings(userId, "PUBLISHED");
        int pendingReviewCount = countListings(userId, "PENDING_REVIEW");
        int inProgressOrders = countStudioOrders(userId, Arrays.asList("WAITING_TRADE", "AFTER_SALE"));
        BigDecimal settledProfit = sumProfit(userId, Arrays.asList("COMPLETED"));
        BigDecimal pendingProfit = sumProfit(userId, Arrays.asList("WAITING_TRADE", "AFTER_SALE"));
        BigDecimal monthGmv = sumMonthlyGmv(userId);
        List<Map<String, Object>> recentListings = loadRecentListings(userId, 5);
        List<Map<String, Object>> recentOrders = loadRecentOrders(userId, 5);
        List<Map<String, Object>> studioServices = loadStudioBoostingServices(studio.getStudioName(), 4);
        List<Map<String, Object>> announcements = loadPublishedAnnouncements(4);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("metrics", Arrays.asList(
            metric("在售账号", String.valueOf(onSaleCount), "待审核 " + pendingReviewCount + " 条"),
            metric("进行中订单", String.valueOf(inProgressOrders), "待交接 / 售后处理中"),
            metric("待结算分润", formatMoney(pendingProfit), "按当前进行中订单估算"),
            metric("已结算分润", formatMoney(settledProfit), "按已完成订单统计"),
            metric("本月成交额", formatMoney(monthGmv), "工作室当前月份 GMV")
        ));
        result.put("profile", mapOf(
            "studioId", studio.getId(),
            "studioName", studio.getStudioName(),
            "ownerUserId", studio.getOwnerUserId(),
            "reviewStrategy", studio.getReviewStrategy(),
            "reviewStrategyText", renderReviewStrategy(studio.getReviewStrategy()),
            "shareRatio", renderShareRatio(studio.getShareRatio()),
            "active", studio.getActive(),
            "activeText", renderCooperationStatus(studio.getCooperationStatus(), studio.getActive())
        ));
        result.put("recentListings", recentListings);
        result.put("recentOrders", recentOrders);
        result.put("boostingServices", studioServices);
        result.put("announcements", announcements);
        log.info(
            "studio dashboard loaded userId={} studioId={} onSale={} orders={} announcements={} costMs={}",
            userId,
            studio.getId(),
            onSaleCount,
            inProgressOrders,
            announcements.size(),
            System.currentTimeMillis() - startAt
        );
        return result;
    }

    public Map<String, Object> loadListings(Long userId, String status) {
        long startAt = System.currentTimeMillis();
        StudioProfileDO studio = requireStudioProfile(userId);
        List<AccountListingDO> rows = accountListingMapper.selectList(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getSellerUserId, userId)
                .eq(StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status), AccountListingDO::getStatus, status)
                .orderByDesc(AccountListingDO::getUpdatedAt)
                .last("LIMIT 120")
        );
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (AccountListingDO row : rows) {
            items.add(mapOf(
                "listingNo", row.getListingNo(),
                "title", row.getTitle(),
                "status", row.getStatus(),
                "statusText", renderListingStatus(row.getStatus()),
                "reviewProgress", renderReviewProgress(row.getStatus()),
                "price", safeMoney(row.getPrice()),
                "viewCount", safeNumber(row.getViewCount()),
                "favoriteCount", safeNumber(row.getFavoriteCount()),
                "salesCount", safeNumber(row.getSalesCount()),
                "cityName", defaultText(row.getCityName(), "-"),
                "coverUrl", previewNullable(row.getCoverImageKey()),
                "updatedAt", formatTime(row.getUpdatedAt()),
                "rejectionReason", defaultText(row.getRejectionReason(), ""),
                "canEdit", canEdit(row.getStatus()),
                "canWithdraw", "PUBLISHED".equals(row.getStatus()),
                "canResubmit", "REJECTED".equals(row.getStatus())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("strategy", mapOf(
            "reviewStrategy", studio.getReviewStrategy(),
            "reviewStrategyText", renderReviewStrategy(studio.getReviewStrategy()),
            "strategyTip", "DIRECT_PUBLISH".equals(studio.getReviewStrategy())
                ? "你的工作室为免审直发，提交后将直接上架"
                : "你的工作室当前需要平台审核，提交后会进入待审核队列"
        ));
        result.put("rows", items);
        result.put("summary", mapOf(
            "published", countListings(rows, "PUBLISHED"),
            "pendingReview", countListings(rows, "PENDING_REVIEW"),
            "rejected", countListings(rows, "REJECTED"),
            "offline", countListings(rows, "OFFLINE")
        ));
        log.info("studio listings loaded userId={} status={} count={} costMs={}", userId, status, items.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    public Map<String, Object> loadListingDetail(Long userId, String listingNo) {
        long startAt = System.currentTimeMillis();
        AccountListingDO row = requireOwnerListing(userId, listingNo);
        List<TradeOrderDO> orders = tradeOrderMapper.selectList(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getListingNo, listingNo)
                .orderByDesc(TradeOrderDO::getCreatedAt)
                .last("LIMIT 20")
        );
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        for (TradeOrderDO order : orders) {
            records.add(mapOf(
                "orderNo", order.getOrderNo(),
                "buyerNickname", order.getBuyerNickname(),
                "status", renderOrderStatus(order.getStatus()),
                "totalAmount", safeMoney(order.getTotalAmount()),
                "createdAt", formatTime(order.getCreatedAt()),
                "completedAt", formatTime(order.getCompletedAt())
            ));
        }
        Map<String, Object> detail = new LinkedHashMap<String, Object>();
        detail.put("summary", mapOf(
            "listingNo", row.getListingNo(),
            "title", row.getTitle(),
            "status", row.getStatus(),
            "statusText", renderListingStatus(row.getStatus()),
            "price", safeMoney(row.getPrice()),
            "coverUrl", previewNullable(row.getCoverImageKey()),
            "imageUrls", buildImageUrls(row.getImageKeysJson()),
            "videoUrl", previewNullable(row.getVideoKey()),
            "description", defaultText(row.getDescription(), "-"),
            "estimateDetail", defaultText(row.getEstimateDetail(), "-"),
            "updatedAt", formatTime(row.getUpdatedAt()),
            "submittedAt", formatTime(row.getSubmittedAt()),
            "publishedAt", formatTime(row.getPublishedAt()),
            "rejectionReason", defaultText(row.getRejectionReason(), "")
        ));
        detail.put("reviewRecords", buildReviewRecords(row));
        detail.put("tradeRecords", records);
        log.info("studio listing detail loaded userId={} listingNo={} tradeCount={} costMs={}", userId, listingNo, records.size(), System.currentTimeMillis() - startAt);
        return detail;
    }

    public ListingPublishService.ActionResult withdrawListing(com.deltatrade.platform.common.auth.AuthPrincipal principal, String listingNo) {
        return listingPublishService.withdrawMyListing(principal, listingNo);
    }

    public ListingPublishService.ActionResult resubmitListing(com.deltatrade.platform.common.auth.AuthPrincipal principal, String listingNo) {
        return listingPublishService.resubmitMyListing(principal, listingNo);
    }

    public Map<String, Object> loadOrders(Long userId, String status) {
        long startAt = System.currentTimeMillis();
        List<TradeOrderDO> rows = tradeOrderMapper.selectList(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getSellerUserId, userId)
                .eq(StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status), TradeOrderDO::getStatus, status)
                .orderByDesc(TradeOrderDO::getCreatedAt)
                .last("LIMIT 120")
        );
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (TradeOrderDO row : rows) {
            items.add(mapOf(
                "orderNo", row.getOrderNo(),
                "listingNo", defaultText(row.getListingNo(), "-"),
                "listingTitle", row.getListingTitle(),
                "buyerNickname", row.getBuyerNickname(),
                "status", row.getStatus(),
                "statusText", renderOrderStatus(row.getStatus()),
                "paymentMethod", renderPaymentMethod(row.getPaymentMethod()),
                "itemAmount", safeMoney(row.getItemAmount()),
                "serviceFee", safeMoney(row.getServiceFee()),
                "totalAmount", safeMoney(row.getTotalAmount()),
                "chatGroupNo", defaultText(row.getChatGroupNo(), "-"),
                "createdAt", formatTime(row.getCreatedAt()),
                "paidAt", formatTime(row.getPaidAt()),
                "completedAt", formatTime(row.getCompletedAt()),
                "refundAmount", safeMoney(row.getRefundAmount()),
                "refundRequestedAt", formatTime(row.getRefundRequestedAt()),
                "canReviewRefund", "REFUND_PENDING".equals(row.getStatus())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rows", items);
        result.put("summary", mapOf(
            "waitingTrade", countOrders(rows, "WAITING_TRADE"),
            "completed", countOrders(rows, "COMPLETED"),
            "refundPending", countOrders(rows, "REFUND_PENDING"),
            "afterSale", countOrders(rows, "AFTER_SALE"),
            "refunded", countOrders(rows, "REFUNDED"),
            "closed", countOrders(rows, "CLOSED")
        ));
        log.info("studio orders loaded userId={} status={} count={} costMs={}", userId, status, items.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    public Map<String, Object> loadOrderDetail(Long userId, String orderNo) {
        long startAt = System.currentTimeMillis();
        TradeOrderDO row = requireStudioOrder(userId, orderNo);
        Map<String, Object> result = mapOf(
            "orderNo", row.getOrderNo(),
            "listingNo", defaultText(row.getListingNo(), "-"),
            "listingTitle", row.getListingTitle(),
            "listingSummary", defaultText(row.getListingSummary(), "-"),
            "listingCoverUrl", previewNullable(row.getListingCoverKey()),
            "buyerNickname", row.getBuyerNickname(),
            "sellerDisplayName", row.getSellerDisplayName(),
            "sellerTypeText", renderSellerType(row.getSellerType()),
            "status", row.getStatus(),
            "statusText", renderOrderStatus(row.getStatus()),
            "paymentMethod", renderPaymentMethod(row.getPaymentMethod()),
            "itemAmount", safeMoney(row.getItemAmount()),
            "serviceFee", safeMoney(row.getServiceFee()),
            "totalAmount", safeMoney(row.getTotalAmount()),
            "chatGroupNo", defaultText(row.getChatGroupNo(), "-"),
            "createdAt", formatTime(row.getCreatedAt()),
            "paidAt", formatTime(row.getPaidAt()),
            "completedAt", formatTime(row.getCompletedAt()),
            "afterSaleAt", formatTime(row.getAfterSaleAt()),
            "afterSaleNote", defaultText(row.getAfterSaleNote(), ""),
            "afterSaleProofKey", defaultText(row.getAfterSaleProofKey(), ""),
            "afterSaleProofUrl", previewNullable(row.getAfterSaleProofKey()),
            "afterSaleHandledAt", formatTime(row.getAfterSaleHandledAt()),
            "refundRequestedAt", formatTime(row.getRefundRequestedAt()),
            "refundReviewedAt", formatTime(row.getRefundReviewedAt()),
            "refundedAt", formatTime(row.getRefundedAt()),
            "refundAmount", safeMoney(row.getRefundAmount()),
            "refundReason", defaultText(row.getRefundReason(), ""),
            "refundReviewNote", defaultText(row.getRefundReviewNote(), ""),
            "canReviewRefund", "REFUND_PENDING".equals(row.getStatus())
        );
        log.info("studio order detail loaded userId={} orderNo={} status={} costMs={}",
            userId, orderNo, row.getStatus(), System.currentTimeMillis() - startAt);
        return result;
    }

    @Transactional
    public Map<String, Object> handleAfterSale(Long userId, String orderNo, String action, String note, String proofKey) {
        TradeOrderDO row = requireStudioOrder(userId, orderNo);
        if (!"AFTER_SALE".equals(row.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅售后中的订单可处理");
        }
        String normalizedAction = normalizeAction(action, Arrays.asList("RESOLVE", "CLOSE"));
        String normalizedNote = normalizeRequired(note, "处理备注不能为空");
        String normalizedProofKey = normalizeOptional(proofKey, 255);
        LocalDateTime now = LocalDateTime.now();
        if ("RESOLVE".equals(normalizedAction)) {
            row.setStatus("COMPLETED");
            row.setCompletedAt(row.getCompletedAt() == null ? now : row.getCompletedAt());
        } else {
            row.setStatus("CLOSED");
            row.setClosedAt(now);
        }
        row.setAfterSaleNote(normalizedNote);
        row.setAfterSaleProofKey(normalizedProofKey);
        row.setAfterSaleHandledAt(now);
        row.setUpdatedAt(now);
        tradeOrderMapper.updateById(row);
        if ("COMPLETED".equals(row.getStatus())) {
            distributionService.onTradeOrderCompleted(row);
        } else if ("CLOSED".equals(row.getStatus())) {
            distributionService.onTradeOrderClosed(row);
        }
        createUserMessage(
            row.getBuyerUserId(),
            "TRADE",
            "售后处理结果通知",
            ("RESOLVE".equals(normalizedAction) ? "你的售后申请已处理完成，订单恢复为已完成。" : "你的售后申请已关闭，订单已关闭。")
                + " 订单号：" + row.getOrderNo()
                + "；处理备注：" + normalizedNote
        );
        log.info("studio after-sale handled userId={} orderNo={} action={} status={}",
            userId, orderNo, normalizedAction, row.getStatus());
        return mapOf(
            "orderNo", orderNo,
            "status", row.getStatus(),
            "statusText", renderOrderStatus(row.getStatus()),
            "afterSaleProofKey", defaultText(row.getAfterSaleProofKey(), ""),
            "afterSaleProofUrl", previewNullable(row.getAfterSaleProofKey()),
            "message", "RESOLVE".equals(normalizedAction) ? "售后单已处理完成，订单恢复为已完成" : "售后单已关闭，订单已关闭"
        );
    }

    @Transactional
    public Map<String, Object> reviewRefund(Long userId, String orderNo, String action, String note) {
        OrderService.OrderDetailResult detail = orderService.reviewRefund(userId, orderNo, action, note);
        return mapOf(
            "orderNo", detail.getOrderNo(),
            "status", detail.getStatusCode(),
            "statusText", detail.getStatusLabel(),
            "message", "APPROVE".equalsIgnoreCase(action) ? "退款已同意，款项已退回买家钱包" : "退款已拒绝，订单已转入售后"
        );
    }

    public Map<String, Object> loadSettlements(Long userId, String range) {
        long startAt = System.currentTimeMillis();
        StudioProfileDO studio = requireStudioProfile(userId);
        BigDecimal shareRatio = resolveShareRatio(studio);
        List<TradeOrderDO> rows = tradeOrderMapper.selectList(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getSellerUserId, userId)
                .orderByDesc(TradeOrderDO::getCompletedAt)
                .orderByDesc(TradeOrderDO::getCreatedAt)
                .last("LIMIT 120")
        );
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        BigDecimal settledTotal = BigDecimal.ZERO;
        BigDecimal pendingTotal = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        for (TradeOrderDO row : rows) {
            if (!matchesRange(row, range, now)) {
                continue;
            }
            BigDecimal calculatedShareAmount = safeAmount(row.getItemAmount()).multiply(shareRatio).setScale(2, RoundingMode.HALF_UP);
            BigDecimal displayShareAmount = BigDecimal.ZERO;
            boolean settled = "COMPLETED".equals(row.getStatus());
            if (settled) {
                displayShareAmount = calculatedShareAmount;
                settledTotal = settledTotal.add(calculatedShareAmount);
            } else if (Arrays.asList("WAITING_TRADE", "AFTER_SALE", "IN_PROGRESS").contains(row.getStatus())) {
                displayShareAmount = calculatedShareAmount;
                pendingTotal = pendingTotal.add(calculatedShareAmount);
            }
            items.add(mapOf(
                "orderNo", row.getOrderNo(),
                "listingTitle", row.getListingTitle(),
                "buyerNickname", row.getBuyerNickname(),
                "grossAmount", safeMoney(row.getTotalAmount()),
                "itemAmount", safeMoney(row.getItemAmount()),
                "shareRatio", renderShareRatio(shareRatio),
                "shareAmount", formatMoney(displayShareAmount),
                "settlementStatus", renderSettlementStatus(row.getStatus()),
                "statusText", renderOrderStatus(row.getStatus()),
                "completedAt", formatTime(row.getCompletedAt()),
                "createdAt", formatTime(row.getCreatedAt())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rows", items);
        result.put("summary", mapOf(
            "settledTotal", formatMoney(settledTotal),
            "pendingTotal", formatMoney(pendingTotal),
            "orderCount", items.size(),
            "shareRatio", renderShareRatio(shareRatio)
        ));
        log.info("studio settlements loaded userId={} range={} count={} costMs={}", userId, range, items.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    public Map<String, Object> loadProfile(Long userId) {
        long startAt = System.currentTimeMillis();
        StudioProfileDO studio = requireStudioProfile(userId);
        AuthUserDO user = authUserMapper.selectById(userId);
        Map<String, Object> result = mapOf(
            "studioId", studio.getId(),
            "studioName", studio.getStudioName(),
            "description", defaultText(studio.getDescription(), ""),
            "contactPhone", defaultText(studio.getContactPhone(), ""),
            "contactWechat", defaultText(studio.getContactWechat(), ""),
            "reviewStrategy", studio.getReviewStrategy(),
            "reviewStrategyText", renderReviewStrategy(studio.getReviewStrategy()),
            "shareRatio", renderShareRatio(studio.getShareRatio()),
            "active", studio.getActive(),
            "activeText", renderCooperationStatus(studio.getCooperationStatus(), studio.getActive()),
            "ownerNickname", user == null ? "-" : user.getNickname(),
            "ownerPhone", user == null ? "-" : maskPhone(user.getPhone()),
            "avatarUrl", user == null ? null : previewNullable(user.getAvatarKey()),
            "verified", user != null && Boolean.TRUE.equals(user.getVerified()),
            "realName", user == null ? null : user.getRealName()
        );
        log.info("studio profile loaded userId={} studioId={} costMs={}", userId, studio.getId(), System.currentTimeMillis() - startAt);
        return result;
    }

    public Map<String, Object> saveProfile(Long userId, String studioName, String description, String contactPhone, String contactWechat) {
        LocalDateTime now = LocalDateTime.now();
        StudioProfileDO studio = requireStudioProfile(userId);
        String normalizedStudioName = normalizeRequired(studioName, "工作室名称不能为空");
        String normalizedPhone = normalizeStudioContactPhone(contactPhone);
        studio.setStudioName(normalizedStudioName);
        studio.setDescription(normalizeOptional(description, 500));
        studio.setContactPhone(normalizedPhone);
        studio.setContactWechat(normalizeOptional(contactWechat, 64));
        studio.setUpdatedAt(now);
        studioProfileMapper.updateById(studio);
        log.info(
            "studio profile saved userId={} studioId={} studioName={} hasDescription={} hasContactWechat={}",
            userId,
            studio.getId(),
            studio.getStudioName(),
            StringUtils.hasText(studio.getDescription()),
            StringUtils.hasText(studio.getContactWechat())
        );
        return mapOf(
            "studioId", studio.getId(),
            "studioName", studio.getStudioName(),
            "message", "工作室资料已保存"
        );
    }

    public Map<String, Object> loadOperators(Long userId, String status, String keyword) {
        long startAt = System.currentTimeMillis();
        StudioProfileDO studio = requireStudioProfile(userId);
        List<StudioOperatorDO> rows = studioOperatorMapper.selectList(
            Wrappers.<StudioOperatorDO>lambdaQuery()
                .eq(StudioOperatorDO::getOwnerUserId, userId)
                .eq(StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status), StudioOperatorDO::getStatus, status)
                .orderByDesc(StudioOperatorDO::getUpdatedAt)
                .last("LIMIT 120")
        );
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (StudioOperatorDO row : rows) {
            String haystack = (defaultText(row.getName(), "") + "|" + defaultText(row.getPhone(), "")).toLowerCase(Locale.ROOT);
            if (normalizedKeyword != null && !haystack.contains(normalizedKeyword)) {
                continue;
            }
            List<String> permissions = readStringList(row.getPermissionsJson());
            items.add(mapOf(
                "operatorId", row.getId(),
                "operatorNo", row.getOperatorNo(),
                "name", row.getName(),
                "phoneRaw", defaultText(row.getPhone(), ""),
                "phone", maskPhone(row.getPhone()),
                "status", row.getStatus(),
                "statusText", "ACTIVE".equals(row.getStatus()) ? "启用中" : "已停用",
                "permissions", permissions,
                "permissionText", permissions.isEmpty() ? "未配置权限" : renderOperatorPermissions(permissions),
                "updatedAt", formatTime(row.getUpdatedAt())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rows", items);
        result.put("summary", mapOf(
            "total", items.size(),
            "active", countOperators(rows, "ACTIVE"),
            "disabled", countOperators(rows, "DISABLED")
        ));
        result.put("studio", mapOf(
            "studioId", studio.getId(),
            "studioName", studio.getStudioName()
        ));
        log.info("studio operators loaded userId={} studioId={} count={} costMs={}",
            userId, studio.getId(), items.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    @Transactional
    public Map<String, Object> saveOperator(Long userId, Long operatorId, String name, String phone, List<String> permissions, String password) {
        StudioProfileDO studio = requireStudioProfile(userId);
        StudioOperatorDO row = operatorId == null ? null : requireStudioOperator(userId, operatorId);
        LocalDateTime now = LocalDateTime.now();
        if (row == null) {
            row = new StudioOperatorDO();
            row.setOperatorNo("OP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
            row.setStudioId(studio.getId());
            row.setOwnerUserId(userId);
            row.setStatus("ACTIVE");
            row.setCreatedAt(now);
            row.setPasswordHash(hashPassword(normalizeRequired(password, "新增操作员必须设置登录密码")));
        } else if (StringUtils.hasText(password)) {
            row.setPasswordHash(hashPassword(password.trim()));
        }
        row.setName(normalizeRequired(name, "操作员姓名不能为空"));
        row.setPhone(normalizeStudioContactPhone(phone));
        row.setPermissionsJson(writeStringList(normalizePermissions(permissions)));
        row.setUpdatedAt(now);
        if (row.getId() == null) {
            studioOperatorMapper.insert(row);
        } else {
            studioOperatorMapper.updateById(row);
        }
        log.info("studio operator saved userId={} studioId={} operatorId={} status={}",
            userId, studio.getId(), row.getId(), row.getStatus());
        return mapOf(
            "operatorId", row.getId(),
            "operatorNo", row.getOperatorNo(),
            "message", "操作员已保存"
        );
    }

    @Transactional
    public Map<String, Object> updateOperatorStatus(Long userId, Long operatorId, String status) {
        StudioOperatorDO row = requireStudioOperator(userId, operatorId);
        row.setStatus(normalizeAction(status, Arrays.asList("ACTIVE", "DISABLED")));
        row.setUpdatedAt(LocalDateTime.now());
        studioOperatorMapper.updateById(row);
        log.info("studio operator status updated userId={} operatorId={} status={}", userId, operatorId, row.getStatus());
        return mapOf("operatorId", operatorId, "status", row.getStatus(), "message", "操作员状态已更新");
    }

    @Transactional
    public Map<String, Object> resetOperatorPassword(Long userId, Long operatorId, String password) {
        StudioOperatorDO row = requireStudioOperator(userId, operatorId);
        row.setPasswordHash(hashPassword(normalizeRequired(password, "新密码不能为空")));
        row.setUpdatedAt(LocalDateTime.now());
        studioOperatorMapper.updateById(row);
        log.info("studio operator password reset userId={} operatorId={}", userId, operatorId);
        return mapOf("operatorId", operatorId, "message", "操作员密码已重置");
    }

    public Map<String, Object> loadFinance(Long userId, String range) {
        long startAt = System.currentTimeMillis();
        StudioProfileDO studio = requireStudioProfile(userId);
        WithdrawAccountDO account = findWithdrawAccount(userId);
        Map<String, Object> settlements = loadSettlements(userId, range);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> statementRows = (List<Map<String, Object>>) settlements.get("rows");
        @SuppressWarnings("unchecked")
        Map<String, Object> settlementSummary = (Map<String, Object>) settlements.get("summary");
        List<StudioWithdrawApplicationDO> withdraws = studioWithdrawApplicationMapper.selectList(
            Wrappers.<StudioWithdrawApplicationDO>lambdaQuery()
                .eq(StudioWithdrawApplicationDO::getOwnerUserId, userId)
                .orderByDesc(StudioWithdrawApplicationDO::getCreatedAt)
                .last("LIMIT 30")
        );
        List<Map<String, Object>> withdrawRows = new ArrayList<Map<String, Object>>();
        BigDecimal pendingWithdraw = BigDecimal.ZERO;
        BigDecimal paidWithdraw = BigDecimal.ZERO;
        for (StudioWithdrawApplicationDO row : withdraws) {
            if ("PENDING".equals(row.getStatus())) {
                pendingWithdraw = pendingWithdraw.add(safeAmount(row.getAmount()));
            } else if ("PAID".equals(row.getStatus())) {
                paidWithdraw = paidWithdraw.add(safeAmount(row.getAmount()));
            }
            withdrawRows.add(mapOf(
                "applicationNo", row.getApplicationNo(),
                "amount", safeMoney(row.getAmount()),
                "channel", row.getChannel(),
                "channelText", renderPaymentMethod(row.getChannel()),
                "accountName", row.getAccountName(),
                "accountNo", maskAccount(row.getAccountNo()),
                "status", row.getStatus(),
                "statusText", renderWithdrawStatus(row.getStatus()),
                "rejectReason", defaultText(row.getRejectReason(), ""),
                "createdAt", formatTime(row.getCreatedAt()),
                "reviewedAt", formatTime(row.getReviewedAt()),
                "paidAt", formatTime(row.getPaidAt())
            ));
        }
        BigDecimal withdrawableTotal = parseMoney(settlementSummary, "settledTotal")
            .subtract(pendingWithdraw)
            .subtract(paidWithdraw)
            .max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("account", account == null ? null : mapOf(
            "channel", account.getChannel(),
            "channelText", renderPaymentMethod(account.getChannel()),
            "accountName", account.getAccountName(),
            "accountNo", maskAccount(account.getAccountNo())
        ));
        result.put("summary", mapOf(
            "studioId", studio.getId(),
            "studioName", studio.getStudioName(),
            "settledTotal", settlementSummary.get("settledTotal"),
            "pendingTotal", settlementSummary.get("pendingTotal"),
            "withdrawPendingTotal", formatMoney(pendingWithdraw),
            "withdrawPaidTotal", formatMoney(paidWithdraw),
            "withdrawableTotal", formatMoney(withdrawableTotal)
        ));
        result.put("withdraws", withdrawRows);
        result.put("statements", statementRows);
        log.info("studio finance loaded userId={} studioId={} withdrawCount={} statementCount={} costMs={}",
            userId, studio.getId(), withdrawRows.size(), statementRows.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    public Map<String, Object> savePayoutAccount(Long userId, String channel, String accountName, String accountNo) {
        StudioProfileDO studio = requireStudioProfile(userId);
        AuthUserDO owner = authUserMapper.selectById(userId);
        if (owner == null || !Boolean.TRUE.equals(owner.getVerified())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先完成实名认证后再维护工作室收款信息");
        }
        String normalizedName = normalizeRequired(accountName, "收款姓名不能为空");
        if (!normalizedName.equals(owner.getRealName())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "收款姓名必须与实名认证姓名一致");
        }
        WithdrawAccountDO account = findWithdrawAccount(userId);
        LocalDateTime now = LocalDateTime.now();
        if (account == null) {
            account = new WithdrawAccountDO();
            account.setUserId(userId);
            account.setCreatedAt(now);
        }
        account.setChannel(normalizeChannel(channel));
        account.setAccountName(normalizedName);
        account.setAccountNo(normalizeRequired(accountNo, "收款账号不能为空"));
        account.setUpdatedAt(now);
        if (account.getId() == null) {
            withdrawAccountMapper.insert(account);
        } else {
            withdrawAccountMapper.updateById(account);
        }
        log.info("studio payout account saved userId={} studioId={} channel={}", userId, studio.getId(), account.getChannel());
        return mapOf("message", "工作室收款信息已保存");
    }

    public Map<String, Object> applyWithdraw(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(new BigDecimal("10.00")) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "提现金额不能低于 10 元");
        }
        StudioProfileDO studio = requireStudioProfile(userId);
        WithdrawAccountDO account = requireWithdrawAccount(userId);
        BigDecimal settledSettlement = parseMoney(loadSettlements(userId, "ALL").get("summary"), "settledTotal");
        BigDecimal consumedWithdraw = sumConsumedStudioWithdraw(userId);
        BigDecimal withdrawable = settledSettlement.subtract(consumedWithdraw).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(withdrawable) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "提现金额不能超过可提现分润余额");
        }
        LocalDateTime now = LocalDateTime.now();
        StudioWithdrawApplicationDO row = new StudioWithdrawApplicationDO();
        row.setApplicationNo("SW" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase(Locale.ROOT));
        row.setStudioId(studio.getId());
        row.setOwnerUserId(userId);
        row.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        row.setChannel(account.getChannel());
        row.setAccountName(account.getAccountName());
        row.setAccountNo(account.getAccountNo());
        row.setStatus("PENDING");
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        studioWithdrawApplicationMapper.insert(row);
        log.info("studio withdraw apply success userId={} studioId={} applicationNo={} amount={}",
            userId, studio.getId(), row.getApplicationNo(), row.getAmount());
        return mapOf("applicationNo", row.getApplicationNo(), "message", "工作室提现申请已提交");
    }

    public Map<String, Object> generateStatement(Long userId, String range) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) loadSettlements(userId, range).get("rows");
        StringBuilder builder = new StringBuilder();
        builder.append("订单号,商品,买家,成交额,货款,分润比例,分润金额,结算状态,订单状态,完成时间\n");
        for (Map<String, Object> row : rows) {
            builder.append(csv(row.get("orderNo"))).append(',')
                .append(csv(row.get("listingTitle"))).append(',')
                .append(csv(row.get("buyerNickname"))).append(',')
                .append(csv(row.get("grossAmount"))).append(',')
                .append(csv(row.get("itemAmount"))).append(',')
                .append(csv(row.get("shareRatio"))).append(',')
                .append(csv(row.get("shareAmount"))).append(',')
                .append(csv(row.get("settlementStatus"))).append(',')
                .append(csv(row.get("statusText"))).append(',')
                .append(csv(row.get("completedAt"))).append('\n');
        }
        log.info("studio statement generated userId={} range={} rowCount={}", userId, range, rows.size());
        return mapOf(
            "fileName", "studio-settlement-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv",
            "content", builder.toString(),
            "rowCount", rows.size()
        );
    }

    private StudioProfileDO requireStudioProfile(Long userId) {
        StudioProfileDO row = studioProfileMapper.selectOne(
            Wrappers.<StudioProfileDO>lambdaQuery()
                .eq(StudioProfileDO::getOwnerUserId, userId)
                .last("LIMIT 1")
        );
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号未开通工作室后台权限");
        }
        return row;
    }

    private WithdrawAccountDO requireWithdrawAccount(Long userId) {
        WithdrawAccountDO row = findWithdrawAccount(userId);
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先维护工作室收款信息");
        }
        return row;
    }

    private AccountListingDO requireOwnerListing(Long userId, String listingNo) {
        AccountListingDO row = accountListingMapper.selectOne(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getSellerUserId, userId)
                .eq(AccountListingDO::getListingNo, listingNo)
                .last("LIMIT 1")
        );
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "商品不存在或不属于当前工作室");
        }
        return row;
    }

    private TradeOrderDO requireStudioOrder(Long userId, String orderNo) {
        TradeOrderDO row = tradeOrderMapper.selectOne(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getSellerUserId, userId)
                .eq(TradeOrderDO::getOrderNo, orderNo)
                .last("LIMIT 1")
        );
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单不存在或不属于当前工作室");
        }
        return row;
    }

    private StudioOperatorDO requireStudioOperator(Long userId, Long operatorId) {
        StudioOperatorDO row = studioOperatorMapper.selectOne(
            Wrappers.<StudioOperatorDO>lambdaQuery()
                .eq(StudioOperatorDO::getOwnerUserId, userId)
                .eq(StudioOperatorDO::getId, operatorId)
                .last("LIMIT 1")
        );
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "操作员不存在或不属于当前工作室");
        }
        return row;
    }

    private int countListings(Long userId, String status) {
        Long count = accountListingMapper.selectCount(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getSellerUserId, userId)
                .eq(AccountListingDO::getStatus, status)
        );
        return count == null ? 0 : count.intValue();
    }

    private int countListings(List<AccountListingDO> rows, String status) {
        int count = 0;
        for (AccountListingDO row : rows) {
            if (status.equals(row.getStatus())) {
                count += 1;
            }
        }
        return count;
    }

    private int countStudioOrders(Long userId, List<String> statuses) {
        Long count = tradeOrderMapper.selectCount(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getSellerUserId, userId)
                .in(TradeOrderDO::getStatus, statuses)
        );
        return count == null ? 0 : count.intValue();
    }

    private int countOrders(List<TradeOrderDO> rows, String status) {
        int count = 0;
        for (TradeOrderDO row : rows) {
            if (status.equals(row.getStatus())) {
                count += 1;
            }
        }
        return count;
    }

    private int countOperators(List<StudioOperatorDO> rows, String status) {
        int count = 0;
        for (StudioOperatorDO row : rows) {
            if (status.equals(row.getStatus())) {
                count += 1;
            }
        }
        return count;
    }

    private BigDecimal sumProfit(Long userId, List<String> statuses) {
        StudioProfileDO studio = requireStudioProfile(userId);
        List<BigDecimal> rows = jdbcTemplate.query(
            "SELECT COALESCE(SUM(item_amount), 0) AS amount FROM trade_order WHERE seller_user_id = ? AND status IN (" + buildInClause(statuses.size()) + ")",
            buildSqlArgs(userId, statuses),
            (rs, rowNum) -> rs.getBigDecimal("amount")
        );
        BigDecimal base = rows.isEmpty() ? BigDecimal.ZERO : safeAmount(rows.get(0));
        return base.multiply(resolveShareRatio(studio)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumMonthlyGmv(Long userId) {
        List<BigDecimal> rows = jdbcTemplate.query(
            "SELECT COALESCE(SUM(total_amount), 0) AS amount FROM trade_order WHERE seller_user_id = ? AND DATE_FORMAT(created_at, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m')",
            new Object[] {userId},
            (rs, rowNum) -> rs.getBigDecimal("amount")
        );
        return rows.isEmpty() ? BigDecimal.ZERO : safeAmount(rows.get(0));
    }

    private List<Map<String, Object>> loadRecentListings(Long userId, int limit) {
        List<AccountListingDO> rows = accountListingMapper.selectList(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getSellerUserId, userId)
                .orderByDesc(AccountListingDO::getUpdatedAt)
                .last("LIMIT " + limit)
        );
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (AccountListingDO row : rows) {
            items.add(mapOf(
                "listingNo", row.getListingNo(),
                "title", row.getTitle(),
                "statusText", renderListingStatus(row.getStatus()),
                "price", safeMoney(row.getPrice()),
                "updatedAt", formatTime(row.getUpdatedAt())
            ));
        }
        return items;
    }

    private List<Map<String, Object>> loadRecentOrders(Long userId, int limit) {
        List<TradeOrderDO> rows = tradeOrderMapper.selectList(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getSellerUserId, userId)
                .orderByDesc(TradeOrderDO::getCreatedAt)
                .last("LIMIT " + limit)
        );
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (TradeOrderDO row : rows) {
            items.add(mapOf(
                "orderNo", row.getOrderNo(),
                "listingTitle", row.getListingTitle(),
                "statusText", renderOrderStatus(row.getStatus()),
                "totalAmount", safeMoney(row.getTotalAmount()),
                "createdAt", formatTime(row.getCreatedAt())
            ));
        }
        return items;
    }

    private List<Map<String, Object>> loadStudioBoostingServices(String studioName, int limit) {
        if (!StringUtils.hasText(studioName)) {
            return Collections.emptyList();
        }
        List<BoostingServiceDO> rows = boostingServiceMapper.selectList(
            Wrappers.<BoostingServiceDO>lambdaQuery()
                .eq(BoostingServiceDO::getProviderName, studioName)
                .orderByAsc(BoostingServiceDO::getSortNo)
                .last("LIMIT " + limit)
        );
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (BoostingServiceDO row : rows) {
            items.add(mapOf(
                "serviceNo", row.getServiceNo(),
                "name", row.getName(),
                "statusText", "ACTIVE".equals(row.getStatus()) ? "启用中" : "已停用",
                "price", safeMoney(row.getPrice()),
                "salesCount", safeNumber(row.getSalesCount())
            ));
        }
        return items;
    }

    private List<Map<String, Object>> loadPublishedAnnouncements(int limit) {
        List<OperationAnnouncementDO> rows = operationAnnouncementMapper.selectList(
            Wrappers.<OperationAnnouncementDO>lambdaQuery()
                .eq(OperationAnnouncementDO::getStatus, "PUBLISHED")
                .orderByDesc(OperationAnnouncementDO::getPinned)
                .orderByDesc(OperationAnnouncementDO::getPublishAt)
                .orderByDesc(OperationAnnouncementDO::getUpdatedAt)
                .last("LIMIT " + limit)
        );
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (OperationAnnouncementDO row : rows) {
            items.add(mapOf(
                "announcementNo", row.getAnnouncementNo(),
                "title", defaultText(row.getTitle(), "-"),
                "content", defaultText(row.getContent(), ""),
                "categoryText", renderAnnouncementCategory(row.getCategory()),
                "pinned", Boolean.TRUE.equals(row.getPinned()),
                "publishAt", formatTime(row.getPublishAt())
            ));
        }
        return items;
    }

    private boolean matchesRange(TradeOrderDO row, String range, LocalDateTime now) {
        if (!StringUtils.hasText(range) || "ALL".equalsIgnoreCase(range)) {
            return true;
        }
        LocalDateTime base = row.getCompletedAt() != null ? row.getCompletedAt() : row.getCreatedAt();
        if (base == null) {
            return false;
        }
        if ("7D".equalsIgnoreCase(range)) {
            return base.isAfter(now.minusDays(7));
        }
        if ("30D".equalsIgnoreCase(range)) {
            return base.isAfter(now.minusDays(30));
        }
        return true;
    }

    private List<Map<String, Object>> buildReviewRecords(AccountListingDO row) {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        items.add(mapOf(
            "label", "提交商品",
            "status", "已提交",
            "time", formatTime(row.getSubmittedAt()),
            "remark", "工作室提交账号商品资料"
        ));
        if ("PUBLISHED".equals(row.getStatus())) {
            items.add(mapOf(
                "label", "审核通过",
                "status", "已上架",
                "time", formatTime(row.getPublishedAt()),
                "remark", "账号已通过审核并对外展示"
            ));
        }
        if ("REJECTED".equals(row.getStatus())) {
            items.add(mapOf(
                "label", "审核驳回",
                "status", "已驳回",
                "time", formatTime(row.getUpdatedAt()),
                "remark", defaultText(row.getRejectionReason(), "平台审核驳回")
            ));
        }
        if ("OFFLINE".equals(row.getStatus())) {
            items.add(mapOf(
                "label", "已下架",
                "status", "已下架",
                "time", formatTime(row.getUpdatedAt()),
                "remark", "商品已从交易大厅移除"
            ));
        }
        return items;
    }

    private boolean canEdit(String status) {
        return "PENDING_REVIEW".equals(status) || "REJECTED".equals(status) || "OFFLINE".equals(status);
    }

    private List<String> buildImageUrls(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        try {
            List<String> keys = objectMapper.readValue(raw, STRING_LIST_TYPE);
            List<String> urls = new ArrayList<String>();
            for (String key : keys) {
                urls.add(previewNullable(key));
            }
            return urls;
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    private String renderReviewStrategy(String strategy) {
        if ("DIRECT_PUBLISH".equals(strategy)) {
            return "免审直发";
        }
        return "需要审核";
    }

    private String renderCooperationStatus(String cooperationStatus, Boolean active) {
        String normalized = cooperationStatus == null ? "" : cooperationStatus.trim().toUpperCase(Locale.ROOT);
        if ("ACTIVE".equals(normalized)) {
            return "合作中";
        }
        if ("PAUSED".equals(normalized)) {
            return "暂停合作";
        }
        if ("CLEARED".equals(normalized)) {
            return "已清退";
        }
        return Boolean.TRUE.equals(active) ? "合作中" : "暂停合作";
    }

    private String renderListingStatus(String status) {
        if ("PUBLISHED".equals(status)) return "已上架";
        if ("PENDING_REVIEW".equals(status)) return "待审核";
        if ("REJECTED".equals(status)) return "已驳回";
        if ("OFFLINE".equals(status)) return "已下架";
        return defaultText(status, "-");
    }

    private String renderReviewProgress(String status) {
        if ("PUBLISHED".equals(status)) return "已通过";
        if ("REJECTED".equals(status)) return "已驳回";
        if ("OFFLINE".equals(status)) return "已下架";
        return "待审核";
    }

    private String renderOrderStatus(String status) {
        if ("PENDING_PAYMENT".equals(status)) return "待付款";
	        if ("WAITING_TRADE".equals(status)) return "交易中";
        if ("IN_PROGRESS".equals(status)) return "交易中";
        if ("COMPLETED".equals(status)) return "已完成";
        if ("REFUND_PENDING".equals(status)) return "退款审核中";
        if ("AFTER_SALE".equals(status)) return "售后中";
        if ("REFUNDED".equals(status)) return "已退款";
        if ("CLOSED".equals(status)) return "已关闭";
        return defaultText(status, "-");
    }

    private String renderSettlementStatus(String status) {
        if ("COMPLETED".equals(status)) return "已结算";
        if ("WAITING_TRADE".equals(status) || "AFTER_SALE".equals(status) || "IN_PROGRESS".equals(status) || "REFUND_PENDING".equals(status)) return "待结算";
        if ("PENDING_PAYMENT".equals(status)) return "未支付";
        if ("CLOSED".equals(status) || "REFUNDED".equals(status)) return "不结算";
        return "-";
    }

    private String renderPaymentMethod(String method) {
        if ("ALIPAY".equalsIgnoreCase(method)) return "支付宝";
        if ("WECHAT".equalsIgnoreCase(method)) return "微信";
        return defaultText(method, "-");
    }

    private BigDecimal resolveShareRatio(StudioProfileDO studio) {
        BigDecimal ratio = studio == null ? null : studio.getShareRatio();
        if (ratio == null || ratio.compareTo(BigDecimal.ZERO) <= 0) {
            return DEFAULT_SHARE_RATIO;
        }
        return ratio.setScale(4, RoundingMode.HALF_UP);
    }

    private String renderShareRatio(BigDecimal ratio) {
        return safeAmount(resolveNullableRatio(ratio).multiply(new BigDecimal("100"))).stripTrailingZeros().toPlainString() + "%";
    }

    private BigDecimal resolveNullableRatio(BigDecimal ratio) {
        if (ratio == null || ratio.compareTo(BigDecimal.ZERO) <= 0) {
            return DEFAULT_SHARE_RATIO;
        }
        return ratio.setScale(4, RoundingMode.HALF_UP);
    }

    private String renderWithdrawStatus(String status) {
        if ("PENDING".equals(status)) return "待审核";
        if ("PAID".equals(status)) return "已到账";
        if ("REJECTED".equals(status)) return "已驳回";
        return defaultText(status, "-");
    }

    private String renderAnnouncementCategory(String category) {
        if ("SYSTEM".equalsIgnoreCase(category)) return "系统公告";
        if ("ACTIVITY".equalsIgnoreCase(category)) return "活动公告";
        if ("TRADE".equalsIgnoreCase(category)) return "交易通知";
        return defaultText(category, "-");
    }

    private String normalizeChannel(String channel) {
        String normalized = channel == null ? "" : channel.trim().toUpperCase(Locale.ROOT);
        if (!"ALIPAY".equals(normalized) && !"WECHAT".equals(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅支持支付宝或微信提现");
        }
        return normalized;
    }

    private String previewNullable(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        return ossStorageService.previewUrl(key);
    }

    private BigDecimal safeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String safeMoney(BigDecimal amount) {
        return formatMoney(safeAmount(amount));
    }

    private String formatMoney(BigDecimal amount) {
        return "¥" + safeAmount(amount).toPlainString();
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "-" : TIME_FORMATTER.format(time);
    }

    private Integer safeNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
        return value.trim();
    }

    private String normalizeAction(String action, List<String> allowed) {
        String normalized = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "非法操作");
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "输入内容过长，请精简后重试");
        }
        return normalized;
    }

    private String normalizeStudioContactPhone(String phone) {
        String normalized = normalizeRequired(phone, "联系电话不能为空");
        if (!normalized.matches("^1\\d{10}$")) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "联系电话需为 11 位手机号");
        }
        return normalized;
    }

    private List<String> normalizePermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> normalized = new ArrayList<String>();
        for (String permission : permissions) {
            if (!StringUtils.hasText(permission)) {
                continue;
            }
            String value = permission.trim();
            if (value.length() > 40) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "操作员权限项过长");
            }
            if (!normalized.contains(value)) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private List<String> readStringList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        try {
            List<String> rows = objectMapper.readValue(raw, STRING_LIST_TYPE);
            return rows == null ? Collections.emptyList() : rows;
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    private String writeStringList(List<String> rows) {
        try {
            return objectMapper.writeValueAsString(rows == null ? Collections.emptyList() : rows);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "操作员权限写入失败");
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                String part = Integer.toHexString(value & 0xff);
                if (part.length() == 1) {
                    builder.append('0');
                }
                builder.append(part);
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "密码加密失败");
        }
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return defaultText(phone, "-");
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskAccount(String accountNo) {
        if (!StringUtils.hasText(accountNo) || accountNo.length() <= 8) {
            return defaultText(accountNo, "-");
        }
        return accountNo.substring(0, 4) + " **** **** " + accountNo.substring(accountNo.length() - 4);
    }

    private String renderSellerType(String sellerType) {
        return "STUDIO".equalsIgnoreCase(defaultText(sellerType, "")) ? "工作室" : "个人";
    }

    private String renderOperatorPermissions(List<String> permissions) {
        List<String> labels = new ArrayList<String>();
        for (String permission : permissions) {
            if ("LISTING".equals(permission)) {
                labels.add("商品管理");
            } else if ("ORDER".equals(permission)) {
                labels.add("订单处理");
            } else if ("AFTER_SALE".equals(permission)) {
                labels.add("售后处理");
            } else if ("FINANCE".equals(permission)) {
                labels.add("财务查看");
            } else if ("PROFILE".equals(permission)) {
                labels.add("资料维护");
            } else {
                labels.add(permission);
            }
        }
        return String.join(" / ", labels);
    }

    private void createUserMessage(Long userId, String category, String title, String content) {
        if (userId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        UserMessageDO row = new UserMessageDO();
        row.setUserId(userId);
        row.setCategory(category);
        row.setTitle(title);
        row.setContent(content);
        row.setReadFlag(Boolean.FALSE);
        row.setDeletedFlag(Boolean.FALSE);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        userMessageMapper.insert(row);
    }

    private WithdrawAccountDO findWithdrawAccount(Long userId) {
        return withdrawAccountMapper.selectOne(
            Wrappers.<WithdrawAccountDO>lambdaQuery()
                .eq(WithdrawAccountDO::getUserId, userId)
                .last("LIMIT 1")
        );
    }

    private BigDecimal sumConsumedStudioWithdraw(Long userId) {
        List<StudioWithdrawApplicationDO> rows = studioWithdrawApplicationMapper.selectList(
            Wrappers.<StudioWithdrawApplicationDO>lambdaQuery()
                .eq(StudioWithdrawApplicationDO::getOwnerUserId, userId)
                .in(StudioWithdrawApplicationDO::getStatus, Arrays.asList("PENDING", "PAID"))
        );
        BigDecimal total = BigDecimal.ZERO;
        for (StudioWithdrawApplicationDO row : rows) {
            total = total.add(safeAmount(row.getAmount()));
        }
        return total;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal parseMoney(Object summaryObject, String key) {
        if (!(summaryObject instanceof Map)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        Object value = ((Map<String, Object>) summaryObject).get(key);
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(String.valueOf(value).replace("¥", "").trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception exception) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private Object[] buildSqlArgs(Long userId, List<String> statuses) {
        List<Object> args = new ArrayList<Object>();
        args.add(userId);
        args.addAll(statuses);
        return args.toArray();
    }

    private String buildInClause(int size) {
        List<String> placeholders = new ArrayList<String>();
        for (int index = 0; index < size; index += 1) {
            placeholders.add("?");
        }
        return String.join(",", placeholders);
    }

    private Map<String, Object> metric(String label, String value, String trend) {
        return mapOf("label", label, "value", value, "trend", trend);
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int index = 0; index < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }
}
