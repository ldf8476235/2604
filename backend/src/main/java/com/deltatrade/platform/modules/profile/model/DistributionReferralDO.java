package com.deltatrade.platform.modules.profile.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("distribution_referral")
public class DistributionReferralDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("promoter_user_id")
    private Long promoterUserId;

    @TableField("referred_user_id")
    private Long referredUserId;

    @TableField("invite_code")
    private String inviteCode;

    @TableField("status")
    private String status;

    @TableField("source_channel")
    private String sourceChannel;

    @TableField("first_paid_order_no")
    private String firstPaidOrderNo;

    @TableField("first_paid_order_type")
    private String firstPaidOrderType;

    @TableField("registered_at")
    private LocalDateTime registeredAt;

    @TableField("effective_at")
    private LocalDateTime effectiveAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPromoterUserId() { return promoterUserId; }
    public void setPromoterUserId(Long promoterUserId) { this.promoterUserId = promoterUserId; }
    public Long getReferredUserId() { return referredUserId; }
    public void setReferredUserId(Long referredUserId) { this.referredUserId = referredUserId; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSourceChannel() { return sourceChannel; }
    public void setSourceChannel(String sourceChannel) { this.sourceChannel = sourceChannel; }
    public String getFirstPaidOrderNo() { return firstPaidOrderNo; }
    public void setFirstPaidOrderNo(String firstPaidOrderNo) { this.firstPaidOrderNo = firstPaidOrderNo; }
    public String getFirstPaidOrderType() { return firstPaidOrderType; }
    public void setFirstPaidOrderType(String firstPaidOrderType) { this.firstPaidOrderType = firstPaidOrderType; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public LocalDateTime getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(LocalDateTime effectiveAt) { this.effectiveAt = effectiveAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
