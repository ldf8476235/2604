package com.deltatrade.platform.modules.profile.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("distribution_order")
public class DistributionOrderDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("distribution_no")
    private String distributionNo;

    @TableField("promoter_user_id")
    private Long promoterUserId;

    @TableField("referred_user_id")
    private Long referredUserId;

    @TableField("buyer_nickname")
    private String buyerNickname;

    @TableField("source_order_no")
    private String sourceOrderNo;

    @TableField("source_order_type")
    private String sourceOrderType;

    @TableField("source_order_status")
    private String sourceOrderStatus;

    @TableField("order_amount")
    private BigDecimal orderAmount;

    @TableField("commission_rate")
    private BigDecimal commissionRate;

    @TableField("commission_amount")
    private BigDecimal commissionAmount;

    @TableField("status")
    private String status;

    @TableField("settled_at")
    private LocalDateTime settledAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDistributionNo() { return distributionNo; }
    public void setDistributionNo(String distributionNo) { this.distributionNo = distributionNo; }
    public Long getPromoterUserId() { return promoterUserId; }
    public void setPromoterUserId(Long promoterUserId) { this.promoterUserId = promoterUserId; }
    public Long getReferredUserId() { return referredUserId; }
    public void setReferredUserId(Long referredUserId) { this.referredUserId = referredUserId; }
    public String getBuyerNickname() { return buyerNickname; }
    public void setBuyerNickname(String buyerNickname) { this.buyerNickname = buyerNickname; }
    public String getSourceOrderNo() { return sourceOrderNo; }
    public void setSourceOrderNo(String sourceOrderNo) { this.sourceOrderNo = sourceOrderNo; }
    public String getSourceOrderType() { return sourceOrderType; }
    public void setSourceOrderType(String sourceOrderType) { this.sourceOrderType = sourceOrderType; }
    public String getSourceOrderStatus() { return sourceOrderStatus; }
    public void setSourceOrderStatus(String sourceOrderStatus) { this.sourceOrderStatus = sourceOrderStatus; }
    public BigDecimal getOrderAmount() { return orderAmount; }
    public void setOrderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public BigDecimal getCommissionAmount() { return commissionAmount; }
    public void setCommissionAmount(BigDecimal commissionAmount) { this.commissionAmount = commissionAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
