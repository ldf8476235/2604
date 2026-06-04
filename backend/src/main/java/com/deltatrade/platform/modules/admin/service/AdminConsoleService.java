package com.deltatrade.platform.modules.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.admin.mapper.AdminRoleMapper;
import com.deltatrade.platform.modules.admin.mapper.AdminRoleMemberMapper;
import com.deltatrade.platform.modules.admin.mapper.OperationAnnouncementMapper;
import com.deltatrade.platform.modules.admin.mapper.OperationBannerMapper;
import com.deltatrade.platform.modules.admin.mapper.OperationShortcutMapper;
import com.deltatrade.platform.modules.admin.model.AdminRoleDO;
import com.deltatrade.platform.modules.admin.model.AdminRoleMemberDO;
import com.deltatrade.platform.modules.admin.model.OperationAnnouncementDO;
import com.deltatrade.platform.modules.admin.model.OperationBannerDO;
import com.deltatrade.platform.modules.admin.model.OperationShortcutDO;
import com.deltatrade.platform.modules.auth.mapper.AuthUserMapper;
import com.deltatrade.platform.modules.auth.model.AuthUserDO;
import com.deltatrade.platform.modules.boosting.mapper.BoostingOrderMapper;
import com.deltatrade.platform.modules.boosting.mapper.BoostingServiceMapper;
import com.deltatrade.platform.modules.boosting.model.BoostingServiceDO;
import com.deltatrade.platform.modules.listing.mapper.AccountListingMapper;
import com.deltatrade.platform.modules.listing.mapper.StudioProfileMapper;
import com.deltatrade.platform.modules.listing.model.AccountListingDO;
import com.deltatrade.platform.modules.listing.model.StudioProfileDO;
import com.deltatrade.platform.modules.order.mapper.TradeOrderMapper;
import com.deltatrade.platform.modules.order.model.TradeOrderDO;
import com.deltatrade.platform.modules.order.service.OrderService;
import com.deltatrade.platform.modules.oss.service.OssStorageService;
import com.deltatrade.platform.modules.profile.mapper.UserMessageMapper;
import com.deltatrade.platform.modules.profile.mapper.UserWalletMapper;
import com.deltatrade.platform.modules.profile.mapper.WalletTransactionMapper;
import com.deltatrade.platform.modules.profile.mapper.WithdrawAccountMapper;
import com.deltatrade.platform.modules.profile.model.UserMessageDO;
import com.deltatrade.platform.modules.profile.service.DistributionService;
import com.deltatrade.platform.modules.profile.mapper.WithdrawApplicationMapper;
import com.deltatrade.platform.modules.profile.model.UserWalletDO;
import com.deltatrade.platform.modules.profile.model.WalletTransactionDO;
import com.deltatrade.platform.modules.profile.model.WithdrawAccountDO;
import com.deltatrade.platform.modules.profile.model.WithdrawApplicationDO;
import com.deltatrade.platform.modules.studio.mapper.StudioWithdrawApplicationMapper;
import com.deltatrade.platform.modules.studio.mapper.StudioApplicationMapper;
import com.deltatrade.platform.modules.studio.model.StudioApplicationDO;
import com.deltatrade.platform.modules.studio.model.StudioWithdrawApplicationDO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminConsoleService {

    private static final Logger log = LoggerFactory.getLogger(AdminConsoleService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };
    private static final BigDecimal STUDIO_SHARE_RATIO = new BigDecimal("0.70");
    private static final List<String> RENTAL_EFFECTIVE_KNIFE_SKINS = Arrays.asList(
        "坠星者", "暗星", "龙牙", "信条", "影锋", "北极星", "黑海", "怜悯", "赤霄"
    );

    private final AccountListingMapper accountListingMapper;
    private final StudioProfileMapper studioProfileMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final BoostingServiceMapper boostingServiceMapper;
    private final BoostingOrderMapper boostingOrderMapper;
    private final WithdrawApplicationMapper withdrawApplicationMapper;
    private final UserMessageMapper userMessageMapper;
    private final UserWalletMapper userWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final WithdrawAccountMapper withdrawAccountMapper;
    private final AuthUserMapper authUserMapper;
    private final OperationBannerMapper operationBannerMapper;
    private final OperationShortcutMapper operationShortcutMapper;
    private final OperationAnnouncementMapper operationAnnouncementMapper;
    private final AdminRoleMapper adminRoleMapper;
    private final AdminRoleMemberMapper adminRoleMemberMapper;
    private final StudioWithdrawApplicationMapper studioWithdrawApplicationMapper;
    private final StudioApplicationMapper studioApplicationMapper;
    private final JdbcTemplate jdbcTemplate;
    private final OssStorageService ossStorageService;
    private final ObjectMapper objectMapper;
    private final DistributionService distributionService;
    private final AdminIntegrationConfigService adminIntegrationConfigService;
    private final OrderService orderService;

    public AdminConsoleService(
        AccountListingMapper accountListingMapper,
        StudioProfileMapper studioProfileMapper,
        TradeOrderMapper tradeOrderMapper,
        BoostingServiceMapper boostingServiceMapper,
        BoostingOrderMapper boostingOrderMapper,
        WithdrawApplicationMapper withdrawApplicationMapper,
        UserMessageMapper userMessageMapper,
        UserWalletMapper userWalletMapper,
        WalletTransactionMapper walletTransactionMapper,
        WithdrawAccountMapper withdrawAccountMapper,
        AuthUserMapper authUserMapper,
        OperationBannerMapper operationBannerMapper,
        OperationShortcutMapper operationShortcutMapper,
        OperationAnnouncementMapper operationAnnouncementMapper,
        AdminRoleMapper adminRoleMapper,
        AdminRoleMemberMapper adminRoleMemberMapper,
        StudioWithdrawApplicationMapper studioWithdrawApplicationMapper,
        StudioApplicationMapper studioApplicationMapper,
        JdbcTemplate jdbcTemplate,
        OssStorageService ossStorageService,
        ObjectMapper objectMapper,
        DistributionService distributionService,
        AdminIntegrationConfigService adminIntegrationConfigService,
        OrderService orderService
    ) {
        this.accountListingMapper = accountListingMapper;
        this.studioProfileMapper = studioProfileMapper;
        this.tradeOrderMapper = tradeOrderMapper;
        this.boostingServiceMapper = boostingServiceMapper;
        this.boostingOrderMapper = boostingOrderMapper;
        this.withdrawApplicationMapper = withdrawApplicationMapper;
        this.userMessageMapper = userMessageMapper;
        this.userWalletMapper = userWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.withdrawAccountMapper = withdrawAccountMapper;
        this.authUserMapper = authUserMapper;
        this.operationBannerMapper = operationBannerMapper;
        this.operationShortcutMapper = operationShortcutMapper;
        this.operationAnnouncementMapper = operationAnnouncementMapper;
        this.adminRoleMapper = adminRoleMapper;
        this.adminRoleMemberMapper = adminRoleMemberMapper;
        this.studioWithdrawApplicationMapper = studioWithdrawApplicationMapper;
        this.studioApplicationMapper = studioApplicationMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.ossStorageService = ossStorageService;
        this.objectMapper = objectMapper;
        this.distributionService = distributionService;
        this.adminIntegrationConfigService = adminIntegrationConfigService;
        this.orderService = orderService;
    }

    public Map<String, Object> loadDashboard() {
        long startAt = System.currentTimeMillis();
        int pendingListingCount = countListingsByStatus("PENDING_REVIEW");
        int publishedListingCount = countListingsByStatus("PUBLISHED");
        int tradingOrderCount = countTradeOrders(Arrays.asList("WAITING_TRADE", "COMPLETED", "AFTER_SALE"));
        int boostingActiveCount = countBoostingOrders(Arrays.asList("WAITING_SERVICE", "IN_SERVICE", "AFTER_SALE"));
        int pendingWithdrawCount = countWithdrawApplications("PENDING");
        BigDecimal todayTradeAmount = sumTradeAmountToday();
        BigDecimal walletFrozenAmount = sumFrozenWalletAmount();
        List<Map<String, Object>> pendingQueue = buildPendingQueue();

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("metrics", Arrays.asList(
            metric("待审核账号", String.valueOf(pendingListingCount), "个人与需审工作室共 " + pendingListingCount + " 条"),
            metric("已上架账号", String.valueOf(publishedListingCount), "当前公开展示商品总量"),
            metric("交易进行中", String.valueOf(tradingOrderCount), "账号交易待交接 / 售后单"),
            metric("代肝进行中", String.valueOf(boostingActiveCount), "待代肝 / 代肝中 / 售后中"),
            metric("待审核提现", String.valueOf(pendingWithdrawCount), "需财务管理员处理"),
            metric("今日成交额", formatMoney(todayTradeAmount), "按今日有支付流水的订单统计")
        ));
        response.put("overview", mapOf(
            "walletFrozenAmount", formatMoney(walletFrozenAmount),
            "studioCount", studioProfileMapper.selectCount(Wrappers.<StudioProfileDO>lambdaQuery()),
            "userCount", authUserMapper.selectCount(Wrappers.<AuthUserDO>lambdaQuery()),
            "boostingServiceCount", boostingServiceMapper.selectCount(Wrappers.<BoostingServiceDO>lambdaQuery())
        ));
        response.put("pendingQueue", pendingQueue);
        log.info(
            "admin dashboard loaded pendingListings={} tradingOrders={} pendingWithdraws={} costMs={}",
            pendingListingCount,
            tradingOrderCount,
            pendingWithdrawCount,
            System.currentTimeMillis() - startAt
        );
        return response;
    }

    public Map<String, Object> loadListings(String status, String sellerType, String keyword) {
        long startAt = System.currentTimeMillis();
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        List<AccountListingDO> rows = accountListingMapper.selectList(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status), AccountListingDO::getStatus, status)
                .eq(StringUtils.hasText(sellerType) && !"ALL".equalsIgnoreCase(sellerType), AccountListingDO::getSellerType, sellerType)
                .and(StringUtils.hasText(normalizedKeyword), wrapper -> wrapper
                    .like(AccountListingDO::getListingNo, normalizedKeyword)
                    .or()
                    .like(AccountListingDO::getTitle, normalizedKeyword)
                    .or()
                    .like(AccountListingDO::getSellerNickname, normalizedKeyword)
                    .or()
                    .like(AccountListingDO::getStudioName, normalizedKeyword)
                    .or()
                    .like(AccountListingDO::getCityName, normalizedKeyword)
                )
                .last("ORDER BY CASE WHEN status = 'PENDING_REVIEW' THEN 0 ELSE 1 END ASC, updated_at DESC LIMIT 120")
        );
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        String normalizedKeywordForMatch = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        for (AccountListingDO row : rows) {
            if (normalizedKeywordForMatch != null && !matchListingKeyword(row, normalizedKeywordForMatch)) {
                continue;
            }
            String exchangeRateLabel = formatRentalRatio(calculateRentalRatio(row, readPublishAttributes(row.getPublishAttributesJson())));
            items.add(mapOf(
                "listingNo", row.getListingNo(),
                "title", row.getTitle(),
                "sellerType", row.getSellerType(),
                "sellerTypeText", renderSellerType(row.getSellerType()),
                "sellerDisplayName", renderSellerDisplayName(row),
                "status", row.getStatus(),
                "statusText", renderListingStatus(row.getStatus()),
                "reviewProgress", renderReviewProgress(row.getStatus()),
                "cityName", defaultText(row.getCityName(), "-"),
                "accountLevel", row.getAccountLevel(),
                "rankName", defaultText(row.getRankName(), "-"),
                "price", safeMoney(row.getPrice()),
                "exchangeRateLabel", exchangeRateLabel,
                "viewCount", safeNumber(row.getViewCount()),
                "favoriteCount", safeNumber(row.getFavoriteCount()),
                "salesCount", safeNumber(row.getSalesCount()),
                "publishedAt", formatTime(row.getPublishedAt()),
                "submittedAt", formatTime(row.getSubmittedAt()),
                "updatedAt", formatTime(row.getUpdatedAt()),
                "rejectionReason", defaultText(row.getRejectionReason(), "")
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rows", items);
        result.put("summary", mapOf(
            "pendingReview", countRows(rows, "PENDING_REVIEW"),
            "published", countRows(rows, "PUBLISHED"),
            "rejected", countRows(rows, "REJECTED"),
            "offline", countRows(rows, "OFFLINE")
        ));
        log.info(
            "admin listing center loaded status={} sellerType={} keyword={} count={} costMs={}",
            status,
            sellerType,
            maskKeyword(keyword),
            items.size(),
            System.currentTimeMillis() - startAt
        );
        return result;
    }

    public Map<String, Object> loadListingDetail(String listingNo) {
        long startAt = System.currentTimeMillis();
        AccountListingDO listing = requireListing(listingNo);
        List<TradeOrderDO> orders = tradeOrderMapper.selectList(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getListingNo, listingNo)
                .orderByDesc(TradeOrderDO::getCreatedAt)
                .last("LIMIT 20")
        );
        List<Map<String, Object>> tradeRecords = new ArrayList<Map<String, Object>>();
        for (TradeOrderDO order : orders) {
            tradeRecords.add(mapOf(
                "orderNo", order.getOrderNo(),
                "buyerNickname", defaultText(order.getBuyerNickname(), "-"),
                "sellerNickname", defaultText(order.getSellerNickname(), "-"),
                "status", order.getStatus(),
                "statusText", renderOrderStatus(order.getStatus()),
                "totalAmount", safeMoney(order.getTotalAmount()),
                "createdAt", formatTime(order.getCreatedAt()),
                "completedAt", formatTime(order.getCompletedAt())
            ));
        }
        AuthUserDO seller = listing.getSellerUserId() == null ? null : authUserMapper.selectById(listing.getSellerUserId());
        String sellerPhone = seller == null ? "-" : defaultText(seller.getRealNamePhone(), defaultText(seller.getPhone(), "-"));
        PublishAttributesSnapshot attributes = readPublishAttributes(listing.getPublishAttributesJson());
        String exchangeRateLabel = formatRentalRatio(calculateRentalRatio(listing, attributes));
        List<Map<String, Object>> detailSections = buildListingDetailSections(listing, attributes, exchangeRateLabel);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("summary", mapOf(
            "listingNo", listing.getListingNo(),
            "title", listing.getTitle(),
            "status", listing.getStatus(),
            "statusText", renderListingStatus(listing.getStatus()),
            "sellerDisplayName", renderSellerDisplayName(listing),
            "sellerPhone", sellerPhone,
            "sellerTypeText", renderSellerType(listing.getSellerType()),
            "price", safeMoney(listing.getPrice()),
            "exchangeRateLabel", exchangeRateLabel,
            "reviewStrategy", renderReviewStrategy(listing.getReviewStrategy()),
            "submittedAt", formatTime(listing.getSubmittedAt()),
            "updatedAt", formatTime(listing.getUpdatedAt()),
            "publishedAt", formatTime(listing.getPublishedAt()),
            "coverUrl", previewNullable(listing.getCoverImageKey()),
            "imageUrls", buildImageUrls(listing.getImageKeysJson()),
            "description", defaultText(listing.getDescription(), "-"),
            "estimateDetail", defaultText(listing.getEstimateDetail(), "-"),
            "rejectionReason", defaultText(listing.getRejectionReason(), "")
        ));
        result.put("detailSections", detailSections);
        result.put("reviewRecords", buildReviewRecords(listing));
        result.put("tradeRecords", tradeRecords);
        log.info("admin listing detail loaded listingNo={} tradeCount={} costMs={}", listingNo, tradeRecords.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    @Transactional
    public Map<String, Object> reviewListing(String listingNo, String action, String reason) {
        AccountListingDO listing = requireListing(listingNo);
        String normalizedAction = normalizeAction(action, Arrays.asList("APPROVE", "REJECT", "OFFLINE"));
        LocalDateTime now = LocalDateTime.now();
        String messageTitle;
        String messageContent;
        if ("APPROVE".equals(normalizedAction)) {
            ensureListingCanBePublished(listing);
            listing.setStatus("PUBLISHED");
            listing.setPublishedAt(listing.getPublishedAt() == null ? now : listing.getPublishedAt());
            listing.setRejectionReason(null);
            messageTitle = "账号发布审核通过";
            messageContent = "你发布的账号《" + defaultText(listing.getTitle(), listingNo) + "》已审核通过，现已在交易大厅展示。";
        } else if ("REJECT".equals(normalizedAction)) {
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "驳回原因不能为空");
            }
            listing.setStatus("REJECTED");
            listing.setRejectionReason(reason.trim());
            listing.setPublishedAt(null);
            messageTitle = "账号发布审核驳回";
            messageContent = "你发布的账号《" + defaultText(listing.getTitle(), listingNo) + "》未通过审核，原因：" + reason.trim();
        } else {
            listing.setStatus("OFFLINE");
            messageTitle = "账号已下架";
            messageContent = "你发布的账号《" + defaultText(listing.getTitle(), listingNo) + "》已被平台下架，请进入我的发布查看详情。";
        }
        listing.setUpdatedAt(now);
        int rows = accountListingMapper.updateById(listing);
        createUserMessage(listing.getSellerUserId(), "TRADE", messageTitle, messageContent);
        log.info("admin listing review success listingNo={} action={} rows={}", listingNo, normalizedAction, rows);
        return mapOf(
            "listingNo", listingNo,
            "status", listing.getStatus(),
            "statusText", renderListingStatus(listing.getStatus()),
            "message", renderListingReviewMessage(normalizedAction)
        );
    }

    @Transactional
    public Map<String, Object> deleteListing(String listingNo) {
        AccountListingDO listing = requireListing(listingNo);
        if (!"OFFLINE".equals(listing.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先下架账号后再删除");
        }
        int rows = accountListingMapper.deleteById(listing.getId());
        createUserMessage(
            listing.getSellerUserId(),
            "TRADE",
            "账号记录已删除",
            "你发布的账号《" + defaultText(listing.getTitle(), listingNo) + "》已被平台删除。"
        );
        log.info("admin listing deleted listingNo={} rows={}", listingNo, rows);
        return mapOf("listingNo", listingNo, "message", "账号记录已删除");
    }

    public Map<String, Object> loadOrders(String status, String sellerType) {
        long startAt = System.currentTimeMillis();
        List<TradeOrderDO> rows = tradeOrderMapper.selectList(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status), TradeOrderDO::getStatus, status)
                .eq(StringUtils.hasText(sellerType) && !"ALL".equalsIgnoreCase(sellerType), TradeOrderDO::getSellerType, sellerType)
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
                "sellerDisplayName", row.getSellerDisplayName(),
                "sellerType", row.getSellerType(),
                "sellerTypeText", renderSellerType(row.getSellerType()),
                "status", row.getStatus(),
                "statusText", renderOrderStatus(row.getStatus()),
                "chatGroupNo", defaultText(row.getChatGroupNo(), ""),
                "paymentMethod", renderPaymentMethod(row.getPaymentMethod()),
                "itemAmount", safeMoney(row.getItemAmount()),
                "serviceFee", safeMoney(row.getServiceFee()),
                "totalAmount", safeMoney(row.getTotalAmount()),
                "createdAt", formatTime(row.getCreatedAt()),
                "paidAt", formatTime(row.getPaidAt()),
                "completedAt", formatTime(row.getCompletedAt()),
                "refundAmount", safeMoney(row.getRefundAmount()),
                "refundRequestedAt", formatTime(row.getRefundRequestedAt()),
                "refundReason", defaultText(row.getRefundReason(), ""),
                "canForceRefund", Arrays.asList("WAITING_TRADE", "IN_PROGRESS", "REFUND_PENDING", "AFTER_SALE").contains(row.getStatus())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rows", items);
        result.put("summary", mapOf(
            "pendingPayment", countOrders(rows, "PENDING_PAYMENT"),
            "waitingTrade", countOrders(rows, "WAITING_TRADE"),
            "completed", countOrders(rows, "COMPLETED"),
            "refundPending", countOrders(rows, "REFUND_PENDING"),
            "afterSale", countOrders(rows, "AFTER_SALE"),
            "refunded", countOrders(rows, "REFUNDED"),
            "closed", countOrders(rows, "CLOSED")
        ));
        log.info("admin orders loaded status={} sellerType={} count={} costMs={}", status, sellerType, items.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    public Map<String, Object> loadOrderDetail(String orderNo) {
        long startAt = System.currentTimeMillis();
        TradeOrderDO order = requireTradeOrder(orderNo);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("summary", mapOf(
            "orderNo", order.getOrderNo(),
            "listingNo", defaultText(order.getListingNo(), "-"),
            "listingTitle", defaultText(order.getListingTitle(), "-"),
            "listingSummary", defaultText(order.getListingSummary(), "-"),
            "buyerUserId", order.getBuyerUserId(),
            "buyerNickname", defaultText(order.getBuyerNickname(), "-"),
            "sellerUserId", order.getSellerUserId(),
            "sellerNickname", defaultText(order.getSellerNickname(), "-"),
            "sellerDisplayName", defaultText(order.getSellerDisplayName(), "-"),
            "sellerType", defaultText(order.getSellerType(), "-"),
            "sellerTypeText", renderSellerType(order.getSellerType()),
            "status", order.getStatus(),
            "statusText", renderOrderStatus(order.getStatus()),
            "chatGroupNo", defaultText(order.getChatGroupNo(), ""),
            "paymentMethod", renderPaymentMethod(order.getPaymentMethod()),
            "paymentTradeType", defaultText(order.getPaymentTradeType(), "-"),
            "paymentTransactionId", defaultText(order.getPaymentTransactionId(), "-"),
            "itemAmount", safeMoney(order.getItemAmount()),
            "serviceFee", safeMoney(order.getServiceFee()),
            "extraItemsAmount", safeMoney(order.getExtraItemsAmount()),
            "totalAmount", safeMoney(order.getTotalAmount()),
            "refundAmount", safeMoney(order.getRefundAmount()),
            "refundReason", defaultText(order.getRefundReason(), "-"),
            "refundReviewNote", defaultText(order.getRefundReviewNote(), "-"),
            "createdAt", formatTime(order.getCreatedAt()),
            "paidAt", formatTime(order.getPaidAt()),
            "tradeStartedAt", formatTime(order.getTradeStartedAt()),
            "completedAt", formatTime(order.getCompletedAt()),
            "closedAt", formatTime(order.getClosedAt()),
            "afterSaleAt", formatTime(order.getAfterSaleAt()),
            "refundRequestedAt", formatTime(order.getRefundRequestedAt()),
            "refundReviewedAt", formatTime(order.getRefundReviewedAt()),
            "refundedAt", formatTime(order.getRefundedAt()),
            "updatedAt", formatTime(order.getUpdatedAt()),
            "canForceRefund", Arrays.asList("WAITING_TRADE", "IN_PROGRESS", "REFUND_PENDING", "AFTER_SALE").contains(order.getStatus())
        ));
        result.put("detailSections", buildOrderDetailSections(order));
        result.put("progress", buildAdminOrderProgress(order));
        log.info("admin order detail loaded orderNo={} costMs={}", orderNo, System.currentTimeMillis() - startAt);
        return result;
    }

    public Map<String, Object> forceRefund(Long operatorUserId, String orderNo, String reason) {
        return orderService.forceRefund(operatorUserId, orderNo, reason);
    }

    public Map<String, Object> loadStudios(String keyword, String active) {
        long startAt = System.currentTimeMillis();
        String normalizedActive = StringUtils.hasText(active) ? active.trim().toUpperCase(Locale.ROOT) : null;
        List<StudioProfileDO> rows = studioProfileMapper.selectList(
            Wrappers.<StudioProfileDO>lambdaQuery()
                .eq(StringUtils.hasText(normalizedActive) && !"ALL".equalsIgnoreCase(normalizedActive), StudioProfileDO::getCooperationStatus, normalizedActive)
                .orderByDesc(StudioProfileDO::getUpdatedAt)
        );
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (StudioProfileDO row : rows) {
            AuthUserDO owner = authUserMapper.selectById(row.getOwnerUserId());
            if (normalizedKeyword != null) {
                String haystack = (
                    defaultText(row.getStudioName(), "") + "|" +
                    defaultText(owner == null ? null : owner.getNickname(), "") + "|" +
                    defaultText(owner == null ? null : owner.getPhone(), "")
                ).toLowerCase(Locale.ROOT);
                if (!haystack.contains(normalizedKeyword)) {
                    continue;
                }
            }
            items.add(mapOf(
                "studioId", row.getId(),
                "studioName", row.getStudioName(),
                "ownerUserId", row.getOwnerUserId(),
                "ownerNickname", owner == null ? "-" : owner.getNickname(),
                "ownerPhoneRaw", owner == null ? "" : defaultText(owner.getPhone(), ""),
                "ownerPhone", owner == null ? "-" : maskPhone(owner.getPhone()),
                "contactName", defaultText(row.getContactName(), "-"),
                "qualificationCode", defaultText(row.getQualificationCode(), "-"),
                "reviewStrategy", row.getReviewStrategy(),
                "reviewStrategyText", renderReviewStrategy(row.getReviewStrategy()),
                "shareRatio", renderShareRatio(row.getShareRatio()),
                "active", row.getActive(),
                "cooperationStatus", normalizeCooperationStatus(row.getCooperationStatus()),
                "activeText", renderCooperationStatus(row.getCooperationStatus(), row.getActive()),
                "listingCount", countStudioListings(row.getOwnerUserId()),
                "orderCount", countStudioOrders(row.getOwnerUserId()),
                "gmv", formatMoney(sumStudioGmv(row.getOwnerUserId())),
                "createdAt", formatTime(row.getCreatedAt())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rows", items);
        result.put("summary", mapOf(
            "activeCount", countActiveStudios(rows),
            "directPublishCount", countStudioStrategy(rows, "DIRECT_PUBLISH"),
            "reviewRequiredCount", countStudioStrategy(rows, "REVIEW_REQUIRED"),
            "clearedCount", countStudioCooperation(rows, "CLEARED")
        ));
        log.info("admin studio center loaded active={} keyword={} count={} costMs={}", normalizedActive, maskKeyword(keyword), items.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    public Map<String, Object> loadStudioDetail(Long studioId) {
        long startAt = System.currentTimeMillis();
        StudioProfileDO studio = requireStudio(studioId);
        AuthUserDO owner = authUserMapper.selectById(studio.getOwnerUserId());
        List<AccountListingDO> recentListings = accountListingMapper.selectList(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getSellerUserId, studio.getOwnerUserId())
                .orderByDesc(AccountListingDO::getUpdatedAt)
                .last("LIMIT 6")
        );
        List<TradeOrderDO> recentOrders = tradeOrderMapper.selectList(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getSellerUserId, studio.getOwnerUserId())
                .orderByDesc(TradeOrderDO::getCreatedAt)
                .last("LIMIT 6")
        );
        List<Map<String, Object>> listingRows = new ArrayList<Map<String, Object>>();
        for (AccountListingDO row : recentListings) {
            listingRows.add(mapOf(
                "listingNo", row.getListingNo(),
                "title", row.getTitle(),
                "statusText", renderListingStatus(row.getStatus()),
                "price", safeMoney(row.getPrice()),
                "updatedAt", formatTime(row.getUpdatedAt())
            ));
        }
        List<Map<String, Object>> orderRows = new ArrayList<Map<String, Object>>();
        for (TradeOrderDO row : recentOrders) {
            orderRows.add(mapOf(
                "orderNo", row.getOrderNo(),
                "listingTitle", row.getListingTitle(),
                "buyerNickname", defaultText(row.getBuyerNickname(), "-"),
                "statusText", renderOrderStatus(row.getStatus()),
                "totalAmount", safeMoney(row.getTotalAmount()),
                "createdAt", formatTime(row.getCreatedAt())
            ));
        }
        Map<String, Object> result = mapOf(
            "summary", mapOf(
                "studioId", studio.getId(),
                "studioName", defaultText(studio.getStudioName(), "-"),
                "description", defaultText(studio.getDescription(), "暂未填写工作室简介"),
                "contactPhone", defaultText(studio.getContactPhone(), "-"),
                "contactName", defaultText(studio.getContactName(), "-"),
                "contactWechat", defaultText(studio.getContactWechat(), "-"),
                "qualificationCode", defaultText(studio.getQualificationCode(), "-"),
                "qualificationMaterialKey", defaultText(studio.getQualificationMaterialKey(), ""),
                "qualificationMaterialUrl", previewNullable(studio.getQualificationMaterialKey()),
                "qualificationNote", defaultText(studio.getQualificationNote(), "-"),
                "ownerNickname", owner == null ? "-" : defaultText(owner.getNickname(), "-"),
                "ownerPhoneRaw", owner == null ? "" : defaultText(owner.getPhone(), ""),
                "ownerPhone", owner == null ? "-" : maskPhone(owner.getPhone()),
                "reviewStrategyText", renderReviewStrategy(studio.getReviewStrategy()),
                "shareRatio", renderShareRatio(studio.getShareRatio()),
                "cooperationStatus", normalizeCooperationStatus(studio.getCooperationStatus()),
                "activeText", renderCooperationStatus(studio.getCooperationStatus(), studio.getActive()),
                "createdAt", formatTime(studio.getCreatedAt()),
                "updatedAt", formatTime(studio.getUpdatedAt())
            ),
            "recentListings", listingRows,
            "recentOrders", orderRows
        );
        log.info("admin studio detail loaded studioId={} ownerUserId={} listingCount={} orderCount={} costMs={}",
            studioId, studio.getOwnerUserId(), listingRows.size(), orderRows.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    @Transactional
    public Map<String, Object> saveStudio(
        Long studioId,
        String ownerPhone,
        String studioName,
        String description,
        String contactPhone,
        String contactName,
        String contactWechat,
        String qualificationCode,
        String qualificationMaterialKey,
        String qualificationNote,
        String reviewStrategy,
        BigDecimal shareRatio,
        Boolean active,
        String cooperationStatus
    ) {
        LocalDateTime now = LocalDateTime.now();
        StudioProfileDO row = studioId == null ? new StudioProfileDO() : requireStudio(studioId);
        AuthUserDO owner = requireUserByPhone(ownerPhone);
        StudioProfileDO duplicated = studioProfileMapper.selectOne(
            Wrappers.<StudioProfileDO>lambdaQuery()
                .eq(StudioProfileDO::getOwnerUserId, owner.getId())
                .ne(studioId != null, StudioProfileDO::getId, studioId)
                .last("LIMIT 1")
        );
        if (duplicated != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该负责人已绑定其他工作室");
        }
        row.setOwnerUserId(owner.getId());
        row.setStudioName(normalizeRequired(studioName, "工作室名称不能为空"));
        row.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        row.setContactPhone(normalizeRequired(contactPhone, "联系电话不能为空"));
        row.setContactName(StringUtils.hasText(contactName) ? contactName.trim() : null);
        row.setContactWechat(StringUtils.hasText(contactWechat) ? contactWechat.trim() : null);
        row.setQualificationCode(StringUtils.hasText(qualificationCode) ? qualificationCode.trim() : null);
        row.setQualificationMaterialKey(StringUtils.hasText(qualificationMaterialKey) ? qualificationMaterialKey.trim() : null);
        row.setQualificationNote(StringUtils.hasText(qualificationNote) ? qualificationNote.trim() : null);
        row.setReviewStrategy(normalizeAction(reviewStrategy, Arrays.asList("DIRECT_PUBLISH", "REVIEW_REQUIRED")));
        row.setShareRatio(normalizeShareRatio(shareRatio));
        String normalizedCooperation = normalizeAction(cooperationStatus, Arrays.asList("ACTIVE", "PAUSED", "CLEARED"));
        row.setCooperationStatus(normalizedCooperation);
        row.setActive(active != null ? active : Boolean.TRUE);
        if ("CLEARED".equals(normalizedCooperation)) {
            row.setActive(Boolean.FALSE);
        } else if ("ACTIVE".equals(normalizedCooperation)) {
            row.setActive(Boolean.TRUE);
        }
        row.setUpdatedAt(now);
        if (studioId == null) {
            row.setCreatedAt(now);
            studioProfileMapper.insert(row);
            log.info("admin studio created studioId={} ownerUserId={} reviewStrategy={} shareRatio={} cooperationStatus={} active={}",
                row.getId(), row.getOwnerUserId(), row.getReviewStrategy(), row.getShareRatio(), row.getCooperationStatus(), row.getActive());
            return mapOf("studioId", row.getId(), "message", "工作室已创建");
        }
        studioProfileMapper.updateById(row);
        log.info("admin studio updated studioId={} ownerUserId={} reviewStrategy={} shareRatio={} cooperationStatus={} active={}",
            row.getId(), row.getOwnerUserId(), row.getReviewStrategy(), row.getShareRatio(), row.getCooperationStatus(), row.getActive());
        return mapOf("studioId", row.getId(), "message", "工作室资料已更新");
    }

    @Transactional
    public Map<String, Object> updateStudioPolicy(Long studioId, String reviewStrategy) {
        StudioProfileDO row = requireStudio(studioId);
        String normalized = normalizeAction(reviewStrategy, Arrays.asList("DIRECT_PUBLISH", "REVIEW_REQUIRED"));
        row.setReviewStrategy(normalized);
        row.setUpdatedAt(LocalDateTime.now());
        int rows = studioProfileMapper.updateById(row);
        log.info("admin studio policy updated studioId={} reviewStrategy={} rows={}", studioId, normalized, rows);
        return mapOf(
            "studioId", studioId,
            "reviewStrategy", normalized,
            "reviewStrategyText", renderReviewStrategy(normalized)
        );
    }

    @Transactional
    public Map<String, Object> updateStudioShareRatio(Long studioId, BigDecimal shareRatio) {
        StudioProfileDO row = requireStudio(studioId);
        BigDecimal normalized = normalizeShareRatio(shareRatio);
        row.setShareRatio(normalized);
        row.setUpdatedAt(LocalDateTime.now());
        studioProfileMapper.updateById(row);
        log.info("admin studio share ratio updated studioId={} shareRatio={}", studioId, normalized);
        return mapOf(
            "studioId", studioId,
            "shareRatio", renderShareRatio(normalized)
        );
    }

    @Transactional
    public Map<String, Object> updateStudioStatus(Long studioId, Boolean active) {
        StudioProfileDO row = requireStudio(studioId);
        row.setActive(active != null ? active : Boolean.FALSE);
        row.setCooperationStatus(Boolean.TRUE.equals(row.getActive()) ? "ACTIVE" : "PAUSED");
        row.setUpdatedAt(LocalDateTime.now());
        int rows = studioProfileMapper.updateById(row);
        log.info("admin studio status updated studioId={} active={} cooperationStatus={} rows={}",
            studioId, row.getActive(), row.getCooperationStatus(), rows);
        return mapOf(
            "studioId", studioId,
            "active", row.getActive(),
            "activeText", renderCooperationStatus(row.getCooperationStatus(), row.getActive())
        );
    }

    public Map<String, Object> loadBoostingServices(String status, String providerType) {
        long startAt = System.currentTimeMillis();
        List<BoostingServiceDO> rows = boostingServiceMapper.selectList(
            Wrappers.<BoostingServiceDO>lambdaQuery()
                .eq(StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status), BoostingServiceDO::getStatus, status)
                .eq(StringUtils.hasText(providerType) && !"ALL".equalsIgnoreCase(providerType), BoostingServiceDO::getProviderType, providerType)
                .orderByAsc(BoostingServiceDO::getSortNo)
                .orderByDesc(BoostingServiceDO::getUpdatedAt)
        );
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (BoostingServiceDO row : rows) {
            items.add(mapOf(
                "serviceNo", row.getServiceNo(),
                "name", row.getName(),
                "categoryLabel", row.getCategoryLabel(),
                "description", row.getDescription(),
                "price", safeMoney(row.getPrice()),
                "cycleLabel", row.getCycleLabel(),
                "guaranteeNote", row.getGuaranteeNote(),
                "providerType", row.getProviderType(),
                "providerTypeText", renderProviderType(row.getProviderType()),
                "providerName", row.getProviderName(),
                "salesCount", safeNumber(row.getSalesCount()),
                "status", row.getStatus(),
                "statusText", "ACTIVE".equals(row.getStatus()) ? "启用中" : "已停用",
                "sortNo", safeNumber(row.getSortNo())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rows", items);
        result.put("summary", mapOf(
            "activeCount", countBoostingServices(rows, "ACTIVE"),
            "disabledCount", countBoostingServices(rows, "DISABLED")
        ));
        log.info("admin boosting services loaded status={} providerType={} count={} costMs={}", status, providerType, items.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    @Transactional
    public Map<String, Object> updateBoostingServiceStatus(String serviceNo, String status) {
        BoostingServiceDO row = requireBoostingService(serviceNo);
        String normalized = normalizeAction(status, Arrays.asList("ACTIVE", "DISABLED"));
        row.setStatus(normalized);
        row.setUpdatedAt(LocalDateTime.now());
        int rows = boostingServiceMapper.updateById(row);
        log.info("admin boosting service status updated serviceNo={} status={} rows={}", serviceNo, normalized, rows);
        return mapOf(
            "serviceNo", serviceNo,
            "status", normalized,
            "statusText", "ACTIVE".equals(normalized) ? "启用中" : "已停用"
        );
    }

    public Map<String, Object> loadWithdrawApplications(String status) {
        long startAt = System.currentTimeMillis();
        List<WithdrawApplicationDO> rows = withdrawApplicationMapper.selectList(
            Wrappers.<WithdrawApplicationDO>lambdaQuery()
                .eq(StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status), WithdrawApplicationDO::getStatus, status)
                .orderByDesc(WithdrawApplicationDO::getCreatedAt)
                .last("LIMIT 120")
        );
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (WithdrawApplicationDO row : rows) {
            AuthUserDO user = authUserMapper.selectById(row.getUserId());
            String qrCodeKey = resolveWithdrawQrCodeKey(row);
            items.add(mapOf(
                "applicationNo", row.getApplicationNo(),
                "userId", row.getUserId(),
                "nickname", user == null ? "-" : user.getNickname(),
                "realName", defaultText(row.getAccountName(), "-"),
                "channel", row.getChannel(),
                "channelText", renderPaymentMethod(row.getChannel()),
                "amount", safeMoney(row.getAmount()),
                "status", row.getStatus(),
                "statusText", renderWithdrawStatus(row.getStatus()),
                "rejectReason", defaultText(row.getRejectReason(), ""),
                "accountNo", defaultText(row.getAccountNo(), "-"),
                "qrCodeUrl", previewNullable(qrCodeKey),
                "createdAt", formatTime(row.getCreatedAt()),
                "reviewedAt", formatTime(row.getReviewedAt()),
                "paidAt", formatTime(row.getPaidAt())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rows", items);
        result.put("summary", mapOf(
            "pending", countWithdraws(rows, "PENDING"),
            "paid", countWithdraws(rows, "PAID"),
            "rejected", countWithdraws(rows, "REJECTED")
        ));
        log.info("admin withdraw center loaded status={} count={} costMs={}", status, items.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    public Map<String, Object> loadStudioWithdrawApplications(String status) {
        long startAt = System.currentTimeMillis();
        List<StudioWithdrawApplicationDO> rows = studioWithdrawApplicationMapper.selectList(
            Wrappers.<StudioWithdrawApplicationDO>lambdaQuery()
                .eq(StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status), StudioWithdrawApplicationDO::getStatus, status)
                .orderByDesc(StudioWithdrawApplicationDO::getCreatedAt)
                .last("LIMIT 120")
        );
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (StudioWithdrawApplicationDO row : rows) {
            AuthUserDO user = authUserMapper.selectById(row.getOwnerUserId());
            StudioProfileDO studio = studioProfileMapper.selectOne(
                Wrappers.<StudioProfileDO>lambdaQuery().eq(StudioProfileDO::getOwnerUserId, row.getOwnerUserId()).last("LIMIT 1")
            );
            items.add(mapOf(
                "applicationNo", row.getApplicationNo(),
                "ownerUserId", row.getOwnerUserId(),
                "studioName", studio == null ? "-" : defaultText(studio.getStudioName(), "-"),
                "ownerNickname", user == null ? "-" : defaultText(user.getNickname(), "-"),
                "accountName", defaultText(row.getAccountName(), "-"),
                "channel", row.getChannel(),
                "channelText", renderPaymentMethod(row.getChannel()),
                "amount", safeMoney(row.getAmount()),
                "status", row.getStatus(),
                "statusText", renderWithdrawStatus(row.getStatus()),
                "rejectReason", defaultText(row.getRejectReason(), ""),
                "accountNo", defaultText(row.getAccountNo(), "-"),
                "createdAt", formatTime(row.getCreatedAt()),
                "reviewedAt", formatTime(row.getReviewedAt()),
                "paidAt", formatTime(row.getPaidAt())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rows", items);
        result.put("summary", mapOf(
            "pending", countStudioWithdraws(rows, "PENDING"),
            "paid", countStudioWithdraws(rows, "PAID"),
            "rejected", countStudioWithdraws(rows, "REJECTED")
        ));
        log.info("admin studio withdraw center loaded status={} count={} costMs={}", status, items.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    @Transactional
    public Map<String, Object> reviewWithdraw(String applicationNo, String action, String reason) {
        WithdrawApplicationDO row = requireWithdraw(applicationNo);
        if (!"PENDING".equals(row.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前提现单状态不允许重复审核");
        }
        UserWalletDO wallet = requireWallet(row.getUserId());
        String normalized = normalizeAction(action, Arrays.asList("APPROVE", "REJECT"));
        LocalDateTime now = LocalDateTime.now();
        row.setReviewedAt(now);
        row.setUpdatedAt(now);
        if ("APPROVE".equals(normalized)) {
            row.setStatus("PAID");
            row.setPaidAt(now);
            wallet.setFrozenBalance(safeAmount(wallet.getFrozenBalance()).subtract(row.getAmount()));
            syncWithdrawTransaction(row, "SUCCESS", "提现申请已审核通过");
            createUserMessage(
                row.getUserId(),
                "SYSTEM",
                "提现申请审核通过",
                "你的提现申请 " + applicationNo + " 已审核通过，金额 " + safeMoney(row.getAmount()) + "，将按" + renderPaymentMethod(row.getChannel()) + "渠道打款。"
            );
        } else {
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "驳回原因不能为空");
            }
            row.setStatus("REJECTED");
            row.setRejectReason(reason.trim());
            wallet.setFrozenBalance(safeAmount(wallet.getFrozenBalance()).subtract(row.getAmount()));
            wallet.setAvailableBalance(safeAmount(wallet.getAvailableBalance()).add(row.getAmount()));
            syncWithdrawTransaction(row, "FAILED", "提现申请已驳回：" + reason.trim());
            createUserMessage(
                row.getUserId(),
                "SYSTEM",
                "提现申请审核驳回",
                "你的提现申请 " + applicationNo + " 已被驳回，金额已退回钱包余额。驳回原因：" + reason.trim()
            );
        }
        wallet.setUpdatedAt(now);
        withdrawApplicationMapper.updateById(row);
        userWalletMapper.updateById(wallet);
        log.info("admin withdraw review success applicationNo={} action={} amount={}", applicationNo, normalized, row.getAmount());
        return mapOf(
            "applicationNo", applicationNo,
            "status", row.getStatus(),
            "statusText", renderWithdrawStatus(row.getStatus()),
            "message", "APPROVE".equals(normalized) ? "提现申请已审核通过" : "提现申请已驳回并退回余额"
        );
    }

    private void syncWithdrawTransaction(WithdrawApplicationDO application, String status, String remark) {
        WalletTransactionDO transaction = walletTransactionMapper.selectOne(
            Wrappers.<WalletTransactionDO>lambdaQuery()
                .eq(WalletTransactionDO::getUserId, application.getUserId())
                .eq(WalletTransactionDO::getRelatedNo, application.getApplicationNo())
                .eq(WalletTransactionDO::getBizType, "WITHDRAW")
                .last("LIMIT 1")
        );
        if (transaction == null) {
            return;
        }
        transaction.setStatus(status);
        transaction.setRemark(remark);
        transaction.setUpdatedAt(LocalDateTime.now());
        walletTransactionMapper.updateById(transaction);
    }

    @Transactional
    public Map<String, Object> reviewStudioWithdraw(String applicationNo, String action, String reason) {
        StudioWithdrawApplicationDO row = requireStudioWithdraw(applicationNo);
        if (!"PENDING".equals(row.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前工作室提现单状态不允许重复审核");
        }
        String normalized = normalizeAction(action, Arrays.asList("APPROVE", "REJECT"));
        LocalDateTime now = LocalDateTime.now();
        row.setReviewedAt(now);
        row.setUpdatedAt(now);
        if ("APPROVE".equals(normalized)) {
            row.setStatus("PAID");
            row.setPaidAt(now);
            createUserMessage(
                row.getOwnerUserId(),
                "DISTRIBUTION",
                "工作室提现审核通过",
                "你的工作室提现申请 " + applicationNo + " 已审核通过，金额 " + safeMoney(row.getAmount()) + "，将按" + renderPaymentMethod(row.getChannel()) + "渠道打款。"
            );
        } else {
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "驳回原因不能为空");
            }
            row.setStatus("REJECTED");
            row.setRejectReason(reason.trim());
            createUserMessage(
                row.getOwnerUserId(),
                "DISTRIBUTION",
                "工作室提现审核驳回",
                "你的工作室提现申请 " + applicationNo + " 已被驳回。驳回原因：" + reason.trim()
            );
        }
        studioWithdrawApplicationMapper.updateById(row);
        log.info("admin studio withdraw review success applicationNo={} action={} amount={}", applicationNo, normalized, row.getAmount());
        return mapOf(
            "applicationNo", applicationNo,
            "status", row.getStatus(),
            "statusText", renderWithdrawStatus(row.getStatus()),
            "message", "APPROVE".equals(normalized) ? "工作室提现申请已审核通过" : "工作室提现申请已驳回"
        );
    }

    public Map<String, Object> loadOperationCenter() {
        long startAt = System.currentTimeMillis();
        List<OperationBannerDO> banners = operationBannerMapper.selectList(
            Wrappers.<OperationBannerDO>lambdaQuery().orderByAsc(OperationBannerDO::getSortNo).orderByDesc(OperationBannerDO::getUpdatedAt)
        );
        List<OperationShortcutDO> shortcuts = operationShortcutMapper.selectList(
            Wrappers.<OperationShortcutDO>lambdaQuery().orderByAsc(OperationShortcutDO::getSortNo).orderByDesc(OperationShortcutDO::getUpdatedAt)
        );
        List<OperationAnnouncementDO> announcements = operationAnnouncementMapper.selectList(
            Wrappers.<OperationAnnouncementDO>lambdaQuery()
                .orderByDesc(OperationAnnouncementDO::getPinned)
                .orderByDesc(OperationAnnouncementDO::getPublishAt)
                .orderByDesc(OperationAnnouncementDO::getUpdatedAt)
                .last("LIMIT 40")
        );
        List<Map<String, Object>> bannerRows = new ArrayList<Map<String, Object>>();
        for (OperationBannerDO row : banners) {
            bannerRows.add(mapOf(
                "bannerId", row.getId(),
                "bannerNo", row.getBannerNo(),
                "title", row.getTitle(),
                "imageUrl", previewNullable(row.getImageKey()),
                "imageKey", row.getImageKey(),
                "linkUrl", defaultText(row.getLinkUrl(), "-"),
                "sortNo", safeNumber(row.getSortNo()),
                "status", row.getStatus(),
                "statusText", renderOperationStatus(row.getStatus()),
                "updatedAt", formatTime(row.getUpdatedAt())
            ));
        }
        List<Map<String, Object>> shortcutRows = new ArrayList<Map<String, Object>>();
        for (OperationShortcutDO row : shortcuts) {
            shortcutRows.add(mapOf(
                "shortcutId", row.getId(),
                "shortcutNo", row.getShortcutNo(),
                "name", row.getName(),
                "iconUrl", previewNullable(row.getIconKey()),
                "iconKey", row.getIconKey(),
                "linkUrl", row.getLinkUrl(),
                "sortNo", safeNumber(row.getSortNo()),
                "status", row.getStatus(),
                "statusText", renderOperationStatus(row.getStatus()),
                "updatedAt", formatTime(row.getUpdatedAt())
            ));
        }
        List<Map<String, Object>> announcementRows = new ArrayList<Map<String, Object>>();
        for (OperationAnnouncementDO row : announcements) {
            announcementRows.add(mapOf(
                "announcementId", row.getId(),
                "announcementNo", row.getAnnouncementNo(),
                "title", row.getTitle(),
                "content", row.getContent(),
                "category", row.getCategory(),
                "categoryText", renderAnnouncementCategory(row.getCategory()),
                "pinned", Boolean.TRUE.equals(row.getPinned()),
                "status", row.getStatus(),
                "statusText", renderAnnouncementStatus(row.getStatus()),
                "publishAt", formatTime(row.getPublishAt()),
                "updatedAt", formatTime(row.getUpdatedAt())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("banners", bannerRows);
        result.put("shortcuts", shortcutRows);
        result.put("announcements", announcementRows);
        result.put("summary", mapOf(
            "bannerCount", bannerRows.size(),
            "shortcutCount", shortcutRows.size(),
            "publishedAnnouncementCount", countAnnouncementStatus(announcements, "PUBLISHED")
        ));
        log.info("admin operation center loaded banners={} shortcuts={} announcements={} costMs={}",
            bannerRows.size(), shortcutRows.size(), announcementRows.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    @Transactional
    public Map<String, Object> saveBanner(Long bannerId, String title, String imageKey, String linkUrl, Integer sortNo, String status) {
        LocalDateTime now = LocalDateTime.now();
        OperationBannerDO row = bannerId == null ? null : operationBannerMapper.selectById(bannerId);
        if (row == null) {
            row = new OperationBannerDO();
            row.setBannerNo("BN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
            row.setCreatedAt(now);
        }
        row.setTitle(normalizeRequired(title, "轮播标题不能为空"));
        row.setImageKey(normalizeRequired(imageKey, "轮播图片不能为空"));
        row.setLinkUrl(StringUtils.hasText(linkUrl) ? linkUrl.trim() : null);
        row.setSortNo(sortNo == null ? 0 : sortNo);
        row.setStatus(normalizeAction(status, Arrays.asList("ACTIVE", "DISABLED")));
        row.setUpdatedAt(now);
        if (row.getId() == null) {
            operationBannerMapper.insert(row);
        } else {
            operationBannerMapper.updateById(row);
        }
        log.info("admin banner saved bannerNo={} bannerId={} status={} sortNo={}", row.getBannerNo(), row.getId(), row.getStatus(), row.getSortNo());
        return mapOf("bannerNo", row.getBannerNo(), "message", "轮播图已保存");
    }

    @Transactional
    public Map<String, Object> saveShortcut(Long shortcutId, String name, String iconKey, String linkUrl, Integer sortNo, String status) {
        LocalDateTime now = LocalDateTime.now();
        OperationShortcutDO row = shortcutId == null ? null : operationShortcutMapper.selectById(shortcutId);
        if (row == null) {
            row = new OperationShortcutDO();
            row.setShortcutNo("SC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
            row.setCreatedAt(now);
        }
        row.setName(normalizeRequired(name, "快捷入口名称不能为空"));
        row.setIconKey(StringUtils.hasText(iconKey) ? iconKey.trim() : null);
        row.setLinkUrl(normalizeRequired(linkUrl, "跳转链接不能为空"));
        row.setSortNo(sortNo == null ? 0 : sortNo);
        row.setStatus(normalizeAction(status, Arrays.asList("ACTIVE", "DISABLED")));
        row.setUpdatedAt(now);
        if (row.getId() == null) {
            operationShortcutMapper.insert(row);
        } else {
            operationShortcutMapper.updateById(row);
        }
        log.info("admin shortcut saved shortcutNo={} shortcutId={} status={} sortNo={}", row.getShortcutNo(), row.getId(), row.getStatus(), row.getSortNo());
        return mapOf("shortcutNo", row.getShortcutNo(), "message", "金刚区入口已保存");
    }

    @Transactional
    public Map<String, Object> saveAnnouncement(Long announcementId, String title, String content, String category, Boolean pinned, String status) {
        LocalDateTime now = LocalDateTime.now();
        OperationAnnouncementDO row = announcementId == null ? null : operationAnnouncementMapper.selectById(announcementId);
        if (row == null) {
            row = new OperationAnnouncementDO();
            row.setAnnouncementNo("AN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
            row.setCreatedAt(now);
        }
        row.setTitle(normalizeRequired(title, "公告标题不能为空"));
        row.setContent(normalizeRequired(content, "公告内容不能为空"));
        row.setCategory(normalizeAction(category, Arrays.asList("SYSTEM", "ACTIVITY", "TRADE")));
        row.setPinned(Boolean.TRUE.equals(pinned));
        row.setStatus(normalizeAction(status, Arrays.asList("DRAFT", "PUBLISHED", "OFFLINE")));
        row.setPublishAt("PUBLISHED".equals(row.getStatus()) ? now : row.getPublishAt());
        row.setUpdatedAt(now);
        if (row.getId() == null) {
            operationAnnouncementMapper.insert(row);
        } else {
            operationAnnouncementMapper.updateById(row);
        }
        log.info("admin announcement saved announcementNo={} announcementId={} status={} pinned={}",
            row.getAnnouncementNo(), row.getId(), row.getStatus(), row.getPinned());
        return mapOf("announcementNo", row.getAnnouncementNo(), "message", "公告已保存");
    }

    @Transactional
    public Map<String, Object> deleteBanner(Long bannerId) {
        OperationBannerDO row = requireBanner(bannerId);
        operationBannerMapper.deleteById(bannerId);
        log.info("admin banner deleted bannerId={} bannerNo={}", bannerId, row.getBannerNo());
        return mapOf("bannerId", bannerId, "message", "轮播图已删除");
    }

    @Transactional
    public Map<String, Object> deleteShortcut(Long shortcutId) {
        OperationShortcutDO row = requireShortcut(shortcutId);
        operationShortcutMapper.deleteById(shortcutId);
        log.info("admin shortcut deleted shortcutId={} shortcutNo={}", shortcutId, row.getShortcutNo());
        return mapOf("shortcutId", shortcutId, "message", "快捷入口已删除");
    }

    @Transactional
    public Map<String, Object> deleteAnnouncement(Long announcementId) {
        OperationAnnouncementDO row = requireAnnouncement(announcementId);
        operationAnnouncementMapper.deleteById(announcementId);
        log.info("admin announcement deleted announcementId={} announcementNo={}", announcementId, row.getAnnouncementNo());
        return mapOf("announcementId", announcementId, "message", "公告已删除");
    }

    @Transactional
    public Map<String, Object> batchUpdateOperationStatus(String type, List<Long> ids, String status) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择至少一条记录");
        }
        String normalizedType = normalizeOperationType(type);
        String normalizedStatus = normalizeAction(status, Arrays.asList("ACTIVE", "DISABLED", "PUBLISHED", "DRAFT", "OFFLINE"));
        LocalDateTime now = LocalDateTime.now();
        int updated;
        if ("banner".equals(normalizedType)) {
            updated = updateBannerStatus(ids, normalizeAction(normalizedStatus, Arrays.asList("ACTIVE", "DISABLED")), now);
        } else if ("shortcut".equals(normalizedType)) {
            updated = updateShortcutStatus(ids, normalizeAction(normalizedStatus, Arrays.asList("ACTIVE", "DISABLED")), now);
        } else {
            updated = updateAnnouncementStatus(ids, normalizeAction(normalizedStatus, Arrays.asList("PUBLISHED", "DRAFT", "OFFLINE")), now);
        }
        log.info("admin operation batch status success type={} status={} count={}", normalizedType, normalizedStatus, updated);
        return mapOf("type", normalizedType, "status", normalizedStatus, "count", updated, "message", "批量状态已更新");
    }

    public Map<String, Object> loadUsers(String keyword, String status, String verified, String studioOwner) {
        long startAt = System.currentTimeMillis();
        List<AuthUserDO> rows = authUserMapper.selectList(
            Wrappers.<AuthUserDO>lambdaQuery()
                .eq(StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status), AuthUserDO::getAccountStatus, status)
                .eq(StringUtils.hasText(verified) && !"ALL".equalsIgnoreCase(verified), AuthUserDO::getVerified, Boolean.valueOf("VERIFIED".equalsIgnoreCase(verified) || "true".equalsIgnoreCase(verified)))
                .orderByDesc(AuthUserDO::getUpdatedAt)
                .last("LIMIT 160")
        );
        Set<Long> studioOwnerIds = loadStudioOwnerIds();
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (AuthUserDO row : rows) {
            boolean isStudioOwner = studioOwnerIds.contains(row.getId());
            if (StringUtils.hasText(studioOwner) && !"ALL".equalsIgnoreCase(studioOwner)) {
                if ("YES".equalsIgnoreCase(studioOwner) && !isStudioOwner) {
                    continue;
                }
                if ("NO".equalsIgnoreCase(studioOwner) && isStudioOwner) {
                    continue;
                }
            }
            if (normalizedKeyword != null) {
                String haystack = (
                    defaultText(row.getNickname(), "") + "|" +
                    defaultText(row.getPhone(), "") + "|" +
                    defaultText(row.getRealName(), "")
                ).toLowerCase(Locale.ROOT);
                if (!haystack.contains(normalizedKeyword)) {
                    continue;
                }
            }
            items.add(mapOf(
                "userId", row.getId(),
                "nickname", row.getNickname(),
                "phone", maskPhone(row.getPhone()),
                "verified", Boolean.TRUE.equals(row.getVerified()),
                "verifiedText", Boolean.TRUE.equals(row.getVerified()) ? "已实名" : "未实名",
                "realNameStatus", defaultText(row.getRealNameStatus(), "UNVERIFIED"),
                "realNameStatusText", renderRealNameStatus(row.getRealNameStatus()),
                "accountStatus", defaultText(row.getAccountStatus(), "ACTIVE"),
                "accountStatusText", renderAccountStatus(row.getAccountStatus()),
                "banReason", defaultText(row.getBanReason(), ""),
                "isStudioOwner", isStudioOwner,
                "studioOwnerText", isStudioOwner ? "工作室管理员" : "普通用户",
                "createdAt", formatTime(row.getCreatedAt()),
                "updatedAt", formatTime(row.getUpdatedAt())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rows", items);
        result.put("summary", mapOf(
            "activeCount", countUserStatus(rows, "ACTIVE"),
            "disabledCount", countUserStatus(rows, "DISABLED"),
            "verifiedCount", countVerified(rows, true),
            "studioOwnerCount", studioOwnerIds.size()
        ));
        log.info("admin users loaded status={} verified={} studioOwner={} keyword={} count={} costMs={}",
            status, verified, studioOwner, maskKeyword(keyword), items.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    public Map<String, Object> loadUserDetail(Long userId) {
        long startAt = System.currentTimeMillis();
        AuthUserDO row = requireUser(userId);
        StudioProfileDO studio = studioProfileMapper.selectOne(
            Wrappers.<StudioProfileDO>lambdaQuery()
                .eq(StudioProfileDO::getOwnerUserId, userId)
                .last("LIMIT 1")
        );
        UserWalletDO wallet = userWalletMapper.selectOne(
            Wrappers.<UserWalletDO>lambdaQuery()
                .eq(UserWalletDO::getUserId, userId)
                .last("LIMIT 1")
        );
        long buyerOrderCount = tradeOrderMapper.selectCount(
            Wrappers.<TradeOrderDO>lambdaQuery().eq(TradeOrderDO::getBuyerUserId, userId)
        );
        long sellerOrderCount = tradeOrderMapper.selectCount(
            Wrappers.<TradeOrderDO>lambdaQuery().eq(TradeOrderDO::getSellerUserId, userId)
        );
        long publishCount = accountListingMapper.selectCount(
            Wrappers.<AccountListingDO>lambdaQuery().eq(AccountListingDO::getSellerUserId, userId)
        );
        long publishedCount = accountListingMapper.selectCount(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getSellerUserId, userId)
                .eq(AccountListingDO::getStatus, "PUBLISHED")
        );
        long pendingListingCount = accountListingMapper.selectCount(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getSellerUserId, userId)
                .eq(AccountListingDO::getStatus, "PENDING_REVIEW")
        );
        long rejectedListingCount = accountListingMapper.selectCount(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getSellerUserId, userId)
                .eq(AccountListingDO::getStatus, "REJECTED")
        );
        List<TradeOrderDO> recentOrders = tradeOrderMapper.selectList(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .and(wrapper -> wrapper.eq(TradeOrderDO::getBuyerUserId, userId).or().eq(TradeOrderDO::getSellerUserId, userId))
                .orderByDesc(TradeOrderDO::getCreatedAt)
                .last("LIMIT 6")
        );
        List<Map<String, Object>> recentOrderRows = new ArrayList<Map<String, Object>>();
        for (TradeOrderDO order : recentOrders) {
            recentOrderRows.add(mapOf(
                "orderNo", order.getOrderNo(),
                "listingTitle", defaultText(order.getListingTitle(), "-"),
                "roleText", order.getBuyerUserId() != null && order.getBuyerUserId().equals(userId) ? "买家" : "卖家",
                "statusText", renderOrderStatus(order.getStatus()),
                "totalAmount", safeMoney(order.getTotalAmount()),
                "createdAt", formatTime(order.getCreatedAt())
            ));
        }
        List<AccountListingDO> recentListings = accountListingMapper.selectList(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getSellerUserId, userId)
                .orderByDesc(AccountListingDO::getUpdatedAt)
                .last("LIMIT 6")
        );
        List<Map<String, Object>> recentListingRows = new ArrayList<Map<String, Object>>();
        for (AccountListingDO listing : recentListings) {
            recentListingRows.add(mapOf(
                "listingNo", listing.getListingNo(),
                "title", defaultText(listing.getTitle(), "-"),
                "statusText", renderListingStatus(listing.getStatus()),
                "price", safeMoney(listing.getPrice()),
                "updatedAt", formatTime(listing.getUpdatedAt())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("summary", mapOf(
            "userId", row.getId(),
            "nickname", defaultText(row.getNickname(), "-"),
            "phone", maskPhone(row.getPhone()),
            "avatarUrl", previewNullable(row.getAvatarKey()),
            "accountStatus", defaultText(row.getAccountStatus(), "ACTIVE"),
            "accountStatusText", renderAccountStatus(row.getAccountStatus()),
            "banReason", defaultText(row.getBanReason(), ""),
            "verified", Boolean.TRUE.equals(row.getVerified()),
            "verifiedText", Boolean.TRUE.equals(row.getVerified()) ? "已实名" : "未实名",
            "realName", defaultText(row.getRealName(), "-"),
            "realNameStatus", defaultText(row.getRealNameStatus(), "UNVERIFIED"),
            "realNameStatusText", renderRealNameStatus(row.getRealNameStatus()),
            "idCardNo", maskIdCard(row.getIdCardNo()),
            "frontUrl", previewNullable(row.getRealNameFrontKey()),
            "backUrl", previewNullable(row.getRealNameBackKey()),
            "loginAlertEnabled", Boolean.TRUE.equals(row.getLoginAlertEnabled()),
            "secondaryVerifyEnabled", Boolean.TRUE.equals(row.getSecondaryVerifyEnabled()),
            "createdAt", formatTime(row.getCreatedAt()),
            "updatedAt", formatTime(row.getUpdatedAt())
        ));
        result.put("wallet", mapOf(
            "availableBalance", wallet == null ? "¥0.00" : safeMoney(wallet.getAvailableBalance()),
            "frozenBalance", wallet == null ? "¥0.00" : safeMoney(wallet.getFrozenBalance()),
            "totalCommission", wallet == null ? "¥0.00" : safeMoney(wallet.getTotalCommission())
        ));
        result.put("studio", mapOf(
            "isStudioOwner", studio != null,
            "studioName", studio == null ? "-" : defaultText(studio.getStudioName(), "-"),
            "reviewStrategyText", studio == null ? "-" : renderReviewStrategy(studio.getReviewStrategy()),
            "activeText", studio == null ? "-" : (Boolean.TRUE.equals(studio.getActive()) ? "合作中" : "已暂停")
        ));
        result.put("stats", mapOf(
            "buyerOrderCount", buyerOrderCount,
            "sellerOrderCount", sellerOrderCount,
            "publishCount", publishCount,
            "publishedCount", publishedCount,
            "pendingListingCount", pendingListingCount,
            "rejectedListingCount", rejectedListingCount
        ));
        result.put("recentOrders", recentOrderRows);
        result.put("recentListings", recentListingRows);
        log.info("admin user detail loaded userId={} buyerOrders={} sellerOrders={} publishCount={} costMs={}",
            userId, buyerOrderCount, sellerOrderCount, publishCount, System.currentTimeMillis() - startAt);
        return result;
    }

    @Transactional
    public Map<String, Object> updateUserStatus(Long userId, String status, String reason) {
        AuthUserDO row = requireUser(userId);
        String normalized = normalizeAction(status, Arrays.asList("ACTIVE", "DISABLED"));
        row.setAccountStatus(normalized);
        row.setBanReason("DISABLED".equals(normalized) ? normalizeRequired(reason, "封禁原因不能为空") : null);
        row.setUpdatedAt(LocalDateTime.now());
        authUserMapper.updateById(row);
        if ("DISABLED".equals(normalized)) {
            createUserMessage(userId, "SYSTEM", "账号已被封禁", "你的账号已被平台封禁。原因：" + row.getBanReason());
        } else {
            createUserMessage(userId, "SYSTEM", "账号已恢复正常", "你的账号已解除封禁限制，可继续正常使用平台服务。");
        }
        log.info("admin user status updated userId={} status={} hasReason={}", userId, normalized, StringUtils.hasText(reason));
        return mapOf("userId", userId, "status", normalized, "statusText", renderAccountStatus(normalized));
    }

    @Transactional
    public Map<String, Object> resetUserPassword(Long userId, String password) {
        AuthUserDO row = requireUser(userId);
        String normalizedPassword = validatePassword(password);
        row.setPasswordHash(hashPassword(normalizedPassword));
        row.setUpdatedAt(LocalDateTime.now());
        authUserMapper.updateById(row);
        log.info("admin user password reset success userId={} updatedAt={}", userId, row.getUpdatedAt());
        return mapOf("userId", userId, "message", "登录密码已重置");
    }

    public Map<String, Object> loadRealNameReviews(String status) {
        long startAt = System.currentTimeMillis();
        List<AuthUserDO> rows = authUserMapper.selectList(
            Wrappers.<AuthUserDO>lambdaQuery()
                .eq(StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status), AuthUserDO::getRealNameStatus, status)
                .orderByDesc(AuthUserDO::getUpdatedAt)
                .last("LIMIT 120")
        );
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (AuthUserDO row : rows) {
            if (!StringUtils.hasText(row.getRealName()) && !StringUtils.hasText(row.getIdCardNo())) {
                continue;
            }
            items.add(mapOf(
                "userId", row.getId(),
                "nickname", row.getNickname(),
                "phone", maskPhone(row.getPhone()),
                "realNamePhone", defaultText(row.getRealNamePhone(), "-"),
                "realName", defaultText(row.getRealName(), "-"),
                "idCardNo", maskIdCard(row.getIdCardNo()),
                "status", defaultText(row.getRealNameStatus(), "UNVERIFIED"),
                "statusText", renderRealNameStatus(row.getRealNameStatus()),
                "frontUrl", previewNullable(row.getRealNameFrontKey()),
                "backUrl", previewNullable(row.getRealNameBackKey()),
                "rejectReason", defaultText(row.getRealNameRejectReason(), ""),
                "updatedAt", formatTime(row.getUpdatedAt())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rows", items);
        result.put("summary", mapOf(
            "pendingCount", countRealNameStatus(rows, "PENDING"),
            "approvedCount", countRealNameStatus(rows, "APPROVED"),
            "rejectedCount", countRealNameStatus(rows, "REJECTED")
        ));
        log.info("admin real-name center loaded status={} count={} costMs={}", status, items.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    @Transactional
    public Map<String, Object> reviewRealName(Long userId, String action, String reason) {
        AuthUserDO row = requireUser(userId);
        String normalized = normalizeAction(action, Arrays.asList("APPROVE", "REJECT"));
        if ("APPROVE".equals(normalized)) {
            row.setVerified(true);
            row.setRealNameStatus("APPROVED");
            row.setRealNameRejectReason(null);
            createUserMessage(userId, "SYSTEM", "实名认证审核通过", "你的实名认证资料已审核通过，平台相关功能现已开放。");
        } else {
            row.setVerified(false);
            row.setRealNameStatus("REJECTED");
            row.setRealNameRejectReason(normalizeRequired(reason, "驳回原因不能为空"));
            createUserMessage(userId, "SYSTEM", "实名认证审核驳回", "你的实名认证资料未通过审核。驳回原因：" + row.getRealNameRejectReason());
        }
        row.setUpdatedAt(LocalDateTime.now());
        authUserMapper.updateById(row);
        if ("APPROVE".equals(normalized)) {
            distributionService.ensureAutoEnabled(userId);
        }
        log.info("admin real-name review success userId={} action={} verified={}", userId, normalized, row.getVerified());
        return mapOf("userId", userId, "status", row.getRealNameStatus(), "statusText", renderRealNameStatus(row.getRealNameStatus()));
    }

    public Map<String, Object> loadRoles() {
        long startAt = System.currentTimeMillis();
        List<AdminRoleDO> roles = adminRoleMapper.selectList(
            Wrappers.<AdminRoleDO>lambdaQuery().orderByAsc(AdminRoleDO::getCreatedAt)
        );
        List<AdminRoleMemberDO> members = adminRoleMemberMapper.selectList(
            Wrappers.<AdminRoleMemberDO>lambdaQuery().orderByAsc(AdminRoleMemberDO::getRoleId)
        );
        Map<Long, List<AdminRoleMemberDO>> memberMap = new LinkedHashMap<Long, List<AdminRoleMemberDO>>();
        for (AdminRoleMemberDO member : members) {
            memberMap.computeIfAbsent(member.getRoleId(), key -> new ArrayList<AdminRoleMemberDO>()).add(member);
        }
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        for (AdminRoleDO role : roles) {
            List<Map<String, Object>> memberRows = new ArrayList<Map<String, Object>>();
            for (AdminRoleMemberDO member : memberMap.getOrDefault(role.getId(), Collections.<AdminRoleMemberDO>emptyList())) {
                AuthUserDO user = authUserMapper.selectById(member.getUserId());
                memberRows.add(mapOf(
                    "userId", member.getUserId(),
                    "nickname", user == null ? "-" : user.getNickname(),
                    "phone", user == null ? "-" : maskPhone(user.getPhone())
                ));
            }
            items.add(mapOf(
                "roleId", role.getId(),
                "roleCode", role.getRoleCode(),
                "roleName", renderRoleName(role.getRoleCode(), role.getRoleName()),
                "description", defaultText(role.getDescription(), "-"),
                "permissions", readStringList(role.getPermissionsJson()),
                "status", role.getStatus(),
                "statusText", "ACTIVE".equals(role.getStatus()) ? "启用中" : "已停用",
                "memberCount", memberRows.size(),
                "members", memberRows,
                "updatedAt", formatTime(role.getUpdatedAt())
            ));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("rows", items);
        result.put("summary", mapOf(
            "roleCount", items.size(),
            "enabledCount", countRoleStatus(roles, "ACTIVE"),
            "memberCount", members.size()
        ));
        log.info("admin roles loaded count={} memberCount={} costMs={}", items.size(), members.size(), System.currentTimeMillis() - startAt);
        return result;
    }

    @Transactional
    public Map<String, Object> saveRole(Long roleId, String roleCode, String roleName, String description, List<String> permissions, String status) {
        LocalDateTime now = LocalDateTime.now();
        AdminRoleDO row = roleId == null ? null : adminRoleMapper.selectById(roleId);
        if (row == null) {
            row = new AdminRoleDO();
            row.setCreatedAt(now);
        }
        row.setRoleCode(normalizeRequired(roleCode, "角色编码不能为空").toUpperCase(Locale.ROOT));
        row.setRoleName(normalizeRequired(roleName, "角色名称不能为空"));
        row.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        row.setPermissionsJson(writeStringList(permissions));
        row.setStatus(normalizeAction(status, Arrays.asList("ACTIVE", "DISABLED")));
        row.setUpdatedAt(now);
        if (row.getId() == null) {
            adminRoleMapper.insert(row);
        } else {
            adminRoleMapper.updateById(row);
        }
        log.info("admin role saved roleId={} roleCode={} status={} permissionCount={}",
            row.getId(), row.getRoleCode(), row.getStatus(), permissions == null ? 0 : permissions.size());
        return mapOf("roleId", row.getId(), "roleCode", row.getRoleCode(), "message", "角色已保存");
    }

    @Transactional
    public Map<String, Object> assignRoleMembers(Long roleId, List<Long> userIds) {
        AdminRoleDO role = requireRole(roleId);
        adminRoleMemberMapper.delete(Wrappers.<AdminRoleMemberDO>lambdaQuery().eq(AdminRoleMemberDO::getRoleId, roleId));
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        if (userIds != null) {
            LinkedHashSet<Long> normalized = new LinkedHashSet<Long>(userIds);
            for (Long userId : normalized) {
                requireUser(userId);
                AdminRoleMemberDO row = new AdminRoleMemberDO();
                row.setRoleId(roleId);
                row.setUserId(userId);
                row.setCreatedAt(now);
                row.setUpdatedAt(now);
                adminRoleMemberMapper.insert(row);
                count += 1;
            }
        }
        log.info("admin role members assigned roleId={} roleCode={} memberCount={}", roleId, role.getRoleCode(), count);
        return mapOf("roleId", roleId, "memberCount", count, "message", "角色成员已更新");
    }

    private List<Map<String, Object>> buildPendingQueue() {
        List<Map<String, Object>> queue = new ArrayList<Map<String, Object>>();
        List<StudioApplicationDO> studioApplications = studioApplicationMapper.selectList(
            Wrappers.<StudioApplicationDO>lambdaQuery()
                .eq(StudioApplicationDO::getStatus, "PENDING")
                .orderByAsc(StudioApplicationDO::getCreatedAt)
                .last("LIMIT 3")
        );
        for (StudioApplicationDO application : studioApplications) {
            AuthUserDO applicant = authUserMapper.selectById(application.getApplicantUserId());
            queue.add(mapOf(
                "type", "STUDIO_APPLICATION_REVIEW",
                "title", application.getStudioName(),
                "subtitle", defaultText(applicant == null ? null : applicant.getNickname(), "未知用户") + " · " + defaultText(application.getContactPhone(), "-"),
                "status", "待审核",
                "primaryKey", application.getApplicationNo()
            ));
        }
        List<AccountListingDO> listings = accountListingMapper.selectList(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getStatus, "PENDING_REVIEW")
                .orderByAsc(AccountListingDO::getSubmittedAt)
                .last("LIMIT 4")
        );
        for (AccountListingDO listing : listings) {
            String exchangeRateLabel = formatRentalRatio(calculateRentalRatio(listing, readPublishAttributes(listing.getPublishAttributesJson())));
            queue.add(mapOf(
                "type", "LISTING_REVIEW",
                "title", listing.getTitle(),
                "subtitle", renderSellerDisplayName(listing) + " · " + renderSellerType(listing.getSellerType()) + " · 比例 " + exchangeRateLabel,
                "status", "待审核",
                "primaryKey", listing.getListingNo()
            ));
        }
        List<WithdrawApplicationDO> withdraws = withdrawApplicationMapper.selectList(
            Wrappers.<WithdrawApplicationDO>lambdaQuery()
                .eq(WithdrawApplicationDO::getStatus, "PENDING")
                .orderByAsc(WithdrawApplicationDO::getCreatedAt)
                .last("LIMIT 3")
        );
        for (WithdrawApplicationDO withdraw : withdraws) {
            queue.add(mapOf(
                "type", "WITHDRAW_REVIEW",
                "title", withdraw.getApplicationNo(),
                "subtitle", renderPaymentMethod(withdraw.getChannel()) + " · " + safeMoney(withdraw.getAmount()),
                "status", "待审核",
                "primaryKey", withdraw.getApplicationNo()
            ));
        }
        return queue;
    }

    private boolean matchListingKeyword(AccountListingDO row, String keyword) {
        String haystack = (
            defaultText(row.getListingNo(), "") + "|" +
            defaultText(row.getTitle(), "") + "|" +
            defaultText(row.getSellerNickname(), "") + "|" +
            defaultText(row.getStudioName(), "") + "|" +
            defaultText(row.getCityName(), "")
        ).toLowerCase(Locale.ROOT);
        return haystack.contains(keyword);
    }

    private void createUserMessage(Long userId, String category, String title, String content) {
        if (userId == null) {
            return;
        }
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
        insertUserMessage(message);
    }

    private void insertUserMessage(UserMessageDO message) {
        long startAt = System.currentTimeMillis();
        int rows = userMessageMapper.insert(message);
        log.info("mysql insert success target=user_message costMs={} rows={} userId={} category={}",
            System.currentTimeMillis() - startAt, rows, message.getUserId(), message.getCategory());
    }

    private List<Map<String, Object>> buildReviewRecords(AccountListingDO listing) {
        List<Map<String, Object>> records = new ArrayList<Map<String, Object>>();
        records.add(mapOf(
            "label", "提交发布",
            "status", "已提交",
            "operator", renderSellerDisplayName(listing),
            "time", formatTime(listing.getSubmittedAt()),
            "remark", "卖家提交账号发布资料"
        ));
        if ("PUBLISHED".equals(listing.getStatus())) {
            records.add(mapOf(
                "label", "审核通过",
                "status", "已上架",
                "operator", "平台审核",
                "time", formatTime(listing.getPublishedAt()),
                "remark", "账号已通过审核并在交易大厅展示"
            ));
        }
        if ("REJECTED".equals(listing.getStatus())) {
            records.add(mapOf(
                "label", "审核驳回",
                "status", "已驳回",
                "operator", "平台审核",
                "time", formatTime(listing.getUpdatedAt()),
                "remark", defaultText(listing.getRejectionReason(), "账号资料未通过审核")
            ));
        }
        if ("OFFLINE".equals(listing.getStatus())) {
            records.add(mapOf(
                "label", "下架处理",
                "status", "已下架",
                "operator", "平台审核",
                "time", formatTime(listing.getUpdatedAt()),
                "remark", defaultText(listing.getRejectionReason(), "账号已从交易大厅移除")
            ));
        }
        return records;
    }

    private List<Map<String, Object>> buildListingDetailSections(AccountListingDO listing, PublishAttributesSnapshot attributes, String exchangeRateLabel) {
        List<Map<String, Object>> sections = new ArrayList<Map<String, Object>>();
        sections.add(detailSection("接入信息", Arrays.asList(
            detailItem("上号方式", renderDeliveryMethod(listing.getDeliveryMethod())),
            detailItem("是否全天在线", Boolean.TRUE.equals(listing.getAlwaysOnline()) ? "是" : "否"),
            detailItem("可上号时间", formatHourRange(attributes.getDeliveryStartHour(), attributes.getDeliveryEndHour())),
            detailItem("所在地区", defaultText(listing.getProvinceName(), "-") + " / " + defaultText(listing.getCityName(), "-"))
        )));
        sections.add(detailSection("账号属性", Arrays.asList(
            detailItem("账号等级", listing.getAccountLevel() == null ? "-" : listing.getAccountLevel() + "级"),
            detailItem("账号段位", renderRankName(listing.getRankName())),
            detailItem("安全箱", renderSafeBoxLevel(listing.getSafeBoxLevel())),
            detailItem("体力", formatLevel(attributes.getStaminaLevel())),
            detailItem("负重", formatLevel(attributes.getCarryLevel())),
            detailItem("潜水等级", renderDiveLevel(attributes.getDiveLevel())),
            detailItem("人脸归属", Boolean.TRUE.equals(attributes.getFaceOwned()) ? "是" : "否"),
            detailItem("解锁赛依德", Boolean.TRUE.equals(attributes.getUnlockSaeed()) ? "是" : "否")
        )));
        sections.add(detailSection("资产与租赁", Arrays.asList(
            detailItem("哈夫币余额", listing.getHafCurrency() == null ? "-" : listing.getHafCurrency() + "M"),
            detailItem("绝密KD", formatDecimal(attributes.getSecretKd())),
            detailItem("默认消耗", renderDefaultSpend(attributes.getDefaultSpend())),
            detailItem("出租天数", attributes.getRentalDays() == null ? "-" : attributes.getRentalDays() + "天"),
            detailItem("发布比例", exchangeRateLabel),
            detailItem("赔付方案", renderCompensationPlan(attributes.getCompensationPlan())),
            detailItem("押金", safeMoney(attributes.getDeposit()))
        )));
        sections.add(detailSection("外观与资产", Arrays.asList(
            detailItem("特殊刀皮", joinTexts(attributes.getKnifeSkins())),
            detailItem("人物红皮", joinTexts(attributes.getRedSkins())),
            detailItem("武器皮肤", joinTexts(attributes.getWeaponSkins())),
            detailItem("人物金皮", joinTexts(attributes.getGoldSkins()))
        )));
        sections.add(detailSection("额外物资", buildExtraItemDetails(attributes.getExtraItems())));
        sections.add(detailSection("备注", Arrays.asList(
            detailItem("卖家备注", defaultText(attributes.getRemarks(), "-")),
            detailItem("估值依据", defaultText(listing.getEstimateDetail(), "-"))
        )));
        return sections;
    }

    private Map<String, Object> detailSection(String title, List<Map<String, String>> items) {
        return mapOf("title", title, "items", items);
    }

    private Map<String, String> detailItem(String label, String value) {
        Map<String, String> item = new LinkedHashMap<String, String>();
        item.put("label", label);
        item.put("value", defaultText(value, "-"));
        return item;
    }

    private List<Map<String, Object>> buildOrderDetailSections(TradeOrderDO order) {
        List<Map<String, Object>> sections = new ArrayList<Map<String, Object>>();
        sections.add(detailSection("订单信息", Arrays.asList(
            detailItem("订单号", order.getOrderNo()),
            detailItem("商品编号", defaultText(order.getListingNo(), "-")),
            detailItem("商品名称", defaultText(order.getListingTitle(), "-")),
            detailItem("商品摘要", defaultText(order.getListingSummary(), "-")),
            detailItem("当前状态", renderOrderStatus(order.getStatus())),
            detailItem("交易群聊", defaultText(order.getChatGroupNo(), "-"))
        )));
        sections.add(detailSection("交易双方", Arrays.asList(
            detailItem("买家", defaultText(order.getBuyerNickname(), "-") + " / ID " + defaultText(order.getBuyerUserId() == null ? null : String.valueOf(order.getBuyerUserId()), "-")),
            detailItem("卖家", defaultText(order.getSellerDisplayName(), defaultText(order.getSellerNickname(), "-"))),
            detailItem("卖家类型", renderSellerType(order.getSellerType())),
            detailItem("卖家用户ID", defaultText(order.getSellerUserId() == null ? null : String.valueOf(order.getSellerUserId()), "-"))
        )));
        sections.add(detailSection("金额与支付", Arrays.asList(
            detailItem("商品金额", safeMoney(order.getItemAmount())),
            detailItem("服务费", safeMoney(order.getServiceFee())),
            detailItem("额外物资", safeMoney(order.getExtraItemsAmount())),
            detailItem("订单总额", safeMoney(order.getTotalAmount())),
            detailItem("支付方式", renderPaymentMethod(order.getPaymentMethod())),
            detailItem("支付类型", defaultText(order.getPaymentTradeType(), "-")),
            detailItem("支付流水号", defaultText(order.getPaymentTransactionId(), "-"))
        )));
        sections.add(detailSection("时间节点", Arrays.asList(
            detailItem("创建时间", formatTime(order.getCreatedAt())),
            detailItem("支付时间", formatTime(order.getPaidAt())),
            detailItem("回调时间", formatTime(order.getPaymentNotifiedAt())),
            detailItem("交易开始", formatTime(order.getTradeStartedAt())),
            detailItem("完成时间", formatTime(order.getCompletedAt())),
            detailItem("关闭时间", formatTime(order.getClosedAt())),
            detailItem("更新时间", formatTime(order.getUpdatedAt()))
        )));
        sections.add(detailSection("退款与售后", Arrays.asList(
            detailItem("退款金额", safeMoney(order.getRefundAmount())),
            detailItem("退款原因", defaultText(order.getRefundReason(), "-")),
            detailItem("审核备注", defaultText(order.getRefundReviewNote(), "-")),
            detailItem("退款申请时间", formatTime(order.getRefundRequestedAt())),
            detailItem("退款审核时间", formatTime(order.getRefundReviewedAt())),
            detailItem("退款完成时间", formatTime(order.getRefundedAt())),
            detailItem("售后备注", defaultText(order.getAfterSaleNote(), "-")),
            detailItem("售后时间", formatTime(order.getAfterSaleAt()))
        )));
        return sections;
    }

    private List<Map<String, Object>> buildAdminOrderProgress(TradeOrderDO order) {
        List<Map<String, Object>> steps = new ArrayList<Map<String, Object>>();
        String status = order.getStatus();
        addOrderProgressStep(steps, "订单创建", "买家提交订单，平台锁定待支付窗口", order.getCreatedAt(), order.getCreatedAt() != null, "PENDING_PAYMENT".equals(status));
        addOrderProgressStep(steps, "付款完成", "微信/支付平台回调确认后写入支付流水", order.getPaidAt(), order.getPaidAt() != null, "WAITING_TRADE".equals(status));
        addOrderProgressStep(steps, "账号交接", "买卖双方在交易群聊中沟通账号交接", order.getTradeStartedAt(), order.getPaidAt() != null && !"PENDING_PAYMENT".equals(status), "IN_PROGRESS".equals(status));
        addOrderProgressStep(steps, "交易完成", "买家确认完成或流程进入完成状态", order.getCompletedAt(), order.getCompletedAt() != null || "COMPLETED".equals(status), "COMPLETED".equals(status));
        if ("REFUND_PENDING".equals(status) || order.getRefundRequestedAt() != null) {
            addOrderProgressStep(steps, "退款审核", "买家已申请退款，等待卖家审核或平台介入", order.getRefundRequestedAt(), order.getRefundRequestedAt() != null, "REFUND_PENDING".equals(status));
        }
        if ("AFTER_SALE".equals(status) || order.getAfterSaleAt() != null) {
            addOrderProgressStep(steps, "售后处理中", "订单进入售后流程，等待平台客服处理", order.getAfterSaleAt(), order.getAfterSaleAt() != null, "AFTER_SALE".equals(status));
        }
        if ("REFUNDED".equals(status) || order.getRefundedAt() != null) {
            addOrderProgressStep(steps, "退款完成", "订单金额已退回买家站内钱包", order.getRefundedAt(), order.getRefundedAt() != null, "REFUNDED".equals(status));
        }
        if ("CLOSED".equals(status) || order.getClosedAt() != null) {
            addOrderProgressStep(steps, "订单关闭", "订单已取消或超时关闭", order.getClosedAt(), order.getClosedAt() != null, "CLOSED".equals(status));
        }
        return steps;
    }

    private void addOrderProgressStep(List<Map<String, Object>> steps, String title, String description, LocalDateTime time, boolean done, boolean current) {
        steps.add(mapOf(
            "title", title,
            "description", description,
            "time", formatTime(time),
            "done", done,
            "current", current
        ));
    }

    private List<Map<String, String>> buildExtraItemDetails(List<ExtraItemSnapshot> extraItems) {
        if (extraItems == null || extraItems.isEmpty()) {
            return Arrays.asList(detailItem("额外物资", "无"));
        }
        List<Map<String, String>> items = new ArrayList<Map<String, String>>();
        for (ExtraItemSnapshot item : extraItems) {
            int count = item.getCount() == null ? 0 : item.getCount();
            if (count <= 0) {
                continue;
            }
            String mode = "gift".equals(item.getChargeMode()) ? "赠送" : "收费";
            items.add(detailItem(item.getLabel(), count + defaultText(item.getUnitLabel(), "") + " · " + mode + " · " + safeMoney(item.getTotalPrice())));
        }
        return items.isEmpty() ? Arrays.asList(detailItem("额外物资", "无")) : items;
    }

    private String joinTexts(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "-";
        }
        List<String> cleaned = new ArrayList<String>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                cleaned.add(value.trim());
            }
        }
        return cleaned.isEmpty() ? "-" : String.join("、", cleaned);
    }

    private List<String> buildImageUrls(String raw) {
        List<String> keys = readStringList(raw);
        List<String> urls = new ArrayList<String>();
        for (String key : keys) {
            urls.add(previewNullable(key));
        }
        return urls;
    }

    private List<String> readStringList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(raw, STRING_LIST_TYPE);
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    private AccountListingDO requireListing(String listingNo) {
        AccountListingDO row = accountListingMapper.selectOne(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getListingNo, listingNo)
                .last("LIMIT 1")
        );
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号资源不存在");
        }
        return row;
    }

    private TradeOrderDO requireTradeOrder(String orderNo) {
        TradeOrderDO row = tradeOrderMapper.selectOne(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getOrderNo, orderNo)
                .last("LIMIT 1")
        );
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "交易订单不存在");
        }
        return row;
    }

    private StudioProfileDO requireStudio(Long studioId) {
        StudioProfileDO row = studioProfileMapper.selectById(studioId);
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "工作室不存在");
        }
        return row;
    }

    private BoostingServiceDO requireBoostingService(String serviceNo) {
        BoostingServiceDO row = boostingServiceMapper.selectOne(
            Wrappers.<BoostingServiceDO>lambdaQuery()
                .eq(BoostingServiceDO::getServiceNo, serviceNo)
                .last("LIMIT 1")
        );
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "代肝服务不存在");
        }
        return row;
    }

    private WithdrawApplicationDO requireWithdraw(String applicationNo) {
        WithdrawApplicationDO row = withdrawApplicationMapper.selectOne(
            Wrappers.<WithdrawApplicationDO>lambdaQuery()
                .eq(WithdrawApplicationDO::getApplicationNo, applicationNo)
                .last("LIMIT 1")
        );
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "提现申请不存在");
        }
        return row;
    }

    private StudioWithdrawApplicationDO requireStudioWithdraw(String applicationNo) {
        StudioWithdrawApplicationDO row = studioWithdrawApplicationMapper.selectOne(
            Wrappers.<StudioWithdrawApplicationDO>lambdaQuery()
                .eq(StudioWithdrawApplicationDO::getApplicationNo, applicationNo)
                .last("LIMIT 1")
        );
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "工作室提现申请不存在");
        }
        return row;
    }

    private UserWalletDO requireWallet(Long userId) {
        UserWalletDO wallet = userWalletMapper.selectOne(
            Wrappers.<UserWalletDO>lambdaQuery()
                .eq(UserWalletDO::getUserId, userId)
                .last("LIMIT 1")
        );
        if (wallet == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户钱包不存在");
        }
        return wallet;
    }

    private int countListingsByStatus(String status) {
        Long count = accountListingMapper.selectCount(
            Wrappers.<AccountListingDO>lambdaQuery().eq(AccountListingDO::getStatus, status)
        );
        return count == null ? 0 : count.intValue();
    }

    private int countTradeOrders(List<String> statuses) {
        Long count = tradeOrderMapper.selectCount(
            Wrappers.<TradeOrderDO>lambdaQuery().in(TradeOrderDO::getStatus, statuses)
        );
        return count == null ? 0 : count.intValue();
    }

    private void ensureListingCanBePublished(AccountListingDO listing) {
        Long activeTradeCount = tradeOrderMapper.selectCount(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getListingNo, listing.getListingNo())
                .in(TradeOrderDO::getStatus, Arrays.asList("PENDING_PAYMENT", "WAITING_TRADE", "IN_PROGRESS", "COMPLETED", "AFTER_SALE"))
        );
        if (activeTradeCount != null && activeTradeCount > 0) {
            log.warn("admin listing publish rejected active trade listingNo={} activeTradeCount={}", listing.getListingNo(), activeTradeCount);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该账号已有交易订单，不能重新上架");
        }
    }

    private int countBoostingOrders(List<String> statuses) {
        Long count = boostingOrderMapper.selectCount(
            Wrappers.<com.deltatrade.platform.modules.boosting.model.BoostingOrderDO>lambdaQuery()
                .in(com.deltatrade.platform.modules.boosting.model.BoostingOrderDO::getStatus, statuses)
        );
        return count == null ? 0 : count.intValue();
    }

    private int countWithdrawApplications(String status) {
        Long count = withdrawApplicationMapper.selectCount(
            Wrappers.<WithdrawApplicationDO>lambdaQuery().eq(WithdrawApplicationDO::getStatus, status)
        );
        return count == null ? 0 : count.intValue();
    }

    private BigDecimal sumTradeAmountToday() {
        LocalDateTime todayStart = LocalDate.now(BEIJING_ZONE).atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        List<BigDecimal> rows = jdbcTemplate.query(
            "SELECT COALESCE(SUM(amount), 0) amount FROM (" +
                "SELECT total_amount AS amount FROM trade_order WHERE paid_at >= ? AND paid_at < ? AND payment_transaction_id IS NOT NULL AND payment_transaction_id <> '' " +
                "UNION ALL " +
                "SELECT price AS amount FROM boosting_order WHERE paid_at >= ? AND paid_at < ? AND payment_transaction_id IS NOT NULL AND payment_transaction_id <> ''" +
            ") paid_orders",
            new Object[] {todayStart, tomorrowStart, todayStart, tomorrowStart},
            (rs, rowNum) -> rs.getBigDecimal("amount")
        );
        return rows.isEmpty() ? BigDecimal.ZERO : safeAmount(rows.get(0));
    }

    private BigDecimal sumFrozenWalletAmount() {
        List<BigDecimal> rows = jdbcTemplate.query(
            "SELECT COALESCE(SUM(frozen_balance), 0) amount FROM user_wallet",
            (rs, rowNum) -> rs.getBigDecimal("amount")
        );
        return rows.isEmpty() ? BigDecimal.ZERO : safeAmount(rows.get(0));
    }

    private int countRows(List<AccountListingDO> rows, String status) {
        int count = 0;
        for (AccountListingDO row : rows) {
            if (status.equals(row.getStatus())) {
                count += 1;
            }
        }
        return count;
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

    private int countActiveStudios(List<StudioProfileDO> rows) {
        int count = 0;
        for (StudioProfileDO row : rows) {
            if ("ACTIVE".equals(normalizeCooperationStatus(row.getCooperationStatus()))) {
                count += 1;
            }
        }
        return count;
    }

    private int countStudioCooperation(List<StudioProfileDO> rows, String cooperationStatus) {
        int count = 0;
        for (StudioProfileDO row : rows) {
            if (cooperationStatus.equals(normalizeCooperationStatus(row.getCooperationStatus()))) {
                count += 1;
            }
        }
        return count;
    }

    private int countStudioStrategy(List<StudioProfileDO> rows, String strategy) {
        int count = 0;
        for (StudioProfileDO row : rows) {
            if (strategy.equals(row.getReviewStrategy())) {
                count += 1;
            }
        }
        return count;
    }

    private int countStudioListings(Long ownerUserId) {
        Long count = accountListingMapper.selectCount(
            Wrappers.<AccountListingDO>lambdaQuery().eq(AccountListingDO::getSellerUserId, ownerUserId)
        );
        return count == null ? 0 : count.intValue();
    }

    private int countStudioOrders(Long ownerUserId) {
        Long count = tradeOrderMapper.selectCount(
            Wrappers.<TradeOrderDO>lambdaQuery().eq(TradeOrderDO::getSellerUserId, ownerUserId)
        );
        return count == null ? 0 : count.intValue();
    }

    private BigDecimal sumStudioGmv(Long ownerUserId) {
        List<BigDecimal> rows = jdbcTemplate.query(
            "SELECT COALESCE(SUM(total_amount), 0) amount FROM trade_order WHERE seller_user_id = ? AND status IN ('WAITING_TRADE','COMPLETED','AFTER_SALE')",
            new Object[] {ownerUserId},
            (rs, rowNum) -> rs.getBigDecimal("amount")
        );
        return rows.isEmpty() ? BigDecimal.ZERO : safeAmount(rows.get(0));
    }

    private int countBoostingServices(List<BoostingServiceDO> rows, String status) {
        int count = 0;
        for (BoostingServiceDO row : rows) {
            if (status.equals(row.getStatus())) {
                count += 1;
            }
        }
        return count;
    }

    private int countWithdraws(List<WithdrawApplicationDO> rows, String status) {
        int count = 0;
        for (WithdrawApplicationDO row : rows) {
            if (status.equals(row.getStatus())) {
                count += 1;
            }
        }
        return count;
    }

    private int countStudioWithdraws(List<StudioWithdrawApplicationDO> rows, String status) {
        int count = 0;
        for (StudioWithdrawApplicationDO row : rows) {
            if (status.equals(row.getStatus())) {
                count += 1;
            }
        }
        return count;
    }

    private int countAnnouncementStatus(List<OperationAnnouncementDO> rows, String status) {
        int count = 0;
        for (OperationAnnouncementDO row : rows) {
            if (status.equals(row.getStatus())) {
                count += 1;
            }
        }
        return count;
    }

    private int countUserStatus(List<AuthUserDO> rows, String status) {
        int count = 0;
        for (AuthUserDO row : rows) {
            if (status.equals(defaultText(row.getAccountStatus(), "ACTIVE"))) {
                count += 1;
            }
        }
        return count;
    }

    private int countVerified(List<AuthUserDO> rows, boolean verified) {
        int count = 0;
        for (AuthUserDO row : rows) {
            if (verified == Boolean.TRUE.equals(row.getVerified())) {
                count += 1;
            }
        }
        return count;
    }

    private int countRealNameStatus(List<AuthUserDO> rows, String status) {
        int count = 0;
        for (AuthUserDO row : rows) {
            if (status.equals(defaultText(row.getRealNameStatus(), "UNVERIFIED"))) {
                count += 1;
            }
        }
        return count;
    }

    private int countRoleStatus(List<AdminRoleDO> rows, String status) {
        int count = 0;
        for (AdminRoleDO row : rows) {
            if (status.equals(row.getStatus())) {
                count += 1;
            }
        }
        return count;
    }

    private String renderListingReviewMessage(String action) {
        if ("APPROVE".equals(action)) {
            return "账号已审核通过并上架";
        }
        if ("REJECT".equals(action)) {
            return "账号已驳回，卖家可修改后重提";
        }
        return "账号已下架，交易大厅将不再展示";
    }

    private String renderSellerDisplayName(AccountListingDO row) {
        if ("STUDIO".equalsIgnoreCase(row.getSellerType()) && StringUtils.hasText(row.getStudioName())) {
            return row.getStudioName();
        }
        return defaultText(row.getSellerNickname(), "-");
    }

    private String renderSellerType(String sellerType) {
        if ("STUDIO".equalsIgnoreCase(sellerType)) {
            return "工作室";
        }
        return "个人";
    }

    private String renderListingStatus(String status) {
        if ("PUBLISHED".equals(status)) {
            return "已上架";
        }
        if ("PENDING_REVIEW".equals(status)) {
            return "待审核";
        }
        if ("REJECTED".equals(status)) {
            return "已驳回";
        }
        if ("OFFLINE".equals(status)) {
            return "已下架";
        }
        return defaultText(status, "-");
    }

    private String renderReviewProgress(String status) {
        if ("PUBLISHED".equals(status)) {
            return "已通过";
        }
        if ("REJECTED".equals(status)) {
            return "已驳回";
        }
        if ("OFFLINE".equals(status)) {
            return "已下架";
        }
        return "待审核";
    }

    private String renderReviewStrategy(String reviewStrategy) {
        if ("DIRECT_PUBLISH".equals(reviewStrategy)) {
            return "免审直发";
        }
        return "需要审核";
    }

    private String renderShareRatio(BigDecimal shareRatio) {
        BigDecimal normalized = shareRatio == null || shareRatio.compareTo(BigDecimal.ZERO) <= 0
            ? new BigDecimal("0.7000")
            : shareRatio.setScale(4, RoundingMode.HALF_UP);
        return normalized.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }

    private BigDecimal normalizeShareRatio(BigDecimal shareRatio) {
        if (shareRatio == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "分润比例不能为空");
        }
        if (shareRatio.compareTo(new BigDecimal("0.10")) < 0 || shareRatio.compareTo(new BigDecimal("1.00")) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "分润比例需在 0.10 到 1.00 之间");
        }
        return shareRatio.setScale(4, RoundingMode.HALF_UP);
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

    private String renderPaymentMethod(String paymentMethod) {
        if ("ALIPAY".equalsIgnoreCase(paymentMethod)) {
            return "支付宝";
        }
        if ("WECHAT".equalsIgnoreCase(paymentMethod)) {
            return "微信";
        }
        return defaultText(paymentMethod, "-");
    }

    private String renderProviderType(String providerType) {
        if ("PLATFORM".equalsIgnoreCase(providerType)) {
            return "平台直营";
        }
        if ("STUDIO".equalsIgnoreCase(providerType)) {
            return "工作室";
        }
        return defaultText(providerType, "-");
    }

    private String normalizeOperationType(String type) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!Arrays.asList("banner", "shortcut", "announcement").contains(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不支持的运营配置类型");
        }
        return normalized;
    }

    private int updateBannerStatus(List<Long> ids, String status, LocalDateTime now) {
        int updated = 0;
        for (Long id : new LinkedHashSet<Long>(ids)) {
            OperationBannerDO row = requireBanner(id);
            row.setStatus(status);
            row.setUpdatedAt(now);
            updated += operationBannerMapper.updateById(row);
        }
        return updated;
    }

    private int updateShortcutStatus(List<Long> ids, String status, LocalDateTime now) {
        int updated = 0;
        for (Long id : new LinkedHashSet<Long>(ids)) {
            OperationShortcutDO row = requireShortcut(id);
            row.setStatus(status);
            row.setUpdatedAt(now);
            updated += operationShortcutMapper.updateById(row);
        }
        return updated;
    }

    private int updateAnnouncementStatus(List<Long> ids, String status, LocalDateTime now) {
        int updated = 0;
        for (Long id : new LinkedHashSet<Long>(ids)) {
            OperationAnnouncementDO row = requireAnnouncement(id);
            row.setStatus(status);
            if ("PUBLISHED".equals(status) && row.getPublishAt() == null) {
                row.setPublishAt(now);
            }
            row.setUpdatedAt(now);
            updated += operationAnnouncementMapper.updateById(row);
        }
        return updated;
    }

    private OperationBannerDO requireBanner(Long bannerId) {
        OperationBannerDO row = operationBannerMapper.selectById(bannerId);
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "轮播图不存在");
        }
        return row;
    }

    private OperationShortcutDO requireShortcut(Long shortcutId) {
        OperationShortcutDO row = operationShortcutMapper.selectById(shortcutId);
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "快捷入口不存在");
        }
        return row;
    }

    private OperationAnnouncementDO requireAnnouncement(Long announcementId) {
        OperationAnnouncementDO row = operationAnnouncementMapper.selectById(announcementId);
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "公告不存在");
        }
        return row;
    }

    private String renderWithdrawStatus(String status) {
        if ("PENDING".equals(status)) return "待审核";
        if ("PAID".equals(status)) return "已到账";
        if ("REJECTED".equals(status)) return "已驳回";
        return defaultText(status, "-");
    }

    private String renderOperationStatus(String status) {
        if ("ACTIVE".equals(status)) return "启用中";
        if ("DISABLED".equals(status)) return "已停用";
        return defaultText(status, "-");
    }

    private String renderCooperationStatus(String cooperationStatus, Boolean active) {
        String normalized = normalizeCooperationStatus(cooperationStatus);
        if ("ACTIVE".equals(normalized)) return "合作中";
        if ("PAUSED".equals(normalized)) return "暂停合作";
        if ("CLEARED".equals(normalized)) return "已清退";
        return Boolean.TRUE.equals(active) ? "合作中" : "暂停合作";
    }

    private String normalizeCooperationStatus(String cooperationStatus) {
        if (!StringUtils.hasText(cooperationStatus)) {
            return "ACTIVE";
        }
        String normalized = cooperationStatus.trim().toUpperCase(Locale.ROOT);
        if ("ACTIVE".equals(normalized) || "PAUSED".equals(normalized) || "CLEARED".equals(normalized)) {
            return normalized;
        }
        return "ACTIVE";
    }

    private String renderAnnouncementCategory(String category) {
        if ("SYSTEM".equals(category)) return "系统公告";
        if ("ACTIVITY".equals(category)) return "活动公告";
        if ("TRADE".equals(category)) return "交易通知";
        return defaultText(category, "-");
    }

    private String renderAnnouncementStatus(String status) {
        if ("DRAFT".equals(status)) return "草稿";
        if ("PUBLISHED".equals(status)) return "已发布";
        if ("OFFLINE".equals(status)) return "已下架";
        return defaultText(status, "-");
    }

    private String renderAccountStatus(String status) {
        if ("DISABLED".equals(status)) return "已封禁";
        return "正常";
    }

    private String renderRealNameStatus(String status) {
        if ("APPROVED".equals(status)) return "已通过";
        if ("REJECTED".equals(status)) return "已驳回";
        if ("PENDING".equals(status)) return "待审核";
        return "未提交";
    }

    private String renderRoleName(String roleCode, String roleName) {
        if ("SUPER_ADMIN".equalsIgnoreCase(roleCode)) return "超级管理员";
        if ("OPS_ADMIN".equalsIgnoreCase(roleCode)) return "运营管理员";
        if ("SERVICE_ADMIN".equalsIgnoreCase(roleCode)) return "客服管理员";
        if ("FINANCE_ADMIN".equalsIgnoreCase(roleCode)) return "财务管理员";
        return defaultText(roleName, "-");
    }

    private String previewNullable(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        return ossStorageService.previewUrl(objectKey);
    }

    private String resolveWithdrawQrCodeKey(WithdrawApplicationDO row) {
        if (row == null) {
            return null;
        }
        if (StringUtils.hasText(row.getQrCodeKey())) {
            return row.getQrCodeKey();
        }
        WithdrawAccountDO account = withdrawAccountMapper.selectOne(
            Wrappers.<WithdrawAccountDO>lambdaQuery()
                .eq(WithdrawAccountDO::getUserId, row.getUserId())
                .last("LIMIT 1")
        );
        return account == null ? null : account.getQrCodeKey();
    }

    private PublishAttributesSnapshot readPublishAttributes(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new PublishAttributesSnapshot();
        }
        try {
            return objectMapper.readValue(raw, PublishAttributesSnapshot.class);
        } catch (Exception exception) {
            return new PublishAttributesSnapshot();
        }
    }

    private String formatExchangeRate(PublishAttributesSnapshot attributes) {
        if (attributes == null) {
            return "-";
        }
        String type = attributes.getExchangeRateType();
        BigDecimal rate = attributes.getCustomExchangeRate();
        if ("custom".equals(type)) {
            return rate == null ? "自定义比例" : formatExchangeRateWan(rate);
        }
        if ("accelerated".equals(type)) {
            return rate == null ? "特惠比例" : formatExchangeRateWan(rate);
        }
        if ("default".equals(type)) {
            return rate == null ? "默认比例" : formatExchangeRateWan(rate);
        }
        return "-";
    }

    private String renderDeliveryMethod(String value) {
        if ("wechat_qr".equals(value)) return "微信扫码";
        if ("qq_account".equals(value)) return "QQ账密";
        if ("qq_qr".equals(value)) return "QQ扫码";
        if ("steam_cn".equals(value)) return "Steam国服";
        if ("steam_global".equals(value)) return "Steam国际服";
        return defaultText(value, "-");
    }

    private String renderRankName(String value) {
        if ("bronze".equals(value)) return "青铜";
        if ("silver".equals(value)) return "白银";
        if ("gold".equals(value)) return "黄金";
        if ("platinum".equals(value)) return "铂金";
        if ("diamond".equals(value)) return "钻石";
        if ("blackhawk".equals(value)) return "黑鹰";
        if ("summit".equals(value)) return "巅峰";
        return defaultText(value, "-");
    }

    private String renderSafeBoxLevel(Integer value) {
        if (Integer.valueOf(1).equals(value)) return "基础安全箱(1*2)";
        if (Integer.valueOf(2).equals(value)) return "进阶安全箱(2*2)";
        if (Integer.valueOf(3).equals(value)) return "高级安全箱(2*3)";
        if (Integer.valueOf(4).equals(value)) return "顶级安全箱(3*3)";
        return "-";
    }

    private String renderDiveLevel(Integer value) {
        if (value == null) return "-";
        if (Integer.valueOf(-1).equals(value)) return "无";
        return value + "级";
    }

    private String formatLevel(Integer value) {
        return value == null ? "-" : value + "级";
    }

    private String formatHourRange(Integer startHour, Integer endHour) {
        if (startHour == null || endHour == null) {
            return "-";
        }
        return startHour + ":00-" + (endHour < startHour ? "次日" : "") + endHour + ":00";
    }

    private String renderDefaultSpend(String value) {
        if ("10m".equals(value)) return "10M/天";
        if ("20m_plus_2".equals(value)) return "20M/天+2";
        if ("30m_plus_3".equals(value)) return "30M/天+3";
        if ("40m_plus_4".equals(value)) return "40M/天+4";
        if ("50m_plus_5".equals(value)) return "50M/天+5";
        return defaultText(value, "-");
    }

    private String renderCompensationPlan(String value) {
        if ("normal".equals(value)) return "普通赔付";
        if ("full".equals(value)) return "全额包赔";
        return defaultText(value, "-");
    }

    private String formatDecimal(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatPlainNumber(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatExchangeRateWan(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        BigDecimal wan = value.divide(new BigDecimal("10000"), 2, RoundingMode.HALF_UP).stripTrailingZeros();
        return wan.toPlainString() + "w/1元";
    }

    private String formatRentalRatio(int ratio) {
        return Math.max(1, ratio) + "w/1元";
    }

    private int calculateRentalRatio(AccountListingDO listing, PublishAttributesSnapshot attributes) {
        if (attributes != null
            && "custom".equals(attributes.getExchangeRateType())
            && attributes.getCustomExchangeRate() != null
            && attributes.getCustomExchangeRate().compareTo(BigDecimal.ZERO) > 0) {
            return rentalBaseRatio(attributes);
        }
        int ratio = rentalBaseRatio(attributes);
        ratio += safeBoxRatioDelta(listing.getSafeBoxLevel());
        ratio += levelRatioDelta(attributes == null ? null : attributes.getStaminaLevel());
        ratio += levelRatioDelta(attributes == null ? null : attributes.getCarryLevel());
        ratio += hafCurrencyRatioDelta(listing.getHafCurrency());
        ratio += defaultSpendRatioOffset(attributes == null ? null : attributes.getDefaultSpend());
        if (hasEffectiveKnifeSkin(attributes == null ? null : attributes.getKnifeSkins())) {
            ratio -= 1;
        }
        return Math.max(1, ratio);
    }

    private int rentalBaseRatio(PublishAttributesSnapshot attributes) {
        if (attributes != null
            && "custom".equals(attributes.getExchangeRateType())
            && attributes.getCustomExchangeRate() != null
            && attributes.getCustomExchangeRate().compareTo(BigDecimal.ZERO) > 0) {
            return Math.max(1, attributes.getCustomExchangeRate().divide(new BigDecimal("10000"), 0, RoundingMode.DOWN).intValue());
        }
        if (attributes != null && "accelerated".equals(attributes.getExchangeRateType())) {
            return defaultRentalBaseRatio() + 5;
        }
        return defaultRentalBaseRatio();
    }

    private int defaultRentalBaseRatio() {
        Object value = adminIntegrationConfigService.loadListingPublishConfig().get("defaultExchangeRate");
        try {
            BigDecimal rate = new BigDecimal(String.valueOf(value));
            return Math.max(1, rate.divide(new BigDecimal("10000"), 0, RoundingMode.DOWN).intValue());
        } catch (Exception ignored) {
            return 38;
        }
    }

    private int defaultSpendRatioOffset(String value) {
        if ("20m_plus_2".equals(value)) return 2;
        if ("30m_plus_3".equals(value)) return 3;
        if ("40m_plus_4".equals(value)) return 4;
        if ("50m_plus_5".equals(value)) return 5;
        return 0;
    }

    private int safeBoxRatioDelta(Integer value) {
        if (Integer.valueOf(4).equals(value)) return -1;
        if (Integer.valueOf(3).equals(value)) return 1;
        if (Integer.valueOf(1).equals(value) || Integer.valueOf(2).equals(value)) return 3;
        return 0;
    }

    private int levelRatioDelta(Integer value) {
        if (Integer.valueOf(6).equals(value)) return 1;
        if (Integer.valueOf(5).equals(value)) return 2;
        if (Integer.valueOf(4).equals(value)) return 3;
        return 0;
    }

    private int hafCurrencyRatioDelta(Long value) {
        if (value == null) return 0;
        if (value >= 1000L) return 4;
        if (value >= 700L) return 3;
        if (value >= 500L) return 2;
        if (value >= 300L) return 1;
        return 0;
    }

    private boolean hasEffectiveKnifeSkin(List<String> values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && RENTAL_EFFECTIVE_KNIFE_SKINS.contains(value.trim())) {
                return true;
            }
        }
        return false;
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

    private Integer safeNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "-" : TIME_FORMATTER.format(time);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
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

    private String maskIdCard(String idCardNo) {
        if (!StringUtils.hasText(idCardNo) || idCardNo.length() <= 8) {
            return defaultText(idCardNo, "-");
        }
        return idCardNo.substring(0, 6) + "********" + idCardNo.substring(idCardNo.length() - 4);
    }

    private String normalizeAction(String action, List<String> allowed) {
        String normalized = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "非法操作");
        }
        return normalized;
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
        return value.trim();
    }

    private String validatePassword(String password) {
        String normalized = normalizeRequired(password, "新密码不能为空");
        if (normalized.length() < 6 || normalized.length() > 18) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "新密码需为 6-18 位");
        }
        if (!normalized.matches("(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+")) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "新密码需同时包含字母和数字");
        }
        return normalized;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : encoded) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "密码摘要算法不可用");
        }
    }

    private String maskKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : "-";
    }

    private Set<Long> loadStudioOwnerIds() {
        List<StudioProfileDO> studios = studioProfileMapper.selectList(Wrappers.<StudioProfileDO>lambdaQuery().select(StudioProfileDO::getOwnerUserId));
        Set<Long> ids = new LinkedHashSet<Long>();
        for (StudioProfileDO studio : studios) {
            if (studio.getOwnerUserId() != null) {
                ids.add(studio.getOwnerUserId());
            }
        }
        return ids;
    }

    private String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? Collections.emptyList() : values);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "角色权限序列化失败");
        }
    }

    private AuthUserDO requireUser(Long userId) {
        AuthUserDO row = authUserMapper.selectById(userId);
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户不存在");
        }
        return row;
    }

    private AuthUserDO requireUserByPhone(String phone) {
        String normalized = normalizeRequired(phone, "负责人手机号不能为空");
        AuthUserDO row = authUserMapper.selectOne(
            Wrappers.<AuthUserDO>lambdaQuery()
                .eq(AuthUserDO::getPhone, normalized)
                .last("LIMIT 1")
        );
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "负责人账号不存在，请先完成平台注册");
        }
        return row;
    }

    private AdminRoleDO requireRole(Long roleId) {
        AdminRoleDO row = adminRoleMapper.selectById(roleId);
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "角色不存在");
        }
        return row;
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

    private static class PublishAttributesSnapshot {
        private Integer deliveryStartHour;
        private Integer deliveryEndHour;
        private Integer staminaLevel;
        private Integer carryLevel;
        private Integer diveLevel;
        private Boolean faceOwned;
        private Boolean unlockSaeed;
        private List<String> knifeSkins;
        private List<String> redSkins;
        private List<String> weaponSkins;
        private List<String> goldSkins;
        private BigDecimal secretKd;
        private String defaultSpend;
        private Integer rentalDays;
        private String exchangeRateType;
        private BigDecimal customExchangeRate;
        private String compensationPlan;
        private BigDecimal deposit;
        private String remarks;
        private List<ExtraItemSnapshot> extraItems;

        public Integer getDeliveryStartHour() { return deliveryStartHour; }
        public void setDeliveryStartHour(Integer deliveryStartHour) { this.deliveryStartHour = deliveryStartHour; }
        public Integer getDeliveryEndHour() { return deliveryEndHour; }
        public void setDeliveryEndHour(Integer deliveryEndHour) { this.deliveryEndHour = deliveryEndHour; }
        public Integer getStaminaLevel() { return staminaLevel; }
        public void setStaminaLevel(Integer staminaLevel) { this.staminaLevel = staminaLevel; }
        public Integer getCarryLevel() { return carryLevel; }
        public void setCarryLevel(Integer carryLevel) { this.carryLevel = carryLevel; }
        public Integer getDiveLevel() { return diveLevel; }
        public void setDiveLevel(Integer diveLevel) { this.diveLevel = diveLevel; }
        public Boolean getFaceOwned() { return faceOwned; }
        public void setFaceOwned(Boolean faceOwned) { this.faceOwned = faceOwned; }
        public Boolean getUnlockSaeed() { return unlockSaeed; }
        public void setUnlockSaeed(Boolean unlockSaeed) { this.unlockSaeed = unlockSaeed; }
        public List<String> getKnifeSkins() { return knifeSkins; }
        public void setKnifeSkins(List<String> knifeSkins) { this.knifeSkins = knifeSkins; }
        public List<String> getRedSkins() { return redSkins; }
        public void setRedSkins(List<String> redSkins) { this.redSkins = redSkins; }
        public List<String> getWeaponSkins() { return weaponSkins; }
        public void setWeaponSkins(List<String> weaponSkins) { this.weaponSkins = weaponSkins; }
        public List<String> getGoldSkins() { return goldSkins; }
        public void setGoldSkins(List<String> goldSkins) { this.goldSkins = goldSkins; }
        public BigDecimal getSecretKd() { return secretKd; }
        public void setSecretKd(BigDecimal secretKd) { this.secretKd = secretKd; }
        public String getDefaultSpend() { return defaultSpend; }
        public void setDefaultSpend(String defaultSpend) { this.defaultSpend = defaultSpend; }
        public Integer getRentalDays() { return rentalDays; }
        public void setRentalDays(Integer rentalDays) { this.rentalDays = rentalDays; }

        public String getExchangeRateType() {
            return exchangeRateType;
        }

        public void setExchangeRateType(String exchangeRateType) {
            this.exchangeRateType = exchangeRateType;
        }

        public BigDecimal getCustomExchangeRate() {
            return customExchangeRate;
        }

        public void setCustomExchangeRate(BigDecimal customExchangeRate) {
            this.customExchangeRate = customExchangeRate;
        }

        public String getCompensationPlan() { return compensationPlan; }
        public void setCompensationPlan(String compensationPlan) { this.compensationPlan = compensationPlan; }
        public BigDecimal getDeposit() { return deposit; }
        public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
        public List<ExtraItemSnapshot> getExtraItems() { return extraItems; }
        public void setExtraItems(List<ExtraItemSnapshot> extraItems) { this.extraItems = extraItems; }
    }

    private static class ExtraItemSnapshot {
        private String label;
        private Integer count;
        private String chargeMode;
        private String unitLabel;
        private BigDecimal totalPrice;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public String getChargeMode() { return chargeMode; }
        public void setChargeMode(String chargeMode) { this.chargeMode = chargeMode; }
        public String getUnitLabel() { return unitLabel; }
        public void setUnitLabel(String unitLabel) { this.unitLabel = unitLabel; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    }
}
