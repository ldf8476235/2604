package com.deltatrade.platform.modules.profile.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.admin.mapper.OperationAnnouncementMapper;
import com.deltatrade.platform.modules.admin.model.OperationAnnouncementDO;
import com.deltatrade.platform.modules.auth.mapper.AuthUserMapper;
import com.deltatrade.platform.modules.auth.model.AuthUserDO;
import com.deltatrade.platform.modules.profile.mapper.UserMessageMapper;
import com.deltatrade.platform.modules.profile.mapper.UserWalletMapper;
import com.deltatrade.platform.modules.profile.mapper.WalletTransactionMapper;
import com.deltatrade.platform.modules.profile.mapper.WithdrawAccountMapper;
import com.deltatrade.platform.modules.profile.mapper.WithdrawApplicationMapper;
import com.deltatrade.platform.modules.profile.model.UserMessageDO;
import com.deltatrade.platform.modules.profile.model.UserWalletDO;
import com.deltatrade.platform.modules.profile.model.WalletTransactionDO;
import com.deltatrade.platform.modules.profile.model.WithdrawAccountDO;
import com.deltatrade.platform.modules.profile.model.WithdrawApplicationDO;
import com.deltatrade.platform.modules.oss.service.OssStorageService;
import com.deltatrade.platform.modules.payment.service.WechatPayGateway;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);
    private static final BigDecimal MIN_RECHARGE_AMOUNT = new BigDecimal("10.00");
    private static final BigDecimal MIN_WITHDRAW_AMOUNT = new BigDecimal("10.00");
    private static final int RECHARGE_PAYMENT_TIMEOUT_MINUTES = 10;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserWalletMapper userWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final WithdrawAccountMapper withdrawAccountMapper;
    private final WithdrawApplicationMapper withdrawApplicationMapper;
    private final UserMessageMapper userMessageMapper;
    private final AuthUserMapper authUserMapper;
    private final OperationAnnouncementMapper operationAnnouncementMapper;
    private final JdbcTemplate jdbcTemplate;
    private final OssStorageService ossStorageService;
    private final WechatPayGateway wechatPayGateway;

    public ProfileService(
        UserWalletMapper userWalletMapper,
        WalletTransactionMapper walletTransactionMapper,
        WithdrawAccountMapper withdrawAccountMapper,
        WithdrawApplicationMapper withdrawApplicationMapper,
        UserMessageMapper userMessageMapper,
        AuthUserMapper authUserMapper,
        OperationAnnouncementMapper operationAnnouncementMapper,
        JdbcTemplate jdbcTemplate,
        OssStorageService ossStorageService,
        WechatPayGateway wechatPayGateway
    ) {
        this.userWalletMapper = userWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.withdrawAccountMapper = withdrawAccountMapper;
        this.withdrawApplicationMapper = withdrawApplicationMapper;
        this.userMessageMapper = userMessageMapper;
        this.authUserMapper = authUserMapper;
        this.operationAnnouncementMapper = operationAnnouncementMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.ossStorageService = ossStorageService;
        this.wechatPayGateway = wechatPayGateway;
    }

    @Transactional
    public WalletOverview getWalletOverview(Long userId) {
        requireUser(userId);
        UserWalletDO wallet = ensureWallet(userId);
        WithdrawAccountDO withdrawAccount = findWithdrawAccount(userId);
        List<WalletTransactionDO> transactions = walletTransactionMapper.selectList(
            Wrappers.<WalletTransactionDO>lambdaQuery()
                .eq(WalletTransactionDO::getUserId, userId)
                .orderByDesc(WalletTransactionDO::getCreatedAt)
                .last("LIMIT 20")
        );
        List<WithdrawApplicationDO> withdrawApplications = withdrawApplicationMapper.selectList(
            Wrappers.<WithdrawApplicationDO>lambdaQuery()
                .eq(WithdrawApplicationDO::getUserId, userId)
                .orderByDesc(WithdrawApplicationDO::getCreatedAt)
                .last("LIMIT 20")
        );
        List<WalletRecord> records = buildWalletRecords(transactions, withdrawApplications);
        log.info("wallet overview loaded userId={} recordCount={} hasWithdrawAccount={}",
            userId, records.size(), withdrawAccount != null);
        return new WalletOverview(
            wallet.getAvailableBalance(),
            wallet.getFrozenBalance(),
            wallet.getTotalCommission(),
            withdrawAccount == null ? null : new WithdrawAccountView(
                withdrawAccount.getChannel(),
                withdrawAccount.getAccountName(),
                maskAccountNo(withdrawAccount.getAccountNo()),
                previewNullable(withdrawAccount.getQrCodeKey())
            ),
            records
        );
    }

    @Transactional
    public WalletOverview recharge(Long userId, BigDecimal amount, String channel) {
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "钱包充值仅支持微信支付，请使用微信充值入口");
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public WalletRechargePayResult createWechatRecharge(Long userId, BigDecimal amount, String notifyUrl, String preferredTradeType) {
        if (amount == null || amount.compareTo(MIN_RECHARGE_AMOUNT) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "充值金额不能低于 10 元");
        }
        AuthUserDO user = requireUser(userId);
        ensureWallet(userId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusMinutes(RECHARGE_PAYMENT_TIMEOUT_MINUTES);
        String rechargeNo = buildSerial("WR");
        String tradeType = resolveWechatTradeType(preferredTradeType, user);
        WechatPayGateway.WechatPayResult payment = wechatPayGateway.createOrder(new WechatPayGateway.CreateOrderRequest(
            rechargeNo,
            "钱包充值",
            amount.setScale(2, RoundingMode.HALF_UP),
            notifyUrl,
            "WALLET_RECHARGE",
            tradeType,
            "JSAPI".equals(tradeType) ? user.getOpenId() : null,
            expireAt
        ));
        jdbcTemplate.update(
            "INSERT INTO wallet_recharge_order (" +
                "recharge_no, user_id, amount, status, payment_method, payment_trade_type, payment_prepay_id, payment_code_url, " +
                "payment_expire_at, created_at, updated_at) VALUES (?, ?, ?, 'PENDING_PAYMENT', 'WECHAT', ?, ?, ?, ?, ?, ?)",
            rechargeNo,
            userId,
            amount.setScale(2, RoundingMode.HALF_UP),
            payment.getTradeType(),
            payment.getPrepayId(),
            payment.getCodeUrl(),
            payment.getExpireAt(),
            now,
            now
        );
        log.info("wallet recharge wechat payment prepared rechargeNo={} userId={} amount={} tradeType={}",
            rechargeNo, userId, amount, payment.getTradeType());
        return new WalletRechargePayResult(
            rechargeNo,
            "WECHAT",
            payment.getTradeType(),
            payment.getCodeUrl(),
            payment.getExpireAt(),
            payment.getJsapiPayParams() == null ? null : new JsapiPayParams(
                payment.getJsapiPayParams().getAppId(),
                payment.getJsapiPayParams().getTimeStamp(),
                payment.getJsapiPayParams().getNonceStr(),
                payment.getJsapiPayParams().getPackageValue(),
                payment.getJsapiPayParams().getSignType(),
                payment.getJsapiPayParams().getPaySign()
            )
        );
    }

    @Transactional(readOnly = true)
    public WalletRechargeStatus getWechatRechargeStatus(Long userId, String rechargeNo) {
        String normalizedRechargeNo = normalizeText(rechargeNo, "充值单号不能为空");
        List<WalletRechargeStatus> rows = jdbcTemplate.query(
            "SELECT recharge_no, amount, status, paid_at FROM wallet_recharge_order WHERE recharge_no = ? AND user_id = ? LIMIT 1",
            (rs, rowNum) -> new WalletRechargeStatus(
                rs.getString("recharge_no"),
                rs.getBigDecimal("amount"),
                rs.getString("status"),
                toLocalDateTime(rs, "paid_at")
            ),
            normalizedRechargeNo,
            userId
        );
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "充值订单不存在");
        }
        return rows.get(0);
    }

    @Transactional
    public void applyWechatRechargeSuccess(String rechargeNo, String transactionId, LocalDateTime paidAt) {
        String normalizedRechargeNo = normalizeText(rechargeNo, "充值单号不能为空");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT * FROM wallet_recharge_order WHERE recharge_no = ? LIMIT 1",
            normalizedRechargeNo
        );
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "钱包充值订单不存在");
        }
        Map<String, Object> row = rows.get(0);
        String status = String.valueOf(row.get("status"));
        Long userId = ((Number) row.get("user_id")).longValue();
        BigDecimal amount = (BigDecimal) row.get("amount");
        if ("SUCCESS".equalsIgnoreCase(status)) {
            log.info("wallet recharge notify ignored because already success rechargeNo={} transactionId={}", rechargeNo, transactionId);
            return;
        }
        Integer existing = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM wallet_transaction WHERE biz_type = 'RECHARGE' AND related_no = ?",
            Integer.class,
            normalizedRechargeNo
        );
        if (existing != null && existing > 0) {
            markRechargeOrderSuccess(normalizedRechargeNo, transactionId, paidAt);
            log.info("wallet recharge transaction already exists rechargeNo={} transactionId={}", rechargeNo, transactionId);
            return;
        }
        UserWalletDO wallet = ensureWallet(userId);
        LocalDateTime now = LocalDateTime.now();
        wallet.setAvailableBalance(safeAmount(wallet.getAvailableBalance()).add(amount));
        wallet.setUpdatedAt(now);
        updateWallet(wallet);
        WalletTransactionDO transaction = createTransaction(
            userId,
            "RECHARGE",
            "钱包充值",
            amount,
            "INCOME",
            "WECHAT",
            "SUCCESS",
            normalizedRechargeNo,
            "微信支付充值到账"
        );
        insertTransaction(transaction);
        markRechargeOrderSuccess(normalizedRechargeNo, transactionId, paidAt);
        createMessage(userId, "SYSTEM", "钱包充值成功", "你提交的 " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString() + " 元微信充值已入账。");
        log.info("wallet recharge success rechargeNo={} userId={} transactionNo={} amount={} wechatTransactionId={}",
            normalizedRechargeNo, userId, transaction.getTransactionNo(), amount, transactionId);
    }

    @Transactional
    public WithdrawAccountView bindWithdrawAccount(Long userId, String channel, String accountName, String accountNo, String qrCodeKey) {
        AuthUserDO user = requireUser(userId);
        if (!Boolean.TRUE.equals(user.getVerified()) || user.getRealName() == null || user.getRealName().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先完成实名认证后再绑定提现账户");
        }
        String normalizedChannel = normalizeChannel(channel);
        String normalizedName = normalizeText(accountName, "账户姓名不能为空");
        String normalizedAccountNo = normalizeText(accountNo, "账户号不能为空");
        String normalizedQrCodeKey = trimNullable(qrCodeKey);
        if (!user.getRealName().trim().equals(normalizedName)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "提现账户姓名必须与实名认证姓名一致");
        }

        WithdrawAccountDO existing = findWithdrawAccount(userId);
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            WithdrawAccountDO created = new WithdrawAccountDO();
            created.setUserId(userId);
            created.setChannel(normalizedChannel);
            created.setAccountName(normalizedName);
            created.setAccountNo(normalizedAccountNo);
            created.setQrCodeKey(normalizedQrCodeKey);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            insertWithdrawAccount(created);
        } else {
            existing.setChannel(normalizedChannel);
            existing.setAccountName(normalizedName);
            existing.setAccountNo(normalizedAccountNo);
            if (normalizedQrCodeKey != null) {
                existing.setQrCodeKey(normalizedQrCodeKey);
            }
            existing.setUpdatedAt(now);
            updateWithdrawAccount(existing);
        }
        createMessage(userId, "SYSTEM", "提现账户已更新", "你的" + renderChannel(normalizedChannel) + "提现账户已成功保存。");
        WithdrawAccountDO result = findWithdrawAccount(userId);
        log.info("withdraw account bind success userId={} channel={} account={}",
            userId, normalizedChannel, maskAccountNo(normalizedAccountNo));
        return new WithdrawAccountView(result.getChannel(), result.getAccountName(), maskAccountNo(result.getAccountNo()), previewNullable(result.getQrCodeKey()));
    }

    @Transactional
    public WalletOverview applyWithdraw(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(MIN_WITHDRAW_AMOUNT) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "提现金额不能低于 10 元");
        }
        AuthUserDO user = requireUser(userId);
        if (!Boolean.TRUE.equals(user.getVerified())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先完成实名认证后再申请提现");
        }

        WithdrawAccountDO account = findWithdrawAccount(userId);
        if (account == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先绑定提现账户");
        }

        UserWalletDO wallet = ensureWallet(userId);
        if (safeAmount(wallet.getAvailableBalance()).compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "提现金额不能超过可用余额");
        }

        LocalDateTime now = LocalDateTime.now();
        wallet.setAvailableBalance(safeAmount(wallet.getAvailableBalance()).subtract(amount));
        wallet.setFrozenBalance(safeAmount(wallet.getFrozenBalance()).add(amount));
        wallet.setUpdatedAt(now);
        updateWallet(wallet);

        WithdrawApplicationDO application = new WithdrawApplicationDO();
        application.setApplicationNo(buildSerial("WD"));
        application.setUserId(userId);
        application.setAmount(amount);
        application.setChannel(account.getChannel());
        application.setAccountName(account.getAccountName());
        application.setAccountNo(account.getAccountNo());
        application.setQrCodeKey(account.getQrCodeKey());
        application.setStatus("PENDING");
        application.setRejectReason(null);
        application.setReviewedAt(null);
        application.setPaidAt(null);
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        insertWithdrawApplication(application);

        WalletTransactionDO transaction = createTransaction(
            userId,
            "WITHDRAW",
            "提现申请",
            amount,
            "EXPENSE",
            account.getChannel(),
            "PENDING",
            application.getApplicationNo(),
            "等待后台审核"
        );
        insertTransaction(transaction);
        createMessage(userId, "SYSTEM", "提现申请已提交", "提现申请 " + application.getApplicationNo() + " 已提交，等待管理员审核。");
        log.info("withdraw apply success userId={} applicationNo={} amount={} channel={}",
            userId, application.getApplicationNo(), amount, account.getChannel());
        return getWalletOverview(userId);
    }

    @Transactional
    public MessageCenterResult getMessageCenter(Long userId, String category) {
        String normalizedCategory = normalizeMessageCategory(category);
        List<UserMessageDO> allMessages = userMessageMapper.selectList(
            Wrappers.<UserMessageDO>lambdaQuery()
                .eq(UserMessageDO::getUserId, userId)
                .eq(UserMessageDO::getDeletedFlag, false)
                .orderByDesc(UserMessageDO::getCreatedAt)
        );
        List<UserMessageDO> filtered = allMessages;
        if (!"ALL".equals(normalizedCategory)) {
            filtered = allMessages.stream()
                .filter(item -> normalizedCategory.equals(item.getCategory()))
                .collect(Collectors.toList());
        }
        MessageCounts counts = buildMessageCounts(allMessages);
        List<MessageItem> rows = filtered.stream().map(this::toMessageItem).collect(Collectors.toList());
        log.info("message center loaded userId={} category={} rows={} unread={}",
            userId, normalizedCategory, rows.size(), counts.getUnread());
        return new MessageCenterResult(counts, rows);
    }

    @Transactional(readOnly = true)
    public NotificationSummary getNotificationSummary(Long userId) {
        requireUser(userId);
        UserMessageDO latestUserMessage = userMessageMapper.selectOne(
            Wrappers.<UserMessageDO>lambdaQuery()
                .eq(UserMessageDO::getUserId, userId)
                .eq(UserMessageDO::getDeletedFlag, false)
                .eq(UserMessageDO::getReadFlag, false)
                .orderByDesc(UserMessageDO::getCreatedAt)
                .last("LIMIT 1")
        );
        Integer unreadMessageCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_message WHERE user_id = ? AND deleted_flag = FALSE AND read_flag = FALSE",
            Integer.class,
            userId
        );

        ImUnreadSnapshot imSnapshot = findLatestUnreadIm(userId);
        int unreadImCount = countUnreadIm(userId);
        NotificationItem latest = chooseLatestNotification(latestUserMessage, imSnapshot);
        int totalUnread = safeNumber(unreadMessageCount) + unreadImCount;
        return new NotificationSummary(totalUnread, safeNumber(unreadMessageCount), unreadImCount, latest);
    }

    @Transactional
    public MessageCenterResult markMessagesRead(Long userId, List<Long> ids, String category) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择需要标记已读的消息");
        }
        LocalDateTime now = LocalDateTime.now();
        List<UserMessageDO> rows = userMessageMapper.selectList(
            Wrappers.<UserMessageDO>lambdaQuery()
                .eq(UserMessageDO::getUserId, userId)
                .eq(UserMessageDO::getDeletedFlag, false)
                .in(UserMessageDO::getId, ids)
        );
        for (UserMessageDO item : rows) {
            if (!Boolean.TRUE.equals(item.getReadFlag())) {
                item.setReadFlag(true);
                item.setUpdatedAt(now);
                updateMessage(item);
            }
        }
        log.info("messages marked read userId={} count={}", userId, rows.size());
        return getMessageCenter(userId, category);
    }

    @Transactional
    public MessageCenterResult markAllMessagesRead(Long userId, String category) {
        String normalizedCategory = normalizeMessageCategory(category);
        LocalDateTime now = LocalDateTime.now();
        List<UserMessageDO> rows = userMessageMapper.selectList(
            Wrappers.<UserMessageDO>lambdaQuery()
                .eq(UserMessageDO::getUserId, userId)
                .eq(UserMessageDO::getDeletedFlag, false)
        );
        for (UserMessageDO item : rows) {
            if (!"ALL".equals(normalizedCategory) && !normalizedCategory.equals(item.getCategory())) {
                continue;
            }
            if (!Boolean.TRUE.equals(item.getReadFlag())) {
                item.setReadFlag(true);
                item.setUpdatedAt(now);
                updateMessage(item);
            }
        }
        log.info("messages marked all read userId={} category={}", userId, normalizedCategory);
        return getMessageCenter(userId, normalizedCategory);
    }

    @Transactional
    public MessageCenterResult deleteMessages(Long userId, List<Long> ids, String category) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择需要删除的消息");
        }
        LocalDateTime now = LocalDateTime.now();
        List<UserMessageDO> rows = userMessageMapper.selectList(
            Wrappers.<UserMessageDO>lambdaQuery()
                .eq(UserMessageDO::getUserId, userId)
                .eq(UserMessageDO::getDeletedFlag, false)
                .in(UserMessageDO::getId, ids)
        );
        for (UserMessageDO item : rows) {
            item.setDeletedFlag(true);
            item.setUpdatedAt(now);
            updateMessage(item);
        }
        log.info("messages deleted userId={} count={}", userId, rows.size());
        return getMessageCenter(userId, category);
    }

    @Transactional(readOnly = true)
    public CouponCenterResult getCouponCenter(Long userId) {
        requireUser(userId);
        LocalDateTime now = LocalDateTime.now();
        List<CouponItem> rows = jdbcTemplate.query(
            "SELECT id, coupon_no, name, amount, condition_text, scope_text, status, related_order_no, expire_at, used_at " +
                "FROM user_coupon WHERE user_id = ? ORDER BY created_at DESC, id DESC",
            (rs, rowNum) -> toCouponItem(rs, now),
            userId
        );
        int availableCount = (int) rows.stream().filter(item -> "available".equals(item.getStatus())).count();
        int historyCount = rows.size() - availableCount;
        log.info("coupon center loaded userId={} rows={} available={}", userId, rows.size(), availableCount);
        return new CouponCenterResult(availableCount, historyCount, rows);
    }

    @Transactional(readOnly = true)
    public AnnouncementCenterResult getPublishedAnnouncements(Integer limit) {
        int safeLimit = limit == null ? 40 : Math.max(1, Math.min(limit.intValue(), 80));
        List<OperationAnnouncementDO> rows = operationAnnouncementMapper.selectList(
            Wrappers.<OperationAnnouncementDO>lambdaQuery()
                .eq(OperationAnnouncementDO::getStatus, "PUBLISHED")
                .orderByDesc(OperationAnnouncementDO::getPinned)
                .orderByDesc(OperationAnnouncementDO::getPublishAt)
                .orderByDesc(OperationAnnouncementDO::getUpdatedAt)
                .last("LIMIT " + safeLimit)
        );
        List<AnnouncementItem> items = new ArrayList<AnnouncementItem>();
        for (OperationAnnouncementDO row : rows) {
            String content = row.getContent() == null ? "" : row.getContent().trim();
            items.add(new AnnouncementItem(
                safeText(row.getAnnouncementNo(), ""),
                safeText(row.getTitle(), "-"),
                content,
                summarizeAnnouncement(content),
                safeText(row.getCategory(), "SYSTEM"),
                renderOperationAnnouncementCategory(row.getCategory()),
                Boolean.TRUE.equals(row.getPinned()),
                formatTime(row.getPublishAt() != null ? row.getPublishAt() : row.getUpdatedAt())
            ));
        }
        log.info("published announcements loaded count={} limit={}", items.size(), safeLimit);
        return new AnnouncementCenterResult(items.size(), items);
    }

    @Transactional(readOnly = true)
    public DistributionCenterResult getDistributionCenter(Long userId) {
        requireUser(userId);
        UserWalletDO wallet = ensureWallet(userId);
        Integer promotedUsers = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT referred_user_id) FROM distribution_order WHERE promoter_user_id = ? AND referred_user_id IS NOT NULL",
            Integer.class,
            userId
        );
        Integer totalOrders = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM distribution_order WHERE promoter_user_id = ?",
            Integer.class,
            userId
        );
        List<DistributionOrderItem> rows = jdbcTemplate.query(
            "SELECT id, source_order_no, buyer_nickname, order_amount, commission_amount, status, created_at " +
                "FROM distribution_order WHERE promoter_user_id = ? ORDER BY created_at DESC, id DESC LIMIT 50",
            (rs, rowNum) -> new DistributionOrderItem(
                String.valueOf(rs.getLong("id")),
                trimNullable(rs.getString("source_order_no")),
                safeText(trimNullable(rs.getString("buyer_nickname")), "匿名用户"),
                renderMoney(rs.getBigDecimal("order_amount")),
                renderMoney(rs.getBigDecimal("commission_amount")),
                renderDistributionStatus(rs.getString("status")),
                formatTime(toLocalDateTime(rs, "created_at"))
            ),
            userId
        );
        DistributionCenterResult result = new DistributionCenterResult(
            "U" + userId,
            "/invite/U" + userId,
            promotedUsers == null ? 0 : promotedUsers,
            totalOrders == null ? 0 : totalOrders,
            renderMoney(wallet.getTotalCommission()),
            rows
        );
        log.info("distribution center loaded userId={} rows={} promotedUsers={} totalOrders={}",
            userId, rows.size(), result.getTotalPromotedUsers(), result.getTotalOrders());
        return result;
    }

    public WithdrawAccountView getWithdrawAccountView(Long userId) {
        WithdrawAccountDO account = findWithdrawAccount(userId);
        if (account == null) {
            return null;
        }
        return new WithdrawAccountView(account.getChannel(), account.getAccountName(), maskAccountNo(account.getAccountNo()), previewNullable(account.getQrCodeKey()));
    }

    private UserWalletDO ensureWallet(Long userId) {
        UserWalletDO wallet = requireWallet(userId);
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
        insertWallet(created);
        return created;
    }

    private UserWalletDO requireWallet(Long userId) {
        long startAt = System.currentTimeMillis();
        UserWalletDO wallet = userWalletMapper.selectOne(
            Wrappers.<UserWalletDO>lambdaQuery().eq(UserWalletDO::getUserId, userId).last("LIMIT 1")
        );
        log.info("mysql query success target=user_wallet_by_user_id costMs={} hit={} userId={}",
            System.currentTimeMillis() - startAt, wallet != null, userId);
        return wallet;
    }

    private WithdrawAccountDO findWithdrawAccount(Long userId) {
        long startAt = System.currentTimeMillis();
        WithdrawAccountDO account = withdrawAccountMapper.selectOne(
            Wrappers.<WithdrawAccountDO>lambdaQuery().eq(WithdrawAccountDO::getUserId, userId).last("LIMIT 1")
        );
        log.info("mysql query success target=withdraw_account_by_user_id costMs={} hit={} userId={}",
            System.currentTimeMillis() - startAt, account != null, userId);
        return account;
    }

    private AuthUserDO requireUser(Long userId) {
        long startAt = System.currentTimeMillis();
        AuthUserDO user = authUserMapper.selectById(userId);
        log.info("mysql query success target=auth_user_by_id costMs={} hit={} userId={}",
            System.currentTimeMillis() - startAt, user != null, userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录用户不存在，请重新登录");
        }
        return user;
    }

    private MessageCounts buildMessageCounts(List<UserMessageDO> rows) {
        int system = 0;
        int trade = 0;
        int service = 0;
        int distribution = 0;
        int unread = 0;
        for (UserMessageDO item : rows) {
            if (!Boolean.TRUE.equals(item.getReadFlag())) {
                unread++;
            }
            if ("SYSTEM".equals(item.getCategory())) {
                system++;
            } else if ("TRADE".equals(item.getCategory())) {
                trade++;
            } else if ("SERVICE".equals(item.getCategory())) {
                service++;
            } else if ("DISTRIBUTION".equals(item.getCategory())) {
                distribution++;
            }
        }
        return new MessageCounts(rows.size(), unread, system, trade, service, distribution);
    }

    private List<WalletRecord> buildWalletRecords(List<WalletTransactionDO> transactions, List<WithdrawApplicationDO> applications) {
        List<WalletRecord> records = new ArrayList<WalletRecord>();
        for (WalletTransactionDO item : transactions) {
            records.add(new WalletRecord(
                "transaction-" + item.getId(),
                item.getTitle(),
                renderSignedAmount(item.getAmount(), item.getDirection()),
                renderTransactionStatus(item.getStatus()),
                formatTime(item.getCreatedAt()),
                item.getBizType(),
                item.getChannel(),
                item.getRelatedNo()
            ));
        }
        for (WithdrawApplicationDO item : applications) {
            records.add(new WalletRecord(
                "withdraw-" + item.getId(),
                "提现申请 " + item.getApplicationNo(),
                "-" + item.getAmount().stripTrailingZeros().toPlainString(),
                renderWithdrawStatus(item.getStatus()),
                formatTime(item.getCreatedAt()),
                "WITHDRAW_APPLICATION",
                item.getChannel(),
                item.getApplicationNo()
            ));
        }
        Collections.sort(records, new Comparator<WalletRecord>() {
            @Override
            public int compare(WalletRecord left, WalletRecord right) {
                return right.getSortTime().compareTo(left.getSortTime());
            }
        });
        if (records.size() > 20) {
            return new ArrayList<WalletRecord>(records.subList(0, 20));
        }
        return records;
    }

    private MessageItem toMessageItem(UserMessageDO item) {
        return new MessageItem(
            item.getId(),
            item.getCategory(),
            renderMessageCategory(item.getCategory()),
            item.getTitle(),
            item.getContent(),
            formatTime(item.getCreatedAt()),
            !Boolean.TRUE.equals(item.getReadFlag())
        );
    }

    private ImUnreadSnapshot findLatestUnreadIm(Long userId) {
        List<ImUnreadSnapshot> rows = jdbcTemplate.query(
            "SELECT c.conversation_no, c.scene_code, c.source_order_no, c.title, c.last_message_excerpt, c.last_message_at, " +
                "COALESCE(p.last_read_message_id, 0) AS last_read_message_id, COALESCE(MAX(m.id), 0) AS latest_message_id " +
                "FROM im_participant p " +
                "JOIN im_conversation c ON c.conversation_no = p.conversation_no " +
                "JOIN im_message m ON m.conversation_no = p.conversation_no " +
                "LEFT JOIN trade_order t ON t.chat_group_no = c.conversation_no AND c.scene_code = 'TRADE_ORDER' " +
                "WHERE p.user_id = ? AND (m.sender_user_id IS NULL OR m.sender_user_id <> ?) " +
                "AND (c.scene_code <> 'TRADE_ORDER' OR t.status IN ('WAITING_TRADE', 'IN_PROGRESS', 'COMPLETED', 'REFUND_PENDING', 'AFTER_SALE')) " +
                "GROUP BY c.conversation_no, c.scene_code, c.source_order_no, c.title, c.last_message_excerpt, c.last_message_at, p.last_read_message_id " +
                "HAVING latest_message_id > last_read_message_id " +
                "ORDER BY c.last_message_at DESC, latest_message_id DESC LIMIT 1",
            (rs, rowNum) -> new ImUnreadSnapshot(
                rs.getString("conversation_no"),
                rs.getString("scene_code"),
                rs.getString("source_order_no"),
                rs.getString("title"),
                rs.getString("last_message_excerpt"),
                toLocalDateTime(rs, "last_message_at"),
                rs.getLong("latest_message_id")
            ),
            userId,
            userId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private int countUnreadIm(Long userId) {
        List<Integer> rows = jdbcTemplate.query(
            "SELECT COUNT(*) AS unread_count FROM (" +
                "SELECT p.conversation_no, COALESCE(p.last_read_message_id, 0) AS last_read_message_id, COALESCE(MAX(m.id), 0) AS latest_message_id " +
                "FROM im_participant p " +
                "JOIN im_conversation c ON c.conversation_no = p.conversation_no " +
                "JOIN im_message m ON m.conversation_no = p.conversation_no " +
                "LEFT JOIN trade_order t ON t.chat_group_no = c.conversation_no AND c.scene_code = 'TRADE_ORDER' " +
                "WHERE p.user_id = ? AND (m.sender_user_id IS NULL OR m.sender_user_id <> ?) " +
                "AND (c.scene_code <> 'TRADE_ORDER' OR t.status IN ('WAITING_TRADE', 'IN_PROGRESS', 'COMPLETED', 'REFUND_PENDING', 'AFTER_SALE')) " +
                "GROUP BY p.conversation_no, p.last_read_message_id " +
                "HAVING latest_message_id > last_read_message_id" +
                ") unread_im",
            (rs, rowNum) -> rs.getInt("unread_count"),
            userId,
            userId
        );
        return rows.isEmpty() ? 0 : safeNumber(rows.get(0));
    }

	    private NotificationItem chooseLatestNotification(UserMessageDO message, ImUnreadSnapshot imSnapshot) {
	        String messageConversationNo = resolveMessageConversationNo(message);
	        NotificationItem messageItem = message == null ? null : new NotificationItem(
	            "MSG-" + message.getId(),
	            "MESSAGE",
	            renderMessageCategory(message.getCategory()),
            message.getTitle(),
	            message.getContent(),
	            formatTime(message.getCreatedAt()),
	            message.getCreatedAt(),
	            messageConversationNo == null ? "/profile?tab=messages" : "/im/" + messageConversationNo,
	            messageConversationNo,
	            message.getId()
	        );
        NotificationItem imItem = imSnapshot == null ? null : new NotificationItem(
            "IM-" + imSnapshot.latestMessageId,
            "IM",
            renderMessageScene(imSnapshot.sceneCode),
            safeText(imSnapshot.title, "新会话消息"),
            safeText(imSnapshot.excerpt, "你有一条新的会话消息"),
            formatTime(imSnapshot.lastMessageAt),
            imSnapshot.lastMessageAt,
            "/im/" + imSnapshot.conversationNo,
            imSnapshot.conversationNo,
            null
        );
        if (messageItem == null) {
            return imItem;
        }
        if (imItem == null) {
            return messageItem;
        }
        LocalDateTime messageTime = messageItem.getSortTime();
        LocalDateTime imTime = imItem.getSortTime();
        if (imTime != null && (messageTime == null || imTime.isAfter(messageTime))) {
            return imItem;
	        }
	        return messageItem;
	    }

	    private String resolveMessageConversationNo(UserMessageDO message) {
	        if (message == null || !isSellerSaleNotification(message)) {
	            return null;
	        }
	        String orderNo = extractOrderNo(message.getContent());
	        if (orderNo == null) {
	            return null;
	        }
	        List<String> rows = jdbcTemplate.query(
	            "SELECT chat_group_no FROM trade_order WHERE order_no = ? AND seller_user_id = ? LIMIT 1",
	            (rs, rowNum) -> rs.getString("chat_group_no"),
	            orderNo,
	            message.getUserId()
	        );
	        if (rows.isEmpty()) {
	            return null;
	        }
	        return trimNullable(rows.get(0));
	    }

	    private boolean isSellerSaleNotification(UserMessageDO message) {
	        return "账号售出成功".equals(message.getTitle()) || "账号被购买".equals(message.getTitle());
	    }

	    private String extractOrderNo(String content) {
	        if (content == null) {
	            return null;
	        }
	        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("订单\\s*([A-Za-z0-9]+)").matcher(content);
	        return matcher.find() ? matcher.group(1) : null;
	    }

	    private WalletTransactionDO createTransaction(
        Long userId,
        String bizType,
        String title,
        BigDecimal amount,
        String direction,
        String channel,
        String status,
        String relatedNo,
        String remark
    ) {
        LocalDateTime now = LocalDateTime.now();
        WalletTransactionDO transaction = new WalletTransactionDO();
        transaction.setTransactionNo(buildSerial("WT"));
        transaction.setUserId(userId);
        transaction.setBizType(bizType);
        transaction.setTitle(title);
        transaction.setAmount(amount);
        transaction.setDirection(direction);
        transaction.setChannel(channel);
        transaction.setStatus(status);
        transaction.setRelatedNo(relatedNo);
        transaction.setRemark(remark);
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);
        return transaction;
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
        insertMessage(message);
    }

    private String normalizeChannel(String value) {
        String normalized = normalizeText(value, "渠道不能为空").toUpperCase(Locale.ROOT);
        if (!"ALIPAY".equals(normalized) && !"WECHAT".equals(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅支持支付宝或微信渠道");
        }
        return normalized;
    }

    private String normalizeMessageCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "ALL";
        }
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        List<String> supported = Arrays.asList("ALL", "SYSTEM", "TRADE", "SERVICE", "DISTRIBUTION");
        if (!supported.contains(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不支持的消息分类");
        }
        return normalized;
    }

    private String normalizeText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
        return value.trim();
    }

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String renderSignedAmount(BigDecimal amount, String direction) {
        String value = safeAmount(amount).stripTrailingZeros().toPlainString();
        return "EXPENSE".equals(direction) ? "-" + value : "+" + value;
    }

    private CouponItem toCouponItem(ResultSet rs, LocalDateTime now) throws SQLException {
        LocalDateTime expireAt = toLocalDateTime(rs, "expire_at");
        LocalDateTime usedAt = toLocalDateTime(rs, "used_at");
        String status = normalizeCouponStatus(trimNullable(rs.getString("status")), expireAt, usedAt, now);
        return new CouponItem(
            String.valueOf(rs.getLong("id")),
            trimNullable(rs.getString("coupon_no")),
            rs.getString("name"),
            renderMoney(rs.getBigDecimal("amount")),
            rs.getString("condition_text"),
            formatTime(expireAt),
            rs.getString("scope_text"),
            status,
            trimNullable(rs.getString("related_order_no")),
            formatTime(usedAt)
        );
    }

    private String normalizeCouponStatus(String status, LocalDateTime expireAt, LocalDateTime usedAt, LocalDateTime now) {
        String normalized = status == null ? "AVAILABLE" : status.toUpperCase(Locale.ROOT);
        if ("USED".equals(normalized) || usedAt != null) {
            return "used";
        }
        if ("VOID".equals(normalized)) {
            return "void";
        }
        if (expireAt != null && expireAt.isBefore(now)) {
            return "expired";
        }
        return "available";
    }

    private String renderMoney(BigDecimal amount) {
        return "¥" + safeAmount(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String resolveWechatTradeType(String preferredTradeType, AuthUserDO user) {
        String normalized = preferredTradeType == null ? "" : preferredTradeType.trim().toUpperCase(Locale.ROOT);
        if ("JSAPI".equals(normalized)) {
            if (user == null || user.getOpenId() == null || user.getOpenId().trim().isEmpty()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号未绑定微信，无法发起公众号支付");
            }
            return "JSAPI";
        }
        return "NATIVE";
    }

    private void markRechargeOrderSuccess(String rechargeNo, String transactionId, LocalDateTime paidAt) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectivePaidAt = paidAt == null ? now : paidAt;
        jdbcTemplate.update(
            "UPDATE wallet_recharge_order SET status = 'SUCCESS', payment_transaction_id = ?, payment_notified_at = ?, paid_at = ?, updated_at = ? WHERE recharge_no = ?",
            transactionId,
            now,
            effectivePaidAt,
            now,
            rechargeNo
        );
    }

    private String previewNullable(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return null;
        }
        return ossStorageService.previewUrl(objectKey.trim());
    }

    private String renderTransactionStatus(String status) {
        if ("SUCCESS".equals(status)) {
            return "已到账";
        }
        if ("PENDING".equals(status)) {
            return "审核中";
        }
        return "失败";
    }

    private String renderWithdrawStatus(String status) {
        if ("SUCCESS".equals(status) || "PAID".equals(status)) {
            return "已到账";
        }
        if ("REJECTED".equals(status)) {
            return "已驳回";
        }
        return "待审核";
    }

    private String renderMessageCategory(String category) {
        if ("SYSTEM".equals(category)) {
            return "系统公告";
        }
        if ("TRADE".equals(category)) {
            return "交易通知";
        }
        if ("SERVICE".equals(category)) {
            return "客服消息";
        }
        return "分销通知";
    }

    private String renderMessageScene(String sceneCode) {
        if ("TRADE_ORDER".equals(sceneCode)) {
            return "交易群聊";
        }
        if ("BOOSTING_ORDER".equals(sceneCode)) {
            return "代肝客服";
        }
        return "售前咨询";
    }

    private String renderOperationAnnouncementCategory(String category) {
        if ("ACTIVITY".equalsIgnoreCase(category)) {
            return "活动公告";
        }
        if ("TRADE".equalsIgnoreCase(category)) {
            return "交易公告";
        }
        return "官方公告";
    }

    private String renderChannel(String channel) {
        return "WECHAT".equals(channel) ? "微信" : "支付宝";
    }

    private String renderDistributionStatus(String status) {
        if (status == null) {
            return "待结算";
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        if ("SETTLED".equals(normalized) || "SUCCESS".equals(normalized)) {
            return "已结算";
        }
        if ("CANCELED".equals(normalized) || "VOID".equals(normalized)) {
            return "已失效";
        }
        return "待结算";
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String summarizeAnnouncement(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replace("\r", " ").replace("\n", " ").trim();
        if (normalized.length() <= 86) {
            return normalized;
        }
        return normalized.substring(0, 86) + "...";
    }

    private String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.length() < 8) {
            return "";
        }
        return accountNo.substring(0, 4) + "****" + accountNo.substring(accountNo.length() - 4);
    }

    private LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMATTER);
    }

    private int safeNumber(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    private String buildSerial(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
            UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase(Locale.ROOT);
    }

    private void insertWallet(UserWalletDO wallet) {
        long startAt = System.currentTimeMillis();
        int rows = userWalletMapper.insert(wallet);
        log.info("mysql insert success target=user_wallet costMs={} rows={} userId={}",
            System.currentTimeMillis() - startAt, rows, wallet.getUserId());
    }

    private void updateWallet(UserWalletDO wallet) {
        long startAt = System.currentTimeMillis();
        int rows = userWalletMapper.updateById(wallet);
        log.info("mysql update success target=user_wallet costMs={} rows={} userId={}",
            System.currentTimeMillis() - startAt, rows, wallet.getUserId());
    }

    private void insertTransaction(WalletTransactionDO transaction) {
        long startAt = System.currentTimeMillis();
        int rows = walletTransactionMapper.insert(transaction);
        log.info("mysql insert success target=wallet_transaction costMs={} rows={} transactionNo={} userId={} bizType={}",
            System.currentTimeMillis() - startAt, rows, transaction.getTransactionNo(), transaction.getUserId(), transaction.getBizType());
    }

    private void insertWithdrawAccount(WithdrawAccountDO account) {
        long startAt = System.currentTimeMillis();
        int rows = withdrawAccountMapper.insert(account);
        log.info("mysql insert success target=withdraw_account costMs={} rows={} userId={} channel={}",
            System.currentTimeMillis() - startAt, rows, account.getUserId(), account.getChannel());
    }

    private void updateWithdrawAccount(WithdrawAccountDO account) {
        long startAt = System.currentTimeMillis();
        int rows = withdrawAccountMapper.updateById(account);
        log.info("mysql update success target=withdraw_account costMs={} rows={} userId={} channel={}",
            System.currentTimeMillis() - startAt, rows, account.getUserId(), account.getChannel());
    }

    private void insertWithdrawApplication(WithdrawApplicationDO application) {
        long startAt = System.currentTimeMillis();
        int rows = withdrawApplicationMapper.insert(application);
        log.info("mysql insert success target=withdraw_application costMs={} rows={} applicationNo={} userId={}",
            System.currentTimeMillis() - startAt, rows, application.getApplicationNo(), application.getUserId());
    }

    private void insertMessage(UserMessageDO message) {
        long startAt = System.currentTimeMillis();
        int rows = userMessageMapper.insert(message);
        log.info("mysql insert success target=user_message costMs={} rows={} userId={} category={}",
            System.currentTimeMillis() - startAt, rows, message.getUserId(), message.getCategory());
    }

    private void updateMessage(UserMessageDO message) {
        long startAt = System.currentTimeMillis();
        int rows = userMessageMapper.updateById(message);
        log.info("mysql update success target=user_message costMs={} rows={} messageId={} userId={}",
            System.currentTimeMillis() - startAt, rows, message.getId(), message.getUserId());
    }

    public static class WalletOverview {
        private final BigDecimal availableBalance;
        private final BigDecimal frozenBalance;
        private final BigDecimal totalCommission;
        private final WithdrawAccountView withdrawAccount;
        private final List<WalletRecord> records;

        public WalletOverview(
            BigDecimal availableBalance,
            BigDecimal frozenBalance,
            BigDecimal totalCommission,
            WithdrawAccountView withdrawAccount,
            List<WalletRecord> records
        ) {
            this.availableBalance = availableBalance;
            this.frozenBalance = frozenBalance;
            this.totalCommission = totalCommission;
            this.withdrawAccount = withdrawAccount;
            this.records = records;
        }

        public BigDecimal getAvailableBalance() { return availableBalance; }
        public BigDecimal getFrozenBalance() { return frozenBalance; }
        public BigDecimal getTotalCommission() { return totalCommission; }
        public WithdrawAccountView getWithdrawAccount() { return withdrawAccount; }
        public List<WalletRecord> getRecords() { return records; }
    }

    public static class JsapiPayParams {
        private final String appId;
        private final String timeStamp;
        private final String nonceStr;
        private final String packageValue;
        private final String signType;
        private final String paySign;

        public JsapiPayParams(String appId, String timeStamp, String nonceStr, String packageValue, String signType, String paySign) {
            this.appId = appId;
            this.timeStamp = timeStamp;
            this.nonceStr = nonceStr;
            this.packageValue = packageValue;
            this.signType = signType;
            this.paySign = paySign;
        }

        public String getAppId() { return appId; }
        public String getTimeStamp() { return timeStamp; }
        public String getNonceStr() { return nonceStr; }
        public String getPackageValue() { return packageValue; }
        public String getSignType() { return signType; }
        public String getPaySign() { return paySign; }
    }

    public static class WalletRechargePayResult {
        private final String orderNo;
        private final String rechargeNo;
        private final String paymentMethod;
        private final String tradeType;
        private final String codeUrl;
        private final LocalDateTime expireAt;
        private final JsapiPayParams jsapiPayParams;

        public WalletRechargePayResult(
            String rechargeNo,
            String paymentMethod,
            String tradeType,
            String codeUrl,
            LocalDateTime expireAt,
            JsapiPayParams jsapiPayParams
        ) {
            this.orderNo = rechargeNo;
            this.rechargeNo = rechargeNo;
            this.paymentMethod = paymentMethod;
            this.tradeType = tradeType;
            this.codeUrl = codeUrl;
            this.expireAt = expireAt;
            this.jsapiPayParams = jsapiPayParams;
        }

        public String getOrderNo() { return orderNo; }
        public String getRechargeNo() { return rechargeNo; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getTradeType() { return tradeType; }
        public String getCodeUrl() { return codeUrl; }
        public LocalDateTime getExpireAt() { return expireAt; }
        public JsapiPayParams getJsapiPayParams() { return jsapiPayParams; }
    }

    public static class WalletRechargeStatus {
        private final String rechargeNo;
        private final BigDecimal amount;
        private final String status;
        private final LocalDateTime paidAt;

        public WalletRechargeStatus(String rechargeNo, BigDecimal amount, String status, LocalDateTime paidAt) {
            this.rechargeNo = rechargeNo;
            this.amount = amount;
            this.status = status;
            this.paidAt = paidAt;
        }

        public String getRechargeNo() { return rechargeNo; }
        public BigDecimal getAmount() { return amount; }
        public String getStatus() { return status; }
        public LocalDateTime getPaidAt() { return paidAt; }
        public boolean isPaid() { return "SUCCESS".equalsIgnoreCase(status); }
    }

    public static class WithdrawAccountView {
        private final String channel;
        private final String accountName;
        private final String maskedAccountNo;
        private final String qrCodeUrl;

        public WithdrawAccountView(String channel, String accountName, String maskedAccountNo, String qrCodeUrl) {
            this.channel = channel;
            this.accountName = accountName;
            this.maskedAccountNo = maskedAccountNo;
            this.qrCodeUrl = qrCodeUrl;
        }

        public String getChannel() { return channel; }
        public String getAccountName() { return accountName; }
        public String getMaskedAccountNo() { return maskedAccountNo; }
        public String getQrCodeUrl() { return qrCodeUrl; }
    }

    public static class WalletRecord {
        private final String id;
        private final String title;
        private final String amount;
        private final String status;
        private final String time;
        private final String type;
        private final String channel;
        private final String referenceNo;
        private final LocalDateTime sortTime;

        public WalletRecord(String id, String title, String amount, String status, String time, String type, String channel, String referenceNo) {
            this.id = id;
            this.title = title;
            this.amount = amount;
            this.status = status;
            this.time = time;
            this.type = type;
            this.channel = channel;
            this.referenceNo = referenceNo;
            this.sortTime = LocalDateTime.parse(time, TIME_FORMATTER);
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getAmount() { return amount; }
        public String getStatus() { return status; }
        public String getTime() { return time; }
        public String getType() { return type; }
        public String getChannel() { return channel; }
        public String getReferenceNo() { return referenceNo; }
        public LocalDateTime getSortTime() { return sortTime; }
    }

    public static class MessageCenterResult {
        private final MessageCounts counts;
        private final List<MessageItem> rows;

        public MessageCenterResult(MessageCounts counts, List<MessageItem> rows) {
            this.counts = counts;
            this.rows = rows;
        }

        public MessageCounts getCounts() { return counts; }
        public List<MessageItem> getRows() { return rows; }
    }

    public static class MessageCounts {
        private final int all;
        private final int unread;
        private final int system;
        private final int trade;
        private final int service;
        private final int distribution;

        public MessageCounts(int all, int unread, int system, int trade, int service, int distribution) {
            this.all = all;
            this.unread = unread;
            this.system = system;
            this.trade = trade;
            this.service = service;
            this.distribution = distribution;
        }

        public int getAll() { return all; }
        public int getUnread() { return unread; }
        public int getSystem() { return system; }
        public int getTrade() { return trade; }
        public int getService() { return service; }
        public int getDistribution() { return distribution; }
    }

    public static class MessageItem {
        private final Long id;
        private final String category;
        private final String categoryLabel;
        private final String title;
        private final String content;
        private final String time;
        private final boolean unread;

        public MessageItem(Long id, String category, String categoryLabel, String title, String content, String time, boolean unread) {
            this.id = id;
            this.category = category;
            this.categoryLabel = categoryLabel;
            this.title = title;
            this.content = content;
            this.time = time;
            this.unread = unread;
        }

        public Long getId() { return id; }
        public String getCategory() { return category; }
        public String getCategoryLabel() { return categoryLabel; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getTime() { return time; }
        public boolean isUnread() { return unread; }
    }

    public static class NotificationSummary {
        private final int totalUnread;
        private final int messageUnread;
        private final int imUnread;
        private final NotificationItem latest;

        public NotificationSummary(int totalUnread, int messageUnread, int imUnread, NotificationItem latest) {
            this.totalUnread = totalUnread;
            this.messageUnread = messageUnread;
            this.imUnread = imUnread;
            this.latest = latest;
        }

        public int getTotalUnread() { return totalUnread; }
        public int getMessageUnread() { return messageUnread; }
        public int getImUnread() { return imUnread; }
        public NotificationItem getLatest() { return latest; }
    }

    public static class NotificationItem {
        private final String id;
        private final String type;
        private final String label;
        private final String title;
        private final String content;
        private final String time;
        private final LocalDateTime sortTime;
        private final String targetUrl;
        private final String conversationNo;
        private final Long messageId;

        public NotificationItem(
            String id,
            String type,
            String label,
            String title,
            String content,
            String time,
            LocalDateTime sortTime,
            String targetUrl,
            String conversationNo,
            Long messageId
        ) {
            this.id = id;
            this.type = type;
            this.label = label;
            this.title = title;
            this.content = content;
            this.time = time;
            this.sortTime = sortTime;
            this.targetUrl = targetUrl;
            this.conversationNo = conversationNo;
            this.messageId = messageId;
        }

        public String getId() { return id; }
        public String getType() { return type; }
        public String getLabel() { return label; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getTime() { return time; }
        public String getTargetUrl() { return targetUrl; }
        public String getConversationNo() { return conversationNo; }
        public Long getMessageId() { return messageId; }
        private LocalDateTime getSortTime() { return sortTime; }
    }

    private static class ImUnreadSnapshot {
        private final String conversationNo;
        private final String sceneCode;
        private final String sourceOrderNo;
        private final String title;
        private final String excerpt;
        private final LocalDateTime lastMessageAt;
        private final long latestMessageId;

        private ImUnreadSnapshot(
            String conversationNo,
            String sceneCode,
            String sourceOrderNo,
            String title,
            String excerpt,
            LocalDateTime lastMessageAt,
            long latestMessageId
        ) {
            this.conversationNo = conversationNo;
            this.sceneCode = sceneCode;
            this.sourceOrderNo = sourceOrderNo;
            this.title = title;
            this.excerpt = excerpt;
            this.lastMessageAt = lastMessageAt;
            this.latestMessageId = latestMessageId;
        }
    }

    public static class CouponCenterResult {
        private final int availableCount;
        private final int historyCount;
        private final List<CouponItem> rows;

        public CouponCenterResult(int availableCount, int historyCount, List<CouponItem> rows) {
            this.availableCount = availableCount;
            this.historyCount = historyCount;
            this.rows = rows;
        }

        public int getAvailableCount() { return availableCount; }
        public int getHistoryCount() { return historyCount; }
        public List<CouponItem> getRows() { return rows; }
    }

    public static class CouponItem {
        private final String id;
        private final String couponNo;
        private final String name;
        private final String amount;
        private final String condition;
        private final String expireAt;
        private final String scope;
        private final String status;
        private final String orderNo;
        private final String usedAt;

        public CouponItem(String id, String couponNo, String name, String amount, String condition, String expireAt, String scope, String status, String orderNo, String usedAt) {
            this.id = id;
            this.couponNo = couponNo;
            this.name = name;
            this.amount = amount;
            this.condition = condition;
            this.expireAt = expireAt;
            this.scope = scope;
            this.status = status;
            this.orderNo = orderNo;
            this.usedAt = usedAt;
        }

        public String getId() { return id; }
        public String getCouponNo() { return couponNo; }
        public String getName() { return name; }
        public String getAmount() { return amount; }
        public String getCondition() { return condition; }
        public String getExpireAt() { return expireAt; }
        public String getScope() { return scope; }
        public String getStatus() { return status; }
        public String getOrderNo() { return orderNo; }
        public String getUsedAt() { return usedAt; }
    }

    public static class AnnouncementCenterResult {
        private final int total;
        private final List<AnnouncementItem> rows;

        public AnnouncementCenterResult(int total, List<AnnouncementItem> rows) {
            this.total = total;
            this.rows = rows;
        }

        public int getTotal() { return total; }
        public List<AnnouncementItem> getRows() { return rows; }
    }

    public static class AnnouncementItem {
        private final String announcementNo;
        private final String title;
        private final String content;
        private final String summary;
        private final String category;
        private final String categoryText;
        private final boolean pinned;
        private final String publishAt;

        public AnnouncementItem(
            String announcementNo,
            String title,
            String content,
            String summary,
            String category,
            String categoryText,
            boolean pinned,
            String publishAt
        ) {
            this.announcementNo = announcementNo;
            this.title = title;
            this.content = content;
            this.summary = summary;
            this.category = category;
            this.categoryText = categoryText;
            this.pinned = pinned;
            this.publishAt = publishAt;
        }

        public String getAnnouncementNo() { return announcementNo; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getSummary() { return summary; }
        public String getCategory() { return category; }
        public String getCategoryText() { return categoryText; }
        public boolean isPinned() { return pinned; }
        public String getPublishAt() { return publishAt; }
    }

    public static class DistributionCenterResult {
        private final String inviteCode;
        private final String invitePath;
        private final int totalPromotedUsers;
        private final int totalOrders;
        private final String totalCommission;
        private final List<DistributionOrderItem> rows;

        public DistributionCenterResult(
            String inviteCode,
            String invitePath,
            int totalPromotedUsers,
            int totalOrders,
            String totalCommission,
            List<DistributionOrderItem> rows
        ) {
            this.inviteCode = inviteCode;
            this.invitePath = invitePath;
            this.totalPromotedUsers = totalPromotedUsers;
            this.totalOrders = totalOrders;
            this.totalCommission = totalCommission;
            this.rows = rows;
        }

        public String getInviteCode() { return inviteCode; }
        public String getInvitePath() { return invitePath; }
        public int getTotalPromotedUsers() { return totalPromotedUsers; }
        public int getTotalOrders() { return totalOrders; }
        public String getTotalCommission() { return totalCommission; }
        public List<DistributionOrderItem> getRows() { return rows; }
    }

    public static class DistributionOrderItem {
        private final String id;
        private final String orderNo;
        private final String nickname;
        private final String amount;
        private final String commission;
        private final String status;
        private final String time;

        public DistributionOrderItem(String id, String orderNo, String nickname, String amount, String commission, String status, String time) {
            this.id = id;
            this.orderNo = orderNo;
            this.nickname = nickname;
            this.amount = amount;
            this.commission = commission;
            this.status = status;
            this.time = time;
        }

        public String getId() { return id; }
        public String getOrderNo() { return orderNo; }
        public String getNickname() { return nickname; }
        public String getAmount() { return amount; }
        public String getCommission() { return commission; }
        public String getStatus() { return status; }
        public String getTime() { return time; }
    }
}
