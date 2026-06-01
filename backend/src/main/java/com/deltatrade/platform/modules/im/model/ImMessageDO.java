package com.deltatrade.platform.modules.im.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("im_message")
public class ImMessageDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("conversation_no")
    private String conversationNo;

    @TableField("sender_role")
    private String senderRole;

    @TableField("sender_user_id")
    private Long senderUserId;

    @TableField("sender_name")
    private String senderName;

    @TableField("message_type")
    private String messageType;

    @TableField("content_text")
    private String contentText;

    @TableField("file_key")
    private String fileKey;

    @TableField("file_name")
    private String fileName;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConversationNo() { return conversationNo; }
    public void setConversationNo(String conversationNo) { this.conversationNo = conversationNo; }
    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public Long getSenderUserId() { return senderUserId; }
    public void setSenderUserId(Long senderUserId) { this.senderUserId = senderUserId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getContentText() { return contentText; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public String getFileKey() { return fileKey; }
    public void setFileKey(String fileKey) { this.fileKey = fileKey; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
