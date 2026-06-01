package com.deltatrade.platform.modules.boosting.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.common.auth.AuthContext;
import com.deltatrade.platform.modules.boosting.service.BoostingCenterService;
import com.deltatrade.platform.modules.payment.config.PlatformWechatPayProperties;
import java.math.BigDecimal;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BoostingController {

    private final BoostingCenterService boostingCenterService;
    private final PlatformWechatPayProperties wechatPayProperties;

    public BoostingController(BoostingCenterService boostingCenterService, PlatformWechatPayProperties wechatPayProperties) {
        this.boostingCenterService = boostingCenterService;
        this.wechatPayProperties = wechatPayProperties;
    }

    @GetMapping("/api/public/boosting/services/meta")
    public ApiResponse<BoostingCenterService.BoostingHallMeta> hallMeta() {
        return ApiResponse.success(boostingCenterService.getHallMeta(), MDC.get("traceId"));
    }

    @GetMapping("/api/public/boosting/services")
    public ApiResponse<BoostingCenterService.BoostingHallResult> hall(
        @RequestParam(value = "category", required = false) String category,
        @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
        @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
        @RequestParam(value = "cycle", required = false) String cycle,
        @RequestParam(value = "sort", required = false) String sort
    ) {
        return ApiResponse.success(boostingCenterService.getHall(category, minPrice, maxPrice, cycle, sort), MDC.get("traceId"));
    }

    @GetMapping("/api/public/boosting/services/{serviceNo}")
    public ApiResponse<BoostingCenterService.BoostingServiceDetail> serviceDetail(@PathVariable("serviceNo") String serviceNo) {
        return ApiResponse.success(boostingCenterService.getServiceDetail(serviceNo), MDC.get("traceId"));
    }

    @PostMapping("/api/boosting/orders")
    public ApiResponse<BoostingCenterService.CreateBoostingOrderResult> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(
            boostingCenterService.createOrder(
                AuthContext.requirePrincipal().getUserId(),
                new BoostingCenterService.CreateBoostingOrderCommand(
                    request.getServiceNo(),
                    request.getGameRegion(),
                    request.getAccountName(),
                    request.getAccountPassword(),
                    request.getCharacterName(),
                    request.getSpecialRequirement(),
                    request.getPaymentMethod(),
                    request.getAgreementCode()
                )
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/api/boosting/orders/{orderNo}/pay")
    public ApiResponse<BoostingCenterService.BoostingOrderDetail> payOrder(
        @PathVariable("orderNo") String orderNo,
        @Valid @RequestBody PayOrderRequest request
    ) {
        return ApiResponse.success(
            boostingCenterService.payOrder(AuthContext.requirePrincipal().getUserId(), orderNo, request.getPaymentMethod()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/api/boosting/orders/{orderNo}/wechat-pay")
    public ApiResponse<BoostingCenterService.WechatPayResult> createWechatPayment(
        @PathVariable("orderNo") String orderNo,
        @Valid @RequestBody WechatPayRequest request,
        javax.servlet.http.HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
            boostingCenterService.createWechatPayment(
                AuthContext.requirePrincipal().getUserId(),
                orderNo,
                resolveWechatNotifyUrl(httpServletRequest),
                request.getTradeType()
            ),
            MDC.get("traceId")
        );
    }

    @GetMapping("/api/boosting/orders")
    public ApiResponse<BoostingCenterService.BoostingOrderCenterResult> orders(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "range", required = false) String range,
        @RequestParam(value = "startDate", required = false) String startDate,
        @RequestParam(value = "endDate", required = false) String endDate
    ) {
        return ApiResponse.success(
            boostingCenterService.getOrders(AuthContext.requirePrincipal().getUserId(), status, range, startDate, endDate),
            MDC.get("traceId")
        );
    }

    @GetMapping("/api/boosting/orders/{orderNo}")
    public ApiResponse<BoostingCenterService.BoostingOrderDetail> orderDetail(@PathVariable("orderNo") String orderNo) {
        return ApiResponse.success(boostingCenterService.getOrderDetail(AuthContext.requirePrincipal().getUserId(), orderNo), MDC.get("traceId"));
    }

    @PostMapping("/api/boosting/orders/{orderNo}/after-sale")
    public ApiResponse<BoostingCenterService.BoostingOrderDetail> afterSale(
        @PathVariable("orderNo") String orderNo,
        @Valid @RequestBody AfterSaleRequest request
    ) {
        return ApiResponse.success(
            boostingCenterService.applyAfterSale(
                AuthContext.requirePrincipal().getUserId(),
                orderNo,
                request.getReason(),
                request.getProofKey()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/api/boosting/orders/{orderNo}/confirm-complete")
    public ApiResponse<BoostingCenterService.BoostingOrderDetail> confirmComplete(@PathVariable("orderNo") String orderNo) {
        return ApiResponse.success(
            boostingCenterService.confirmComplete(AuthContext.requirePrincipal().getUserId(), orderNo),
            MDC.get("traceId")
        );
    }

    public static class CreateOrderRequest {
        @NotBlank(message = "服务编号不能为空")
        private String serviceNo;
        @NotBlank(message = "游戏区服不能为空")
        private String gameRegion;
        @NotBlank(message = "账号不能为空")
        private String accountName;
        @NotBlank(message = "账号密码不能为空")
        private String accountPassword;
        @NotBlank(message = "角色名称不能为空")
        private String characterName;
        private String specialRequirement;
        @NotBlank(message = "支付方式不能为空")
        private String paymentMethod;
        @NotBlank(message = "请先同意代肝协议")
        private String agreementCode;

        public String getServiceNo() { return serviceNo; }
        public void setServiceNo(String serviceNo) { this.serviceNo = serviceNo; }
        public String getGameRegion() { return gameRegion; }
        public void setGameRegion(String gameRegion) { this.gameRegion = gameRegion; }
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        public String getAccountPassword() { return accountPassword; }
        public void setAccountPassword(String accountPassword) { this.accountPassword = accountPassword; }
        public String getCharacterName() { return characterName; }
        public void setCharacterName(String characterName) { this.characterName = characterName; }
        public String getSpecialRequirement() { return specialRequirement; }
        public void setSpecialRequirement(String specialRequirement) { this.specialRequirement = specialRequirement; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getAgreementCode() { return agreementCode; }
        public void setAgreementCode(String agreementCode) { this.agreementCode = agreementCode; }
    }

    public static class PayOrderRequest {
        @NotBlank(message = "支付方式不能为空")
        private String paymentMethod;

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }

    public static class WechatPayRequest {
        private String tradeType;

        public String getTradeType() { return tradeType; }
        public void setTradeType(String tradeType) { this.tradeType = tradeType; }
    }

    public static class AfterSaleRequest {
        @NotBlank(message = "售后原因不能为空")
        private String reason;
        @NotBlank(message = "售后凭证不能为空")
        private String proofKey;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public String getProofKey() { return proofKey; }
        public void setProofKey(String proofKey) { this.proofKey = proofKey; }
    }

    private String resolveWechatNotifyUrl(javax.servlet.http.HttpServletRequest request) {
        if (StringUtils.hasText(wechatPayProperties.getNotifyUrl())) {
            return wechatPayProperties.getNotifyUrl().trim();
        }
        String proto = java.util.Optional.ofNullable(request.getHeader("X-Forwarded-Proto")).orElse(request.getScheme());
        String host = java.util.Optional.ofNullable(request.getHeader("X-Forwarded-Host"))
            .orElseGet(() -> java.util.Optional.ofNullable(request.getHeader("Host")).orElse(request.getServerName() + ":" + request.getServerPort()));
        return proto + "://" + host + request.getContextPath() + "/api/payments/wechat/notify";
    }
}
