package com.deltatrade.platform.modules.boosting.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.auth.mapper.AuthUserMapper;
import com.deltatrade.platform.modules.auth.model.AuthUserDO;
import com.deltatrade.platform.modules.boosting.mapper.BoostingOrderMapper;
import com.deltatrade.platform.modules.boosting.mapper.BoostingProgressLogMapper;
import com.deltatrade.platform.modules.boosting.mapper.BoostingServiceMapper;
import com.deltatrade.platform.modules.boosting.model.BoostingOrderDO;
import com.deltatrade.platform.modules.boosting.model.BoostingProgressLogDO;
import com.deltatrade.platform.modules.boosting.model.BoostingServiceDO;
import com.deltatrade.platform.modules.oss.service.OssStorageService;
import com.deltatrade.platform.modules.payment.service.WechatPayGateway;
import com.deltatrade.platform.modules.profile.service.DistributionService;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoostingCenterService {

    private static final Logger log = LoggerFactory.getLogger(BoostingCenterService.class);
    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final List<String> ORDER_STATUS_SEQUENCE = Arrays.asList(
        "PENDING_PAYMENT",
        "WAITING_SERVICE",
        "IN_SERVICE",
        "COMPLETED",
        "AFTER_SALE",
        "CANCELED"
    );

    private final BoostingServiceMapper boostingServiceMapper;
    private final BoostingOrderMapper boostingOrderMapper;
    private final BoostingProgressLogMapper boostingProgressLogMapper;
    private final AuthUserMapper authUserMapper;
    private final OssStorageService ossStorageService;
    private final String passwordSecret;
    private final WechatPayGateway wechatPayGateway;
    private final DistributionService distributionService;

    public BoostingCenterService(
        BoostingServiceMapper boostingServiceMapper,
        BoostingOrderMapper boostingOrderMapper,
        BoostingProgressLogMapper boostingProgressLogMapper,
        AuthUserMapper authUserMapper,
        OssStorageService ossStorageService,
        @Value("${platform.boosting.password-secret}") String passwordSecret,
        WechatPayGateway wechatPayGateway,
        DistributionService distributionService
    ) {
        this.boostingServiceMapper = boostingServiceMapper;
        this.boostingOrderMapper = boostingOrderMapper;
        this.boostingProgressLogMapper = boostingProgressLogMapper;
        this.authUserMapper = authUserMapper;
        this.ossStorageService = ossStorageService;
        this.passwordSecret = passwordSecret;
        this.wechatPayGateway = wechatPayGateway;
        this.distributionService = distributionService;
    }

    public BoostingHallMeta getHallMeta() {
        List<BoostingServiceDO> services = boostingServiceMapper.selectList(
            Wrappers.<BoostingServiceDO>lambdaQuery()
                .eq(BoostingServiceDO::getStatus, "ACTIVE")
                .orderByAsc(BoostingServiceDO::getSortNo)
                .orderByDesc(BoostingServiceDO::getSalesCount)
        );
        List<OptionItem> categories = new ArrayList<OptionItem>();
        categories.add(new OptionItem("ALL", "全部服务"));
        services.stream()
            .collect(Collectors.toMap(BoostingServiceDO::getCategoryCode, BoostingServiceDO::getCategoryLabel, (left, right) -> left, LinkedHashMap::new))
            .forEach((key, value) -> categories.add(new OptionItem(key, value)));

        List<OptionItem> cycles = new ArrayList<OptionItem>();
        cycles.add(new OptionItem("ALL", "全部周期"));
        services.stream()
            .collect(Collectors.toMap(BoostingServiceDO::getCycleCode, BoostingServiceDO::getCycleLabel, (left, right) -> left, LinkedHashMap::new))
            .forEach((key, value) -> cycles.add(new OptionItem(key, value)));

        return new BoostingHallMeta(
            categories,
            cycles,
            Arrays.asList(
                new OptionItem("SALES_DESC", "销量优先"),
                new OptionItem("PRICE_ASC", "价格升序"),
                new OptionItem("PRICE_DESC", "价格降序"),
                new OptionItem("CYCLE_ASC", "周期优先")
            )
        );
    }

    public BoostingHallResult getHall(String category, BigDecimal minPrice, BigDecimal maxPrice, String cycle, String sort) {
        long startAt = System.currentTimeMillis();
        String normalizedCategory = normalizeCategory(category);
        String normalizedCycle = normalizeCycle(cycle);
        String normalizedSort = normalizeSort(sort);

        List<BoostingServiceDO> rows = boostingServiceMapper.selectList(
            Wrappers.<BoostingServiceDO>lambdaQuery()
                .eq(BoostingServiceDO::getStatus, "ACTIVE")
                .eq(!"ALL".equals(normalizedCategory), BoostingServiceDO::getCategoryCode, normalizedCategory)
                .eq(!"ALL".equals(normalizedCycle), BoostingServiceDO::getCycleCode, normalizedCycle)
                .ge(minPrice != null, BoostingServiceDO::getPrice, minPrice)
                .le(maxPrice != null, BoostingServiceDO::getPrice, maxPrice)
        );

        rows.sort(buildServiceComparator(normalizedSort));
        List<BoostingServiceCard> cards = rows.stream().map(this::toCard).collect(Collectors.toList());
        log.info("boosting hall loaded category={} cycle={} sort={} rows={} costMs={}",
            normalizedCategory, normalizedCycle, normalizedSort, cards.size(), System.currentTimeMillis() - startAt);
        return new BoostingHallResult(normalizedCategory, normalizedCycle, normalizedSort, cards);
    }

    public BoostingServiceDetail getServiceDetail(String serviceNo) {
        BoostingServiceDO service = requireService(serviceNo);
        return toDetail(service);
    }

    @Transactional
    public CreateBoostingOrderResult createOrder(Long userId, CreateBoostingOrderCommand command) {
        long startAt = System.currentTimeMillis();
        AuthUserDO buyer = requireUser(userId);
        BoostingServiceDO service = requireService(command.getServiceNo());
        String gameRegion = normalizeText(command.getGameRegion(), "游戏区服不能为空");
        String accountName = normalizeText(command.getAccountName(), "账号不能为空");
        String accountPassword = normalizeText(command.getAccountPassword(), "账号密码不能为空");
        String characterName = normalizeText(command.getCharacterName(), "角色名称不能为空");
        String agreement = normalizeText(command.getAgreementCode(), "请先勾选代肝协议");
        if (!"BOOSTING_AGREEMENT".equals(agreement)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先同意代肝协议");
        }
        String paymentMethod = normalizePaymentMethod(command.getPaymentMethod());
        LocalDateTime now = LocalDateTime.now();

        BoostingOrderDO order = new BoostingOrderDO();
        order.setOrderNo(buildOrderNo());
        order.setServiceNo(service.getServiceNo());
        order.setServiceName(service.getName());
        order.setServiceCategory(service.getCategoryLabel());
        order.setServiceDescription(service.getDescription());
        order.setPrice(service.getPrice());
        order.setCycleLabel(service.getCycleLabel());
        order.setGuaranteeNote(service.getGuaranteeNote());
        order.setProviderType(service.getProviderType());
        order.setProviderName(service.getProviderName());
        order.setBuyerUserId(userId);
        order.setBuyerNickname(buyer.getNickname());
        order.setGameRegion(gameRegion);
        order.setAccountName(accountName);
        order.setAccountPasswordCipher(encryptPassword(accountPassword));
        order.setCharacterName(characterName);
        order.setSpecialRequirement(trimNullable(command.getSpecialRequirement()));
        order.setStatus("PENDING_PAYMENT");
        order.setPaymentMethod(paymentMethod);
        order.setProgressPercent(0);
        order.setProgressSummary("待完成支付，支付成功后将进入待代肝");
        order.setChatGroupNo(buildChatGroupNo());
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        insertOrder(order);
        insertProgressLog(order.getOrderNo(), 0, "订单已创建", "订单已生成，等待完成支付后开始排期。", "SYSTEM", now);
        log.info("boosting order create success orderNo={} serviceNo={} userId={} paymentMethod={} costMs={}",
            order.getOrderNo(), service.getServiceNo(), userId, paymentMethod, System.currentTimeMillis() - startAt);
        return new CreateBoostingOrderResult(order.getOrderNo(), "PENDING_PAYMENT", "订单已创建，请完成支付后进入待代肝阶段");
    }

    @Transactional
    public BoostingOrderDetail payOrder(Long userId, String orderNo, String paymentMethod) {
        BoostingOrderDO order = requireOrderForUser(userId, orderNo);
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "支付结果必须由支付平台回调确认，不能手动确认付款");
    }

    @Transactional
    public WechatPayResult createWechatPayment(Long userId, String orderNo, String notifyUrl, String preferredTradeType) {
        BoostingOrderDO order = requireOrderForUser(userId, orderNo);
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前代肝订单状态不允许发起支付");
        }
        AuthUserDO buyer = requireUser(userId);
        String tradeType = resolveWechatTradeType(preferredTradeType, buyer.getOpenId());
        WechatPayGateway.WechatPayResult payment = wechatPayGateway.createOrder(new WechatPayGateway.CreateOrderRequest(
            order.getOrderNo(),
            order.getServiceName(),
            order.getPrice(),
            notifyUrl,
            "BOOSTING",
            tradeType,
            "JSAPI".equals(tradeType) ? buyer.getOpenId() : null
        ));
        LocalDateTime now = LocalDateTime.now();
        order.setPaymentMethod("WECHAT");
        order.setPaymentTradeType(payment.getTradeType());
        order.setPaymentPrepayId(payment.getPrepayId());
        order.setPaymentCodeUrl(payment.getCodeUrl());
        order.setPaymentExpireAt(payment.getExpireAt());
        order.setUpdatedAt(now);
        updateOrder(order);
        log.info("boosting wechat payment prepared orderNo={} userId={} tradeType={}",
            orderNo, userId, payment.getTradeType());
        return new WechatPayResult(
            order.getOrderNo(),
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

    @Transactional
    public BoostingOrderCenterResult getOrders(Long userId, String status, String range, String startDate, String endDate) {
        long startAt = System.currentTimeMillis();
        String normalizedStatus = normalizeOrderStatus(status);
        DateRange resolvedRange = resolveDateRange(range, startDate, endDate);
        List<BoostingOrderDO> rows = boostingOrderMapper.selectList(
            Wrappers.<BoostingOrderDO>lambdaQuery()
                .eq(BoostingOrderDO::getBuyerUserId, userId)
                .eq(!"ALL".equals(normalizedStatus), BoostingOrderDO::getStatus, normalizedStatus)
                .ge(resolvedRange.getStartAt() != null, BoostingOrderDO::getCreatedAt, resolvedRange.getStartAt())
                .lt(resolvedRange.getEndAt() != null, BoostingOrderDO::getCreatedAt, resolvedRange.getEndAt())
                .orderByDesc(BoostingOrderDO::getCreatedAt)
        );
        BoostingOrderCounts counts = buildOrderCounts(rows);
        List<BoostingOrderListItem> items = rows.stream().map(this::toOrderListItem).collect(Collectors.toList());
        log.info("boosting orders loaded userId={} status={} range={} rows={} costMs={}",
            userId, normalizedStatus, resolvedRange.getLabel(), items.size(), System.currentTimeMillis() - startAt);
        return new BoostingOrderCenterResult(normalizedStatus, resolvedRange.getLabel(), counts, items);
    }

    public BoostingOrderDetail getOrderDetail(Long userId, String orderNo) {
        long startAt = System.currentTimeMillis();
        BoostingOrderDO order = requireOrderForUser(userId, orderNo);
        BoostingOrderDetail detail = toOrderDetail(order);
        log.info("boosting order detail loaded orderNo={} userId={} status={} costMs={}",
            orderNo, userId, order.getStatus(), System.currentTimeMillis() - startAt);
        return detail;
    }

    @Transactional
    public BoostingOrderDetail applyAfterSale(Long userId, String orderNo, String reason, String proofKey) {
        BoostingOrderDO order = requireOrderForUser(userId, orderNo);
        if (!"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅已完成订单可申请售后");
        }
        if (order.getCompletedAt() == null || order.getCompletedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "售后申请窗口已关闭");
        }
        String normalizedReason = normalizeText(reason, "请填写售后原因");
        String normalizedProofKey = normalizeText(proofKey, "请上传售后凭证截图");
        LocalDateTime now = LocalDateTime.now();
        order.setStatus("AFTER_SALE");
        order.setAfterSaleReason(normalizedReason);
        order.setAfterSaleProofKey(normalizedProofKey);
        order.setAfterSaleAt(now);
        order.setProgressSummary("订单已提交售后申请，平台客服将介入处理");
        order.setUpdatedAt(now);
        updateOrder(order);
        insertProgressLog(order.getOrderNo(), safePercent(order.getProgressPercent()), "售后申请已提交", "买家已提交售后原因与凭证，等待客服处理。", "BUYER", now);
        log.info("boosting after-sale success orderNo={} userId={} proofKey={}", orderNo, userId, normalizedProofKey);
        return toOrderDetail(order);
    }

    @Transactional
    public void applyWechatPaymentSuccess(String orderNo, String transactionId, LocalDateTime paidAt) {
        BoostingOrderDO order = boostingOrderMapper.selectOne(
            Wrappers.<BoostingOrderDO>lambdaQuery()
                .eq(BoostingOrderDO::getOrderNo, orderNo)
                .last("LIMIT 1")
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "代肝订单不存在");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            log.info("boosting wechat payment ignored orderNo={} currentStatus={} transactionId={}",
                orderNo, order.getStatus(), transactionId);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        order.setPaymentMethod("WECHAT");
        order.setPaymentTransactionId(transactionId);
        order.setPaymentNotifiedAt(now);
        order.setStatus("WAITING_SERVICE");
        order.setPaidAt(paidAt == null ? now : paidAt);
        order.setProgressPercent(10);
        order.setProgressSummary("订单已支付，等待代肝人员接单");
        order.setUpdatedAt(now);
        updateOrder(order);
        incrementServiceSales(order.getServiceNo());
        insertProgressLog(order.getOrderNo(), 10, "支付成功", "订单已支付成功，平台客服正在安排代肝排期。", "SYSTEM", now);
        distributionService.onBoostingOrderPaid(order);
        log.info("boosting wechat payment success orderNo={} transactionId={} paidAt={}",
            orderNo, transactionId, order.getPaidAt());
    }

    @Transactional
    public BoostingOrderDetail confirmComplete(Long userId, String orderNo) {
        BoostingOrderDO order = requireOrderForUser(userId, orderNo);
        if (!"WAITING_SERVICE".equals(order.getStatus()) && !"IN_SERVICE".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前代肝订单状态不允许确认完成");
        }
        LocalDateTime now = LocalDateTime.now();
        order.setStatus("COMPLETED");
        order.setCompletedAt(now);
        order.setProgressPercent(100);
        order.setProgressSummary("用户已确认代肝完成，佣金与售后窗口同步开启");
        order.setUpdatedAt(now);
        updateOrder(order);
        insertProgressLog(order.getOrderNo(), 100, "用户确认完成", "用户已确认本次代肝服务完成，订单已归档。", "BUYER", now);
        distributionService.onBoostingOrderCompleted(order);
        log.info("boosting complete success orderNo={} userId={} completedAt={}", orderNo, userId, now);
        return toOrderDetail(order);
    }

    private BoostingServiceCard toCard(BoostingServiceDO item) {
        return new BoostingServiceCard(
            item.getServiceNo(),
            item.getCategoryCode(),
            item.getCategoryLabel(),
            item.getName(),
            item.getDescription(),
            item.getPrice(),
            item.getCycleLabel(),
            item.getGuaranteeNote(),
            renderProvider(item.getProviderType(), item.getProviderName()),
            safeNumber(item.getSalesCount())
        );
    }

    private BoostingServiceDetail toDetail(BoostingServiceDO item) {
        return new BoostingServiceDetail(
            item.getServiceNo(),
            item.getCategoryCode(),
            item.getCategoryLabel(),
            item.getName(),
            item.getDescription(),
            item.getPrice(),
            item.getCycleCode(),
            item.getCycleLabel(),
            item.getGuaranteeNote(),
            item.getProviderType(),
            renderProvider(item.getProviderType(), item.getProviderName()),
            safeNumber(item.getSalesCount()),
            Arrays.asList(
                "代肝期间不私下交易、不转移账号资产",
                "超时未完成可申请赔付",
                "订单全程保留站内记录与客服接入入口"
            )
        );
    }

    private BoostingOrderListItem toOrderListItem(BoostingOrderDO order) {
        boolean canPay = "PENDING_PAYMENT".equals(order.getStatus());
        boolean canConfirmComplete = "WAITING_SERVICE".equals(order.getStatus()) || "IN_SERVICE".equals(order.getStatus());
        boolean canAfterSale = "COMPLETED".equals(order.getStatus())
            && order.getCompletedAt() != null
            && !order.getCompletedAt().plusHours(24).isBefore(LocalDateTime.now());
        boolean canContactService = !"CANCELED".equals(order.getStatus());
        return new BoostingOrderListItem(
            order.getOrderNo(),
            order.getServiceName(),
            order.getPrice(),
            formatTime(order.getCreatedAt()),
            renderOrderStatus(order.getStatus()),
            order.getStatus(),
            safePercent(order.getProgressPercent()),
            defaultText(order.getProgressSummary(), "等待平台接单"),
            canPay,
            canConfirmComplete,
            canAfterSale,
            canContactService
        );
    }

    private BoostingOrderDetail toOrderDetail(BoostingOrderDO order) {
        List<BoostingProgressLogDO> logs = boostingProgressLogMapper.selectList(
            Wrappers.<BoostingProgressLogDO>lambdaQuery()
                .eq(BoostingProgressLogDO::getOrderNo, order.getOrderNo())
                .orderByAsc(BoostingProgressLogDO::getCreatedAt)
        );
        List<BoostingProgressItem> progress = logs.stream()
            .map(item -> new BoostingProgressItem(
                item.getTitle(),
                item.getContent(),
                safePercent(item.getProgressPercent()),
                formatTime(item.getCreatedAt()),
                item.getCreatedBy()
            ))
            .collect(Collectors.toList());
        boolean canPay = "PENDING_PAYMENT".equals(order.getStatus());
        boolean canConfirmComplete = "WAITING_SERVICE".equals(order.getStatus()) || "IN_SERVICE".equals(order.getStatus());
        boolean canAfterSale = "COMPLETED".equals(order.getStatus())
            && order.getCompletedAt() != null
            && !order.getCompletedAt().plusHours(24).isBefore(LocalDateTime.now());
        return new BoostingOrderDetail(
            order.getOrderNo(),
            order.getServiceNo(),
            order.getServiceName(),
            order.getServiceCategory(),
            order.getServiceDescription(),
            order.getPrice(),
            order.getCycleLabel(),
            order.getGuaranteeNote(),
            renderProvider(order.getProviderType(), order.getProviderName()),
            renderOrderStatus(order.getStatus()),
            order.getStatus(),
            renderPaymentMethod(order.getPaymentMethod()),
            formatTime(order.getCreatedAt()),
            formatTime(order.getPaidAt()),
            formatTime(order.getCompletedAt()),
            order.getGameRegion(),
            order.getAccountName(),
            maskPassword(decryptPassword(order.getAccountPasswordCipher())),
            order.getCharacterName(),
            defaultText(order.getSpecialRequirement(), "无特殊需求"),
            safePercent(order.getProgressPercent()),
            defaultText(order.getProgressSummary(), "等待平台接单"),
            order.getChatGroupNo(),
            canPay,
            canConfirmComplete,
            canAfterSale,
            !"CANCELED".equals(order.getStatus()),
            order.getAfterSaleReason(),
            previewNullable(order.getAfterSaleProofKey()),
            progress
        );
    }

    private void incrementServiceSales(String serviceNo) {
        BoostingServiceDO service = requireService(serviceNo);
        service.setSalesCount(safeNumber(service.getSalesCount()) + 1);
        service.setUpdatedAt(LocalDateTime.now());
        boostingServiceMapper.updateById(service);
    }

    private Comparator<BoostingServiceDO> buildServiceComparator(String sort) {
        if ("PRICE_ASC".equals(sort)) {
            return Comparator.comparing(BoostingServiceDO::getPrice).thenComparing(BoostingServiceDO::getSortNo);
        }
        if ("PRICE_DESC".equals(sort)) {
            return Comparator.comparing(BoostingServiceDO::getPrice).reversed().thenComparing(BoostingServiceDO::getSortNo);
        }
        if ("CYCLE_ASC".equals(sort)) {
            return Comparator.comparingInt(item -> cycleWeight(item.getCycleCode()));
        }
        return Comparator.comparing(BoostingServiceDO::getSalesCount, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(BoostingServiceDO::getSortNo);
    }

    private int cycleWeight(String cycleCode) {
        if ("D1_3".equals(cycleCode)) {
            return 1;
        }
        if ("D3_5".equals(cycleCode)) {
            return 2;
        }
        if ("D5_7".equals(cycleCode)) {
            return 3;
        }
        return 4;
    }

    private BoostingOrderCounts buildOrderCounts(List<BoostingOrderDO> rows) {
        int pending = 0;
        int waiting = 0;
        int inService = 0;
        int completed = 0;
        int afterSale = 0;
        int canceled = 0;
        for (BoostingOrderDO row : rows) {
            if ("PENDING_PAYMENT".equals(row.getStatus())) {
                pending++;
            } else if ("WAITING_SERVICE".equals(row.getStatus())) {
                waiting++;
            } else if ("IN_SERVICE".equals(row.getStatus())) {
                inService++;
            } else if ("COMPLETED".equals(row.getStatus())) {
                completed++;
            } else if ("AFTER_SALE".equals(row.getStatus())) {
                afterSale++;
            } else if ("CANCELED".equals(row.getStatus())) {
                canceled++;
            }
        }
        return new BoostingOrderCounts(rows.size(), pending, waiting, inService, completed, afterSale, canceled);
    }

    private DateRange resolveDateRange(String range, String startDate, String endDate) {
        String normalizedRange = range == null || range.trim().isEmpty() ? "ALL" : range.trim().toUpperCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        if ("D7".equals(normalizedRange)) {
            return new DateRange("D7", now.minusDays(7), now.plusMinutes(1));
        }
        if ("D30".equals(normalizedRange)) {
            return new DateRange("D30", now.minusDays(30), now.plusMinutes(1));
        }
        if ("CUSTOM".equals(normalizedRange)) {
            if (startDate == null || endDate == null || startDate.trim().isEmpty() || endDate.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "自定义时间范围需填写开始和结束日期");
            }
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            return new DateRange("CUSTOM", start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        }
        return new DateRange("ALL", null, null);
    }

    private BoostingServiceDO requireService(String serviceNo) {
        BoostingServiceDO service = boostingServiceMapper.selectOne(
            Wrappers.<BoostingServiceDO>lambdaQuery().eq(BoostingServiceDO::getServiceNo, serviceNo)
        );
        if (service == null || !"ACTIVE".equals(service.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "代肝服务不存在或已下架");
        }
        return service;
    }

    private BoostingOrderDO requireOrderForUser(Long userId, String orderNo) {
        BoostingOrderDO order = boostingOrderMapper.selectOne(
            Wrappers.<BoostingOrderDO>lambdaQuery()
                .eq(BoostingOrderDO::getOrderNo, orderNo)
                .eq(BoostingOrderDO::getBuyerUserId, userId)
        );
        if (order == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "代肝订单不存在");
        }
        return order;
    }

    private AuthUserDO requireUser(Long userId) {
        AuthUserDO user = authUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前用户不存在");
        }
        return user;
    }

    private void insertOrder(BoostingOrderDO order) {
        int rows = boostingOrderMapper.insert(order);
        log.info("mysql insert success target=boosting_order orderNo={} rows={}", order.getOrderNo(), rows);
    }

    private void updateOrder(BoostingOrderDO order) {
        int rows = boostingOrderMapper.updateById(order);
        log.info("mysql update success target=boosting_order orderNo={} rows={}", order.getOrderNo(), rows);
    }

    private void insertProgressLog(String orderNo, int percent, String title, String content, String createdBy, LocalDateTime createdAt) {
        BoostingProgressLogDO row = new BoostingProgressLogDO();
        row.setOrderNo(orderNo);
        row.setProgressPercent(percent);
        row.setTitle(title);
        row.setContent(content);
        row.setCreatedBy(createdBy);
        row.setCreatedAt(createdAt);
        int rows = boostingProgressLogMapper.insert(row);
        log.info("mysql insert success target=boosting_progress_log orderNo={} title={} rows={}", orderNo, title, rows);
    }

    private String encryptPassword(String raw) {
        try {
            byte[] key = Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(passwordSecret.getBytes(StandardCharsets.UTF_8)), 16);
            byte[] iv = UUID.randomUUID().toString().replace("-", "").substring(0, 12).getBytes(StandardCharsets.UTF_8);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "账号密码加密失败");
        }
    }

    private String decryptPassword(String cipherText) {
        try {
            byte[] key = Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(passwordSecret.getBytes(StandardCharsets.UTF_8)), 16);
            byte[] raw = Base64.getDecoder().decode(cipherText);
            byte[] iv = Arrays.copyOfRange(raw, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(raw, 12, raw.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("boosting password decrypt failed", ex);
            return "******";
        }
    }

    private String buildOrderNo() {
        return LocalDateTime.now().format(ORDER_NO_FORMATTER) + String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
    }

    private String buildChatGroupNo() {
        return "BG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "ALL";
        }
        String value = category.trim().toUpperCase(Locale.ROOT);
        if (Arrays.asList("ALL", "HAF_COIN", "SAFE_BOX").contains(value)) {
            return value;
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不支持的代肝分类");
    }

    private String normalizeCycle(String cycle) {
        if (cycle == null || cycle.trim().isEmpty()) {
            return "ALL";
        }
        String value = cycle.trim().toUpperCase(Locale.ROOT);
        if (Arrays.asList("ALL", "D1_3", "D3_5", "D5_7", "D7_PLUS").contains(value)) {
            return value;
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不支持的周期筛选");
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return "SALES_DESC";
        }
        String value = sort.trim().toUpperCase(Locale.ROOT);
        if (Arrays.asList("SALES_DESC", "PRICE_ASC", "PRICE_DESC", "CYCLE_ASC").contains(value)) {
            return value;
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不支持的排序方式");
    }

    private String normalizeOrderStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "ALL";
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        if (ORDER_STATUS_SEQUENCE.contains(value) || "ALL".equals(value)) {
            return value;
        }
        throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不支持的订单状态");
    }

    private String normalizePaymentMethod(String paymentMethod) {
        String value = paymentMethod == null || paymentMethod.trim().isEmpty() ? "ALIPAY" : paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("ALIPAY", "WECHAT").contains(value)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "支付方式不支持");
        }
        return value;
    }

    private String resolveWechatTradeType(String preferredTradeType, String openId) {
        if ("JSAPI".equalsIgnoreCase(preferredTradeType) && openId != null && !openId.trim().isEmpty()) {
            return "JSAPI";
        }
        return "NATIVE";
    }

    private String normalizeText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
        return value.trim();
    }

    private String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String renderProvider(String providerType, String providerName) {
        if ("STUDIO".equals(providerType)) {
            return providerName + " · 工作室";
        }
        return providerName + " · 平台";
    }

    private String renderPaymentMethod(String paymentMethod) {
        if ("WECHAT".equals(paymentMethod)) {
            return "微信支付";
        }
        return "支付宝支付";
    }

    private String renderOrderStatus(String status) {
        if ("PENDING_PAYMENT".equals(status)) {
            return "待付款";
        }
        if ("WAITING_SERVICE".equals(status)) {
            return "待代肝";
        }
        if ("IN_SERVICE".equals(status)) {
            return "代肝中";
        }
        if ("COMPLETED".equals(status)) {
            return "已完成";
        }
        if ("AFTER_SALE".equals(status)) {
            return "售后中";
        }
        if ("CANCELED".equals(status)) {
            return "已取消";
        }
        return status;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String previewNullable(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return "";
        }
        return ossStorageService.previewUrl(objectKey);
    }

    private String formatTime(LocalDateTime value) {
        if (value == null) {
            return "—";
        }
        return DISPLAY_TIME_FORMATTER.format(value);
    }

    private int safeNumber(Integer value) {
        return value == null ? 0 : value.intValue();
    }

    private int safePercent(Integer value) {
        if (value == null) {
            return 0;
        }
        return Math.max(0, Math.min(100, value.intValue()));
    }

    private String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "******";
        }
        if (password.length() <= 2) {
            return "******";
        }
        return password.substring(0, 1) + "******" + password.substring(password.length() - 1);
    }

    public static class OptionItem {
        private final String value;
        private final String label;

        public OptionItem(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() { return value; }
        public String getLabel() { return label; }
    }

    public static class BoostingHallMeta {
        private final List<OptionItem> categories;
        private final List<OptionItem> cycleOptions;
        private final List<OptionItem> sortOptions;

        public BoostingHallMeta(List<OptionItem> categories, List<OptionItem> cycleOptions, List<OptionItem> sortOptions) {
            this.categories = categories;
            this.cycleOptions = cycleOptions;
            this.sortOptions = sortOptions;
        }

        public List<OptionItem> getCategories() { return categories; }
        public List<OptionItem> getCycleOptions() { return cycleOptions; }
        public List<OptionItem> getSortOptions() { return sortOptions; }
    }

    public static class WechatPayResult {
        private final String orderNo;
        private final String tradeType;
        private final String codeUrl;
        private final LocalDateTime expireAt;
        private final JsapiPayParams jsapiPayParams;

        public WechatPayResult(String orderNo, String tradeType, String codeUrl, LocalDateTime expireAt, JsapiPayParams jsapiPayParams) {
            this.orderNo = orderNo;
            this.tradeType = tradeType;
            this.codeUrl = codeUrl;
            this.expireAt = expireAt;
            this.jsapiPayParams = jsapiPayParams;
        }

        public String getOrderNo() { return orderNo; }
        public String getTradeType() { return tradeType; }
        public String getCodeUrl() { return codeUrl; }
        public LocalDateTime getExpireAt() { return expireAt; }
        public JsapiPayParams getJsapiPayParams() { return jsapiPayParams; }
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

    public static class BoostingHallResult {
        private final String category;
        private final String cycle;
        private final String sort;
        private final List<BoostingServiceCard> rows;

        public BoostingHallResult(String category, String cycle, String sort, List<BoostingServiceCard> rows) {
            this.category = category;
            this.cycle = cycle;
            this.sort = sort;
            this.rows = rows;
        }

        public String getCategory() { return category; }
        public String getCycle() { return cycle; }
        public String getSort() { return sort; }
        public List<BoostingServiceCard> getRows() { return rows; }
    }

    public static class BoostingServiceCard {
        private final String serviceNo;
        private final String categoryCode;
        private final String categoryLabel;
        private final String name;
        private final String description;
        private final BigDecimal price;
        private final String cycleLabel;
        private final String guaranteeNote;
        private final String providerLabel;
        private final int salesCount;

        public BoostingServiceCard(
            String serviceNo,
            String categoryCode,
            String categoryLabel,
            String name,
            String description,
            BigDecimal price,
            String cycleLabel,
            String guaranteeNote,
            String providerLabel,
            int salesCount
        ) {
            this.serviceNo = serviceNo;
            this.categoryCode = categoryCode;
            this.categoryLabel = categoryLabel;
            this.name = name;
            this.description = description;
            this.price = price;
            this.cycleLabel = cycleLabel;
            this.guaranteeNote = guaranteeNote;
            this.providerLabel = providerLabel;
            this.salesCount = salesCount;
        }

        public String getServiceNo() { return serviceNo; }
        public String getCategoryCode() { return categoryCode; }
        public String getCategoryLabel() { return categoryLabel; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public BigDecimal getPrice() { return price; }
        public String getCycleLabel() { return cycleLabel; }
        public String getGuaranteeNote() { return guaranteeNote; }
        public String getProviderLabel() { return providerLabel; }
        public int getSalesCount() { return salesCount; }
    }

    public static class BoostingServiceDetail extends BoostingServiceCard {
        private final String cycleCode;
        private final String providerType;
        private final List<String> notices;

        public BoostingServiceDetail(
            String serviceNo,
            String categoryCode,
            String categoryLabel,
            String name,
            String description,
            BigDecimal price,
            String cycleCode,
            String cycleLabel,
            String guaranteeNote,
            String providerType,
            String providerLabel,
            int salesCount,
            List<String> notices
        ) {
            super(serviceNo, categoryCode, categoryLabel, name, description, price, cycleLabel, guaranteeNote, providerLabel, salesCount);
            this.cycleCode = cycleCode;
            this.providerType = providerType;
            this.notices = notices;
        }

        public String getCycleCode() { return cycleCode; }
        public String getProviderType() { return providerType; }
        public List<String> getNotices() { return notices; }
    }

    public static class CreateBoostingOrderCommand {
        private final String serviceNo;
        private final String gameRegion;
        private final String accountName;
        private final String accountPassword;
        private final String characterName;
        private final String specialRequirement;
        private final String paymentMethod;
        private final String agreementCode;

        public CreateBoostingOrderCommand(
            String serviceNo,
            String gameRegion,
            String accountName,
            String accountPassword,
            String characterName,
            String specialRequirement,
            String paymentMethod,
            String agreementCode
        ) {
            this.serviceNo = serviceNo;
            this.gameRegion = gameRegion;
            this.accountName = accountName;
            this.accountPassword = accountPassword;
            this.characterName = characterName;
            this.specialRequirement = specialRequirement;
            this.paymentMethod = paymentMethod;
            this.agreementCode = agreementCode;
        }

        public String getServiceNo() { return serviceNo; }
        public String getGameRegion() { return gameRegion; }
        public String getAccountName() { return accountName; }
        public String getAccountPassword() { return accountPassword; }
        public String getCharacterName() { return characterName; }
        public String getSpecialRequirement() { return specialRequirement; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getAgreementCode() { return agreementCode; }
    }

    public static class CreateBoostingOrderResult {
        private final String orderNo;
        private final String status;
        private final String message;

        public CreateBoostingOrderResult(String orderNo, String status, String message) {
            this.orderNo = orderNo;
            this.status = status;
            this.message = message;
        }

        public String getOrderNo() { return orderNo; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }

    public static class BoostingOrderCounts {
        private final int total;
        private final int pendingPayment;
        private final int waitingService;
        private final int inService;
        private final int completed;
        private final int afterSale;
        private final int canceled;

        public BoostingOrderCounts(int total, int pendingPayment, int waitingService, int inService, int completed, int afterSale, int canceled) {
            this.total = total;
            this.pendingPayment = pendingPayment;
            this.waitingService = waitingService;
            this.inService = inService;
            this.completed = completed;
            this.afterSale = afterSale;
            this.canceled = canceled;
        }

        public int getTotal() { return total; }
        public int getPendingPayment() { return pendingPayment; }
        public int getWaitingService() { return waitingService; }
        public int getInService() { return inService; }
        public int getCompleted() { return completed; }
        public int getAfterSale() { return afterSale; }
        public int getCanceled() { return canceled; }
    }

    public static class BoostingOrderCenterResult {
        private final String status;
        private final String range;
        private final BoostingOrderCounts counts;
        private final List<BoostingOrderListItem> rows;

        public BoostingOrderCenterResult(String status, String range, BoostingOrderCounts counts, List<BoostingOrderListItem> rows) {
            this.status = status;
            this.range = range;
            this.counts = counts;
            this.rows = rows;
        }

        public String getStatus() { return status; }
        public String getRange() { return range; }
        public BoostingOrderCounts getCounts() { return counts; }
        public List<BoostingOrderListItem> getRows() { return rows; }
    }

    public static class BoostingOrderListItem {
        private final String orderNo;
        private final String serviceName;
        private final BigDecimal price;
        private final String createdAt;
        private final String statusLabel;
        private final String statusCode;
        private final int progressPercent;
        private final String progressSummary;
        private final boolean canPay;
        private final boolean canConfirmComplete;
        private final boolean canApplyAfterSale;
        private final boolean canContactService;

        public BoostingOrderListItem(
            String orderNo,
            String serviceName,
            BigDecimal price,
            String createdAt,
            String statusLabel,
            String statusCode,
            int progressPercent,
            String progressSummary,
            boolean canPay,
            boolean canConfirmComplete,
            boolean canApplyAfterSale,
            boolean canContactService
        ) {
            this.orderNo = orderNo;
            this.serviceName = serviceName;
            this.price = price;
            this.createdAt = createdAt;
            this.statusLabel = statusLabel;
            this.statusCode = statusCode;
            this.progressPercent = progressPercent;
            this.progressSummary = progressSummary;
            this.canPay = canPay;
            this.canConfirmComplete = canConfirmComplete;
            this.canApplyAfterSale = canApplyAfterSale;
            this.canContactService = canContactService;
        }

        public String getOrderNo() { return orderNo; }
        public String getServiceName() { return serviceName; }
        public BigDecimal getPrice() { return price; }
        public String getCreatedAt() { return createdAt; }
        public String getStatusLabel() { return statusLabel; }
        public String getStatusCode() { return statusCode; }
        public int getProgressPercent() { return progressPercent; }
        public String getProgressSummary() { return progressSummary; }
        public boolean isCanPay() { return canPay; }
        public boolean isCanConfirmComplete() { return canConfirmComplete; }
        public boolean isCanApplyAfterSale() { return canApplyAfterSale; }
        public boolean isCanContactService() { return canContactService; }
    }

    public static class BoostingOrderDetail {
        private final String orderNo;
        private final String serviceNo;
        private final String serviceName;
        private final String serviceCategory;
        private final String serviceDescription;
        private final BigDecimal price;
        private final String cycleLabel;
        private final String guaranteeNote;
        private final String providerLabel;
        private final String statusLabel;
        private final String statusCode;
        private final String paymentMethod;
        private final String createdAt;
        private final String paidAt;
        private final String completedAt;
        private final String gameRegion;
        private final String accountName;
        private final String maskedPassword;
        private final String characterName;
        private final String specialRequirement;
        private final int progressPercent;
        private final String progressSummary;
        private final String chatGroupNo;
        private final boolean canPay;
        private final boolean canConfirmComplete;
        private final boolean canApplyAfterSale;
        private final boolean canContactService;
        private final String afterSaleReason;
        private final String afterSaleProofUrl;
        private final List<BoostingProgressItem> progressLogs;

        public BoostingOrderDetail(
            String orderNo,
            String serviceNo,
            String serviceName,
            String serviceCategory,
            String serviceDescription,
            BigDecimal price,
            String cycleLabel,
            String guaranteeNote,
            String providerLabel,
            String statusLabel,
            String statusCode,
            String paymentMethod,
            String createdAt,
            String paidAt,
            String completedAt,
            String gameRegion,
            String accountName,
            String maskedPassword,
            String characterName,
            String specialRequirement,
            int progressPercent,
            String progressSummary,
            String chatGroupNo,
            boolean canPay,
            boolean canConfirmComplete,
            boolean canApplyAfterSale,
            boolean canContactService,
            String afterSaleReason,
            String afterSaleProofUrl,
            List<BoostingProgressItem> progressLogs
        ) {
            this.orderNo = orderNo;
            this.serviceNo = serviceNo;
            this.serviceName = serviceName;
            this.serviceCategory = serviceCategory;
            this.serviceDescription = serviceDescription;
            this.price = price;
            this.cycleLabel = cycleLabel;
            this.guaranteeNote = guaranteeNote;
            this.providerLabel = providerLabel;
            this.statusLabel = statusLabel;
            this.statusCode = statusCode;
            this.paymentMethod = paymentMethod;
            this.createdAt = createdAt;
            this.paidAt = paidAt;
            this.completedAt = completedAt;
            this.gameRegion = gameRegion;
            this.accountName = accountName;
            this.maskedPassword = maskedPassword;
            this.characterName = characterName;
            this.specialRequirement = specialRequirement;
            this.progressPercent = progressPercent;
            this.progressSummary = progressSummary;
            this.chatGroupNo = chatGroupNo;
            this.canPay = canPay;
            this.canConfirmComplete = canConfirmComplete;
            this.canApplyAfterSale = canApplyAfterSale;
            this.canContactService = canContactService;
            this.afterSaleReason = afterSaleReason;
            this.afterSaleProofUrl = afterSaleProofUrl;
            this.progressLogs = progressLogs;
        }

        public String getOrderNo() { return orderNo; }
        public String getServiceNo() { return serviceNo; }
        public String getServiceName() { return serviceName; }
        public String getServiceCategory() { return serviceCategory; }
        public String getServiceDescription() { return serviceDescription; }
        public BigDecimal getPrice() { return price; }
        public String getCycleLabel() { return cycleLabel; }
        public String getGuaranteeNote() { return guaranteeNote; }
        public String getProviderLabel() { return providerLabel; }
        public String getStatusLabel() { return statusLabel; }
        public String getStatusCode() { return statusCode; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getCreatedAt() { return createdAt; }
        public String getPaidAt() { return paidAt; }
        public String getCompletedAt() { return completedAt; }
        public String getGameRegion() { return gameRegion; }
        public String getAccountName() { return accountName; }
        public String getMaskedPassword() { return maskedPassword; }
        public String getCharacterName() { return characterName; }
        public String getSpecialRequirement() { return specialRequirement; }
        public int getProgressPercent() { return progressPercent; }
        public String getProgressSummary() { return progressSummary; }
        public String getChatGroupNo() { return chatGroupNo; }
        public boolean isCanPay() { return canPay; }
        public boolean isCanConfirmComplete() { return canConfirmComplete; }
        public boolean isCanApplyAfterSale() { return canApplyAfterSale; }
        public boolean isCanContactService() { return canContactService; }
        public String getAfterSaleReason() { return afterSaleReason; }
        public String getAfterSaleProofUrl() { return afterSaleProofUrl; }
        public List<BoostingProgressItem> getProgressLogs() { return progressLogs; }
    }

    public static class BoostingProgressItem {
        private final String title;
        private final String content;
        private final int progressPercent;
        private final String createdAt;
        private final String createdBy;

        public BoostingProgressItem(String title, String content, int progressPercent, String createdAt, String createdBy) {
            this.title = title;
            this.content = content;
            this.progressPercent = progressPercent;
            this.createdAt = createdAt;
            this.createdBy = createdBy;
        }

        public String getTitle() { return title; }
        public String getContent() { return content; }
        public int getProgressPercent() { return progressPercent; }
        public String getCreatedAt() { return createdAt; }
        public String getCreatedBy() { return createdBy; }
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
