package com.deltatrade.platform.modules.im.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("im_support_read_state")
public class ImSupportReadStateDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("conversation_no")
    private String conversationNo;

    @TableField("last_read_message_id")
    private Long lastReadMessageId;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConversationNo() { return conversationNo; }
    public void setConversationNo(String conversationNo) { this.conversationNo = conversationNo; }
    public Long getLastReadMessageId() { return lastReadMessageId; }
    public void setLastReadMessageId(Long lastReadMessageId) { this.lastReadMessageId = lastReadMessageId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
