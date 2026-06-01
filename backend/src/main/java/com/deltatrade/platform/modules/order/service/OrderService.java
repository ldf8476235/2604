package com.deltatrade.platform.modules.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.auth.mapper.AuthUserMapper;
import com.deltatrade.platform.modules.auth.model.AuthUserDO;
import com.deltatrade.platform.modules.listing.mapper.AccountListingMapper;
import com.deltatrade.platform.modules.listing.model.AccountListingDO;
import com.deltatrade.platform.modules.im.service.ImService;
import com.deltatrade.platform.modules.order.mapper.TradeOrderMapper;
import com.deltatrade.platform.modules.order.model.TradeOrderDO;
import com.deltatrade.platform.modules.oss.service.OssStorageService;
import com.deltatrade.platform.modules.payment.service.WechatPayGateway;
import com.deltatrade.platform.modules.profile.mapper.UserMessageMapper;
import com.deltatrade.platform.modules.profile.mapper.UserWalletMapper;
import com.deltatrade.platform.modules.profile.mapper.WalletTransactionMapper;
import com.deltatrade.platform.modules.profile.model.UserMessageDO;
import com.deltatrade.platform.modules.profile.model.UserWalletDO;
import com.deltatrade.platform.modules.profile.model.WalletTransactionDO;
import com.deltatrade.platform.modules.profile.service.DistributionService;
import com.deltatrade.platform.modules.admin.service.AdminIntegrationConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int TRADE_PAYMENT_TIMEOUT_MINUTES = 10;
    private static final List<String> ORDER_STATUS_SEQUENCE = Arrays.asList(
        "PENDING_PAYMENT",
        "WAITING_TRADE",
        "IN_PROGRESS",
        "COMPLETED",
        "REFUND_PENDING",
        "AFTER_SALE",
        "REFUNDED",
        "CLOSED"
    );

    private final TradeOrderMapper tradeOrderMapper;
    private final AccountListingMapper accountListingMapper;
    private final AuthUserMapper authUserMapper;
    private final OssStorageService ossStorageService;
	    private final WechatPayGateway wechatPayGateway;
	    private final ImService imService;
	    private final DistributionService distributionService;
    private final AdminIntegrationConfigService adminIntegrationConfigService;
    private final UserWalletMapper userWalletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final UserMessageMapper userMessageMapper;
    private final ObjectMapper objectMapper;

    public OrderService(
        TradeOrderMapper tradeOrderMapper,
        AccountListingMapper accountListingMapper,
        AuthUserMapper authUserMapper,
	        OssStorageService ossStorageService,
	        WechatPayGateway wechatPayGateway,
	        ImService imService,
	        DistributionService distributionService,
        AdminIntegrationConfigService adminIntegrationConfigService,
        UserWalletMapper userWalletMapper,
        WalletTransactionMapper walletTransactionMapper,
        UserMessageMapper userMessageMapper,
        ObjectMapper objectMapper
    ) {
        this.tradeOrderMapper = tradeOrderMapper;
        this.accountListingMapper = accountListingMapper;
        this.authUserMapper = authUserMapper;
	        this.ossStorageService = ossStorageService;
	        this.wechatPayGateway = wechatPayGateway;
	        this.imService = imService;
	        this.distributionService = distributionService;
        this.adminIntegrationConfigService = adminIntegrationConfigService;
        this.userWalletMapper = userWalletMapper;
        this.walletTransactionMapper = walletTransactionMapper;
        this.userMessageMapper = userMessageMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateOrderResult create(CreateOrderCommand command) {
        long startedAt = System.currentTimeMillis();
        AccountListingDO listing = requireListing(command.getListingId());
        if (!"PUBLISHED".equals(listing.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号已被锁定或下架");
        }
        AuthUserDO buyer = requireUser(command.getBuyerId());
        if (listing.getSellerUserId().equals(command.getBuyerId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不能购买自己发布的账号");
        }
	        OrderExtraItemsSnapshot extraItemsSnapshot = buildExtraItemsSnapshot(listing, command.isIncludeExtraItems());
	        BigDecimal rentAmount = calculateRentAmount(listing);
	        BigDecimal depositAmount = calculateDepositAmount(listing);
	        BigDecimal itemAmount = rentAmount.add(extraItemsSnapshot.getAmount()).setScale(2, RoundingMode.HALF_UP);
	        BigDecimal totalAmount = itemAmount.add(depositAmount).setScale(2, RoundingMode.HALF_UP);
	        BigDecimal serviceFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
	        String orderNo = buildOrderNo();
	        LocalDateTime now = LocalDateTime.now();
	        LocalDateTime paymentExpireAt = now.plusMinutes(TRADE_PAYMENT_TIMEOUT_MINUTES);

	        TradeOrderDO order = new TradeOrderDO();
        order.setOrderNo(orderNo);
        order.setListingNo(listing.getListingNo());
        order.setListingTitle(listing.getTitle());
        order.setListingSummary(buildListingSummary(listing));
        order.setListingCoverKey(listing.getCoverImageKey());
        order.setBuyerUserId(command.getBuyerId());
        order.setBuyerNickname(buyer.getNickname());
        order.setSellerUserId(listing.getSellerUserId());
        order.setSellerNickname(listing.getSellerNickname());
        order.setSellerType(listing.getSellerType());
        order.setSellerDisplayName(resolveSellerDisplayName(listing.getSellerType(), listing.getStudioName(), listing.getSellerNickname()));
        order.setStatus("PENDING_PAYMENT");
        order.setPaymentMethod("ALIPAY");
        order.setPaymentExpireAt(paymentExpireAt);
        order.setItemAmount(itemAmount);
        order.setServiceFee(serviceFee);
        order.setTotalAmount(totalAmount);
        order.setDepositAmount(depositAmount);
        order.setExtraItemsIncluded(extraItemsSnapshot.isIncluded());
        order.setExtraItemsAmount(extraItemsSnapshot.getAmount());
        order.setExtraItemsSnapshotJson(writeJson(extraItemsSnapshot.getItems()));
        order.setChatGroupNo(buildChatGroupNo());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        insertOrder(order);
        createSellerTradeMessage(
            order,
            "账号有新订单",
            "买家 " + safeText(order.getBuyerNickname(), "用户") + " 已下单账号《" + safeText(order.getListingTitle(), "账号商品") + "》，订单号 "
                + order.getOrderNo() + "，当前等待买家支付。"
        );
        log.info("order create success orderNo={} listingNo={} buyerId={} sellerId={} totalAmount={} costMs={}",
            orderNo, listing.getListingNo(), command.getBuyerId(), listing.getSellerUserId(), totalAmount, System.currentTimeMillis() - startedAt);
        return new CreateOrderResult(orderNo, "PENDING_PAYMENT", paymentExpireAt);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public WechatPayResult createWechatPayment(Long userId, String orderNo, String notifyUrl, String preferredTradeType) {
        long startedAt = System.currentTimeMillis();
        TradeOrderDO order = requireOrderForUser(userId, orderNo);
        if (!userId.equals(order.getBuyerUserId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有买家可以发起支付");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单状态不允许发起支付");
        }
        ensurePendingPaymentNotExpired(order);
        AuthUserDO buyer = requireUser(userId);
        String tradeType = resolveWechatTradeType(preferredTradeType, buyer);
        WechatPayGateway.WechatPayResult payment = wechatPayGateway.createOrder(new WechatPayGateway.CreateOrderRequest(
            order.getOrderNo(),
            order.getListingTitle(),
            order.getTotalAmount(),
            notifyUrl,
            "TRADE",
            tradeType,
            "JSAPI".equals(tradeType) ? buyer.getOpenId() : null,
            order.getPaymentExpireAt()
        ));
        LocalDateTime now = LocalDateTime.now();
        order.setPaymentMethod("WECHAT");
        order.setPaymentTradeType(payment.getTradeType());
        order.setPaymentPrepayId(payment.getPrepayId());
        order.setPaymentCodeUrl(payment.getCodeUrl());
        order.setUpdatedAt(now);
        updateOrder(order);
        log.info("trade wechat payment prepared orderNo={} userId={} tradeType={} costMs={}",
            orderNo, userId, payment.getTradeType(), System.currentTimeMillis() - startedAt);
        return new WechatPayResult(
            order.getOrderNo(),
            "WECHAT",
            payment.getTradeType(),
            payment.getCodeUrl(),
            order.getPaymentExpireAt(),
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

    @Transactional(noRollbackFor = BusinessException.class)
    public QrPaymentResult createAlipayPayment(Long userId, String orderNo) {
        long startedAt = System.currentTimeMillis();
        TradeOrderDO order = requireOrderForUser(userId, orderNo);
        if (!userId.equals(order.getBuyerUserId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有买家可以发起支付");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单状态不允许发起支付");
        }
        ensurePendingPaymentNotExpired(order);
        LocalDateTime now = LocalDateTime.now();
        order.setPaymentMethod("ALIPAY");
        order.setPaymentTradeType("NATIVE");
        order.setPaymentCodeUrl(buildMockAlipayUrl(order));
        order.setUpdatedAt(now);
        updateOrder(order);
        log.info("trade alipay payment prepared orderNo={} userId={} costMs={}",
            orderNo, userId, System.currentTimeMillis() - startedAt);
        return new QrPaymentResult(order.getOrderNo(), "ALIPAY", "NATIVE", order.getPaymentCodeUrl(), order.getPaymentExpireAt());
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public OrderDetailResult payOrder(Long userId, String orderNo, String paymentMethod) {
        TradeOrderDO order = requireOrderForUser(userId, orderNo);
        if (!userId.equals(order.getBuyerUserId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有买家可以确认支付");
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "支付结果必须由支付平台回调确认，不能手动确认付款");
    }

    @Transactional
    public OrderCenterResult getOrderCenter(Long userId, String role, String status, String range, String startDate, String endDate) {
        long startedAt = System.currentTimeMillis();
        closeExpiredPendingPaymentOrders();
        String normalizedRole = normalizeRole(role);
        String normalizedStatus = normalizeStatus(status);
        DateRange resolvedRange = resolveRange(range, startDate, endDate);
        List<TradeOrderDO> orders = tradeOrderMapper.selectList(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq("BUY".equals(normalizedRole), TradeOrderDO::getBuyerUserId, userId)
                .eq("SELL".equals(normalizedRole), TradeOrderDO::getSellerUserId, userId)
                .isNull("BUY".equals(normalizedRole), TradeOrderDO::getBuyerDeletedAt)
                .isNull("SELL".equals(normalizedRole), TradeOrderDO::getSellerDeletedAt)
                .ge(resolvedRange.getStartAt() != null, TradeOrderDO::getCreatedAt, resolvedRange.getStartAt())
                .lt(resolvedRange.getEndAt() != null, TradeOrderDO::getCreatedAt, resolvedRange.getEndAt())
                .eq(!"ALL".equals(normalizedStatus), TradeOrderDO::getStatus, normalizedStatus)
                .orderByDesc(TradeOrderDO::getCreatedAt)
        );
        OrderCounts counts = buildCounts(orders);
        List<OrderListItem> rows = orders.stream()
            .map(order -> toListItem(order, normalizedRole, userId))
            .collect(Collectors.toList());
        log.info("order center loaded userId={} role={} status={} range={} rows={} costMs={}",
            userId, normalizedRole, normalizedStatus, resolvedRange.getLabel(), rows.size(), System.currentTimeMillis() - startedAt);
        return new OrderCenterResult(normalizedRole, normalizedStatus, resolvedRange.getLabel(), counts, rows);
    }

    @Transactional
    public OrderDetailResult getOrderDetail(Long userId, String orderNo) {
        long startedAt = System.currentTimeMillis();
        TradeOrderDO order = requireOrderForUser(userId, orderNo);
        if ("PENDING_PAYMENT".equals(order.getStatus()) && isPendingPaymentExpired(order, LocalDateTime.now())) {
            closeExpiredPendingPaymentOrder(order, LocalDateTime.now());
        }
        OrderDetailResult result = toDetail(order, userId);
        log.info("order detail loaded orderNo={} userId={} status={} costMs={}",
            orderNo, userId, order.getStatus(), System.currentTimeMillis() - startedAt);
        return result;
    }

    @Transactional
    public OrderDetailResult cancelOrder(Long userId, String orderNo) {
        TradeOrderDO order = requireOrderForUser(userId, orderNo);
        boolean isBuyer = userId.equals(order.getBuyerUserId());
        boolean isSeller = userId.equals(order.getSellerUserId());
        if (!isCancelableStatus(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单状态不允许取消");
        }
        if ("PENDING_PAYMENT".equals(order.getStatus())) {
            closeUnpaidOrder(order, userId, isSeller ? "SELLER" : "BUYER", isSeller ? "卖家取消未付款订单" : "买家取消未付款订单");
            log.info("order cancel success orderNo={} userId={} previousStatus=PENDING_PAYMENT", orderNo, userId);
            return toDetail(order, userId);
        }
        if (isSeller) {
            approveRefund(order, userId, "SELLER", "卖家取消订单并同意退款");
            log.info("seller cancel paid order refunded orderNo={} sellerUserId={}", orderNo, userId);
            return toDetail(order, userId);
        }
        if (!isBuyer) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "无权取消该订单");
        }
        if ("REFUND_PENDING".equals(order.getStatus())) {
            return toDetail(order, userId);
        }
        requestRefund(order, userId, "买家取消订单");
        log.info("buyer cancel paid order refund requested orderNo={} buyerUserId={}", orderNo, userId);
        return toDetail(order, userId);
    }

    @Transactional
    public Map<String, Object> deleteOrderForUser(Long userId, String orderNo) {
        TradeOrderDO order = requireOrderForUser(userId, orderNo);
        if (!isDeletableStatus(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有已关闭、已完成或已退款订单可以删除");
        }
        LocalDateTime now = LocalDateTime.now();
        if (userId.equals(order.getBuyerUserId())) {
            order.setBuyerDeletedAt(now);
        } else if (userId.equals(order.getSellerUserId())) {
            order.setSellerDeletedAt(now);
        } else {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "无权删除该订单");
        }
        order.setUpdatedAt(now);
        updateOrder(order);
        log.info("order hidden for user orderNo={} userId={}", orderNo, userId);
        return mapOf("orderNo", order.getOrderNo(), "deleted", true);
    }

    @Transactional
    public OrderDetailResult applyRefund(Long userId, String orderNo, String reason) {
        TradeOrderDO order = requireOrderForUser(userId, orderNo);
        if (!userId.equals(order.getBuyerUserId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有买家可以申请退款");
        }
        if (!Arrays.asList("WAITING_TRADE", "IN_PROGRESS").contains(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单状态不允许申请退款");
        }
        requestRefund(order, userId, reason);
        log.info("order refund requested orderNo={} userId={} amount={}", orderNo, userId, order.getRefundAmount());
        return toDetail(order, userId);
    }

    @Transactional
    public OrderDetailResult reviewRefund(Long sellerUserId, String orderNo, String action, String note) {
        TradeOrderDO order = requireOrderForUser(sellerUserId, orderNo);
        if (!sellerUserId.equals(order.getSellerUserId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有卖家可以审核退款");
        }
        String normalizedAction = normalizeRefundAction(action);
        String normalizedNote = normalizeRequiredText(note, "退款审核备注不能为空", 500);
        if ("APPROVE".equals(normalizedAction)) {
            approveRefund(order, sellerUserId, "SELLER", normalizedNote);
            log.info("order refund approved orderNo={} sellerUserId={}", orderNo, sellerUserId);
            return toDetail(order, sellerUserId);
        }
        rejectRefund(order, sellerUserId, "SELLER", normalizedNote);
        log.info("order refund rejected orderNo={} sellerUserId={}", orderNo, sellerUserId);
        return toDetail(order, sellerUserId);
    }

    @Transactional
    public Map<String, Object> forceRefund(Long operatorUserId, String orderNo, String reason) {
        TradeOrderDO order = requireOrderByNo(orderNo);
        String normalizedReason = normalizeRequiredText(reason, "强制退款原因不能为空", 500);
        approveRefund(order, operatorUserId, "ADMIN", normalizedReason);
        log.info("order force refund success orderNo={} operatorUserId={}", orderNo, operatorUserId);
        return mapOf(
            "orderNo", order.getOrderNo(),
            "status", order.getStatus(),
            "statusText", renderStatus(order.getStatus()),
            "refundAmount", order.getRefundAmount(),
            "message", "强制退款已完成，款项已退回买家钱包"
        );
    }

    @Transactional
    public OrderDetailResult confirmComplete(Long userId, String orderNo) {
        TradeOrderDO order = requireOrderForUser(userId, orderNo);
        boolean isBuyer = userId.equals(order.getBuyerUserId());
        boolean isSeller = userId.equals(order.getSellerUserId());
        if (!isBuyer && !isSeller) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "无权确认完成该订单");
        }
        if (!"WAITING_TRADE".equals(order.getStatus()) && !"IN_PROGRESS".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单状态不允许确认完成");
        }
        LocalDateTime now = LocalDateTime.now();
        if (isBuyer && order.getBuyerConfirmedAt() == null) {
            order.setBuyerConfirmedAt(now);
        }
        if (isSeller && order.getSellerConfirmedAt() == null) {
            order.setSellerConfirmedAt(now);
        }
        boolean bothConfirmed = order.getBuyerConfirmedAt() != null && order.getSellerConfirmedAt() != null;
        if (bothConfirmed) {
            order.setStatus("COMPLETED");
            order.setCompletedAt(now);
        } else if ("WAITING_TRADE".equals(order.getStatus())) {
            order.setStatus("IN_PROGRESS");
        }
        order.setUpdatedAt(now);
        updateOrder(order);
        if (bothConfirmed) {
            settleSellerIncome(order);
            refundDepositToBuyer(order);
            distributionService.onTradeOrderCompleted(order);
        }
        log.info("order confirm complete success orderNo={} userId={} buyerConfirmed={} sellerConfirmed={} completed={}",
            orderNo, userId, order.getBuyerConfirmedAt() != null, order.getSellerConfirmedAt() != null, bothConfirmed);
        return toDetail(order, userId);
    }

    @Transactional
    public OrderDetailResult applyAfterSale(Long userId, String orderNo) {
        TradeOrderDO order = requireOrderForUser(userId, orderNo);
        if (!userId.equals(order.getBuyerUserId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "只有买家可以申请售后");
        }
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅已完成订单可申请售后");
        }
        if (order.getCompletedAt() == null || order.getCompletedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "售后申请窗口已关闭");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus("AFTER_SALE");
        order.setAfterSaleAt(now);
        order.setUpdatedAt(now);
        updateOrder(order);
        log.info("order after-sale success orderNo={} userId={} afterSaleAt={}", orderNo, userId, now);
        return toDetail(order, userId);
    }

    public CertificateResult getCertificate(Long userId, String orderNo) {
        TradeOrderDO order = requireOrderForUser(userId, orderNo);
        StringBuilder builder = new StringBuilder();
        builder.append("订单凭证\n");
        builder.append("订单号：").append(order.getOrderNo()).append('\n');
        builder.append("商品名称：").append(order.getListingTitle()).append('\n');
        builder.append("订单状态：").append(renderStatus(order.getStatus())).append('\n');
        builder.append("创建时间：").append(formatTime(order.getCreatedAt())).append('\n');
        builder.append("支付时间：").append(formatTime(order.getPaidAt())).append('\n');
        builder.append("完成时间：").append(formatTime(order.getCompletedAt())).append('\n');
        builder.append("支付方式：").append(renderPaymentMethod(order.getPaymentMethod())).append('\n');
        builder.append("商品金额：").append(formatAmount(order.getItemAmount())).append('\n');
        builder.append("平台服务费：").append(formatAmount(order.getServiceFee())).append('\n');
        builder.append("订单总金额：").append(formatAmount(order.getTotalAmount())).append('\n');
        builder.append("买家昵称：").append(order.getBuyerNickname()).append('\n');
        builder.append("卖家昵称：").append(order.getSellerNickname()).append('\n');
        builder.append("卖家类型：").append(renderSellerType(order.getSellerType())).append('\n');
        builder.append("交易群聊：").append(order.getChatGroupNo()).append('\n');
        builder.append("备注：当前凭证为测试环境文本凭证，支付截图与聊天记录将在 IM / 支付模块接入后补齐。\n");
        log.info("order certificate generated orderNo={} userId={}", orderNo, userId);
        return new CertificateResult(orderNo + "-certificate.txt", builder.toString());
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public void applyWechatPaymentSuccess(String orderNo, String transactionId, LocalDateTime paidAt) {
        TradeOrderDO order = tradeOrderMapper.selectOne(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getOrderNo, orderNo)
                .last("LIMIT 1")
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号交易订单不存在");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            log.info("trade wechat payment ignored orderNo={} currentStatus={} transactionId={}",
                orderNo, order.getStatus(), transactionId);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectivePaidAt = paidAt == null ? now : paidAt;
        if (isPendingPaymentExpired(order, effectivePaidAt)) {
            closeExpiredPendingPaymentOrder(order, now);
            log.warn("trade wechat payment ignored expired orderNo={} transactionId={} paidAt={} expireAt={}",
                orderNo, transactionId, effectivePaidAt, resolvePaymentExpireAt(order));
            return;
        }
	        offlineListingAfterPayment(order.getListingNo(), now);
	        order.setStatus("WAITING_TRADE");
	        order.setPaymentMethod("WECHAT");
        order.setPaymentTransactionId(transactionId);
        order.setPaymentNotifiedAt(now);
	        order.setPaidAt(effectivePaidAt);
	        order.setUpdatedAt(now);
	        updateOrder(order);
	        imService.ensureTradeConversation(order.getChatGroupNo());
	        distributionService.onTradeOrderPaid(order);
	        createSellerTradeMessage(
	            order,
	            "账号售出成功",
	            "账号已售出，请进入群聊进行交接。订单 " + order.getOrderNo()
        );
        log.info("trade wechat payment success orderNo={} transactionId={} paidAt={}",
            orderNo, transactionId, order.getPaidAt());
    }

    @Scheduled(fixedDelay = 60000L, initialDelay = 60000L)
    @Transactional
    public void closeExpiredPendingPaymentOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<TradeOrderDO> expiredOrders = tradeOrderMapper.selectList(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getStatus, "PENDING_PAYMENT")
                .and(wrapper -> wrapper
                    .le(TradeOrderDO::getPaymentExpireAt, now)
                    .or(inner -> inner.isNull(TradeOrderDO::getPaymentExpireAt).le(TradeOrderDO::getCreatedAt, now.minusMinutes(TRADE_PAYMENT_TIMEOUT_MINUTES)))
                )
                .last("LIMIT 100")
        );
        for (TradeOrderDO order : expiredOrders) {
            closeExpiredPendingPaymentOrder(order, now);
        }
        if (!expiredOrders.isEmpty()) {
            log.info("expired trade orders closed count={} costAt={}", expiredOrders.size(), now);
        }
    }

    public List<OrderSummary> listMyOrders(Long userId) {
        List<OrderListItem> rows = getOrderCenter(userId, "BUY", "ALL", "ALL", null, null).getRows();
        return rows.stream()
            .map(item -> new OrderSummary(item.getOrderNo(), item.getStatusLabel(), item.getTotalAmount()))
            .collect(Collectors.toList());
    }

    private OrderDetailResult toDetail(TradeOrderDO order, Long userId) {
        String role = userId.equals(order.getBuyerUserId()) ? "BUY" : "SELL";
        boolean canCancel = isCancelableStatus(order.getStatus())
            && (userId.equals(order.getBuyerUserId()) || userId.equals(order.getSellerUserId()));
        boolean canApplyRefund = userId.equals(order.getBuyerUserId()) && Arrays.asList("WAITING_TRADE", "IN_PROGRESS").contains(order.getStatus());
        boolean canReviewRefund = userId.equals(order.getSellerUserId()) && "REFUND_PENDING".equals(order.getStatus());
        boolean canDelete = userId.equals(order.getBuyerUserId()) || userId.equals(order.getSellerUserId())
            ? isDeletableStatus(order.getStatus())
            : false;
        boolean canApplyAfterSale = userId.equals(order.getBuyerUserId())
            && "COMPLETED".equals(order.getStatus())
            && order.getCompletedAt() != null
            && !order.getCompletedAt().plusHours(24).isBefore(LocalDateTime.now());
        boolean canConfirmComplete = Arrays.asList("WAITING_TRADE", "IN_PROGRESS").contains(order.getStatus())
            && ((userId.equals(order.getBuyerUserId()) && order.getBuyerConfirmedAt() == null)
                || (userId.equals(order.getSellerUserId()) && order.getSellerConfirmedAt() == null));
        boolean canEnterChat = !"PENDING_PAYMENT".equals(order.getStatus()) && !"CLOSED".equals(order.getStatus()) && !"REFUNDED".equals(order.getStatus());

        return new OrderDetailResult(
            order.getOrderNo(),
            role,
            order.getListingNo(),
            order.getListingTitle(),
            order.getListingSummary(),
            previewNullable(order.getListingCoverKey()),
            renderStatus(order.getStatus()),
            order.getStatus(),
            formatTime(order.getCreatedAt()),
            formatTime(order.getPaidAt()),
            formatTime(order.getCompletedAt()),
            renderPaymentMethod(order.getPaymentMethod()),
            order.getItemAmount(),
            order.getServiceFee(),
            order.getTotalAmount(),
            safeAmount(order.getDepositAmount()),
            order.getBuyerConfirmedAt() != null,
            order.getSellerConfirmedAt() != null,
            order.getBuyerNickname(),
            order.getSellerNickname(),
            renderSellerType(order.getSellerType()),
            order.getSellerDisplayName(),
            canCancel,
            canConfirmComplete,
            canApplyAfterSale,
            canApplyRefund,
            canReviewRefund,
            canDelete,
            canEnterChat,
            canEnterChat ? order.getChatGroupNo() : "",
            order.getPaymentExpireAt(),
            "平台审核通过后生成订单，交易完成后 24 小时内可申请售后。",
            "若发现辱骂、私下交易或虚假交付，平台将保留取消订单和封禁账号的权利。",
            buildProgress(order)
        );
    }

    private OrderListItem toListItem(TradeOrderDO order, String role, Long userId) {
        boolean canCancel = isCancelableStatus(order.getStatus())
            && (userId.equals(order.getBuyerUserId()) || userId.equals(order.getSellerUserId()));
        boolean canApplyRefund = userId.equals(order.getBuyerUserId()) && Arrays.asList("WAITING_TRADE", "IN_PROGRESS").contains(order.getStatus());
        boolean canReviewRefund = userId.equals(order.getSellerUserId()) && "REFUND_PENDING".equals(order.getStatus());
        boolean canDelete = (userId.equals(order.getBuyerUserId()) || userId.equals(order.getSellerUserId()))
            && isDeletableStatus(order.getStatus());
        boolean canAfterSale = userId.equals(order.getBuyerUserId())
            && "COMPLETED".equals(order.getStatus())
            && order.getCompletedAt() != null
            && !order.getCompletedAt().plusHours(24).isBefore(LocalDateTime.now());
        boolean canConfirmComplete = Arrays.asList("WAITING_TRADE", "IN_PROGRESS").contains(order.getStatus())
            && ((userId.equals(order.getBuyerUserId()) && order.getBuyerConfirmedAt() == null)
                || (userId.equals(order.getSellerUserId()) && order.getSellerConfirmedAt() == null));
        boolean canEnterChat = !"PENDING_PAYMENT".equals(order.getStatus()) && !"CLOSED".equals(order.getStatus()) && !"REFUNDED".equals(order.getStatus());
        return new OrderListItem(
            order.getOrderNo(),
            order.getListingTitle(),
            order.getListingSummary(),
            previewNullable(order.getListingCoverKey()),
            order.getTotalAmount(),
            formatTime(order.getCreatedAt()),
            renderStatus(order.getStatus()),
            order.getStatus(),
            order.getBuyerNickname(),
            order.getSellerNickname(),
            renderSellerType(order.getSellerType()),
            order.getSellerDisplayName(),
            order.getChatGroupNo(),
            canCancel,
            canConfirmComplete,
            canAfterSale,
            canApplyRefund,
            canReviewRefund,
            canEnterChat,
            canDelete,
            order.getPaymentExpireAt(),
            true
        );
    }

    private OrderCounts buildCounts(List<TradeOrderDO> orders) {
        Map<String, Long> grouped = orders.stream().collect(Collectors.groupingBy(TradeOrderDO::getStatus, LinkedHashMap::new, Collectors.counting()));
        return new OrderCounts(
            orders.size(),
            grouped.getOrDefault("PENDING_PAYMENT", 0L).intValue(),
            grouped.getOrDefault("WAITING_TRADE", 0L).intValue(),
            grouped.getOrDefault("IN_PROGRESS", 0L).intValue(),
            grouped.getOrDefault("COMPLETED", 0L).intValue(),
            grouped.getOrDefault("REFUND_PENDING", 0L).intValue(),
            grouped.getOrDefault("AFTER_SALE", 0L).intValue(),
            grouped.getOrDefault("REFUNDED", 0L).intValue(),
            grouped.getOrDefault("CLOSED", 0L).intValue()
        );
    }

    private List<ProgressStep> buildProgress(TradeOrderDO order) {
        int currentStep = resolveCurrentProgressStep(order.getStatus());
        List<ProgressStep> steps = new ArrayList<>();
        steps.add(new ProgressStep("待付款", "买家提交订单，等待完成支付", currentStep > 1, currentStep == 1));
        steps.add(new ProgressStep("已支付", "支付成功，平台已锁定账号资源，押金由平台托管", currentStep > 2, currentStep == 2));
        steps.add(new ProgressStep("双方确认中", buildConfirmProgressDescription(order), currentStep > 3, currentStep == 3));
        steps.add(new ProgressStep("交易完成", "双方确认完成后，租金结算给号主，押金退还给付款方", currentStep >= 4, currentStep == 4));
        if ("AFTER_SALE".equals(order.getStatus())) {
            steps.add(new ProgressStep("售后处理中", "订单已转入售后流程，等待平台处理", true, true));
        }
        if ("REFUND_PENDING".equals(order.getStatus())) {
            steps.add(new ProgressStep("退款审核中", "买家已申请退款，等待卖家审核", true, true));
        }
        if ("REFUNDED".equals(order.getStatus())) {
            steps.add(new ProgressStep("已退款", "订单已退款到买家站内钱包", true, true));
        }
        if ("CLOSED".equals(order.getStatus())) {
            steps.add(new ProgressStep("订单已关闭", "订单已取消或超时关闭，不再支持群聊与交付", true, true));
        }
        return steps;
    }

    private String buildConfirmProgressDescription(TradeOrderDO order) {
        boolean buyerConfirmed = order.getBuyerConfirmedAt() != null;
        boolean sellerConfirmed = order.getSellerConfirmedAt() != null;
        if (buyerConfirmed && sellerConfirmed) {
            return "买家和卖家均已确认完成，平台正在结算租金并退还押金";
        }
        if (buyerConfirmed) {
            return "买家已确认完成，等待卖家确认账号安全无异常";
        }
        if (sellerConfirmed) {
            return "卖家已确认账号安全无异常，等待买家确认完成";
        }
        return "买家打完后确认完成，卖家核验账号安全后确认完成";
    }

    private int resolveCurrentProgressStep(String status) {
        if ("PENDING_PAYMENT".equals(status)) {
            return 1;
        }
        if ("WAITING_TRADE".equals(status)) {
            return 2;
        }
        if ("IN_PROGRESS".equals(status)) {
            return 3;
        }
        return 4;
    }

    private DateRange resolveRange(String range, String startDate, String endDate) {
        String normalized = range == null || range.trim().isEmpty() ? "D7" : range.trim().toUpperCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        if ("ALL".equals(normalized)) {
            return new DateRange("ALL", null, null);
        }
        if ("D30".equals(normalized)) {
            return new DateRange("D30", now.minusDays(30), now.plusSeconds(1));
        }
        if ("CUSTOM".equals(normalized)) {
            if (startDate == null || endDate == null || startDate.trim().isEmpty() || endDate.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "自定义时间范围不能为空");
            }
            LocalDate start = LocalDate.parse(startDate.trim());
            LocalDate end = LocalDate.parse(endDate.trim());
            if (end.isBefore(start)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "结束日期不能早于开始日期");
            }
            return new DateRange("CUSTOM", start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        }
        return new DateRange("D7", now.minusDays(7), now.plusSeconds(1));
    }

    private String normalizeRole(String role) {
        if ("SELL".equalsIgnoreCase(role)) {
            return "SELL";
        }
        return "BUY";
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "ALL";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (ORDER_STATUS_SEQUENCE.contains(normalized) || "ALL".equals(normalized)) {
            return normalized;
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单状态不支持");
    }

    private TradeOrderDO requireOrderForUser(Long userId, String orderNo) {
        TradeOrderDO order = tradeOrderMapper.selectOne(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getOrderNo, orderNo)
                .and(wrapper -> wrapper
                    .and(buyer -> buyer.eq(TradeOrderDO::getBuyerUserId, userId).isNull(TradeOrderDO::getBuyerDeletedAt))
                    .or(seller -> seller.eq(TradeOrderDO::getSellerUserId, userId).isNull(TradeOrderDO::getSellerDeletedAt))
                )
                .last("LIMIT 1")
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单不存在");
        }
        return order;
    }

    private TradeOrderDO requireOrderByNo(String orderNo) {
        TradeOrderDO order = tradeOrderMapper.selectOne(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getOrderNo, orderNo)
                .last("LIMIT 1")
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单不存在");
        }
        return order;
    }

    private void approveRefund(TradeOrderDO order, Long operatorUserId, String operatorRole, String note) {
        if (!Arrays.asList("WAITING_TRADE", "IN_PROGRESS", "REFUND_PENDING", "AFTER_SALE").contains(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前订单状态不允许退款");
        }
        LocalDateTime now = LocalDateTime.now();
        BigDecimal refundAmount = resolveRefundAmount(order);
        order.setStatus("REFUNDED");
        if (order.getRefundRequestedAt() == null) {
            order.setRefundRequestedAt(now);
        }
        order.setRefundReviewedAt(now);
        order.setRefundedAt(now);
        order.setRefundAmount(refundAmount);
        order.setRefundReviewNote(note);
        order.setRefundOperatorUserId(operatorUserId);
        order.setRefundOperatorRole(operatorRole);
        order.setClosedAt(now);
        order.setUpdatedAt(now);
        updateOrder(order);
        creditBuyerRefund(order, refundAmount, note);
        releaseListingIfNoActiveTrade(order.getListingNo(), now);
        distributionService.onTradeOrderClosed(order);
        createUserMessage(
            order.getBuyerUserId(),
            "TRADE",
            "订单退款已完成",
            "订单 " + order.getOrderNo() + " 已退款 " + formatAmount(refundAmount) + " 至你的站内钱包。"
        );
        createSellerTradeMessage(
            order,
            "订单退款已完成",
            "订单 " + order.getOrderNo() + " 已完成退款，退款金额 " + formatAmount(refundAmount) + "。"
        );
    }

    private void rejectRefund(TradeOrderDO order, Long operatorUserId, String operatorRole, String note) {
        if (!"REFUND_PENDING".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅退款审核中的订单可拒绝退款");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus("AFTER_SALE");
        order.setAfterSaleAt(now);
        order.setAfterSaleNote("卖家拒绝退款：" + note);
        order.setRefundReviewedAt(now);
        order.setRefundReviewNote(note);
        order.setRefundOperatorUserId(operatorUserId);
        order.setRefundOperatorRole(operatorRole);
        order.setUpdatedAt(now);
        updateOrder(order);
        createUserMessage(
            order.getBuyerUserId(),
            "TRADE",
            "退款申请已转入售后",
            "订单 " + order.getOrderNo() + " 的退款申请被卖家拒绝，已转入售后处理中。处理备注：" + note
        );
    }

    private void creditBuyerRefund(TradeOrderDO order, BigDecimal refundAmount, String remark) {
        Long existing = walletTransactionMapper.selectCount(
            Wrappers.<WalletTransactionDO>lambdaQuery()
                .eq(WalletTransactionDO::getBizType, "TRADE_REFUND")
                .eq(WalletTransactionDO::getRelatedNo, order.getOrderNo())
        );
        if (existing != null && existing > 0) {
            log.info("trade refund wallet credit skipped duplicate orderNo={}", order.getOrderNo());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        UserWalletDO wallet = ensureWallet(order.getBuyerUserId());
        wallet.setAvailableBalance(safeAmount(wallet.getAvailableBalance()).add(refundAmount));
        wallet.setUpdatedAt(now);
        userWalletMapper.updateById(wallet);

        WalletTransactionDO transaction = new WalletTransactionDO();
        transaction.setTransactionNo(buildWalletTransactionNo());
        transaction.setUserId(order.getBuyerUserId());
        transaction.setBizType("TRADE_REFUND");
        transaction.setTitle("订单退款");
        transaction.setAmount(refundAmount);
        transaction.setDirection("INCOME");
        transaction.setChannel("TRADE");
        transaction.setStatus("SUCCESS");
        transaction.setRelatedNo(order.getOrderNo());
        transaction.setRemark(normalizeOptionalText(remark, 500));
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);
        walletTransactionMapper.insert(transaction);
        log.info("trade refund wallet credited orderNo={} buyerId={} amount={}", order.getOrderNo(), order.getBuyerUserId(), refundAmount);
    }

    private void requestRefund(TradeOrderDO order, Long buyerUserId, String reason) {
        LocalDateTime now = LocalDateTime.now();
        order.setStatus("REFUND_PENDING");
        order.setRefundRequestedAt(now);
        order.setRefundAmount(resolveRefundAmount(order));
        order.setRefundReason(normalizeOptionalText(reason, 500));
        order.setUpdatedAt(now);
        updateOrder(order);
        createSellerTradeMessage(
            order,
            "账号订单退款申请",
            "买家 " + safeText(order.getBuyerNickname(), "用户") + " 已申请订单 " + order.getOrderNo()
                + " 退款，请在订单中心审核。"
        );
    }

    private void closeUnpaidOrder(TradeOrderDO order, Long operatorUserId, String operatorRole, String reason) {
        LocalDateTime now = LocalDateTime.now();
        order.setStatus("CLOSED");
        order.setClosedAt(now);
        order.setRefundReviewNote(reason);
        order.setRefundOperatorUserId(operatorUserId);
        order.setRefundOperatorRole(operatorRole);
        order.setUpdatedAt(now);
        updateOrder(order);
        releaseListingIfNoActiveTrade(order.getListingNo(), now);
        distributionService.onTradeOrderClosed(order);
        if ("SELLER".equals(operatorRole)) {
            createUserMessage(
                order.getBuyerUserId(),
                "TRADE",
                "订单已取消",
                "订单 " + order.getOrderNo() + " 已由卖家取消，未付款订单不会产生退款。"
            );
        } else {
            createSellerTradeMessage(
                order,
                "订单已取消",
                "订单 " + order.getOrderNo() + " 已由买家取消。"
            );
        }
    }

    private boolean isCancelableStatus(String status) {
        return Arrays.asList("PENDING_PAYMENT", "WAITING_TRADE", "IN_PROGRESS", "REFUND_PENDING", "AFTER_SALE").contains(status);
    }

    private boolean isDeletableStatus(String status) {
        return Arrays.asList("CLOSED", "COMPLETED", "REFUNDED").contains(status);
    }

	    private void ensurePendingPaymentNotExpired(TradeOrderDO order) {
        if (!isPendingPaymentExpired(order, LocalDateTime.now())) {
            return;
        }
        closeExpiredPendingPaymentOrder(order, LocalDateTime.now());
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单已超时关闭，请重新下单");
    }

    private boolean isPendingPaymentExpired(TradeOrderDO order, LocalDateTime referenceAt) {
        LocalDateTime expireAt = resolvePaymentExpireAt(order);
        return expireAt != null && (referenceAt == null || !expireAt.isAfter(referenceAt));
    }

    private LocalDateTime resolvePaymentExpireAt(TradeOrderDO order) {
        if (order.getPaymentExpireAt() != null) {
            return order.getPaymentExpireAt();
        }
        return order.getCreatedAt() == null ? null : order.getCreatedAt().plusMinutes(TRADE_PAYMENT_TIMEOUT_MINUTES);
    }

    private void closeExpiredPendingPaymentOrder(TradeOrderDO order, LocalDateTime now) {
        int rows = tradeOrderMapper.update(
            null,
            Wrappers.<TradeOrderDO>lambdaUpdate()
                .eq(TradeOrderDO::getId, order.getId())
                .eq(TradeOrderDO::getStatus, "PENDING_PAYMENT")
                .set(TradeOrderDO::getStatus, "CLOSED")
                .set(TradeOrderDO::getClosedAt, now)
                .set(TradeOrderDO::getUpdatedAt, now)
        );
        if (rows != 1) {
            log.info("expired trade order close skipped orderNo={} status={}", order.getOrderNo(), order.getStatus());
            return;
        }
        order.setStatus("CLOSED");
        order.setClosedAt(now);
        order.setUpdatedAt(now);
	        distributionService.onTradeOrderClosed(order);
	        log.info("expired trade order closed orderNo={} listingNo={}", order.getOrderNo(), order.getListingNo());
    }

    private void releaseListingIfNoActiveTrade(String listingNo, LocalDateTime now) {
        if (listingNo == null || listingNo.trim().isEmpty()) {
            return;
        }
        Long activeCount = tradeOrderMapper.selectCount(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getListingNo, listingNo)
                .in(TradeOrderDO::getStatus, Arrays.asList("PENDING_PAYMENT", "WAITING_TRADE", "IN_PROGRESS", "COMPLETED", "REFUND_PENDING", "AFTER_SALE"))
        );
        if (activeCount != null && activeCount > 0) {
            log.info("listing release skipped listingNo={} activeTradeCount={}", listingNo, activeCount);
            return;
        }
        int rows = accountListingMapper.update(
            null,
            Wrappers.<AccountListingDO>lambdaUpdate()
                .eq(AccountListingDO::getListingNo, listingNo)
                .eq(AccountListingDO::getStatus, "OFFLINE")
                .set(AccountListingDO::getStatus, "PUBLISHED")
                .set(AccountListingDO::getPublishedAt, now)
                .set(AccountListingDO::getUpdatedAt, now)
        );
        log.info("listing released after unpaid order closed listingNo={} rows={}", listingNo, rows);
    }

    private void offlineListingAfterPayment(String listingNo, LocalDateTime now) {
        if (listingNo == null || listingNo.trim().isEmpty()) {
            return;
        }
	        int rows = accountListingMapper.update(
	            null,
	            Wrappers.<AccountListingDO>lambdaUpdate()
	                .eq(AccountListingDO::getListingNo, listingNo)
	                .eq(AccountListingDO::getStatus, "PUBLISHED")
	                .set(AccountListingDO::getStatus, "OFFLINE")
	                .set(AccountListingDO::getUpdatedAt, now)
	        );
	        if (rows != 1) {
	            log.warn("listing offline after payment rejected listingNo={} rows={}", listingNo, rows);
	            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号已被锁定或下架，请重新选择账号");
	        }
	        log.info("listing offline after payment listingNo={} rows={}", listingNo, rows);
	    }

    private AccountListingDO requireListing(String listingId) {
        AccountListingDO byNo = accountListingMapper.selectOne(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getListingNo, listingId)
                .last("LIMIT 1")
        );
        if (byNo != null) {
            return byNo;
        }
        try {
            long numericId = Long.parseLong(listingId);
            AccountListingDO byId = accountListingMapper.selectById(numericId);
            if (byId != null) {
                return byId;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号商品不存在");
    }

    private BigDecimal calculateRentAmount(AccountListingDO listing) {
        BigDecimal rent = listing.getPrice() == null ? BigDecimal.ZERO : listing.getPrice();
        return rent.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDepositAmount(AccountListingDO listing) {
        BigDecimal deposit = readPublishAttributes(listing).getDeposit();
        if (deposit == null) {
            deposit = BigDecimal.ZERO;
        }
        return deposit.setScale(2, RoundingMode.HALF_UP);
    }

    private OrderExtraItemsSnapshot buildExtraItemsSnapshot(AccountListingDO listing, boolean includeExtraItems) {
        if (!includeExtraItems) {
            return new OrderExtraItemsSnapshot(false, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), Collections.<OrderExtraItemLine>emptyList());
        }
        List<OrderExtraItemLine> lines = new ArrayList<OrderExtraItemLine>();
        BigDecimal amount = BigDecimal.ZERO;
        for (PublishExtraItemSnapshot item : readPublishAttributes(listing).getExtraItems()) {
            int count = item.getCount() == null ? 0 : item.getCount();
            if (count <= 0 || !"charge".equals(item.getChargeMode())) {
                continue;
            }
            BigDecimal unitPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
            BigDecimal subtotal = item.getTotalPrice() == null
                ? unitPrice.multiply(new BigDecimal(count)).setScale(2, RoundingMode.HALF_UP)
                : item.getTotalPrice().setScale(2, RoundingMode.HALF_UP);
            amount = amount.add(subtotal);
            lines.add(new OrderExtraItemLine(item.getCode(), item.getLabel(), count, unitPrice.setScale(2, RoundingMode.HALF_UP), subtotal));
        }
        return new OrderExtraItemsSnapshot(!lines.isEmpty(), amount.setScale(2, RoundingMode.HALF_UP), lines);
    }

    private PublishAttributesSnapshot readPublishAttributes(AccountListingDO listing) {
        if (listing == null || listing.getPublishAttributesJson() == null || listing.getPublishAttributesJson().trim().isEmpty()) {
            return new PublishAttributesSnapshot();
        }
        try {
            return objectMapper.readValue(listing.getPublishAttributesJson(), PublishAttributesSnapshot.class);
        } catch (Exception exception) {
            log.warn("read publish attributes failed listingNo={}", listing.getListingNo(), exception);
            return new PublishAttributesSnapshot();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Collections.emptyList() : value);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单额外物资快照生成失败");
        }
    }

    private AuthUserDO requireUser(Long userId) {
        AuthUserDO user = authUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "用户不存在");
        }
        return user;
    }

    private void insertOrder(TradeOrderDO order) {
        long startedAt = System.currentTimeMillis();
        int rows = tradeOrderMapper.insert(order);
        log.info("mysql insert success target=trade_order costMs={} rows={} orderNo={} buyerId={} sellerId={}",
            System.currentTimeMillis() - startedAt, rows, order.getOrderNo(), order.getBuyerUserId(), order.getSellerUserId());
    }

    private void updateOrder(TradeOrderDO order) {
        long startedAt = System.currentTimeMillis();
        int rows = tradeOrderMapper.updateById(order);
        log.info("mysql update success target=trade_order costMs={} rows={} orderNo={} status={}",
            System.currentTimeMillis() - startedAt, rows, order.getOrderNo(), order.getStatus());
    }

    private BigDecimal resolveRefundAmount(TradeOrderDO order) {
        BigDecimal amount = order.getRefundAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            amount = safeAmount(order.getTotalAmount());
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeRefundAction(String action) {
        String normalized = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("APPROVE", "REJECT").contains(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "退款审核动作不支持");
        }
        return normalized;
    }

    private String normalizeRequiredText(String value, String message, int maxLength) {
        String normalized = normalizeOptionalText(value, maxLength);
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private void createUserMessage(Long userId, String category, String title, String content) {
        if (userId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        UserMessageDO message = new UserMessageDO();
        message.setUserId(userId);
        message.setCategory(category);
        message.setTitle(title);
        message.setContent(content);
        message.setReadFlag(false);
        message.setDeletedFlag(false);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        userMessageMapper.insert(message);
    }

    private void settleSellerIncome(TradeOrderDO order) {
        if (!"PERSONAL".equalsIgnoreCase(order.getSellerType())) {
            log.info("seller income settlement skipped orderNo={} sellerType={}", order.getOrderNo(), order.getSellerType());
            return;
        }
        if (walletTransactionMapper.selectCount(
            Wrappers.<WalletTransactionDO>lambdaQuery()
                .eq(WalletTransactionDO::getBizType, "TRADE_SELLER_INCOME")
                .eq(WalletTransactionDO::getRelatedNo, order.getOrderNo())
        ) > 0) {
            log.info("seller income settlement skipped duplicate orderNo={}", order.getOrderNo());
            return;
        }
        BigDecimal rate = readPersonalSellerCommissionRate();
	        BigDecimal baseAmount = safeAmount(order.getItemAmount());
        BigDecimal commissionAmount = baseAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal incomeAmount = baseAmount.subtract(commissionAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        UserWalletDO wallet = ensureWallet(order.getSellerUserId());
        wallet.setAvailableBalance(safeAmount(wallet.getAvailableBalance()).add(incomeAmount));
        wallet.setUpdatedAt(LocalDateTime.now());
        userWalletMapper.updateById(wallet);

        WalletTransactionDO transaction = new WalletTransactionDO();
        transaction.setTransactionNo(buildWalletTransactionNo());
        transaction.setUserId(order.getSellerUserId());
        transaction.setBizType("TRADE_SELLER_INCOME");
        transaction.setTitle("账号交易收入");
        transaction.setAmount(incomeAmount);
        transaction.setDirection("INCOME");
        transaction.setChannel("TRADE");
        transaction.setStatus("SUCCESS");
        transaction.setRelatedNo(order.getOrderNo());
        transaction.setRemark("平台抽成 " + renderPercent(rate) + "，扣除 " + commissionAmount.stripTrailingZeros().toPlainString() + " 元");
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        walletTransactionMapper.insert(transaction);

        UserMessageDO message = new UserMessageDO();
        message.setUserId(order.getSellerUserId());
        message.setCategory("TRADE");
        message.setTitle("账号交易收入已到账");
        message.setContent("订单 " + order.getOrderNo() + " 已完成，收入 "
            + incomeAmount.stripTrailingZeros().toPlainString() + " 元已入账，平台抽成 "
            + renderPercent(rate) + "。");
        message.setReadFlag(false);
        message.setDeletedFlag(false);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        userMessageMapper.insert(message);
        log.info("seller income settled orderNo={} sellerId={} base={} rate={} income={} commission={}",
            order.getOrderNo(), order.getSellerUserId(), baseAmount, rate, incomeAmount, commissionAmount);
    }

    private void refundDepositToBuyer(TradeOrderDO order) {
        BigDecimal depositAmount = safeAmount(order.getDepositAmount());
        if (depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (walletTransactionMapper.selectCount(
            Wrappers.<WalletTransactionDO>lambdaQuery()
                .eq(WalletTransactionDO::getBizType, "TRADE_DEPOSIT_REFUND")
                .eq(WalletTransactionDO::getRelatedNo, order.getOrderNo())
        ) > 0) {
            log.info("deposit refund skipped duplicate orderNo={}", order.getOrderNo());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        UserWalletDO wallet = ensureWallet(order.getBuyerUserId());
        wallet.setAvailableBalance(safeAmount(wallet.getAvailableBalance()).add(depositAmount));
        wallet.setUpdatedAt(now);
        userWalletMapper.updateById(wallet);

        WalletTransactionDO transaction = new WalletTransactionDO();
        transaction.setTransactionNo(buildWalletTransactionNo());
        transaction.setUserId(order.getBuyerUserId());
        transaction.setBizType("TRADE_DEPOSIT_REFUND");
        transaction.setTitle("订单押金退还");
        transaction.setAmount(depositAmount);
        transaction.setDirection("INCOME");
        transaction.setChannel("TRADE");
        transaction.setStatus("SUCCESS");
        transaction.setRelatedNo(order.getOrderNo());
        transaction.setRemark("双方确认完成后，平台退还托管押金");
        transaction.setCreatedAt(now);
        transaction.setUpdatedAt(now);
        walletTransactionMapper.insert(transaction);
        createUserMessage(
            order.getBuyerUserId(),
            "TRADE",
            "订单押金已退还",
            "订单 " + order.getOrderNo() + " 双方已确认完成，押金 "
                + depositAmount.stripTrailingZeros().toPlainString() + " 元已退回你的钱包。"
        );
        log.info("deposit refunded orderNo={} buyerId={} amount={}", order.getOrderNo(), order.getBuyerUserId(), depositAmount);
    }

    private void createSellerTradeMessage(TradeOrderDO order, String title, String content) {
        if (order == null || order.getSellerUserId() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        UserMessageDO message = new UserMessageDO();
        message.setUserId(order.getSellerUserId());
        message.setCategory("TRADE");
        message.setTitle(title);
        message.setContent(content);
        message.setReadFlag(false);
        message.setDeletedFlag(false);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        userMessageMapper.insert(message);
    }

    private BigDecimal readPersonalSellerCommissionRate() {
        Object value = adminIntegrationConfigService.loadListingPublishConfig().get("personalSellerCommissionRate");
        try {
            BigDecimal rate = new BigDecimal(String.valueOf(value));
            if (rate.compareTo(BigDecimal.ZERO) >= 0 && rate.compareTo(BigDecimal.ONE) <= 0) {
                return rate.setScale(4, RoundingMode.HALF_UP);
            }
        } catch (Exception ignored) {
        }
        return new BigDecimal("0.1000");
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

    private BigDecimal safeAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String buildWalletTransactionNo() {
        return "WT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase(Locale.ROOT);
    }

    private String renderPercent(BigDecimal rate) {
        return rate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%";
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String buildOrderNo() {
        return LocalDateTime.now().format(ORDER_NO_FORMATTER) + String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
    }

    private String buildChatGroupNo() {
        return "CHAT" + String.format("%08d", ThreadLocalRandom.current().nextInt(10000000, 99999999));
    }

    private String buildListingSummary(AccountListingDO listing) {
        List<String> parts = new ArrayList<>();
        parts.add("等级" + listing.getAccountLevel());
        parts.add(listing.getRankName());
        parts.add("哈夫币" + listing.getHafCurrency());
        if (listing.getCityName() != null && !listing.getCityName().trim().isEmpty()) {
            parts.add(listing.getCityName());
        }
        return String.join("｜", parts);
    }

    private String resolveSellerDisplayName(String sellerType, String studioName, String sellerNickname) {
        if ("STUDIO".equalsIgnoreCase(sellerType) && studioName != null && !studioName.trim().isEmpty()) {
            return studioName.trim();
        }
        return sellerNickname == null || sellerNickname.trim().isEmpty() ? "未知卖家" : sellerNickname.trim();
    }

    private String resolveWechatTradeType(String preferredTradeType, AuthUserDO buyer) {
        if ("JSAPI".equalsIgnoreCase(preferredTradeType) && buyer.getOpenId() != null && !buyer.getOpenId().trim().isEmpty()) {
            return "JSAPI";
        }
        return "NATIVE";
    }

    private String renderStatus(String status) {
        if ("PENDING_PAYMENT".equals(status)) return "待付款";
	        if ("WAITING_TRADE".equals(status)) return "交易中";
        if ("IN_PROGRESS".equals(status)) return "交易中";
        if ("COMPLETED".equals(status)) return "已完成";
        if ("REFUND_PENDING".equals(status)) return "退款审核中";
        if ("AFTER_SALE".equals(status)) return "售后中";
        if ("REFUNDED".equals(status)) return "已退款";
        if ("CLOSED".equals(status)) return "已关闭";
        return status;
    }

    private String renderPaymentMethod(String paymentMethod) {
        if ("ALIPAY".equalsIgnoreCase(paymentMethod)) return "支付宝";
        if ("WECHAT".equalsIgnoreCase(paymentMethod)) return "微信支付";
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) return "未支付";
        return paymentMethod;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String normalized = paymentMethod == null ? "ALIPAY" : paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!"ALIPAY".equals(normalized) && !"WECHAT".equals(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅支持支付宝或微信支付");
        }
        return normalized;
    }

    private String buildMockAlipayUrl(TradeOrderDO order) {
        String nonce = UUID.nameUUIDFromBytes((order.getOrderNo() + ":" + order.getBuyerUserId()).getBytes(StandardCharsets.UTF_8))
            .toString()
            .replace("-", "");
        return "mock://alipay/" + order.getOrderNo() + "?nonce=" + nonce.substring(0, 16);
    }

    private String renderSellerType(String sellerType) {
        if ("STUDIO".equalsIgnoreCase(sellerType)) {
            return "工作室";
        }
        return "个人";
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "—" : DISPLAY_TIME_FORMATTER.format(time);
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "¥0.00" : "¥" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < pairs.length; index += 2) {
            map.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return map;
    }

    private String previewNullable(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return "";
        }
        return ossStorageService.previewUrl(objectKey);
    }

    public static class CreateOrderCommand {
        private final String listingId;
        private final Long buyerId;
        private final BigDecimal totalAmount;
        private final Boolean includeExtraItems;

        public CreateOrderCommand(String listingId, Long buyerId, BigDecimal totalAmount, Boolean includeExtraItems) {
            this.listingId = listingId;
            this.buyerId = buyerId;
            this.totalAmount = totalAmount;
            this.includeExtraItems = includeExtraItems;
        }

        public String getListingId() {
            return listingId;
        }

        public Long getBuyerId() {
            return buyerId;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public boolean isIncludeExtraItems() {
            return Boolean.TRUE.equals(includeExtraItems);
        }
    }

    public static class CreateOrderResult {
        private final String orderNo;
        private final String status;
        private final LocalDateTime paymentExpireAt;

        public CreateOrderResult(String orderNo, String status, LocalDateTime paymentExpireAt) {
            this.orderNo = orderNo;
            this.status = status;
            this.paymentExpireAt = paymentExpireAt;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public String getStatus() {
            return status;
        }

        public LocalDateTime getPaymentExpireAt() {
            return paymentExpireAt;
        }
    }

    public static class OrderSummary {
        private final String orderNo;
        private final String status;
        private final BigDecimal totalAmount;

        public OrderSummary(String orderNo, String status, BigDecimal totalAmount) {
            this.orderNo = orderNo;
            this.status = status;
            this.totalAmount = totalAmount;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public String getStatus() {
            return status;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }
    }

    public static class WechatPayResult {
        private final String orderNo;
        private final String paymentMethod;
        private final String tradeType;
        private final String codeUrl;
        private final LocalDateTime expireAt;
        private final JsapiPayParams jsapiPayParams;

        public WechatPayResult(String orderNo, String paymentMethod, String tradeType, String codeUrl, LocalDateTime expireAt, JsapiPayParams jsapiPayParams) {
            this.orderNo = orderNo;
            this.paymentMethod = paymentMethod;
            this.tradeType = tradeType;
            this.codeUrl = codeUrl;
            this.expireAt = expireAt;
            this.jsapiPayParams = jsapiPayParams;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public String getTradeType() {
            return tradeType;
        }

        public String getCodeUrl() {
            return codeUrl;
        }

        public LocalDateTime getExpireAt() {
            return expireAt;
        }

        public JsapiPayParams getJsapiPayParams() {
            return jsapiPayParams;
        }
    }

    public static class QrPaymentResult {
        private final String orderNo;
        private final String paymentMethod;
        private final String tradeType;
        private final String codeUrl;
        private final LocalDateTime expireAt;

        public QrPaymentResult(String orderNo, String paymentMethod, String tradeType, String codeUrl, LocalDateTime expireAt) {
            this.orderNo = orderNo;
            this.paymentMethod = paymentMethod;
            this.tradeType = tradeType;
            this.codeUrl = codeUrl;
            this.expireAt = expireAt;
        }

        public String getOrderNo() { return orderNo; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getTradeType() { return tradeType; }
        public String getCodeUrl() { return codeUrl; }
        public LocalDateTime getExpireAt() { return expireAt; }
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

        public String getAppId() {
            return appId;
        }

        public String getTimeStamp() {
            return timeStamp;
        }

        public String getNonceStr() {
            return nonceStr;
        }

        public String getPackageValue() {
            return packageValue;
        }

        public String getSignType() {
            return signType;
        }

        public String getPaySign() {
            return paySign;
        }
    }

    public static class OrderCenterResult {
        private final String role;
        private final String status;
        private final String range;
        private final OrderCounts counts;
        private final List<OrderListItem> rows;

        public OrderCenterResult(String role, String status, String range, OrderCounts counts, List<OrderListItem> rows) {
            this.role = role;
            this.status = status;
            this.range = range;
            this.counts = counts;
            this.rows = rows;
        }

        public String getRole() {
            return role;
        }

        public String getStatus() {
            return status;
        }

        public String getRange() {
            return range;
        }

        public OrderCounts getCounts() {
            return counts;
        }

        public List<OrderListItem> getRows() {
            return rows;
        }
    }

    public static class OrderCounts {
        private final int total;
        private final int pendingPayment;
        private final int waitingTrade;
        private final int inProgress;
        private final int completed;
        private final int refundPending;
        private final int afterSale;
        private final int refunded;
        private final int closed;

        public OrderCounts(int total, int pendingPayment, int waitingTrade, int inProgress, int completed, int refundPending, int afterSale, int refunded, int closed) {
            this.total = total;
            this.pendingPayment = pendingPayment;
            this.waitingTrade = waitingTrade;
            this.inProgress = inProgress;
            this.completed = completed;
            this.refundPending = refundPending;
            this.afterSale = afterSale;
            this.refunded = refunded;
            this.closed = closed;
        }

        public int getTotal() {
            return total;
        }

        public int getPendingPayment() {
            return pendingPayment;
        }

        public int getWaitingTrade() {
            return waitingTrade;
        }

        public int getInProgress() {
            return inProgress;
        }

        public int getCompleted() {
            return completed;
        }

        public int getRefundPending() {
            return refundPending;
        }

        public int getAfterSale() {
            return afterSale;
        }

        public int getRefunded() {
            return refunded;
        }

        public int getClosed() {
            return closed;
        }
    }

    public static class OrderListItem {
        private final String orderNo;
        private final String title;
        private final String summary;
        private final String coverUrl;
        private final BigDecimal totalAmount;
        private final String tradeTime;
        private final String statusLabel;
        private final String statusCode;
        private final String buyerNickname;
        private final String sellerNickname;
        private final String sellerType;
        private final String sellerDisplayName;
        private final String chatGroupNo;
        private final boolean canCancel;
        private final boolean canConfirmComplete;
        private final boolean canApplyAfterSale;
        private final boolean canApplyRefund;
        private final boolean canReviewRefund;
        private final boolean canEnterChat;
        private final boolean canDelete;
        private final LocalDateTime paymentExpireAt;
        private final boolean canDownloadCertificate;

        public OrderListItem(
            String orderNo,
            String title,
            String summary,
            String coverUrl,
            BigDecimal totalAmount,
            String tradeTime,
            String statusLabel,
            String statusCode,
            String buyerNickname,
            String sellerNickname,
            String sellerType,
            String sellerDisplayName,
            String chatGroupNo,
            boolean canCancel,
            boolean canConfirmComplete,
            boolean canApplyAfterSale,
            boolean canApplyRefund,
            boolean canReviewRefund,
            boolean canEnterChat,
            boolean canDelete,
            LocalDateTime paymentExpireAt,
            boolean canDownloadCertificate
        ) {
            this.orderNo = orderNo;
            this.title = title;
            this.summary = summary;
            this.coverUrl = coverUrl;
            this.totalAmount = totalAmount;
            this.tradeTime = tradeTime;
            this.statusLabel = statusLabel;
            this.statusCode = statusCode;
            this.buyerNickname = buyerNickname;
            this.sellerNickname = sellerNickname;
            this.sellerType = sellerType;
            this.sellerDisplayName = sellerDisplayName;
            this.chatGroupNo = chatGroupNo;
            this.canCancel = canCancel;
            this.canConfirmComplete = canConfirmComplete;
            this.canApplyAfterSale = canApplyAfterSale;
            this.canApplyRefund = canApplyRefund;
            this.canReviewRefund = canReviewRefund;
            this.canEnterChat = canEnterChat;
            this.canDelete = canDelete;
            this.paymentExpireAt = paymentExpireAt;
            this.canDownloadCertificate = canDownloadCertificate;
        }

        public String getOrderNo() { return orderNo; }
        public String getTitle() { return title; }
        public String getSummary() { return summary; }
        public String getCoverUrl() { return coverUrl; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public String getTradeTime() { return tradeTime; }
        public String getStatusLabel() { return statusLabel; }
        public String getStatusCode() { return statusCode; }
        public String getBuyerNickname() { return buyerNickname; }
        public String getSellerNickname() { return sellerNickname; }
        public String getSellerType() { return sellerType; }
        public String getSellerDisplayName() { return sellerDisplayName; }
        public String getChatGroupNo() { return chatGroupNo; }
        public boolean isCanCancel() { return canCancel; }
        public boolean isCanConfirmComplete() { return canConfirmComplete; }
        public boolean isCanApplyAfterSale() { return canApplyAfterSale; }
        public boolean isCanApplyRefund() { return canApplyRefund; }
        public boolean isCanReviewRefund() { return canReviewRefund; }
        public boolean isCanEnterChat() { return canEnterChat; }
        public boolean isCanDelete() { return canDelete; }
        public LocalDateTime getPaymentExpireAt() { return paymentExpireAt; }
        public boolean isCanDownloadCertificate() { return canDownloadCertificate; }
    }

    public static class OrderDetailResult {
        private final String orderNo;
        private final String role;
        private final String listingNo;
        private final String title;
        private final String summary;
        private final String coverUrl;
        private final String statusLabel;
        private final String statusCode;
        private final String createdAt;
        private final String paidAt;
        private final String completedAt;
        private final String paymentMethod;
        private final BigDecimal itemAmount;
        private final BigDecimal serviceFee;
        private final BigDecimal totalAmount;
        private final BigDecimal depositAmount;
        private final boolean buyerConfirmed;
        private final boolean sellerConfirmed;
        private final String buyerNickname;
        private final String sellerNickname;
        private final String sellerType;
        private final String sellerDisplayName;
        private final boolean canCancel;
        private final boolean canConfirmComplete;
        private final boolean canApplyAfterSale;
        private final boolean canApplyRefund;
        private final boolean canReviewRefund;
        private final boolean canDelete;
        private final boolean canEnterChat;
        private final String chatGroupNo;
        private final LocalDateTime paymentExpireAt;
        private final String guaranteeNote;
        private final String violationNote;
        private final List<ProgressStep> progress;

        public OrderDetailResult(
            String orderNo,
            String role,
            String listingNo,
            String title,
            String summary,
            String coverUrl,
            String statusLabel,
            String statusCode,
            String createdAt,
            String paidAt,
            String completedAt,
            String paymentMethod,
            BigDecimal itemAmount,
            BigDecimal serviceFee,
            BigDecimal totalAmount,
            BigDecimal depositAmount,
            boolean buyerConfirmed,
            boolean sellerConfirmed,
            String buyerNickname,
            String sellerNickname,
            String sellerType,
            String sellerDisplayName,
            boolean canCancel,
            boolean canConfirmComplete,
            boolean canApplyAfterSale,
            boolean canApplyRefund,
            boolean canReviewRefund,
            boolean canDelete,
            boolean canEnterChat,
            String chatGroupNo,
            LocalDateTime paymentExpireAt,
            String guaranteeNote,
            String violationNote,
            List<ProgressStep> progress
        ) {
            this.orderNo = orderNo;
            this.role = role;
            this.listingNo = listingNo;
            this.title = title;
            this.summary = summary;
            this.coverUrl = coverUrl;
            this.statusLabel = statusLabel;
            this.statusCode = statusCode;
            this.createdAt = createdAt;
            this.paidAt = paidAt;
            this.completedAt = completedAt;
            this.paymentMethod = paymentMethod;
            this.itemAmount = itemAmount;
            this.serviceFee = serviceFee;
            this.totalAmount = totalAmount;
            this.depositAmount = depositAmount;
            this.buyerConfirmed = buyerConfirmed;
            this.sellerConfirmed = sellerConfirmed;
            this.buyerNickname = buyerNickname;
            this.sellerNickname = sellerNickname;
            this.sellerType = sellerType;
            this.sellerDisplayName = sellerDisplayName;
            this.canCancel = canCancel;
            this.canConfirmComplete = canConfirmComplete;
            this.canApplyAfterSale = canApplyAfterSale;
            this.canApplyRefund = canApplyRefund;
            this.canReviewRefund = canReviewRefund;
            this.canDelete = canDelete;
            this.canEnterChat = canEnterChat;
            this.chatGroupNo = chatGroupNo;
            this.paymentExpireAt = paymentExpireAt;
            this.guaranteeNote = guaranteeNote;
            this.violationNote = violationNote;
            this.progress = progress;
        }

        public String getOrderNo() { return orderNo; }
        public String getRole() { return role; }
        public String getListingNo() { return listingNo; }
        public String getTitle() { return title; }
        public String getSummary() { return summary; }
        public String getCoverUrl() { return coverUrl; }
        public String getStatusLabel() { return statusLabel; }
        public String getStatusCode() { return statusCode; }
        public String getCreatedAt() { return createdAt; }
        public String getPaidAt() { return paidAt; }
        public String getCompletedAt() { return completedAt; }
        public String getPaymentMethod() { return paymentMethod; }
        public BigDecimal getItemAmount() { return itemAmount; }
        public BigDecimal getServiceFee() { return serviceFee; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public BigDecimal getDepositAmount() { return depositAmount; }
        public boolean isBuyerConfirmed() { return buyerConfirmed; }
        public boolean isSellerConfirmed() { return sellerConfirmed; }
        public String getBuyerNickname() { return buyerNickname; }
        public String getSellerNickname() { return sellerNickname; }
        public String getSellerType() { return sellerType; }
        public String getSellerDisplayName() { return sellerDisplayName; }
        public boolean isCanCancel() { return canCancel; }
        public boolean isCanConfirmComplete() { return canConfirmComplete; }
        public boolean isCanApplyAfterSale() { return canApplyAfterSale; }
        public boolean isCanApplyRefund() { return canApplyRefund; }
        public boolean isCanReviewRefund() { return canReviewRefund; }
        public boolean isCanDelete() { return canDelete; }
        public boolean isCanEnterChat() { return canEnterChat; }
        public String getChatGroupNo() { return chatGroupNo; }
        public LocalDateTime getPaymentExpireAt() { return paymentExpireAt; }
        public String getGuaranteeNote() { return guaranteeNote; }
        public String getViolationNote() { return violationNote; }
        public List<ProgressStep> getProgress() { return progress; }
    }

    public static class ProgressStep {
        private final String title;
        private final String description;
        private final boolean done;
        private final boolean current;

        public ProgressStep(String title, String description, boolean done, boolean current) {
            this.title = title;
            this.description = description;
            this.done = done;
            this.current = current;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public boolean isDone() { return done; }
        public boolean isCurrent() { return current; }
    }

    public static class CertificateResult {
        private final String filename;
        private final String content;

        public CertificateResult(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }

        public String getFilename() { return filename; }
        public String getContent() { return content; }
    }

    private static class PublishAttributesSnapshot {
        private BigDecimal deposit;
        private List<PublishExtraItemSnapshot> extraItems;

        public BigDecimal getDeposit() { return deposit; }
        public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }
        public List<PublishExtraItemSnapshot> getExtraItems() {
            return extraItems == null ? Collections.<PublishExtraItemSnapshot>emptyList() : extraItems;
        }
        public void setExtraItems(List<PublishExtraItemSnapshot> extraItems) { this.extraItems = extraItems; }
    }

    private static class PublishExtraItemSnapshot {
        private String code;
        private String label;
        private Integer count;
        private String chargeMode;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public String getChargeMode() { return chargeMode; }
        public void setChargeMode(String chargeMode) { this.chargeMode = chargeMode; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
    }

    private static class OrderExtraItemsSnapshot {
        private final boolean included;
        private final BigDecimal amount;
        private final List<OrderExtraItemLine> items;

        public OrderExtraItemsSnapshot(boolean included, BigDecimal amount, List<OrderExtraItemLine> items) {
            this.included = included;
            this.amount = amount;
            this.items = items == null ? Collections.<OrderExtraItemLine>emptyList() : items;
        }

        public boolean isIncluded() { return included; }
        public BigDecimal getAmount() { return amount; }
        public List<OrderExtraItemLine> getItems() { return items; }
    }

    private static class OrderExtraItemLine {
        private final String code;
        private final String label;
        private final int count;
        private final BigDecimal unitPrice;
        private final BigDecimal subtotal;

        public OrderExtraItemLine(String code, String label, int count, BigDecimal unitPrice, BigDecimal subtotal) {
            this.code = code;
            this.label = label;
            this.count = count;
            this.unitPrice = unitPrice;
            this.subtotal = subtotal;
        }

        public String getCode() { return code; }
        public String getLabel() { return label; }
        public int getCount() { return count; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getSubtotal() { return subtotal; }
    }

    private static class DateRange {
        private final String label;
        private final LocalDateTime startAt;
        private final LocalDateTime endAt;

        private DateRange(String label, LocalDateTime startAt, LocalDateTime endAt) {
            this.label = label;
            this.startAt = startAt;
            this.endAt = endAt;
        }

        public String getLabel() { return label; }
        public LocalDateTime getStartAt() { return startAt; }
        public LocalDateTime getEndAt() { return endAt; }
    }

}
