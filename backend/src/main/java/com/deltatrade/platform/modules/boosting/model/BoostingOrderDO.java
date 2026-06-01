package com.deltatrade.platform.modules.boosting.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("boosting_order")
public class BoostingOrderDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("service_no")
    private String serviceNo;

    @TableField("service_name")
    private String serviceName;

    @TableField("service_category")
    private String serviceCategory;

    @TableField("service_description")
    private String serviceDescription;

    @TableField("price")
    private BigDecimal price;

    @TableField("cycle_label")
    private String cycleLabel;

    @TableField("guarantee_note")
    private String guaranteeNote;

    @TableField("provider_type")
    private String providerType;

    @TableField("provider_name")
    private String providerName;

    @TableField("buyer_user_id")
    private Long buyerUserId;

    @TableField("buyer_nickname")
    private String buyerNickname;

    @TableField("game_region")
    private String gameRegion;

    @TableField("account_name")
    private String accountName;

    @TableField("account_password_cipher")
    private String accountPasswordCipher;

    @TableField("character_name")
    private String characterName;

    @TableField("special_requirement")
    private String specialRequirement;

    @TableField("status")
    private String status;

    @TableField("payment_method")
    private String paymentMethod;

    @TableField("payment_trade_type")
    private String paymentTradeType;

    @TableField("payment_prepay_id")
    private String paymentPrepayId;

    @TableField("payment_code_url")
    private String paymentCodeUrl;

    @TableField("payment_transaction_id")
    private String paymentTransactionId;

    @TableField("payment_expire_at")
    private LocalDateTime paymentExpireAt;

    @TableField("payment_notified_at")
    private LocalDateTime paymentNotifiedAt;

    @TableField("progress_percent")
    private Integer progressPercent;

    @TableField("progress_summary")
    private String progressSummary;

    @TableField("chat_group_no")
    private String chatGroupNo;

    @TableField("after_sale_reason")
    private String afterSaleReason;

    @TableField("after_sale_proof_key")
    private String afterSaleProofKey;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("canceled_at")
    private LocalDateTime canceledAt;

    @TableField("after_sale_at")
    private LocalDateTime afterSaleAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getServiceNo() { return serviceNo; }
    public void setServiceNo(String serviceNo) { this.serviceNo = serviceNo; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getServiceCategory() { return serviceCategory; }
    public void setServiceCategory(String serviceCategory) { this.serviceCategory = serviceCategory; }
    public String getServiceDescription() { return serviceDescription; }
    public void setServiceDescription(String serviceDescription) { this.serviceDescription = serviceDescription; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCycleLabel() { return cycleLabel; }
    public void setCycleLabel(String cycleLabel) { this.cycleLabel = cycleLabel; }
    public String getGuaranteeNote() { return guaranteeNote; }
    public void setGuaranteeNote(String guaranteeNote) { this.guaranteeNote = guaranteeNote; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public Long getBuyerUserId() { return buyerUserId; }
    public void setBuyerUserId(Long buyerUserId) { this.buyerUserId = buyerUserId; }
    public String getBuyerNickname() { return buyerNickname; }
    public void setBuyerNickname(String buyerNickname) { this.buyerNickname = buyerNickname; }
    public String getGameRegion() { return gameRegion; }
    public void setGameRegion(String gameRegion) { this.gameRegion = gameRegion; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public String getAccountPasswordCipher() { return accountPasswordCipher; }
    public void setAccountPasswordCipher(String accountPasswordCipher) { this.accountPasswordCipher = accountPasswordCipher; }
    public String getCharacterName() { return characterName; }
    public void setCharacterName(String characterName) { this.characterName = characterName; }
    public String getSpecialRequirement() { return specialRequirement; }
    public void setSpecialRequirement(String specialRequirement) { this.specialRequirement = specialRequirement; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getPaymentTradeType() { return paymentTradeType; }
    public void setPaymentTradeType(String paymentTradeType) { this.paymentTradeType = paymentTradeType; }
    public String getPaymentPrepayId() { return paymentPrepayId; }
    public void setPaymentPrepayId(String paymentPrepayId) { this.paymentPrepayId = paymentPrepayId; }
    public String getPaymentCodeUrl() { return paymentCodeUrl; }
    public void setPaymentCodeUrl(String paymentCodeUrl) { this.paymentCodeUrl = paymentCodeUrl; }
    public String getPaymentTransactionId() { return paymentTransactionId; }
    public void setPaymentTransactionId(String paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; }
    public LocalDateTime getPaymentExpireAt() { return paymentExpireAt; }
    public void setPaymentExpireAt(LocalDateTime paymentExpireAt) { this.paymentExpireAt = paymentExpireAt; }
    public LocalDateTime getPaymentNotifiedAt() { return paymentNotifiedAt; }
    public void setPaymentNotifiedAt(LocalDateTime paymentNotifiedAt) { this.paymentNotifiedAt = paymentNotifiedAt; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public String getProgressSummary() { return progressSummary; }
    public void setProgressSummary(String progressSummary) { this.progressSummary = progressSummary; }
    public String getChatGroupNo() { return chatGroupNo; }
    public void setChatGroupNo(String chatGroupNo) { this.chatGroupNo = chatGroupNo; }
    public String getAfterSaleReason() { return afterSaleReason; }
    public void setAfterSaleReason(String afterSaleReason) { this.afterSaleReason = afterSaleReason; }
    public String getAfterSaleProofKey() { return afterSaleProofKey; }
    public void setAfterSaleProofKey(String afterSaleProofKey) { this.afterSaleProofKey = afterSaleProofKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCanceledAt() { return canceledAt; }
    public void setCanceledAt(LocalDateTime canceledAt) { this.canceledAt = canceledAt; }
    public LocalDateTime getAfterSaleAt() { return afterSaleAt; }
    public void setAfterSaleAt(LocalDateTime afterSaleAt) { this.afterSaleAt = afterSaleAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
