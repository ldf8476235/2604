package com.deltatrade.platform.modules.im.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("im_conversation")
public class ImConversationDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("conversation_no")
    private String conversationNo;

    @TableField("scene_code")
    private String sceneCode;

    @TableField("source_order_no")
    private String sourceOrderNo;

    @TableField("title")
    private String title;

    @TableField("buyer_user_id")
    private Long buyerUserId;

    @TableField("seller_user_id")
    private Long sellerUserId;

    @TableField("support_display_name")
    private String supportDisplayName;

    @TableField("last_message_excerpt")
    private String lastMessageExcerpt;

    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConversationNo() { return conversationNo; }
    public void setConversationNo(String conversationNo) { this.conversationNo = conversationNo; }
    public String getSceneCode() { return sceneCode; }
    public void setSceneCode(String sceneCode) { this.sceneCode = sceneCode; }
    public String getSourceOrderNo() { return sourceOrderNo; }
    public void setSourceOrderNo(String sourceOrderNo) { this.sourceOrderNo = sourceOrderNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getBuyerUserId() { return buyerUserId; }
    public void setBuyerUserId(Long buyerUserId) { this.buyerUserId = buyerUserId; }
    public Long getSellerUserId() { return sellerUserId; }
    public void setSellerUserId(Long sellerUserId) { this.sellerUserId = sellerUserId; }
    public String getSupportDisplayName() { return supportDisplayName; }
    public void setSupportDisplayName(String supportDisplayName) { this.supportDisplayName = supportDisplayName; }
    public String getLastMessageExcerpt() { return lastMessageExcerpt; }
    public void setLastMessageExcerpt(String lastMessageExcerpt) { this.lastMessageExcerpt = lastMessageExcerpt; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
