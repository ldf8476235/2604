package com.deltatrade.platform.modules.admin.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.common.auth.AuthContext;
import com.deltatrade.platform.modules.admin.service.AdminConsoleService;
import com.deltatrade.platform.modules.admin.service.AdminIntegrationConfigService;
import com.deltatrade.platform.modules.console.service.ConsoleAccessService;
import com.deltatrade.platform.modules.guncode.service.GunCodeService;
import com.deltatrade.platform.modules.studio.service.StudioApplicationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminConsoleService adminConsoleService;
    private final AdminIntegrationConfigService adminIntegrationConfigService;
    private final ConsoleAccessService consoleAccessService;
    private final StudioApplicationService studioApplicationService;
    private final GunCodeService gunCodeService;

    public AdminController(
        AdminConsoleService adminConsoleService,
        AdminIntegrationConfigService adminIntegrationConfigService,
        ConsoleAccessService consoleAccessService,
        StudioApplicationService studioApplicationService,
        GunCodeService gunCodeService
    ) {
        this.adminConsoleService = adminConsoleService;
        this.adminIntegrationConfigService = adminIntegrationConfigService;
        this.consoleAccessService = consoleAccessService;
        this.studioApplicationService = studioApplicationService;
        this.gunCodeService = gunCodeService;
    }

    @GetMapping("/session")
    public ApiResponse<Map<String, Object>> session() {
        return ApiResponse.success(consoleAccessService.loadAdminSession(AuthContext.requirePrincipal()), MDC.get("traceId"));
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "dashboard");
        return ApiResponse.success(adminConsoleService.loadDashboard(), MDC.get("traceId"));
    }

    @GetMapping("/listings")
    public ApiResponse<Map<String, Object>> listings(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "sellerType", required = false) String sellerType,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "listing");
        return ApiResponse.success(adminConsoleService.loadListings(status, sellerType, keyword), MDC.get("traceId"));
    }

    @GetMapping("/listings/{listingNo}")
    public ApiResponse<Map<String, Object>> listingDetail(@PathVariable String listingNo) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "listing");
        return ApiResponse.success(adminConsoleService.loadListingDetail(listingNo), MDC.get("traceId"));
    }

    @PostMapping("/listings/{listingNo}/review")
    public ApiResponse<Map<String, Object>> reviewListing(
        @PathVariable String listingNo,
        @Valid @RequestBody ReviewListingRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "listing");
        return ApiResponse.success(
            adminConsoleService.reviewListing(listingNo, request.getAction(), request.getReason()),
            MDC.get("traceId")
        );
    }

    @DeleteMapping("/listings/{listingNo}")
    public ApiResponse<Map<String, Object>> deleteListing(@PathVariable String listingNo) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "listing");
        return ApiResponse.success(adminConsoleService.deleteListing(listingNo), MDC.get("traceId"));
    }

    @GetMapping("/orders")
    public ApiResponse<Map<String, Object>> orders(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "sellerType", required = false) String sellerType
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "order");
        return ApiResponse.success(adminConsoleService.loadOrders(status, sellerType), MDC.get("traceId"));
    }

    @GetMapping("/orders/{orderNo}")
    public ApiResponse<Map<String, Object>> orderDetail(@PathVariable String orderNo) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "order");
        return ApiResponse.success(adminConsoleService.loadOrderDetail(orderNo), MDC.get("traceId"));
    }

    @PostMapping("/orders/{orderNo}/force-refund")
    public ApiResponse<Map<String, Object>> forceRefund(
        @PathVariable String orderNo,
        @Valid @RequestBody ForceRefundRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "order");
        return ApiResponse.success(
            adminConsoleService.forceRefund(AuthContext.requirePrincipal().getUserId(), orderNo, request.getReason()),
            MDC.get("traceId")
        );
    }

    @GetMapping("/studios")
    public ApiResponse<Map<String, Object>> studios(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "active", required = false) String active
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "studio");
        return ApiResponse.success(adminConsoleService.loadStudios(keyword, active), MDC.get("traceId"));
    }

    @GetMapping("/studio-applications")
    public ApiResponse<Map<String, Object>> studioApplications(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "keyword", required = false) String keyword
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "studio");
        return ApiResponse.success(studioApplicationService.loadAdminApplications(status, keyword), MDC.get("traceId"));
    }

    @PostMapping("/studio-applications/{applicationNo}/review")
    public ApiResponse<Map<String, Object>> reviewStudioApplication(
        @PathVariable String applicationNo,
        @Valid @RequestBody ReviewStudioApplicationRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "studio");
        return ApiResponse.success(
            studioApplicationService.reviewApplication(applicationNo, request.getAction(), request.getReason(), AuthContext.requirePrincipal().getUserId()),
            MDC.get("traceId")
        );
    }

    @GetMapping("/studios/{studioId}")
    public ApiResponse<Map<String, Object>> studioDetail(@PathVariable Long studioId) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "studio");
        return ApiResponse.success(adminConsoleService.loadStudioDetail(studioId), MDC.get("traceId"));
    }

    @PostMapping("/studios")
    public ApiResponse<Map<String, Object>> saveStudio(@Valid @RequestBody SaveStudioRequest request) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "studio");
        return ApiResponse.success(
            adminConsoleService.saveStudio(
                request.getStudioId(),
                request.getOwnerPhone(),
                request.getStudioName(),
                request.getDescription(),
                request.getContactPhone(),
                request.getContactName(),
                request.getContactWechat(),
                request.getQualificationCode(),
                request.getQualificationMaterialKey(),
                request.getQualificationNote(),
                request.getReviewStrategy(),
                request.getShareRatio(),
                request.getActive(),
                request.getCooperationStatus()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/studios/{studioId}/policy")
    public ApiResponse<Map<String, Object>> updateStudioPolicy(
        @PathVariable Long studioId,
        @Valid @RequestBody UpdateStudioPolicyRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "studio");
        return ApiResponse.success(adminConsoleService.updateStudioPolicy(studioId, request.getReviewStrategy()), MDC.get("traceId"));
    }

    @PostMapping("/studios/{studioId}/status")
    public ApiResponse<Map<String, Object>> updateStudioStatus(
        @PathVariable Long studioId,
        @Valid @RequestBody UpdateStudioStatusRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "studio");
        return ApiResponse.success(adminConsoleService.updateStudioStatus(studioId, request.getActive()), MDC.get("traceId"));
    }

    @PostMapping("/studios/{studioId}/share-ratio")
    public ApiResponse<Map<String, Object>> updateStudioShareRatio(
        @PathVariable Long studioId,
        @Valid @RequestBody UpdateStudioShareRatioRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "studio");
        return ApiResponse.success(adminConsoleService.updateStudioShareRatio(studioId, request.getShareRatio()), MDC.get("traceId"));
    }

    @GetMapping("/boosting/services")
    public ApiResponse<Map<String, Object>> boostingServices(
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "providerType", required = false) String providerType
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "boosting");
        return ApiResponse.success(adminConsoleService.loadBoostingServices(status, providerType), MDC.get("traceId"));
    }

    @PostMapping("/boosting/services/{serviceNo}/status")
    public ApiResponse<Map<String, Object>> updateBoostingServiceStatus(
        @PathVariable String serviceNo,
        @Valid @RequestBody UpdateServiceStatusRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "boosting");
        return ApiResponse.success(adminConsoleService.updateBoostingServiceStatus(serviceNo, request.getStatus()), MDC.get("traceId"));
    }

    @GetMapping("/withdraws")
    public ApiResponse<Map<String, Object>> withdraws(@RequestParam(value = "status", required = false) String status) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "withdraw");
        return ApiResponse.success(adminConsoleService.loadWithdrawApplications(status), MDC.get("traceId"));
    }

    @PostMapping("/withdraws/{applicationNo}/review")
    public ApiResponse<Map<String, Object>> reviewWithdraw(
        @PathVariable String applicationNo,
        @Valid @RequestBody ReviewWithdrawRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "withdraw");
        return ApiResponse.success(
            adminConsoleService.reviewWithdraw(applicationNo, request.getAction(), request.getReason()),
            MDC.get("traceId")
        );
    }

    @GetMapping("/studio-withdraws")
    public ApiResponse<Map<String, Object>> studioWithdraws(@RequestParam(value = "status", required = false) String status) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "studio");
        return ApiResponse.success(adminConsoleService.loadStudioWithdrawApplications(status), MDC.get("traceId"));
    }

    @PostMapping("/studio-withdraws/{applicationNo}/review")
    public ApiResponse<Map<String, Object>> reviewStudioWithdraw(
        @PathVariable String applicationNo,
        @Valid @RequestBody ReviewWithdrawRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "studio");
        return ApiResponse.success(
            adminConsoleService.reviewStudioWithdraw(applicationNo, request.getAction(), request.getReason()),
            MDC.get("traceId")
        );
    }

    @GetMapping("/operations")
    public ApiResponse<Map<String, Object>> operations() {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(adminConsoleService.loadOperationCenter(), MDC.get("traceId"));
    }

    @GetMapping("/gun-codes")
    public ApiResponse<GunCodeService.AdminGunCodeCenter> gunCodes() {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(gunCodeService.loadAdminCenter(), MDC.get("traceId"));
    }

    @PostMapping("/gun-codes/import")
    public ApiResponse<GunCodeService.AdminGunCodeImportResult> importGunCodes(@RequestBody ImportGunCodesRequest request) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(gunCodeService.importRows(request.getRows(), request.isReplaceExisting()), MDC.get("traceId"));
    }

    @GetMapping("/integration-configs")
    public ApiResponse<Map<String, Object>> integrationConfigs() {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(adminIntegrationConfigService.loadConfigs(), MDC.get("traceId"));
    }

    @PostMapping("/integration-configs/payment")
    public ApiResponse<Map<String, Object>> savePaymentConfig(@Valid @RequestBody SavePaymentConfigRequest request) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(adminIntegrationConfigService.savePaymentConfig(request.toMap()), MDC.get("traceId"));
    }

    @PostMapping("/integration-configs/login")
    public ApiResponse<Map<String, Object>> saveLoginConfig(@Valid @RequestBody SaveLoginConfigRequest request) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(adminIntegrationConfigService.saveLoginConfig(request.toMap()), MDC.get("traceId"));
    }

    @PostMapping("/integration-configs/distribution")
    public ApiResponse<Map<String, Object>> saveDistributionConfig(@Valid @RequestBody SaveDistributionConfigRequest request) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(adminIntegrationConfigService.saveDistributionConfig(request.toMap()), MDC.get("traceId"));
    }

    @PostMapping("/integration-configs/listing-publish")
    public ApiResponse<Map<String, Object>> saveListingPublishConfig(@Valid @RequestBody SaveListingPublishConfigRequest request) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(adminIntegrationConfigService.saveListingPublishConfig(request.toMap()), MDC.get("traceId"));
    }

    @PostMapping("/operations/banners")
    public ApiResponse<Map<String, Object>> saveBanner(@Valid @RequestBody SaveBannerRequest request) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(
            adminConsoleService.saveBanner(request.getBannerId(), request.getTitle(), request.getImageKey(), request.getLinkUrl(), request.getSortNo(), request.getStatus()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/operations/shortcuts")
    public ApiResponse<Map<String, Object>> saveShortcut(@Valid @RequestBody SaveShortcutRequest request) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(
            adminConsoleService.saveShortcut(request.getShortcutId(), request.getName(), request.getIconKey(), request.getLinkUrl(), request.getSortNo(), request.getStatus()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/operations/announcements")
    public ApiResponse<Map<String, Object>> saveAnnouncement(@Valid @RequestBody SaveAnnouncementRequest request) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(
            adminConsoleService.saveAnnouncement(
                request.getAnnouncementId(),
                request.getTitle(),
                request.getContent(),
                request.getCategory(),
                request.getPinned(),
                request.getStatus()
            ),
            MDC.get("traceId")
        );
    }

    @DeleteMapping("/operations/banners/{bannerId}")
    public ApiResponse<Map<String, Object>> deleteBanner(@PathVariable Long bannerId) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(adminConsoleService.deleteBanner(bannerId), MDC.get("traceId"));
    }

    @DeleteMapping("/operations/shortcuts/{shortcutId}")
    public ApiResponse<Map<String, Object>> deleteShortcut(@PathVariable Long shortcutId) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(adminConsoleService.deleteShortcut(shortcutId), MDC.get("traceId"));
    }

    @DeleteMapping("/operations/announcements/{announcementId}")
    public ApiResponse<Map<String, Object>> deleteAnnouncement(@PathVariable Long announcementId) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(adminConsoleService.deleteAnnouncement(announcementId), MDC.get("traceId"));
    }

    @PostMapping("/operations/{type}/batch-status")
    public ApiResponse<Map<String, Object>> batchUpdateOperationStatus(
        @PathVariable String type,
        @Valid @RequestBody BatchOperationStatusRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "operation");
        return ApiResponse.success(
            adminConsoleService.batchUpdateOperationStatus(type, request.getIds(), request.getStatus()),
            MDC.get("traceId")
        );
    }

    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> users(
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "verified", required = false) String verified,
        @RequestParam(value = "studioOwner", required = false) String studioOwner
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "user");
        return ApiResponse.success(adminConsoleService.loadUsers(keyword, status, verified, studioOwner), MDC.get("traceId"));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<Map<String, Object>> userDetail(@PathVariable Long userId) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "user");
        return ApiResponse.success(adminConsoleService.loadUserDetail(userId), MDC.get("traceId"));
    }

    @PostMapping("/users/{userId}/status")
    public ApiResponse<Map<String, Object>> updateUserStatus(
        @PathVariable Long userId,
        @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "user");
        return ApiResponse.success(adminConsoleService.updateUserStatus(userId, request.getStatus(), request.getReason()), MDC.get("traceId"));
    }

    @PostMapping("/users/{userId}/reset-password")
    public ApiResponse<Map<String, Object>> resetUserPassword(
        @PathVariable Long userId,
        @Valid @RequestBody ResetUserPasswordRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "user");
        return ApiResponse.success(adminConsoleService.resetUserPassword(userId, request.getPassword()), MDC.get("traceId"));
    }

    @GetMapping("/real-name/reviews")
    public ApiResponse<Map<String, Object>> realNameReviews(@RequestParam(value = "status", required = false) String status) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "realName");
        return ApiResponse.success(adminConsoleService.loadRealNameReviews(status), MDC.get("traceId"));
    }

    @PostMapping("/real-name/reviews/{userId}")
    public ApiResponse<Map<String, Object>> reviewRealName(
        @PathVariable Long userId,
        @Valid @RequestBody ReviewRealNameRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "realName");
        return ApiResponse.success(adminConsoleService.reviewRealName(userId, request.getAction(), request.getReason()), MDC.get("traceId"));
    }

    @GetMapping("/roles")
    public ApiResponse<Map<String, Object>> roles() {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "role");
        return ApiResponse.success(adminConsoleService.loadRoles(), MDC.get("traceId"));
    }

    @PostMapping("/roles")
    public ApiResponse<Map<String, Object>> saveRole(@Valid @RequestBody SaveRoleRequest request) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "role");
        return ApiResponse.success(
            adminConsoleService.saveRole(
                request.getRoleId(),
                request.getRoleCode(),
                request.getRoleName(),
                request.getDescription(),
                request.getPermissions(),
                request.getStatus()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/roles/{roleId}/members")
    public ApiResponse<Map<String, Object>> assignRoleMembers(
        @PathVariable Long roleId,
        @Valid @RequestBody AssignRoleMembersRequest request
    ) {
        consoleAccessService.requireAdminAccess(AuthContext.requirePrincipal(), "role");
        return ApiResponse.success(adminConsoleService.assignRoleMembers(roleId, request.getUserIds()), MDC.get("traceId"));
    }

    public static class ReviewListingRequest {
        @NotBlank(message = "操作不能为空")
        private String action;
        private String reason;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class ForceRefundRequest {
        @NotBlank(message = "强制退款原因不能为空")
        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class ImportGunCodesRequest {
        private boolean replaceExisting;
        private List<GunCodeService.GunCodeImportRow> rows;

        public boolean isReplaceExisting() {
            return replaceExisting;
        }

        public void setReplaceExisting(boolean replaceExisting) {
            this.replaceExisting = replaceExisting;
        }

        public List<GunCodeService.GunCodeImportRow> getRows() {
            return rows;
        }

        public void setRows(List<GunCodeService.GunCodeImportRow> rows) {
            this.rows = rows;
        }
    }

    public static class UpdateStudioPolicyRequest {
        @NotBlank(message = "审核策略不能为空")
        private String reviewStrategy;

        public String getReviewStrategy() {
            return reviewStrategy;
        }

        public void setReviewStrategy(String reviewStrategy) {
            this.reviewStrategy = reviewStrategy;
        }
    }

    public static class SaveStudioRequest {
        private Long studioId;
        @NotBlank(message = "负责人手机号不能为空")
        private String ownerPhone;
        @NotBlank(message = "工作室名称不能为空")
        private String studioName;
        private String description;
        @NotBlank(message = "联系电话不能为空")
        private String contactPhone;
        private String contactName;
        private String contactWechat;
        private String qualificationCode;
        private String qualificationMaterialKey;
        private String qualificationNote;
        @NotBlank(message = "审核策略不能为空")
        private String reviewStrategy;
        @NotNull(message = "分润比例不能为空")
        private BigDecimal shareRatio;
        @NotNull(message = "合作状态不能为空")
        private Boolean active;
        @NotBlank(message = "合作档案状态不能为空")
        private String cooperationStatus;

        public Long getStudioId() { return studioId; }
        public void setStudioId(Long studioId) { this.studioId = studioId; }
        public String getOwnerPhone() { return ownerPhone; }
        public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
        public String getStudioName() { return studioName; }
        public void setStudioName(String studioName) { this.studioName = studioName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public String getContactName() { return contactName; }
        public void setContactName(String contactName) { this.contactName = contactName; }
        public String getContactWechat() { return contactWechat; }
        public void setContactWechat(String contactWechat) { this.contactWechat = contactWechat; }
        public String getQualificationCode() { return qualificationCode; }
        public void setQualificationCode(String qualificationCode) { this.qualificationCode = qualificationCode; }
        public String getQualificationMaterialKey() { return qualificationMaterialKey; }
        public void setQualificationMaterialKey(String qualificationMaterialKey) { this.qualificationMaterialKey = qualificationMaterialKey; }
        public String getQualificationNote() { return qualificationNote; }
        public void setQualificationNote(String qualificationNote) { this.qualificationNote = qualificationNote; }
        public String getReviewStrategy() { return reviewStrategy; }
        public void setReviewStrategy(String reviewStrategy) { this.reviewStrategy = reviewStrategy; }
        public BigDecimal getShareRatio() { return shareRatio; }
        public void setShareRatio(BigDecimal shareRatio) { this.shareRatio = shareRatio; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        public String getCooperationStatus() { return cooperationStatus; }
        public void setCooperationStatus(String cooperationStatus) { this.cooperationStatus = cooperationStatus; }
    }

    public static class UpdateStudioStatusRequest {
        @NotNull(message = "合作状态不能为空")
        private Boolean active;

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }

    public static class UpdateStudioShareRatioRequest {
        @NotNull(message = "分润比例不能为空")
        private BigDecimal shareRatio;

        public BigDecimal getShareRatio() {
            return shareRatio;
        }

        public void setShareRatio(BigDecimal shareRatio) {
            this.shareRatio = shareRatio;
        }
    }

    public static class UpdateServiceStatusRequest {
        @NotBlank(message = "服务状态不能为空")
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class ReviewWithdrawRequest {
        @NotBlank(message = "审核动作不能为空")
        private String action;
        private String reason;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class ReviewStudioApplicationRequest {
        @NotBlank(message = "审核动作不能为空")
        private String action;
        private String reason;
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class SaveBannerRequest {
        private Long bannerId;
        @NotBlank(message = "轮播标题不能为空")
        private String title;
        @NotBlank(message = "轮播图片不能为空")
        private String imageKey;
        private String linkUrl;
        private Integer sortNo;
        @NotBlank(message = "状态不能为空")
        private String status;
        public Long getBannerId() { return bannerId; }
        public void setBannerId(Long bannerId) { this.bannerId = bannerId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getImageKey() { return imageKey; }
        public void setImageKey(String imageKey) { this.imageKey = imageKey; }
        public String getLinkUrl() { return linkUrl; }
        public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
        public Integer getSortNo() { return sortNo; }
        public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class BatchOperationStatusRequest {
        @NotNull(message = "状态不能为空")
        private String status;
        @NotNull(message = "ID 列表不能为空")
        private java.util.List<Long> ids;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public java.util.List<Long> getIds() { return ids; }
        public void setIds(java.util.List<Long> ids) { this.ids = ids; }
    }

    public static class SaveShortcutRequest {
        private Long shortcutId;
        @NotBlank(message = "名称不能为空")
        private String name;
        private String iconKey;
        @NotBlank(message = "跳转链接不能为空")
        private String linkUrl;
        private Integer sortNo;
        @NotBlank(message = "状态不能为空")
        private String status;
        public Long getShortcutId() { return shortcutId; }
        public void setShortcutId(Long shortcutId) { this.shortcutId = shortcutId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getIconKey() { return iconKey; }
        public void setIconKey(String iconKey) { this.iconKey = iconKey; }
        public String getLinkUrl() { return linkUrl; }
        public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
        public Integer getSortNo() { return sortNo; }
        public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class SaveAnnouncementRequest {
        private Long announcementId;
        @NotBlank(message = "公告标题不能为空")
        private String title;
        @NotBlank(message = "公告内容不能为空")
        private String content;
        @NotBlank(message = "公告分类不能为空")
        private String category;
        @NotNull(message = "置顶状态不能为空")
        private Boolean pinned;
        @NotBlank(message = "公告状态不能为空")
        private String status;
        public Long getAnnouncementId() { return announcementId; }
        public void setAnnouncementId(Long announcementId) { this.announcementId = announcementId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public Boolean getPinned() { return pinned; }
        public void setPinned(Boolean pinned) { this.pinned = pinned; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class SavePaymentConfigRequest {
        private Boolean wechatEnabled;
        private Boolean wechatMockMode;
        private String wechatAppId;
        private String wechatMchId;
        private String wechatNotifyUrl;
        private Boolean alipayEnabled;
        private Boolean alipayMockMode;
        private String alipayAppId;
        private String alipayMerchantNo;
        private String alipayNotifyUrl;

        public Map<String, Object> toMap() {
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
            result.put("wechatEnabled", wechatEnabled);
            result.put("wechatMockMode", wechatMockMode);
            result.put("wechatAppId", wechatAppId);
            result.put("wechatMchId", wechatMchId);
            result.put("wechatNotifyUrl", wechatNotifyUrl);
            result.put("alipayEnabled", alipayEnabled);
            result.put("alipayMockMode", alipayMockMode);
            result.put("alipayAppId", alipayAppId);
            result.put("alipayMerchantNo", alipayMerchantNo);
            result.put("alipayNotifyUrl", alipayNotifyUrl);
            return result;
        }

        public Boolean getWechatEnabled() { return wechatEnabled; }
        public void setWechatEnabled(Boolean wechatEnabled) { this.wechatEnabled = wechatEnabled; }
        public Boolean getWechatMockMode() { return wechatMockMode; }
        public void setWechatMockMode(Boolean wechatMockMode) { this.wechatMockMode = wechatMockMode; }
        public String getWechatAppId() { return wechatAppId; }
        public void setWechatAppId(String wechatAppId) { this.wechatAppId = wechatAppId; }
        public String getWechatMchId() { return wechatMchId; }
        public void setWechatMchId(String wechatMchId) { this.wechatMchId = wechatMchId; }
        public String getWechatNotifyUrl() { return wechatNotifyUrl; }
        public void setWechatNotifyUrl(String wechatNotifyUrl) { this.wechatNotifyUrl = wechatNotifyUrl; }
        public Boolean getAlipayEnabled() { return alipayEnabled; }
        public void setAlipayEnabled(Boolean alipayEnabled) { this.alipayEnabled = alipayEnabled; }
        public Boolean getAlipayMockMode() { return alipayMockMode; }
        public void setAlipayMockMode(Boolean alipayMockMode) { this.alipayMockMode = alipayMockMode; }
        public String getAlipayAppId() { return alipayAppId; }
        public void setAlipayAppId(String alipayAppId) { this.alipayAppId = alipayAppId; }
        public String getAlipayMerchantNo() { return alipayMerchantNo; }
        public void setAlipayMerchantNo(String alipayMerchantNo) { this.alipayMerchantNo = alipayMerchantNo; }
        public String getAlipayNotifyUrl() { return alipayNotifyUrl; }
        public void setAlipayNotifyUrl(String alipayNotifyUrl) { this.alipayNotifyUrl = alipayNotifyUrl; }
    }

    public static class SaveLoginConfigRequest {
        private Boolean smsEnabled;
        private Boolean smsMockMode;
        private String smsSignName;
        private String smsTemplateCode;
        private Boolean wechatOpenEnabled;
        private Boolean wechatOpenMockMode;
        private String wechatOpenAppId;
        private String wechatOpenRedirectUri;

        public Map<String, Object> toMap() {
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
            result.put("smsEnabled", smsEnabled);
            result.put("smsMockMode", smsMockMode);
            result.put("smsSignName", smsSignName);
            result.put("smsTemplateCode", smsTemplateCode);
            result.put("wechatOpenEnabled", wechatOpenEnabled);
            result.put("wechatOpenMockMode", wechatOpenMockMode);
            result.put("wechatOpenAppId", wechatOpenAppId);
            result.put("wechatOpenRedirectUri", wechatOpenRedirectUri);
            return result;
        }

        public Boolean getSmsEnabled() { return smsEnabled; }
        public void setSmsEnabled(Boolean smsEnabled) { this.smsEnabled = smsEnabled; }
        public Boolean getSmsMockMode() { return smsMockMode; }
        public void setSmsMockMode(Boolean smsMockMode) { this.smsMockMode = smsMockMode; }
        public String getSmsSignName() { return smsSignName; }
        public void setSmsSignName(String smsSignName) { this.smsSignName = smsSignName; }
        public String getSmsTemplateCode() { return smsTemplateCode; }
        public void setSmsTemplateCode(String smsTemplateCode) { this.smsTemplateCode = smsTemplateCode; }
        public Boolean getWechatOpenEnabled() { return wechatOpenEnabled; }
        public void setWechatOpenEnabled(Boolean wechatOpenEnabled) { this.wechatOpenEnabled = wechatOpenEnabled; }
        public Boolean getWechatOpenMockMode() { return wechatOpenMockMode; }
        public void setWechatOpenMockMode(Boolean wechatOpenMockMode) { this.wechatOpenMockMode = wechatOpenMockMode; }
        public String getWechatOpenAppId() { return wechatOpenAppId; }
        public void setWechatOpenAppId(String wechatOpenAppId) { this.wechatOpenAppId = wechatOpenAppId; }
        public String getWechatOpenRedirectUri() { return wechatOpenRedirectUri; }
        public void setWechatOpenRedirectUri(String wechatOpenRedirectUri) { this.wechatOpenRedirectUri = wechatOpenRedirectUri; }
    }

    public static class SaveDistributionConfigRequest {
        private Boolean autoEnableAfterVerified;
        private String defaultTradeCommissionRate;
        private String defaultBoostingCommissionRate;

        public Map<String, Object> toMap() {
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
            result.put("autoEnableAfterVerified", autoEnableAfterVerified);
            result.put("defaultTradeCommissionRate", defaultTradeCommissionRate);
            result.put("defaultBoostingCommissionRate", defaultBoostingCommissionRate);
            return result;
        }

        public Boolean getAutoEnableAfterVerified() { return autoEnableAfterVerified; }
        public void setAutoEnableAfterVerified(Boolean autoEnableAfterVerified) { this.autoEnableAfterVerified = autoEnableAfterVerified; }
        public String getDefaultTradeCommissionRate() { return defaultTradeCommissionRate; }
        public void setDefaultTradeCommissionRate(String defaultTradeCommissionRate) { this.defaultTradeCommissionRate = defaultTradeCommissionRate; }
        public String getDefaultBoostingCommissionRate() { return defaultBoostingCommissionRate; }
        public void setDefaultBoostingCommissionRate(String defaultBoostingCommissionRate) { this.defaultBoostingCommissionRate = defaultBoostingCommissionRate; }
    }

    public static class SaveListingPublishConfigRequest {
        private String defaultExchangeRate;
        private String personalSellerCommissionRate;

        public Map<String, Object> toMap() {
            java.util.Map<String, Object> result = new java.util.LinkedHashMap<String, Object>();
            result.put("defaultExchangeRate", defaultExchangeRate);
            result.put("personalSellerCommissionRate", personalSellerCommissionRate);
            return result;
        }

        public String getDefaultExchangeRate() { return defaultExchangeRate; }
        public void setDefaultExchangeRate(String defaultExchangeRate) { this.defaultExchangeRate = defaultExchangeRate; }
        public String getPersonalSellerCommissionRate() { return personalSellerCommissionRate; }
        public void setPersonalSellerCommissionRate(String personalSellerCommissionRate) { this.personalSellerCommissionRate = personalSellerCommissionRate; }
    }

    public static class UpdateUserStatusRequest {
        @NotBlank(message = "账号状态不能为空")
        private String status;
        private String reason;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ResetUserPasswordRequest {
        @NotBlank(message = "新密码不能为空")
        private String password;

        public String getPassword() { return password; }

        public void setPassword(String password) { this.password = password; }
    }

    public static class ReviewRealNameRequest {
        @NotBlank(message = "审核动作不能为空")
        private String action;
        private String reason;
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class SaveRoleRequest {
        private Long roleId;
        @NotBlank(message = "角色编码不能为空")
        private String roleCode;
        @NotBlank(message = "角色名称不能为空")
        private String roleName;
        private String description;
        @NotNull(message = "权限不能为空")
        private java.util.List<String> permissions;
        @NotBlank(message = "状态不能为空")
        private String status;
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public java.util.List<String> getPermissions() { return permissions; }
        public void setPermissions(java.util.List<String> permissions) { this.permissions = permissions; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class AssignRoleMembersRequest {
        @NotNull(message = "成员列表不能为空")
        private java.util.List<Long> userIds;
        public java.util.List<Long> getUserIds() { return userIds; }
        public void setUserIds(java.util.List<Long> userIds) { this.userIds = userIds; }
    }
}
