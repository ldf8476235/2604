package com.deltatrade.platform.modules.order.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.common.auth.AuthContext;
import com.deltatrade.platform.modules.order.service.OrderService;
import com.deltatrade.platform.modules.payment.config.PlatformWechatPayProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PlatformWechatPayProperties wechatPayProperties;

    public OrderController(OrderService orderService, PlatformWechatPayProperties wechatPayProperties) {
        this.orderService = orderService;
        this.wechatPayProperties = wechatPayProperties;
    }

    @PostMapping
    public ApiResponse<OrderService.CreateOrderResult> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success(
            orderService.create(new OrderService.CreateOrderCommand(
                request.getListingId(),
                AuthContext.requirePrincipal().getUserId(),
                request.getTotalAmount(),
                request.getIncludeExtraItems()
            )),
            MDC.get("traceId")
        );
    }

    @GetMapping("/mine")
    public ApiResponse<List<OrderService.OrderSummary>> myOrders() {
        return ApiResponse.success(orderService.listMyOrders(AuthContext.requirePrincipal().getUserId()), MDC.get("traceId"));
    }

    @GetMapping("/center")
    public ApiResponse<OrderService.OrderCenterResult> orderCenter(
        @RequestParam(value = "role", required = false) String role,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "range", required = false) String range,
        @RequestParam(value = "startDate", required = false) String startDate,
        @RequestParam(value = "endDate", required = false) String endDate
    ) {
        return ApiResponse.success(
            orderService.getOrderCenter(AuthContext.requirePrincipal().getUserId(), role, status, range, startDate, endDate),
            MDC.get("traceId")
        );
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderService.OrderDetailResult> orderDetail(@PathVariable("orderNo") String orderNo) {
        return ApiResponse.success(orderService.getOrderDetail(AuthContext.requirePrincipal().getUserId(), orderNo), MDC.get("traceId"));
    }

    @PostMapping("/{orderNo}/wechat-pay")
    public ApiResponse<OrderService.WechatPayResult> createWechatPayment(
        @PathVariable("orderNo") String orderNo,
        @Valid @RequestBody WechatPayRequest request,
        javax.servlet.http.HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
            orderService.createWechatPayment(
                AuthContext.requirePrincipal().getUserId(),
                orderNo,
                resolveWechatNotifyUrl(httpServletRequest),
                request.getTradeType()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/{orderNo}/alipay")
    public ApiResponse<OrderService.QrPaymentResult> createAlipayPayment(@PathVariable("orderNo") String orderNo) {
        return ApiResponse.success(
            orderService.createAlipayPayment(AuthContext.requirePrincipal().getUserId(), orderNo),
            MDC.get("traceId")
        );
    }

    @PostMapping("/{orderNo}/pay")
    public ApiResponse<OrderService.OrderDetailResult> payOrder(
        @PathVariable("orderNo") String orderNo,
        @Valid @RequestBody PayOrderRequest request
    ) {
        return ApiResponse.success(
            orderService.payOrder(AuthContext.requirePrincipal().getUserId(), orderNo, request.getPaymentMethod()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<OrderService.OrderDetailResult> cancelOrder(@PathVariable("orderNo") String orderNo) {
        return ApiResponse.success(orderService.cancelOrder(AuthContext.requirePrincipal().getUserId(), orderNo), MDC.get("traceId"));
    }

    @DeleteMapping("/{orderNo}")
    public ApiResponse<Map<String, Object>> deleteOrder(@PathVariable("orderNo") String orderNo) {
        return ApiResponse.success(orderService.deleteOrderForUser(AuthContext.requirePrincipal().getUserId(), orderNo), MDC.get("traceId"));
    }

    @PostMapping("/{orderNo}/refund-requests")
    public ApiResponse<OrderService.OrderDetailResult> applyRefund(
        @PathVariable("orderNo") String orderNo,
        @RequestBody(required = false) RefundRequest request
    ) {
        return ApiResponse.success(
            orderService.applyRefund(AuthContext.requirePrincipal().getUserId(), orderNo, request == null ? null : request.getReason()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/{orderNo}/refund-review")
    public ApiResponse<OrderService.OrderDetailResult> reviewRefund(
        @PathVariable("orderNo") String orderNo,
        @Valid @RequestBody RefundReviewRequest request
    ) {
        return ApiResponse.success(
            orderService.reviewRefund(AuthContext.requirePrincipal().getUserId(), orderNo, request.getAction(), request.getNote()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/{orderNo}/after-sale")
    public ApiResponse<OrderService.OrderDetailResult> applyAfterSale(@PathVariable("orderNo") String orderNo) {
        return ApiResponse.success(orderService.applyAfterSale(AuthContext.requirePrincipal().getUserId(), orderNo), MDC.get("traceId"));
    }

    @PostMapping("/{orderNo}/confirm-complete")
    public ApiResponse<OrderService.OrderDetailResult> confirmComplete(@PathVariable("orderNo") String orderNo) {
        return ApiResponse.success(orderService.confirmComplete(AuthContext.requirePrincipal().getUserId(), orderNo), MDC.get("traceId"));
    }

    @GetMapping("/{orderNo}/certificate")
    public ApiResponse<OrderService.CertificateResult> certificate(@PathVariable("orderNo") String orderNo) {
        return ApiResponse.success(orderService.getCertificate(AuthContext.requirePrincipal().getUserId(), orderNo), MDC.get("traceId"));
    }

    public static class CreateOrderRequest {
        @NotBlank(message = "商品ID不能为空")
        private String listingId;
        @NotNull(message = "订单金额不能为空")
        @DecimalMin(value = "0.01", message = "订单金额必须大于0")
        private BigDecimal totalAmount;
        private Boolean includeExtraItems;

        public String getListingId() {
            return listingId;
        }

        public void setListingId(String listingId) {
            this.listingId = listingId;
        }

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public Boolean getIncludeExtraItems() {
            return includeExtraItems;
        }

        public void setIncludeExtraItems(Boolean includeExtraItems) {
            this.includeExtraItems = includeExtraItems;
        }
    }

    public static class WechatPayRequest {
        private String tradeType;

        public String getTradeType() {
            return tradeType;
        }

        public void setTradeType(String tradeType) {
            this.tradeType = tradeType;
        }
    }

    public static class PayOrderRequest {
        @NotBlank(message = "支付方式不能为空")
        private String paymentMethod;

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }
    }

    public static class RefundRequest {
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class RefundReviewRequest {
        @NotBlank(message = "审核动作不能为空")
        private String action;
        @NotBlank(message = "处理备注不能为空")
        private String note;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
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
