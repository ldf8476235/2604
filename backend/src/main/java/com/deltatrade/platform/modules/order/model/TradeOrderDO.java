package com.deltatrade.platform.modules.order.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("trade_order")
public class TradeOrderDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("order_no")
    private String orderNo;

    @TableField("listing_no")
    private String listingNo;

    @TableField("listing_title")
    private String listingTitle;

    @TableField("listing_summary")
    private String listingSummary;

    @TableField("listing_cover_key")
    private String listingCoverKey;

    @TableField("buyer_user_id")
    private Long buyerUserId;

    @TableField("buyer_nickname")
    private String buyerNickname;

    @TableField("seller_user_id")
    private Long sellerUserId;

    @TableField("seller_nickname")
    private String sellerNickname;

    @TableField("seller_type")
    private String sellerType;

    @TableField("seller_display_name")
    private String sellerDisplayName;

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

    @TableField("item_amount")
    private BigDecimal itemAmount;

    @TableField("service_fee")
    private BigDecimal serviceFee;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    @TableField("deposit_amount")
    private BigDecimal depositAmount;

    @TableField("buyer_confirmed_at")
    private LocalDateTime buyerConfirmedAt;

    @TableField("seller_confirmed_at")
    private LocalDateTime sellerConfirmedAt;

    @TableField("extra_items_included")
    private Boolean extraItemsIncluded;

    @TableField("extra_items_amount")
    private BigDecimal extraItemsAmount;

    @TableField("extra_items_snapshot_json")
    private String extraItemsSnapshotJson;

    @TableField("chat_group_no")
    private String chatGroupNo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("paid_at")
    private LocalDateTime paidAt;

    @TableField("trade_started_at")
    private LocalDateTime tradeStartedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("closed_at")
    private LocalDateTime closedAt;

    @TableField("after_sale_at")
    private LocalDateTime afterSaleAt;

    @TableField("after_sale_note")
    private String afterSaleNote;

    @TableField("after_sale_proof_key")
    private String afterSaleProofKey;

    @TableField("after_sale_handled_at")
    private LocalDateTime afterSaleHandledAt;

    @TableField("refund_requested_at")
    private LocalDateTime refundRequestedAt;

    @TableField("refund_reviewed_at")
    private LocalDateTime refundReviewedAt;

    @TableField("refunded_at")
    private LocalDateTime refundedAt;

    @TableField("refund_amount")
    private BigDecimal refundAmount;

    @TableField("refund_reason")
    private String refundReason;

    @TableField("refund_review_note")
    private String refundReviewNote;

    @TableField("refund_operator_user_id")
    private Long refundOperatorUserId;

    @TableField("refund_operator_role")
    private String refundOperatorRole;

    @TableField("buyer_deleted_at")
    private LocalDateTime buyerDeletedAt;

    @TableField("seller_deleted_at")
    private LocalDateTime sellerDeletedAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getListingNo() {
        return listingNo;
    }

    public void setListingNo(String listingNo) {
        this.listingNo = listingNo;
    }

    public String getListingTitle() {
        return listingTitle;
    }

    public void setListingTitle(String listingTitle) {
        this.listingTitle = listingTitle;
    }

    public String getListingSummary() {
        return listingSummary;
    }

    public void setListingSummary(String listingSummary) {
        this.listingSummary = listingSummary;
    }

    public String getListingCoverKey() {
        return listingCoverKey;
    }

    public void setListingCoverKey(String listingCoverKey) {
        this.listingCoverKey = listingCoverKey;
    }

    public Long getBuyerUserId() {
        return buyerUserId;
    }

    public void setBuyerUserId(Long buyerUserId) {
        this.buyerUserId = buyerUserId;
    }

    public String getBuyerNickname() {
        return buyerNickname;
    }

    public void setBuyerNickname(String buyerNickname) {
        this.buyerNickname = buyerNickname;
    }

    public Long getSellerUserId() {
        return sellerUserId;
    }

    public void setSellerUserId(Long sellerUserId) {
        this.sellerUserId = sellerUserId;
    }

    public String getSellerNickname() {
        return sellerNickname;
    }

    public void setSellerNickname(String sellerNickname) {
        this.sellerNickname = sellerNickname;
    }

    public String getSellerType() {
        return sellerType;
    }

    public void setSellerType(String sellerType) {
        this.sellerType = sellerType;
    }

    public String getSellerDisplayName() {
        return sellerDisplayName;
    }

    public void setSellerDisplayName(String sellerDisplayName) {
        this.sellerDisplayName = sellerDisplayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentTradeType() {
        return paymentTradeType;
    }

    public void setPaymentTradeType(String paymentTradeType) {
        this.paymentTradeType = paymentTradeType;
    }

    public String getPaymentPrepayId() {
        return paymentPrepayId;
    }

    public void setPaymentPrepayId(String paymentPrepayId) {
        this.paymentPrepayId = paymentPrepayId;
    }

    public String getPaymentCodeUrl() {
        return paymentCodeUrl;
    }

    public void setPaymentCodeUrl(String paymentCodeUrl) {
        this.paymentCodeUrl = paymentCodeUrl;
    }

    public String getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public void setPaymentTransactionId(String paymentTransactionId) {
        this.paymentTransactionId = paymentTransactionId;
    }

    public LocalDateTime getPaymentExpireAt() {
        return paymentExpireAt;
    }

    public void setPaymentExpireAt(LocalDateTime paymentExpireAt) {
        this.paymentExpireAt = paymentExpireAt;
    }

    public LocalDateTime getPaymentNotifiedAt() {
        return paymentNotifiedAt;
    }

    public void setPaymentNotifiedAt(LocalDateTime paymentNotifiedAt) {
        this.paymentNotifiedAt = paymentNotifiedAt;
    }

    public BigDecimal getItemAmount() {
        return itemAmount;
    }

    public void setItemAmount(BigDecimal itemAmount) {
        this.itemAmount = itemAmount;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(BigDecimal serviceFee) {
        this.serviceFee = serviceFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public LocalDateTime getBuyerConfirmedAt() {
        return buyerConfirmedAt;
    }

    public void setBuyerConfirmedAt(LocalDateTime buyerConfirmedAt) {
        this.buyerConfirmedAt = buyerConfirmedAt;
    }

    public LocalDateTime getSellerConfirmedAt() {
        return sellerConfirmedAt;
    }

    public void setSellerConfirmedAt(LocalDateTime sellerConfirmedAt) {
        this.sellerConfirmedAt = sellerConfirmedAt;
    }

    public Boolean getExtraItemsIncluded() {
        return extraItemsIncluded;
    }

    public void setExtraItemsIncluded(Boolean extraItemsIncluded) {
        this.extraItemsIncluded = extraItemsIncluded;
    }

    public BigDecimal getExtraItemsAmount() {
        return extraItemsAmount;
    }

    public void setExtraItemsAmount(BigDecimal extraItemsAmount) {
        this.extraItemsAmount = extraItemsAmount;
    }

    public String getExtraItemsSnapshotJson() {
        return extraItemsSnapshotJson;
    }

    public void setExtraItemsSnapshotJson(String extraItemsSnapshotJson) {
        this.extraItemsSnapshotJson = extraItemsSnapshotJson;
    }

    public String getChatGroupNo() {
        return chatGroupNo;
    }

    public void setChatGroupNo(String chatGroupNo) {
        this.chatGroupNo = chatGroupNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getTradeStartedAt() {
        return tradeStartedAt;
    }

    public void setTradeStartedAt(LocalDateTime tradeStartedAt) {
        this.tradeStartedAt = tradeStartedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public LocalDateTime getAfterSaleAt() {
        return afterSaleAt;
    }

    public void setAfterSaleAt(LocalDateTime afterSaleAt) {
        this.afterSaleAt = afterSaleAt;
    }

    public String getAfterSaleNote() {
        return afterSaleNote;
    }

    public void setAfterSaleNote(String afterSaleNote) {
        this.afterSaleNote = afterSaleNote;
    }

    public String getAfterSaleProofKey() {
        return afterSaleProofKey;
    }

    public void setAfterSaleProofKey(String afterSaleProofKey) {
        this.afterSaleProofKey = afterSaleProofKey;
    }

    public LocalDateTime getAfterSaleHandledAt() {
        return afterSaleHandledAt;
    }

    public void setAfterSaleHandledAt(LocalDateTime afterSaleHandledAt) {
        this.afterSaleHandledAt = afterSaleHandledAt;
    }

    public LocalDateTime getRefundRequestedAt() {
        return refundRequestedAt;
    }

    public void setRefundRequestedAt(LocalDateTime refundRequestedAt) {
        this.refundRequestedAt = refundRequestedAt;
    }

    public LocalDateTime getRefundReviewedAt() {
        return refundReviewedAt;
    }

    public void setRefundReviewedAt(LocalDateTime refundReviewedAt) {
        this.refundReviewedAt = refundReviewedAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public String getRefundReviewNote() {
        return refundReviewNote;
    }

    public void setRefundReviewNote(String refundReviewNote) {
        this.refundReviewNote = refundReviewNote;
    }

    public Long getRefundOperatorUserId() {
        return refundOperatorUserId;
    }

    public void setRefundOperatorUserId(Long refundOperatorUserId) {
        this.refundOperatorUserId = refundOperatorUserId;
    }

    public String getRefundOperatorRole() {
        return refundOperatorRole;
    }

    public void setRefundOperatorRole(String refundOperatorRole) {
        this.refundOperatorRole = refundOperatorRole;
    }

    public LocalDateTime getBuyerDeletedAt() {
        return buyerDeletedAt;
    }

    public void setBuyerDeletedAt(LocalDateTime buyerDeletedAt) {
        this.buyerDeletedAt = buyerDeletedAt;
    }

    public LocalDateTime getSellerDeletedAt() {
        return sellerDeletedAt;
    }

    public void setSellerDeletedAt(LocalDateTime sellerDeletedAt) {
        this.sellerDeletedAt = sellerDeletedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
