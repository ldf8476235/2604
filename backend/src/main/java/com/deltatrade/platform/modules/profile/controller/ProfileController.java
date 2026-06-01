package com.deltatrade.platform.modules.profile.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.common.auth.AuthContext;
import com.deltatrade.platform.modules.payment.config.PlatformWechatPayProperties;
import com.deltatrade.platform.modules.profile.service.DistributionService;
import com.deltatrade.platform.modules.profile.service.ProfileService;
import com.deltatrade.platform.modules.studio.service.StudioApplicationService;
import java.math.BigDecimal;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
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
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final DistributionService distributionService;
    private final StudioApplicationService studioApplicationService;
    private final PlatformWechatPayProperties wechatPayProperties;

    public ProfileController(
        ProfileService profileService,
        DistributionService distributionService,
        StudioApplicationService studioApplicationService,
        PlatformWechatPayProperties wechatPayProperties
    ) {
        this.profileService = profileService;
        this.distributionService = distributionService;
        this.studioApplicationService = studioApplicationService;
        this.wechatPayProperties = wechatPayProperties;
    }

    @GetMapping("/wallet")
    public ApiResponse<ProfileService.WalletOverview> walletOverview() {
        return ApiResponse.success(profileService.getWalletOverview(AuthContext.requirePrincipal().getUserId()), MDC.get("traceId"));
    }

    @PostMapping("/wallet/recharge")
    public ApiResponse<ProfileService.WalletOverview> recharge(@Valid @RequestBody RechargeRequest request) {
        return ApiResponse.success(
            profileService.recharge(AuthContext.requirePrincipal().getUserId(), request.getAmount(), request.getChannel()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/wallet/wechat-recharge")
    public ApiResponse<ProfileService.WalletRechargePayResult> createWechatRecharge(
        @Valid @RequestBody WechatRechargeRequest request,
        javax.servlet.http.HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(
            profileService.createWechatRecharge(
                AuthContext.requirePrincipal().getUserId(),
                request.getAmount(),
                resolveWechatNotifyUrl(httpServletRequest),
                request.getTradeType()
            ),
            MDC.get("traceId")
        );
    }

    @GetMapping("/wallet/recharge-orders/{rechargeNo}")
    public ApiResponse<ProfileService.WalletRechargeStatus> rechargeStatus(@PathVariable("rechargeNo") String rechargeNo) {
        return ApiResponse.success(
            profileService.getWechatRechargeStatus(AuthContext.requirePrincipal().getUserId(), rechargeNo),
            MDC.get("traceId")
        );
    }

    @PostMapping("/wallet/withdraw-account")
    public ApiResponse<ProfileService.WithdrawAccountView> bindWithdrawAccount(@Valid @RequestBody BindWithdrawAccountRequest request) {
        return ApiResponse.success(
            profileService.bindWithdrawAccount(
                AuthContext.requirePrincipal().getUserId(),
                request.getChannel(),
                request.getAccountName(),
                request.getAccountNo(),
                request.getQrCodeKey()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/wallet/withdraw")
    public ApiResponse<ProfileService.WalletOverview> withdraw(@Valid @RequestBody WithdrawRequest request) {
        return ApiResponse.success(
            profileService.applyWithdraw(AuthContext.requirePrincipal().getUserId(), request.getAmount()),
            MDC.get("traceId")
        );
    }

    @GetMapping("/messages")
    public ApiResponse<ProfileService.MessageCenterResult> messageCenter(@RequestParam(value = "category", required = false) String category) {
        return ApiResponse.success(profileService.getMessageCenter(AuthContext.requirePrincipal().getUserId(), category), MDC.get("traceId"));
    }

    @GetMapping("/notifications/summary")
    public ApiResponse<ProfileService.NotificationSummary> notificationSummary() {
        return ApiResponse.success(profileService.getNotificationSummary(AuthContext.requirePrincipal().getUserId()), MDC.get("traceId"));
    }

    @GetMapping("/coupons")
    public ApiResponse<ProfileService.CouponCenterResult> couponCenter() {
        return ApiResponse.success(profileService.getCouponCenter(AuthContext.requirePrincipal().getUserId()), MDC.get("traceId"));
    }

    @GetMapping("/distribution")
    public ApiResponse<DistributionService.DistributionCenterResult> distributionCenter(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "range", required = false) String range,
        @RequestParam(value = "startDate", required = false) String startDate,
        @RequestParam(value = "endDate", required = false) String endDate
    ) {
        return ApiResponse.success(
            distributionService.getDistributionCenter(AuthContext.requirePrincipal().getUserId(), keyword, range, startDate, endDate),
            MDC.get("traceId")
        );
    }

    @PostMapping("/distribution/link")
    public ApiResponse<DistributionService.InviteLinkResult> generateDistributionLink(@Valid @RequestBody GenerateDistributionLinkRequest request) {
        return ApiResponse.success(
            distributionService.generateInviteLink(AuthContext.requirePrincipal().getUserId(), request.isRegenerate()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/messages/read")
    public ApiResponse<ProfileService.MessageCenterResult> markRead(@Valid @RequestBody MessageActionRequest request) {
        return ApiResponse.success(
            profileService.markMessagesRead(AuthContext.requirePrincipal().getUserId(), request.getMessageIds(), request.getCategory()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/messages/read-all")
    public ApiResponse<ProfileService.MessageCenterResult> markReadAll(@Valid @RequestBody MessageReadAllRequest request) {
        return ApiResponse.success(
            profileService.markAllMessagesRead(AuthContext.requirePrincipal().getUserId(), request.getCategory()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/messages/delete")
    public ApiResponse<ProfileService.MessageCenterResult> deleteMessages(@Valid @RequestBody MessageActionRequest request) {
        return ApiResponse.success(
            profileService.deleteMessages(AuthContext.requirePrincipal().getUserId(), request.getMessageIds(), request.getCategory()),
            MDC.get("traceId")
        );
    }

    @GetMapping("/studio/application")
    public ApiResponse<StudioApplicationService.StudioApplicationView> studioApplication() {
        return ApiResponse.success(
            studioApplicationService.loadMyApplication(AuthContext.requirePrincipal().getUserId()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/studio/application")
    public ApiResponse<StudioApplicationService.StudioApplicationView> submitStudioApplication(
        @Valid @RequestBody SubmitStudioApplicationRequest request
    ) {
        return ApiResponse.success(
            studioApplicationService.submitApplication(
                AuthContext.requirePrincipal().getUserId(),
                request.getStudioName(),
                request.getQualificationCode(),
                request.getQualificationNote(),
                request.getContactName(),
                request.getContactPhone(),
                request.getQualificationMaterialKey()
            ),
            MDC.get("traceId")
        );
    }

    public static class RechargeRequest {
        @NotNull(message = "充值金额不能为空")
        @DecimalMin(value = "10.00", message = "充值金额不能低于 10 元")
        private BigDecimal amount;

        @NotBlank(message = "充值渠道不能为空")
        private String channel;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
    }

    public static class WechatRechargeRequest {
        @NotNull(message = "充值金额不能为空")
        @DecimalMin(value = "10.00", message = "充值金额不能低于 10 元")
        private BigDecimal amount;

        private String tradeType;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getTradeType() { return tradeType; }
        public void setTradeType(String tradeType) { this.tradeType = tradeType; }
    }

    public static class BindWithdrawAccountRequest {
        @NotBlank(message = "提现渠道不能为空")
        private String channel;

        @NotBlank(message = "账户姓名不能为空")
        private String accountName;

        @NotBlank(message = "账户号不能为空")
        private String accountNo;

        private String qrCodeKey;

        public String getChannel() { return channel; }
        public void setChannel(String channel) { this.channel = channel; }
        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }
        public String getAccountNo() { return accountNo; }
        public void setAccountNo(String accountNo) { this.accountNo = accountNo; }
        public String getQrCodeKey() { return qrCodeKey; }
        public void setQrCodeKey(String qrCodeKey) { this.qrCodeKey = qrCodeKey; }
    }

    public static class WithdrawRequest {
        @NotNull(message = "提现金额不能为空")
        @DecimalMin(value = "10.00", message = "提现金额不能低于 10 元")
        private BigDecimal amount;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    public static class MessageActionRequest {
        @NotEmpty(message = "消息不能为空")
        private List<Long> messageIds;

        private String category;

        public List<Long> getMessageIds() { return messageIds; }
        public void setMessageIds(List<Long> messageIds) { this.messageIds = messageIds; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    public static class MessageReadAllRequest {
        private String category;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }

    public static class GenerateDistributionLinkRequest {
        private boolean regenerate;

        public boolean isRegenerate() { return regenerate; }
        public void setRegenerate(boolean regenerate) { this.regenerate = regenerate; }
    }

    public static class SubmitStudioApplicationRequest {
        @NotBlank(message = "工作室名称不能为空")
        private String studioName;

        @NotBlank(message = "统一社会信用代码或主体说明不能为空")
        private String qualificationCode;

        private String qualificationNote;

        @NotBlank(message = "联系人不能为空")
        private String contactName;

        @NotBlank(message = "联系电话不能为空")
        private String contactPhone;

        @NotBlank(message = "请上传营业执照或证明材料")
        private String qualificationMaterialKey;

        public String getStudioName() { return studioName; }
        public void setStudioName(String studioName) { this.studioName = studioName; }
        public String getQualificationCode() { return qualificationCode; }
        public void setQualificationCode(String qualificationCode) { this.qualificationCode = qualificationCode; }
        public String getQualificationNote() { return qualificationNote; }
        public void setQualificationNote(String qualificationNote) { this.qualificationNote = qualificationNote; }
        public String getContactName() { return contactName; }
        public void setContactName(String contactName) { this.contactName = contactName; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public String getQualificationMaterialKey() { return qualificationMaterialKey; }
        public void setQualificationMaterialKey(String qualificationMaterialKey) { this.qualificationMaterialKey = qualificationMaterialKey; }
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
