package com.deltatrade.platform.modules.payment.service;

import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.boosting.service.BoostingCenterService;
import com.deltatrade.platform.modules.order.service.OrderService;
import com.deltatrade.platform.modules.profile.service.ProfileService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WechatPaymentCallbackService {

    private static final Logger log = LoggerFactory.getLogger(WechatPaymentCallbackService.class);

    private final WechatPayGateway wechatPayGateway;
    private final OrderService orderService;
    private final BoostingCenterService boostingCenterService;
    private final ProfileService profileService;

    public WechatPaymentCallbackService(
        WechatPayGateway wechatPayGateway,
        OrderService orderService,
        BoostingCenterService boostingCenterService,
        ProfileService profileService
    ) {
        this.wechatPayGateway = wechatPayGateway;
        this.orderService = orderService;
        this.boostingCenterService = boostingCenterService;
        this.profileService = profileService;
    }

    @Transactional
    public void handleNotify(String requestBody) {
        long startedAt = System.currentTimeMillis();
        WechatPayGateway.PaymentNotification notification = wechatPayGateway.decryptNotification(requestBody);
        if (!"TRANSACTION.SUCCESS".equalsIgnoreCase(notification.getEventType())) {
            log.info("wechat pay notify ignored eventType={} orderNo={} tradeState={}",
                notification.getEventType(), notification.getOutTradeNo(), notification.getTradeState());
            return;
        }
        if (notification.getOutTradeNo() == null || notification.getOutTradeNo().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付回调缺少订单号");
        }
        WechatPayGateway.TransactionResult queryResult = wechatPayGateway.queryOrder(notification.getOutTradeNo());
        if (!"SUCCESS".equalsIgnoreCase(queryResult.getTradeState())) {
            log.warn("wechat pay notify query not success orderNo={} tradeState={} costMs={}",
                notification.getOutTradeNo(), queryResult.getTradeState(), System.currentTimeMillis() - startedAt);
            return;
        }
        String attach = queryResult.getAttach();
        LocalDateTime paidAt = queryResult.getSuccessTime();
        if ("BOOSTING".equalsIgnoreCase(attach)) {
            boostingCenterService.applyWechatPaymentSuccess(queryResult.getOutTradeNo(), queryResult.getTransactionId(), paidAt);
            log.info("wechat pay notify applied target=boosting orderNo={} transactionId={} costMs={}",
                queryResult.getOutTradeNo(), queryResult.getTransactionId(), System.currentTimeMillis() - startedAt);
            return;
        }
        if ("WALLET_RECHARGE".equalsIgnoreCase(attach)) {
            profileService.applyWechatRechargeSuccess(queryResult.getOutTradeNo(), queryResult.getTransactionId(), paidAt);
            log.info("wechat pay notify applied target=walletRecharge rechargeNo={} transactionId={} costMs={}",
                queryResult.getOutTradeNo(), queryResult.getTransactionId(), System.currentTimeMillis() - startedAt);
            return;
        }
        orderService.applyWechatPaymentSuccess(queryResult.getOutTradeNo(), queryResult.getTransactionId(), paidAt);
        log.info("wechat pay notify applied target=trade orderNo={} transactionId={} costMs={}",
            queryResult.getOutTradeNo(), queryResult.getTransactionId(), System.currentTimeMillis() - startedAt);
    }
}
