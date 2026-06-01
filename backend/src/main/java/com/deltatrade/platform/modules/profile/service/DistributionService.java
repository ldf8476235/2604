package com.deltatrade.platform.modules.profile.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.auth.mapper.AuthUserMapper;
import com.deltatrade.platform.modules.auth.model.AuthUserDO;
import com.deltatrade.platform.modules.boosting.mapper.BoostingServiceMapper;
import com.deltatrade.platform.modules.boosting.model.BoostingOrderDO;
import com.deltatrade.platform.modules.boosting.model.BoostingServiceDO;
import com.deltatrade.platform.modules.listing.mapper.AccountListingMapper;
import com.deltatrade.platform.modules.listing.mapper.StudioProfileMapper;
import com.deltatrade.platform.modules.listing.model.AccountListingDO;
import com.deltatrade.platform.modules.listing.model.StudioProfileDO;
import com.deltatrade.platform.modules.order.model.TradeOrderDO;
import com.deltatrade.platform.modules.oss.service.OssStorageService;
import com.deltatrade.platform.modules.profile.mapper.DistributionInviteLinkMapper;
import com.deltatrade.platform.modules.profile.mapper.DistributionOrderMapper;
import com.deltatrade.platform.modules.profile.mapper.DistributionReferralMapper;
import com.deltatrade.platform.modules.profile.mapper.UserMessageMapper;
import com.deltatrade.platform.modules.profile.mapper.UserWalletMapper;
import com.deltatrade.platform.modules.profile.mapper.WalletTransactionMapper;
import com.deltatrade.platform.modules.profile.model.DistributionInviteLinkDO;
import com.deltatrade.platform.modules.profile.model.DistributionOrderDO;
import com.deltatrade.platform.modules.profile.model.DistributionReferralDO;
import com.deltatrade.platform.modules.profile.model.UserMessageDO;
import com.deltatrade.platform.modules.profile.model.UserWalletDO;
import com.deltatrade.platform.modules.profile.model.WalletTransactionDO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DistributionService {

    private static final Logger log = LoggerFactory.getLogger(DistributionService.class);
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };
    private static final String DISTRIBUTION_CONFIG_KEY = "distribution";

    private final AuthUserMapper authUserMapper;
    private final UserWalletMapper userWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final UserMessageMapper userMessageMapper;
    private final DistributionInviteLinkMapper distributionInviteLinkMapper;
    private final DistributionReferralMapper distributionReferralMapper;
    private final DistributionOrderMapper distributionOrderMapper;
    private final AccountListingMapper accountListingMapper;
    private final StudioProfileMapper studioProfileMapper;
    private final BoostingServiceMapper boostingServiceMapper;
    private final OssStorageService ossStorageService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DistributionService(
        AuthUserMapper authUserMapper,
        UserWalletMapper userWalletMapper,
        WalletTransactionMapper walletTransactionMapper,
        UserMessageMapper userMessageMapper,
        DistributionInviteLinkMapper distributionInviteLinkMapper,
        DistributionReferralMapper distributionReferralMapper,
        DistributionOrderMapper distributionOrderMapper,
        AccountListingMapper accountListingMapper,
        StudioProfileMapper studioProfileMapper,
        BoostingServiceMapper boostingServiceMapper,
        OssStorageService ossStorageService,
        JdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper
    ) {
        this.authUserMapper = authUserMapper;
        this.userWalletMapper = userWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.userMessageMapper = userMessageMapper;
        this.distributionInviteLinkMapper = distributionInviteLinkMapper;
        this.distributionReferralMapper = distributionReferralMapper;
        this.distributionOrderMapper = distributionOrderMapper;
        this.accountListingMapper = accountListingMapper;
        this.studioProfileMapper = studioProfileMapper;
        this.boostingServiceMapper = boostingServiceMapper;
        this.ossStorageService = ossStorageService;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void ensureAutoEnabled(Long userId) {
        AuthUserDO user = requireUser(userId);
        ensureDistributionAccess(user);
    }

    @Transactional
    public DistributionCenterResult getDistributionCenter(Long userId, String keyword, String range, String startDate, String endDate) {
        AuthUserDO user = requireUser(userId);
        ensureDistributionAccess(user);
        user = requireUser(userId);
        DistributionAccess access = inspectAccess(user);
        if (!access.isEnabled()) {
            return DistributionCenterResult.locked(access.getLockedReason());
        }

        DistributionInviteLinkDO inviteLink = findActiveInviteLink(userId);
        if (inviteLink != null && !isShortInviteCode(inviteLink.getInviteCode())) {
            inviteLink = recreateInviteLink(userId, inviteLink, user);
        }
        DateRange dateRange = resolveDateRange(range, startDate, endDate);
        String normalizedKeyword = normalizeKeyword(keyword);

        Integer totalPromotedUsers = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM distribution_referral WHERE promoter_user_id = ?",
            Integer.class,
            userId
        );
        Integer totalOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM distribution_order WHERE promoter_user_id = ? AND status <> 'VOID'",
            Integer.class,
            userId
        );
        BigDecimal totalCommission = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(commission_amount), 0) FROM distribution_order WHERE promoter_user_id = ? AND status = 'SETTLED'",
            BigDecimal.class,
            userId
        );
        BigDecimal monthCommission = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(commission_amount), 0) FROM distribution_order WHERE promoter_user_id = ? AND status = 'SETTLED' " +
                "AND settled_at >= DATE_FORMAT(CURDATE(), '%Y-%m-01') AND settled_at < DATE_ADD(LAST_DAY(CURDATE()), INTERVAL 1 DAY)",
            BigDecimal.class,
            userId
        );
        BigDecimal yesterdayCommission = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(commission_amount), 0) FROM distribution_order WHERE promoter_user_id = ? AND status = 'SETTLED' " +
                "AND settled_at >= DATE_SUB(CURDATE(), INTERVAL 1 DAY) AND settled_at < CURDATE()",
            BigDecimal.class,
            userId
        );
        BigDecimal todayEstimatedCommission = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(commission_amount), 0) FROM distribution_order WHERE promoter_user_id = ? AND status = 'PENDING_SETTLEMENT' " +
                "AND created_at >= CURDATE() AND created_at < DATE_ADD(CURDATE(), INTERVAL 1 DAY)",
            BigDecimal.class,
            userId
        );

        String likeKeyword = normalizedKeyword == null ? null : "%" + normalizedKeyword + "%";
        List<FanItem> fans = jdbcTemplate.query(
            "SELECT r.id, u.nickname, u.phone, r.status, r.registered_at, r.effective_at, r.first_paid_order_no " +
                "FROM distribution_referral r LEFT JOIN auth_user u ON u.id = r.referred_user_id " +
                "WHERE r.promoter_user_id = ? " +
                "AND (? IS NULL OR u.nickname LIKE ? OR u.phone LIKE ?) " +
                "ORDER BY r.registered_at DESC, r.id DESC LIMIT 80",
            (rs, rowNum) -> new FanItem(
                String.valueOf(rs.getLong("id")),
                safeText(rs.getString("nickname"), "新用户"),
                maskPhone(rs.getString("phone")),
                renderReferralStatus(rs.getString("status")),
                formatTime(toLocalDateTime(rs.getTimestamp("registered_at"))),
                formatTime(toLocalDateTime(rs.getTimestamp("effective_at"))),
                trimNullable(rs.getString("first_paid_order_no"))
            ),
            userId,
            likeKeyword,
            likeKeyword,
            likeKeyword
        );

        List<DistributionOrderItem> orderRows = jdbcTemplate.query(
            "SELECT id, source_order_no, source_order_type, buyer_nickname, order_amount, commission_amount, status, settled_at, created_at " +
                "FROM distribution_order WHERE promoter_user_id = ? " +
                "AND (? IS NULL OR buyer_nickname LIKE ? OR source_order_no LIKE ?) " +
                "AND (? IS NULL OR created_at >= ?) " +
                "AND (? IS NULL OR created_at < ?) " +
                "ORDER BY created_at DESC, id DESC LIMIT 80",
            (rs, rowNum) -> new DistributionOrderItem(
                String.valueOf(rs.getLong("id")),
                trimNullable(rs.getString("source_order_no")),
                safeText(rs.getString("buyer_nickname"), "新用户"),
                renderOrderType(rs.getString("source_order_type")),
                renderMoney(rs.getBigDecimal("order_amount")),
                renderMoney(rs.getBigDecimal("commission_amount")),
                renderDistributionOrderStatus(rs.getString("status")),
                formatTime(toLocalDateTime(rs.getTimestamp("settled_at"))),
                formatTime(toLocalDateTime(rs.getTimestamp("created_at")))
            ),
            userId,
            likeKeyword,
            likeKeyword,
            likeKeyword,
            dateRange.getStartAt(),
            dateRange.getStartAt(),
            dateRange.getEndAt(),
            dateRange.getEndAt()
        );

        List<CommissionItem> commissionRows = jdbcTemplate.query(
            "SELECT id, source_order_no, commission_amount, status, settled_at, created_at " +
                "FROM distribution_order WHERE promoter_user_id = ? " +
                "AND (? IS NULL OR source_order_no LIKE ?) " +
                "AND (? IS NULL OR created_at >= ?) " +
                "AND (? IS NULL OR created_at < ?) " +
                "ORDER BY created_at DESC, id DESC LIMIT 80",
            (rs, rowNum) -> new CommissionItem(
                String.valueOf(rs.getLong("id")),
                trimNullable(rs.getString("source_order_no")),
                renderMoney(rs.getBigDecimal("commission_amount")),
                renderDistributionOrderStatus(rs.getString("status")),
                formatTime(toLocalDateTime(rs.getTimestamp("settled_at"))),
                formatTime(toLocalDateTime(rs.getTimestamp("created_at")))
            ),
            userId,
            likeKeyword,
            likeKeyword,
            dateRange.getStartAt(),
            dateRange.getStartAt(),
            dateRange.getEndAt(),
            dateRange.getEndAt()
        );

        return new DistributionCenterResult(
            true,
            "",
            inviteLink == null ? "" : inviteLink.getInviteCode(),
            inviteLink == null ? "" : inviteLink.getInvitePath(),
            previewNullable(inviteLink == null ? null : inviteLink.getPosterKey()),
            totalPromotedUsers == null ? 0 : totalPromotedUsers.intValue(),
            totalOrders == null ? 0 : totalOrders.intValue(),
            renderMoney(totalCommission),
            renderMoney(monthCommission),
            renderMoney(yesterdayCommission),
            renderMoney(todayEstimatedCommission),
            fans,
            orderRows,
            commissionRows
        );
    }

    @Transactional
    public InviteLinkResult generateInviteLink(Long userId, boolean regenerate) {
        AuthUserDO user = requireUser(userId);
        ensureDistributionAccess(user);
        user = requireUser(userId);
        DistributionAccess access = inspectAccess(user);
        if (!access.isEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, access.getLockedReason());
        }
        DistributionInviteLinkDO active = findActiveInviteLink(userId);
        if (active != null && !regenerate && isShortInviteCode(active.getInviteCode())) {
            return toInviteLinkResult(active);
        }

        DistributionInviteLinkDO next = recreateInviteLink(userId, active, user);
        log.info("distribution invite link generated userId={} code={} regenerate={}", userId, next.getInviteCode(), regenerate);
        return toInviteLinkResult(next);
    }

    @Transactional(readOnly = true)
    public PublicInviteLandingResult resolveInvite(String inviteCode) {
        DistributionInviteLinkDO link = requireActiveInviteLink(inviteCode);
        AuthUserDO promoter = requireUser(link.getPromoterUserId());
        return new PublicInviteLandingResult(
            true,
            promoter.getNickname(),
            maskPhone(promoter.getPhone()),
            link.getInviteCode(),
            link.getInvitePath(),
            previewNullable(link.getPosterKey()),
            "新用户通过专属链接注册并完成首单后，邀请人可获得一级分销佣金。"
        );
    }

    @Transactional
    public void bindReferralOnRegister(Long referredUserId, String inviteCode, String sourceChannel) {
        if (!StringUtils.hasText(inviteCode)) {
            return;
        }
        DistributionInviteLinkDO link = findActiveInviteLinkByCode(inviteCode.trim());
        if (link == null) {
            log.info("distribution referral ignored because inviteCode invalid code={}", inviteCode);
            return;
        }
        if (link.getPromoterUserId().equals(referredUserId)) {
            return;
        }
        DistributionReferralDO existing = distributionReferralMapper.selectOne(
            Wrappers.<DistributionReferralDO>lambdaQuery()
                .eq(DistributionReferralDO::getReferredUserId, referredUserId)
                .last("LIMIT 1")
        );
        if (existing != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        DistributionReferralDO row = new DistributionReferralDO();
        row.setPromoterUserId(link.getPromoterUserId());
        row.setReferredUserId(referredUserId);
        row.setInviteCode(link.getInviteCode());
        row.setStatus("REGISTERED");
        row.setSourceChannel(StringUtils.hasText(sourceChannel) ? sourceChannel.trim().toUpperCase(Locale.ROOT) : "REGISTER");
        row.setRegisteredAt(now);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        distributionReferralMapper.insert(row);
        createUserMessage(link.getPromoterUserId(), "分销新注册用户", "你邀请的新用户已注册成功，待其完成首单后将自动结算佣金。");
        log.info("distribution referral bound promoterUserId={} referredUserId={} code={} source={}",
            link.getPromoterUserId(), referredUserId, link.getInviteCode(), row.getSourceChannel());
    }

    @Transactional
    public void onTradeOrderPaid(TradeOrderDO order) {
        DistributionReferralDO referral = findReferral(order.getBuyerUserId());
        if (referral == null || referral.getPromoterUserId() == null) {
            return;
        }
        if (!allowCreateDistributionOrder(referral, order.getOrderNo(), "TRADE")) {
            return;
        }
        AccountListingDO listing = accountListingMapper.selectOne(
            Wrappers.<AccountListingDO>lambdaQuery().eq(AccountListingDO::getListingNo, order.getListingNo()).last("LIMIT 1")
        );
        BigDecimal commissionBase = safeAmount(order.getItemAmount());
        if (commissionBase.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal rate = resolveTradeCommissionRate(listing, order.getSellerUserId());
        createPendingDistributionOrder(referral, order.getOrderNo(), "TRADE", order.getStatus(), order.getBuyerNickname(), commissionBase, rate);
    }

    @Transactional
    public void onTradeOrderCompleted(TradeOrderDO order) {
        settleDistributionOrder(order.getOrderNo(), "TRADE", order.getStatus(), true);
    }

    @Transactional
    public void onTradeOrderClosed(TradeOrderDO order) {
        settleDistributionOrder(order.getOrderNo(), "TRADE", order.getStatus(), false);
    }

    @Transactional
    public void onBoostingOrderPaid(BoostingOrderDO order) {
        DistributionReferralDO referral = findReferral(order.getBuyerUserId());
        if (referral == null || referral.getPromoterUserId() == null) {
            return;
        }
        if (!allowCreateDistributionOrder(referral, order.getOrderNo(), "BOOSTING")) {
            return;
        }
        BoostingServiceDO service = boostingServiceMapper.selectOne(
            Wrappers.<BoostingServiceDO>lambdaQuery().eq(BoostingServiceDO::getServiceNo, order.getServiceNo()).last("LIMIT 1")
        );
        BigDecimal commissionBase = safeAmount(order.getPrice());
        if (commissionBase.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal rate = resolveBoostingCommissionRate(service);
        createPendingDistributionOrder(referral, order.getOrderNo(), "BOOSTING", order.getStatus(), order.getBuyerNickname(), commissionBase, rate);
    }

    @Transactional
    public void onBoostingOrderCompleted(BoostingOrderDO order) {
        settleDistributionOrder(order.getOrderNo(), "BOOSTING", order.getStatus(), true);
    }

    @Transactional
    public void onBoostingOrderCanceled(BoostingOrderDO order) {
        settleDistributionOrder(order.getOrderNo(), "BOOSTING", order.getStatus(), false);
    }

    private DistributionAccess inspectAccess(AuthUserDO user) {
        DistributionConfig config = loadConfig();
        boolean verified = Boolean.TRUE.equals(user.getVerified()) && "APPROVED".equalsIgnoreCase(trimNullable(user.getRealNameStatus()));
        boolean enabled = Boolean.TRUE.equals(user.getDistributionEnabled());
        if (!enabled && config.isAutoEnableAfterVerified() && verified) {
            enabled = true;
        }
        return new DistributionAccess(enabled, verified ? "分销功能已关闭，请联系平台管理员开启自动开通" : "完成实名认证审核通过后即可开通分销");
    }

    private void ensureDistributionAccess(AuthUserDO user) {
        DistributionConfig config = loadConfig();
        boolean verified = Boolean.TRUE.equals(user.getVerified()) && "APPROVED".equalsIgnoreCase(trimNullable(user.getRealNameStatus()));
        if (!Boolean.TRUE.equals(user.getDistributionEnabled()) && config.isAutoEnableAfterVerified() && verified) {
            user.setDistributionEnabled(true);
            user.setDistributionOpenedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            authUserMapper.updateById(user);
            ensureWallet(user.getId());
            log.info("distribution auto enabled userId={} verified={}", user.getId(), verified);
        }
    }

    private boolean allowCreateDistributionOrder(DistributionReferralDO referral, String orderNo, String orderType) {
        DistributionOrderDO existingByOrder = distributionOrderMapper.selectOne(
            Wrappers.<DistributionOrderDO>lambdaQuery()
                .eq(DistributionOrderDO::getSourceOrderNo, orderNo)
                .eq(DistributionOrderDO::getSourceOrderType, orderType)
                .last("LIMIT 1")
        );
        if (existingByOrder != null) {
            return false;
        }
        if (StringUtils.hasText(referral.getFirstPaidOrderNo())
            && StringUtils.hasText(referral.getFirstPaidOrderType())
            && !"VOID".equalsIgnoreCase(referral.getStatus())) {
            return orderNo.equals(referral.getFirstPaidOrderNo()) && orderType.equals(referral.getFirstPaidOrderType());
        }
        return true;
    }

    private void createPendingDistributionOrder(
        DistributionReferralDO referral,
        String orderNo,
        String orderType,
        String sourceOrderStatus,
        String buyerNickname,
        BigDecimal commissionBase,
        BigDecimal rate
    ) {
        LocalDateTime now = LocalDateTime.now();
        DistributionOrderDO distributionOrder = new DistributionOrderDO();
        distributionOrder.setDistributionNo(buildDistributionNo());
        distributionOrder.setPromoterUserId(referral.getPromoterUserId());
        distributionOrder.setReferredUserId(referral.getReferredUserId());
        distributionOrder.setBuyerNickname(safeText(buyerNickname, "新用户"));
        distributionOrder.setSourceOrderNo(orderNo);
        distributionOrder.setSourceOrderType(orderType);
        distributionOrder.setSourceOrderStatus(sourceOrderStatus);
        distributionOrder.setOrderAmount(commissionBase.setScale(2, RoundingMode.HALF_UP));
        distributionOrder.setCommissionRate(rate);
        distributionOrder.setCommissionAmount(commissionBase.multiply(rate).setScale(2, RoundingMode.HALF_UP));
        distributionOrder.setStatus("PENDING_SETTLEMENT");
        distributionOrder.setCreatedAt(now);
        distributionOrder.setUpdatedAt(now);
        distributionOrderMapper.insert(distributionOrder);

        referral.setStatus("QUALIFIED_PENDING");
        referral.setFirstPaidOrderNo(orderNo);
        referral.setFirstPaidOrderType(orderType);
        referral.setUpdatedAt(now);
        distributionReferralMapper.updateById(referral);
        createUserMessage(
            referral.getPromoterUserId(),
            "分销首单已支付",
            "你邀请的新用户已完成首单支付，订单完成后佣金将自动发放到钱包。"
        );
        log.info("distribution pending order created promoterUserId={} referredUserId={} sourceOrderNo={} type={} commission={}",
            referral.getPromoterUserId(), referral.getReferredUserId(), orderNo, orderType, distributionOrder.getCommissionAmount());
    }

    private void settleDistributionOrder(String orderNo, String orderType, String sourceOrderStatus, boolean settled) {
        DistributionOrderDO row = distributionOrderMapper.selectOne(
            Wrappers.<DistributionOrderDO>lambdaQuery()
                .eq(DistributionOrderDO::getSourceOrderNo, orderNo)
                .eq(DistributionOrderDO::getSourceOrderType, orderType)
                .last("LIMIT 1")
        );
        if (row == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        row.setSourceOrderStatus(sourceOrderStatus);
        row.setUpdatedAt(now);
        DistributionReferralDO referral = findReferral(row.getReferredUserId());
        if (settled) {
            if ("SETTLED".equals(row.getStatus())) {
                return;
            }
            row.setStatus("SETTLED");
            row.setSettledAt(now);
            distributionOrderMapper.updateById(row);
            settleWalletCommission(row);
            if (referral != null) {
                referral.setStatus("EFFECTIVE");
                if (referral.getEffectiveAt() == null) {
                    referral.setEffectiveAt(now);
                }
                referral.setUpdatedAt(now);
                distributionReferralMapper.updateById(referral);
            }
            createUserMessage(
                row.getPromoterUserId(),
                "分销佣金已到账",
                "推广订单 " + safeText(row.getSourceOrderNo(), "-") + " 已完成，佣金 " + renderMoney(row.getCommissionAmount()) + " 已发放到钱包可用余额。"
            );
            log.info("distribution order settled distributionNo={} orderNo={} type={} amount={}",
                row.getDistributionNo(), orderNo, orderType, row.getCommissionAmount());
            return;
        }

        if (!"SETTLED".equals(row.getStatus())) {
            row.setStatus("VOID");
            distributionOrderMapper.updateById(row);
            if (referral != null && referral.getEffectiveAt() == null
                && orderNo.equals(referral.getFirstPaidOrderNo()) && orderType.equals(referral.getFirstPaidOrderType())) {
                referral.setStatus("REGISTERED");
                referral.setFirstPaidOrderNo(null);
                referral.setFirstPaidOrderType(null);
                referral.setUpdatedAt(now);
                distributionReferralMapper.updateById(referral);
            }
            log.info("distribution order voided distributionNo={} orderNo={} type={} sourceStatus={}",
                row.getDistributionNo(), orderNo, orderType, sourceOrderStatus);
        }
    }

    private void settleWalletCommission(DistributionOrderDO row) {
        UserWalletDO wallet = ensureWallet(row.getPromoterUserId());
        wallet.setAvailableBalance(safeAmount(wallet.getAvailableBalance()).add(safeAmount(row.getCommissionAmount())));
        wallet.setTotalCommission(safeAmount(wallet.getTotalCommission()).add(safeAmount(row.getCommissionAmount())));
        wallet.setUpdatedAt(LocalDateTime.now());
        userWalletMapper.updateById(wallet);

        WalletTransactionDO transaction = new WalletTransactionDO();
        transaction.setTransactionNo("WT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase(Locale.ROOT));
        transaction.setUserId(row.getPromoterUserId());
        transaction.setBizType("DISTRIBUTION_COMMISSION");
        transaction.setTitle("分销佣金到账");
        transaction.setAmount(safeAmount(row.getCommissionAmount()));
        transaction.setDirection("IN");
        transaction.setChannel("DISTRIBUTION");
        transaction.setStatus("SUCCESS");
        transaction.setRelatedNo(row.getSourceOrderNo());
        transaction.setRemark("推广订单佣金自动发放");
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        walletTransactionMapper.insert(transaction);
    }

    private DistributionInviteLinkDO findActiveInviteLink(Long userId) {
        return distributionInviteLinkMapper.selectOne(
            Wrappers.<DistributionInviteLinkDO>lambdaQuery()
                .eq(DistributionInviteLinkDO::getPromoterUserId, userId)
                .eq(DistributionInviteLinkDO::getActive, true)
                .orderByDesc(DistributionInviteLinkDO::getCreatedAt)
                .last("LIMIT 1")
        );
    }

    private DistributionInviteLinkDO findActiveInviteLinkByCode(String inviteCode) {
        return distributionInviteLinkMapper.selectOne(
            Wrappers.<DistributionInviteLinkDO>lambdaQuery()
                .eq(DistributionInviteLinkDO::getInviteCode, inviteCode)
                .eq(DistributionInviteLinkDO::getActive, true)
                .last("LIMIT 1")
        );
    }

    private DistributionInviteLinkDO requireActiveInviteLink(String inviteCode) {
        DistributionInviteLinkDO link = findActiveInviteLinkByCode(inviteCode);
        if (link == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "推广链接已失效，请重新获取最新链接");
        }
        return link;
    }

    private DistributionReferralDO findReferral(Long referredUserId) {
        return distributionReferralMapper.selectOne(
            Wrappers.<DistributionReferralDO>lambdaQuery()
                .eq(DistributionReferralDO::getReferredUserId, referredUserId)
                .last("LIMIT 1")
        );
    }

    private InviteLinkResult toInviteLinkResult(DistributionInviteLinkDO row) {
        return new InviteLinkResult(row.getInviteCode(), row.getInvitePath(), previewNullable(row.getPosterKey()));
    }

    private OssStorageService.OssUploadedFile generatePoster(DistributionInviteLinkDO link, AuthUserDO promoter) {
        String shareUrl = "https://webfeng.org" + link.getInvitePath();
        byte[] qrCode = buildQrPng(shareUrl);
        String qrBase64 = Base64.getEncoder().encodeToString(qrCode);
        String svg = ""
            + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"960\" height=\"1520\" viewBox=\"0 0 960 1520\">"
            + "<defs><linearGradient id=\"bg\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">"
            + "<stop offset=\"0%\" stop-color=\"#0d1b16\"/><stop offset=\"100%\" stop-color=\"#17352c\"/></linearGradient></defs>"
            + "<rect width=\"960\" height=\"1520\" rx=\"48\" fill=\"url(#bg)\"/>"
            + "<rect x=\"56\" y=\"56\" width=\"848\" height=\"1408\" rx=\"38\" fill=\"#f6fbf7\"/>"
            + "<text x=\"100\" y=\"156\" fill=\"#123127\" font-size=\"54\" font-family=\"PingFang SC, Microsoft YaHei, sans-serif\" font-weight=\"700\">DELTA TRADE</text>"
            + "<text x=\"100\" y=\"228\" fill=\"#2d5f50\" font-size=\"28\" font-family=\"PingFang SC, Microsoft YaHei, sans-serif\">邀请好友完成首单，佣金自动发放到钱包</text>"
            + "<rect x=\"100\" y=\"280\" width=\"760\" height=\"310\" rx=\"30\" fill=\"#103429\"/>"
            + "<text x=\"140\" y=\"362\" fill=\"#d5efe0\" font-size=\"32\" font-family=\"PingFang SC, Microsoft YaHei, sans-serif\">邀请人</text>"
            + "<text x=\"140\" y=\"428\" fill=\"#ffffff\" font-size=\"56\" font-family=\"PingFang SC, Microsoft YaHei, sans-serif\" font-weight=\"700\">"
            + escapeXml(promoter.getNickname()) + "</text>"
            + "<text x=\"140\" y=\"486\" fill=\"#a9d5bf\" font-size=\"26\" font-family=\"PingFang SC, Microsoft YaHei, sans-serif\">邀请码 " + escapeXml(link.getInviteCode()) + "</text>"
            + "<text x=\"140\" y=\"540\" fill=\"#8cc6a9\" font-size=\"22\" font-family=\"PingFang SC, Microsoft YaHei, sans-serif\">新人通过专属链接注册并完成首单后，佣金自动结算</text>"
            + "<rect x=\"100\" y=\"648\" width=\"760\" height=\"640\" rx=\"36\" fill=\"#ffffff\" stroke=\"#d9ece1\"/>"
            + "<image href=\"data:image/png;base64," + qrBase64 + "\" x=\"218\" y=\"726\" width=\"524\" height=\"524\"/>"
            + "<text x=\"480\" y=\"1362\" text-anchor=\"middle\" fill=\"#355f4d\" font-size=\"24\" font-family=\"PingFang SC, Microsoft YaHei, sans-serif\">"
            + escapeXml(shareUrl) + "</text>"
            + "<text x=\"480\" y=\"1410\" text-anchor=\"middle\" fill=\"#6b8f80\" font-size=\"22\" font-family=\"PingFang SC, Microsoft YaHei, sans-serif\">"
            + "保存海报，发给好友扫码注册即可" + "</text>"
            + "</svg>";
        byte[] bytes = svg.getBytes(StandardCharsets.UTF_8);
        return ossStorageService.uploadFile(
            "distribution-posters",
            "poster-" + link.getInviteCode() + ".svg",
            "image/svg+xml",
            bytes.length,
            new ByteArrayInputStream(bytes)
        );
    }

    private byte[] buildQrPng(String content) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 520, 520, hints);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | java.io.IOException exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "推广海报二维码生成失败");
        }
    }

    private DistributionConfig loadConfig() {
        try {
            String payload = jdbcTemplate.query(
                "SELECT config_value FROM system_configs WHERE config_key = ? LIMIT 1",
                new Object[] { DISTRIBUTION_CONFIG_KEY },
                (rs) -> rs.next() ? rs.getString(1) : null
            );
            if (!StringUtils.hasText(payload)) {
                return DistributionConfig.defaults();
            }
            Map<String, Object> row = objectMapper.readValue(payload, MAP_TYPE);
            return new DistributionConfig(
                readBoolean(row, "autoEnableAfterVerified", true),
                readRate(row, "defaultTradeCommissionRate", new BigDecimal("0.1000")),
                readRate(row, "defaultBoostingCommissionRate", new BigDecimal("0.1000"))
            );
        } catch (Exception exception) {
            log.warn("distribution config parse failed", exception);
            return DistributionConfig.defaults();
        }
    }

    private AuthUserDO requireUser(Long userId) {
        AuthUserDO user = authUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户不存在");
        }
        return user;
    }

    private UserWalletDO ensureWallet(Long userId) {
        UserWalletDO wallet = userWalletMapper.selectOne(
            Wrappers.<UserWalletDO>lambdaQuery().eq(UserWalletDO::getUserId, userId).last("LIMIT 1")
        );
        if (wallet != null) {
            return wallet;
        }
        LocalDateTime now = LocalDateTime.now();
        UserWalletDO created = new UserWalletDO();
        created.setUserId(userId);
        created.setAvailableBalance(BigDecimal.ZERO);
        created.setFrozenBalance(BigDecimal.ZERO);
        created.setTotalCommission(BigDecimal.ZERO);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        userWalletMapper.insert(created);
        return created;
    }

    private void createUserMessage(Long userId, String title, String content) {
        UserMessageDO row = new UserMessageDO();
        row.setUserId(userId);
        row.setCategory("DISTRIBUTION");
        row.setTitle(title);
        row.setContent(content);
        row.setReadFlag(false);
        row.setDeletedFlag(false);
        row.setCreatedAt(LocalDateTime.now());
        row.setUpdatedAt(LocalDateTime.now());
        userMessageMapper.insert(row);
    }

    private BigDecimal resolveTradeCommissionRate(AccountListingDO listing, Long sellerUserId) {
        if (listing != null && listing.getDistributionCommissionRate() != null && listing.getDistributionCommissionRate().compareTo(BigDecimal.ZERO) > 0) {
            return listing.getDistributionCommissionRate().setScale(4, RoundingMode.HALF_UP);
        }
        StudioProfileDO studio = studioProfileMapper.selectOne(
            Wrappers.<StudioProfileDO>lambdaQuery().eq(StudioProfileDO::getOwnerUserId, sellerUserId).last("LIMIT 1")
        );
        if (studio != null && studio.getDistributionCommissionRate() != null && studio.getDistributionCommissionRate().compareTo(BigDecimal.ZERO) > 0) {
            return studio.getDistributionCommissionRate().setScale(4, RoundingMode.HALF_UP);
        }
        return loadConfig().getDefaultTradeCommissionRate();
    }

    private BigDecimal resolveBoostingCommissionRate(BoostingServiceDO service) {
        if (service != null && service.getDistributionCommissionRate() != null && service.getDistributionCommissionRate().compareTo(BigDecimal.ZERO) > 0) {
            return service.getDistributionCommissionRate().setScale(4, RoundingMode.HALF_UP);
        }
        return loadConfig().getDefaultBoostingCommissionRate();
    }

    private DateRange resolveDateRange(String range, String startDate, String endDate) {
        String normalized = range == null || range.trim().isEmpty() ? "D30" : range.trim().toUpperCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        if ("ALL".equals(normalized)) {
            return new DateRange(null, null);
        }
        if ("D7".equals(normalized)) {
            return new DateRange(now.minusDays(7), now.plusDays(1));
        }
        if ("CUSTOM".equals(normalized) && StringUtils.hasText(startDate) && StringUtils.hasText(endDate)) {
            LocalDate start = LocalDate.parse(startDate.trim());
            LocalDate end = LocalDate.parse(endDate.trim());
            return new DateRange(start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        }
        return new DateRange(now.minusDays(30), now.plusDays(1));
    }

    private DistributionInviteLinkDO recreateInviteLink(Long userId, DistributionInviteLinkDO active, AuthUserDO user) {
        LocalDateTime now = LocalDateTime.now();
        if (active != null) {
            active.setActive(false);
            active.setInvalidatedAt(now);
            active.setUpdatedAt(now);
            distributionInviteLinkMapper.updateById(active);
        }

        DistributionInviteLinkDO next = new DistributionInviteLinkDO();
        next.setPromoterUserId(userId);
        next.setInviteCode(buildInviteCode());
        next.setInvitePath("/invite/" + next.getInviteCode());
        next.setActive(true);
        next.setCreatedAt(now);
        next.setUpdatedAt(now);
        distributionInviteLinkMapper.insert(next);
        next.setPosterKey(generatePoster(next, user).getObjectKey());
        next.setUpdatedAt(LocalDateTime.now());
        distributionInviteLinkMapper.updateById(next);
        return next;
    }

    private String buildInviteCode() {
        for (int attempt = 0; attempt < 100; attempt++) {
            String inviteCode = String.format(Locale.ROOT, "%06d", Integer.valueOf(ThreadLocalRandom.current().nextInt(0, 1000000)));
            DistributionInviteLinkDO existing = distributionInviteLinkMapper.selectOne(
                Wrappers.<DistributionInviteLinkDO>lambdaQuery()
                    .eq(DistributionInviteLinkDO::getInviteCode, inviteCode)
                    .last("LIMIT 1")
            );
            if (existing == null) {
                return inviteCode;
            }
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "推广码生成失败，请稍后重试");
    }

    private boolean isShortInviteCode(String inviteCode) {
        return inviteCode != null && inviteCode.matches("^\\d{6}$");
    }

    private String buildDistributionNo() {
        return "FX" + UUID.randomUUID().toString().replace("-", "").substring(0, 18).toUpperCase(Locale.ROOT);
    }

    private boolean readBoolean(Map<String, Object> payload, String key, boolean fallback) {
        Object value = payload.get(key);
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if ("true".equalsIgnoreCase(text) || "1".equals(text)) {
                return true;
            }
            if ("false".equalsIgnoreCase(text) || "0".equals(text)) {
                return false;
            }
        }
        return fallback;
    }

    private BigDecimal readRate(Map<String, Object> payload, String key, BigDecimal fallback) {
        Object value = payload.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            BigDecimal rate = new BigDecimal(String.valueOf(value).trim());
            if (rate.compareTo(BigDecimal.ZERO) < 0) {
                return fallback;
            }
            if (rate.compareTo(BigDecimal.ONE) > 0) {
                return fallback;
            }
            return rate.setScale(4, RoundingMode.HALF_UP);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String renderMoney(BigDecimal amount) {
        return "¥" + safeAmount(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String renderReferralStatus(String status) {
        if ("REGISTERED".equalsIgnoreCase(status)) return "已注册";
        if ("QUALIFIED_PENDING".equalsIgnoreCase(status)) return "首单待结算";
        if ("EFFECTIVE".equalsIgnoreCase(status)) return "有效推广";
        return safeText(status, "待生效");
    }

    private String renderDistributionOrderStatus(String status) {
        if ("PENDING_SETTLEMENT".equalsIgnoreCase(status)) return "待结算";
        if ("SETTLED".equalsIgnoreCase(status)) return "已到账";
        if ("VOID".equalsIgnoreCase(status)) return "已失效";
        return safeText(status, "-");
    }

    private String renderOrderType(String orderType) {
        if ("TRADE".equalsIgnoreCase(orderType)) return "账号交易";
        if ("BOOSTING".equalsIgnoreCase(orderType)) return "代肝服务";
        return safeText(orderType, "-");
    }

    private String previewNullable(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return "";
        }
        return ossStorageService.previewUrl(objectKey.trim());
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return keyword.trim();
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return safeText(phone, "");
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "-" : DISPLAY_TIME.format(value);
    }

    private LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String escapeXml(String value) {
        String text = safeText(value, "");
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private static class DistributionAccess {
        private final boolean enabled;
        private final String lockedReason;

        private DistributionAccess(boolean enabled, String lockedReason) {
            this.enabled = enabled;
            this.lockedReason = lockedReason;
        }

        public boolean isEnabled() { return enabled; }
        public String getLockedReason() { return lockedReason; }
    }

    private static class DistributionConfig {
        private final boolean autoEnableAfterVerified;
        private final BigDecimal defaultTradeCommissionRate;
        private final BigDecimal defaultBoostingCommissionRate;

        private DistributionConfig(boolean autoEnableAfterVerified, BigDecimal defaultTradeCommissionRate, BigDecimal defaultBoostingCommissionRate) {
            this.autoEnableAfterVerified = autoEnableAfterVerified;
            this.defaultTradeCommissionRate = defaultTradeCommissionRate;
            this.defaultBoostingCommissionRate = defaultBoostingCommissionRate;
        }

        public static DistributionConfig defaults() {
            return new DistributionConfig(true, new BigDecimal("0.1000"), new BigDecimal("0.1000"));
        }

        public boolean isAutoEnableAfterVerified() { return autoEnableAfterVerified; }
        public BigDecimal getDefaultTradeCommissionRate() { return defaultTradeCommissionRate; }
        public BigDecimal getDefaultBoostingCommissionRate() { return defaultBoostingCommissionRate; }
    }

    private static class DateRange {
        private final LocalDateTime startAt;
        private final LocalDateTime endAt;

        private DateRange(LocalDateTime startAt, LocalDateTime endAt) {
            this.startAt = startAt;
            this.endAt = endAt;
        }

        public LocalDateTime getStartAt() { return startAt; }
        public LocalDateTime getEndAt() { return endAt; }
    }

    public static class InviteLinkResult {
        private final String inviteCode;
        private final String invitePath;
        private final String posterUrl;

        public InviteLinkResult(String inviteCode, String invitePath, String posterUrl) {
            this.inviteCode = inviteCode;
            this.invitePath = invitePath;
            this.posterUrl = posterUrl;
        }

        public String getInviteCode() { return inviteCode; }
        public String getInvitePath() { return invitePath; }
        public String getPosterUrl() { return posterUrl; }
    }

    public static class PublicInviteLandingResult {
        private final boolean active;
        private final String promoterNickname;
        private final String promoterPhone;
        private final String inviteCode;
        private final String invitePath;
        private final String posterUrl;
        private final String description;

        public PublicInviteLandingResult(
            boolean active,
            String promoterNickname,
            String promoterPhone,
            String inviteCode,
            String invitePath,
            String posterUrl,
            String description
        ) {
            this.active = active;
            this.promoterNickname = promoterNickname;
            this.promoterPhone = promoterPhone;
            this.inviteCode = inviteCode;
            this.invitePath = invitePath;
            this.posterUrl = posterUrl;
            this.description = description;
        }

        public boolean isActive() { return active; }
        public String getPromoterNickname() { return promoterNickname; }
        public String getPromoterPhone() { return promoterPhone; }
        public String getInviteCode() { return inviteCode; }
        public String getInvitePath() { return invitePath; }
        public String getPosterUrl() { return posterUrl; }
        public String getDescription() { return description; }
    }

    public static class DistributionCenterResult {
        private final boolean enabled;
        private final String lockedReason;
        private final String inviteCode;
        private final String invitePath;
        private final String posterUrl;
        private final int totalPromotedUsers;
        private final int totalOrders;
        private final String totalCommission;
        private final String monthCommission;
        private final String yesterdayCommission;
        private final String todayEstimatedCommission;
        private final List<FanItem> fans;
        private final List<DistributionOrderItem> orderRows;
        private final List<CommissionItem> commissionRows;

        public DistributionCenterResult(
            boolean enabled,
            String lockedReason,
            String inviteCode,
            String invitePath,
            String posterUrl,
            int totalPromotedUsers,
            int totalOrders,
            String totalCommission,
            String monthCommission,
            String yesterdayCommission,
            String todayEstimatedCommission,
            List<FanItem> fans,
            List<DistributionOrderItem> orderRows,
            List<CommissionItem> commissionRows
        ) {
            this.enabled = enabled;
            this.lockedReason = lockedReason;
            this.inviteCode = inviteCode;
            this.invitePath = invitePath;
            this.posterUrl = posterUrl;
            this.totalPromotedUsers = totalPromotedUsers;
            this.totalOrders = totalOrders;
            this.totalCommission = totalCommission;
            this.monthCommission = monthCommission;
            this.yesterdayCommission = yesterdayCommission;
            this.todayEstimatedCommission = todayEstimatedCommission;
            this.fans = fans;
            this.orderRows = orderRows;
            this.commissionRows = commissionRows;
        }

        public static DistributionCenterResult locked(String reason) {
            return new DistributionCenterResult(
                false,
                reason,
                "",
                "",
                "",
                0,
                0,
                "¥0.00",
                "¥0.00",
                "¥0.00",
                "¥0.00",
                Collections.<FanItem>emptyList(),
                Collections.<DistributionOrderItem>emptyList(),
                Collections.<CommissionItem>emptyList()
            );
        }

        public boolean isEnabled() { return enabled; }
        public String getLockedReason() { return lockedReason; }
        public String getInviteCode() { return inviteCode; }
        public String getInvitePath() { return invitePath; }
        public String getPosterUrl() { return posterUrl; }
        public int getTotalPromotedUsers() { return totalPromotedUsers; }
        public int getTotalOrders() { return totalOrders; }
        public String getTotalCommission() { return totalCommission; }
        public String getMonthCommission() { return monthCommission; }
        public String getYesterdayCommission() { return yesterdayCommission; }
        public String getTodayEstimatedCommission() { return todayEstimatedCommission; }
        public List<FanItem> getFans() { return fans; }
        public List<DistributionOrderItem> getOrderRows() { return orderRows; }
        public List<CommissionItem> getCommissionRows() { return commissionRows; }
    }

    public static class FanItem {
        private final String id;
        private final String nickname;
        private final String phone;
        private final String status;
        private final String registeredAt;
        private final String effectiveAt;
        private final String firstOrderNo;

        public FanItem(String id, String nickname, String phone, String status, String registeredAt, String effectiveAt, String firstOrderNo) {
            this.id = id;
            this.nickname = nickname;
            this.phone = phone;
            this.status = status;
            this.registeredAt = registeredAt;
            this.effectiveAt = effectiveAt;
            this.firstOrderNo = firstOrderNo;
        }

        public String getId() { return id; }
        public String getNickname() { return nickname; }
        public String getPhone() { return phone; }
        public String getStatus() { return status; }
        public String getRegisteredAt() { return registeredAt; }
        public String getEffectiveAt() { return effectiveAt; }
        public String getFirstOrderNo() { return firstOrderNo; }
    }

    public static class DistributionOrderItem {
        private final String id;
        private final String orderNo;
        private final String nickname;
        private final String orderType;
        private final String amount;
        private final String commission;
        private final String status;
        private final String settledAt;
        private final String createdAt;

        public DistributionOrderItem(
            String id,
            String orderNo,
            String nickname,
            String orderType,
            String amount,
            String commission,
            String status,
            String settledAt,
            String createdAt
        ) {
            this.id = id;
            this.orderNo = orderNo;
            this.nickname = nickname;
            this.orderType = orderType;
            this.amount = amount;
            this.commission = commission;
            this.status = status;
            this.settledAt = settledAt;
            this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public String getOrderNo() { return orderNo; }
        public String getNickname() { return nickname; }
        public String getOrderType() { return orderType; }
        public String getAmount() { return amount; }
        public String getCommission() { return commission; }
        public String getStatus() { return status; }
        public String getSettledAt() { return settledAt; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class CommissionItem {
        private final String id;
        private final String orderNo;
        private final String amount;
        private final String status;
        private final String settledAt;
        private final String createdAt;

        public CommissionItem(String id, String orderNo, String amount, String status, String settledAt, String createdAt) {
            this.id = id;
            this.orderNo = orderNo;
            this.amount = amount;
            this.status = status;
            this.settledAt = settledAt;
            this.createdAt = createdAt;
        }

        public String getId() { return id; }
        public String getOrderNo() { return orderNo; }
        public String getAmount() { return amount; }
        public String getStatus() { return status; }
        public String getSettledAt() { return settledAt; }
        public String getCreatedAt() { return createdAt; }
    }
}
