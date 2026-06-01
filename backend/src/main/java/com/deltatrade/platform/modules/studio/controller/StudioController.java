package com.deltatrade.platform.modules.studio.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.common.auth.AuthContext;
import com.deltatrade.platform.modules.console.service.ConsoleAccessService;
import com.deltatrade.platform.modules.listing.service.ListingPublishService;
import com.deltatrade.platform.modules.studio.service.StudioConsoleService;
import java.math.BigDecimal;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/studio")
public class StudioController {

    private final StudioConsoleService studioConsoleService;
    private final ConsoleAccessService consoleAccessService;

    public StudioController(StudioConsoleService studioConsoleService, ConsoleAccessService consoleAccessService) {
        this.studioConsoleService = studioConsoleService;
        this.consoleAccessService = consoleAccessService;
    }

    @GetMapping("/session")
    public ApiResponse<Map<String, Object>> session() {
        return ApiResponse.success(consoleAccessService.loadStudioSession(AuthContext.requirePrincipal()), MDC.get("traceId"));
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(studioConsoleService.loadDashboard(AuthContext.requirePrincipal().getUserId()), MDC.get("traceId"));
    }

    @GetMapping("/listings")
    public ApiResponse<Map<String, Object>> listings(@RequestParam(value = "status", required = false) String status) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(studioConsoleService.loadListings(AuthContext.requirePrincipal().getUserId(), status), MDC.get("traceId"));
    }

    @GetMapping("/listings/{listingNo}")
    public ApiResponse<Map<String, Object>> listingDetail(@PathVariable String listingNo) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(studioConsoleService.loadListingDetail(AuthContext.requirePrincipal().getUserId(), listingNo), MDC.get("traceId"));
    }

    @PostMapping("/listings/{listingNo}/withdraw")
    public ApiResponse<ListingPublishService.ActionResult> withdraw(@PathVariable String listingNo) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(studioConsoleService.withdrawListing(AuthContext.requirePrincipal(), listingNo), MDC.get("traceId"));
    }

    @PostMapping("/listings/{listingNo}/resubmit")
    public ApiResponse<ListingPublishService.ActionResult> resubmit(@PathVariable String listingNo) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(studioConsoleService.resubmitListing(AuthContext.requirePrincipal(), listingNo), MDC.get("traceId"));
    }

    @GetMapping("/orders")
    public ApiResponse<Map<String, Object>> orders(@RequestParam(value = "status", required = false) String status) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(studioConsoleService.loadOrders(AuthContext.requirePrincipal().getUserId(), status), MDC.get("traceId"));
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResponse<Map<String, Object>> orderDetail(@PathVariable String orderNo) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(studioConsoleService.loadOrderDetail(AuthContext.requirePrincipal().getUserId(), orderNo), MDC.get("traceId"));
    }

    @PostMapping("/orders/{orderNo}/after-sale")
    public ApiResponse<Map<String, Object>> handleAfterSale(@PathVariable String orderNo, @Valid @RequestBody HandleAfterSaleRequest request) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(
            studioConsoleService.handleAfterSale(
                AuthContext.requirePrincipal().getUserId(),
                orderNo,
                request.getAction(),
                request.getNote(),
                request.getProofKey()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/orders/{orderNo}/refund-review")
    public ApiResponse<Map<String, Object>> reviewRefund(@PathVariable String orderNo, @Valid @RequestBody RefundReviewRequest request) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(
            studioConsoleService.reviewRefund(AuthContext.requirePrincipal().getUserId(), orderNo, request.getAction(), request.getNote()),
            MDC.get("traceId")
        );
    }

    @GetMapping("/operators")
    public ApiResponse<Map<String, Object>> operators(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(
            studioConsoleService.loadOperators(AuthContext.requirePrincipal().getUserId(), status, keyword),
            MDC.get("traceId")
        );
    }

    @PostMapping("/operators")
    public ApiResponse<Map<String, Object>> saveOperator(@Valid @RequestBody SaveStudioOperatorRequest request) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(
            studioConsoleService.saveOperator(
                AuthContext.requirePrincipal().getUserId(),
                request.getOperatorId(),
                request.getName(),
                request.getPhone(),
                request.getPermissions(),
                request.getPassword()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/operators/{operatorId}/status")
    public ApiResponse<Map<String, Object>> updateOperatorStatus(
        @PathVariable Long operatorId,
        @Valid @RequestBody UpdateOperatorStatusRequest request
    ) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(
            studioConsoleService.updateOperatorStatus(AuthContext.requirePrincipal().getUserId(), operatorId, request.getStatus()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/operators/{operatorId}/reset-password")
    public ApiResponse<Map<String, Object>> resetOperatorPassword(
        @PathVariable Long operatorId,
        @Valid @RequestBody ResetOperatorPasswordRequest request
    ) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(
            studioConsoleService.resetOperatorPassword(AuthContext.requirePrincipal().getUserId(), operatorId, request.getPassword()),
            MDC.get("traceId")
        );
    }

    @GetMapping("/settlements")
    public ApiResponse<Map<String, Object>> settlements(@RequestParam(value = "range", required = false) String range) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(studioConsoleService.loadSettlements(AuthContext.requirePrincipal().getUserId(), range), MDC.get("traceId"));
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(studioConsoleService.loadProfile(AuthContext.requirePrincipal().getUserId()), MDC.get("traceId"));
    }

    @PostMapping("/profile")
    public ApiResponse<Map<String, Object>> saveProfile(@Valid @RequestBody SaveStudioProfileRequest request) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(
            studioConsoleService.saveProfile(
                AuthContext.requirePrincipal().getUserId(),
                request.getStudioName(),
                request.getDescription(),
                request.getContactPhone(),
                request.getContactWechat()
            ),
            MDC.get("traceId")
        );
    }

    @GetMapping("/finance")
    public ApiResponse<Map<String, Object>> finance(@RequestParam(value = "range", required = false) String range) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(studioConsoleService.loadFinance(AuthContext.requirePrincipal().getUserId(), range), MDC.get("traceId"));
    }

    @PostMapping("/finance/payout-account")
    public ApiResponse<Map<String, Object>> savePayoutAccount(@Valid @RequestBody SavePayoutAccountRequest request) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(
            studioConsoleService.savePayoutAccount(
                AuthContext.requirePrincipal().getUserId(),
                request.getChannel(),
                request.getAccountName(),
                request.getAccountNo()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/finance/withdraw")
    public ApiResponse<Map<String, Object>> applyWithdraw(@Valid @RequestBody ApplyWithdrawRequest request) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(
            studioConsoleService.applyWithdraw(AuthContext.requirePrincipal().getUserId(), request.getAmount()),
            MDC.get("traceId")
        );
    }

    @GetMapping("/finance/statement")
    public ApiResponse<Map<String, Object>> statement(@RequestParam(value = "range", required = false) String range) {
        consoleAccessService.requireStudioAccess(AuthContext.requirePrincipal());
        return ApiResponse.success(studioConsoleService.generateStatement(AuthContext.requirePrincipal().getUserId(), range), MDC.get("traceId"));
    }

    public static class SavePayoutAccountRequest {
        @NotBlank(message = "收款渠道不能为空")
        private String channel;
        @NotBlank(message = "收款姓名不能为空")
        private String accountName;
        @NotBlank(message = "收款账号不能为空")
        private String accountNo;
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        public String getAccountNo() { return accountNo; }
        public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
    }

    public static class SaveStudioProfileRequest {
        @NotBlank(message = "工作室名称不能为空")
        private String studioName;
        private String description;
        @NotBlank(message = "联系电话不能为空")
        private String contactPhone;
        private String contactWechat;

        public String getStudioName() { return studioName; }
        public void setStudioName(String studioName) { this.studioName = studioName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public String getContactWechat() { return contactWechat; }
        public void setContactWechat(String contactWechat) { this.contactWechat = contactWechat; }
    }

    public static class ApplyWithdrawRequest {
        @NotNull(message = "提现金额不能为空")
        @DecimalMin(value = "10.00", message = "提现金额不能低于 10 元")
        private BigDecimal amount;
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    public static class HandleAfterSaleRequest {
        @NotBlank(message = "处理动作不能为空")
        private String action;
        @NotBlank(message = "处理备注不能为空")
        private String note;
        private String proofKey;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public String getProofKey() { return proofKey; }
        public void setProofKey(String proofKey) { this.proofKey = proofKey; }
    }

    public static class RefundReviewRequest {
        @NotBlank(message = "审核动作不能为空")
        private String action;
        @NotBlank(message = "处理备注不能为空")
        private String note;

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    public static class SaveStudioOperatorRequest {
        private Long operatorId;
        @NotBlank(message = "操作员姓名不能为空")
        private String name;
        @NotBlank(message = "操作员手机号不能为空")
        private String phone;
        private java.util.List<String> permissions;
        private String password;

        public Long getOperatorId() { return operatorId; }
        public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public java.util.List<String> getPermissions() { return permissions; }
        public void setPermissions(java.util.List<String> permissions) { this.permissions = permissions; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class UpdateOperatorStatusRequest {
        @NotBlank(message = "操作员状态不能为空")
        private String status;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class ResetOperatorPasswordRequest {
        @NotBlank(message = "新密码不能为空")
        private String password;

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
