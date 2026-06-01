package com.deltatrade.platform.modules.im.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("im_participant")
public class ImParticipantDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("conversation_no")
    private String conversationNo;

    @TableField("user_id")
    private Long userId;

    @TableField("participant_role")
    private String participantRole;

    @TableField("last_read_message_id")
    private Long lastReadMessageId;

    @TableField("joined_at")
    private LocalDateTime joinedAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConversationNo() { return conversationNo; }
    public void setConversationNo(String conversationNo) { this.conversationNo = conversationNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getParticipantRole() { return participantRole; }
    public void setParticipantRole(String participantRole) { this.participantRole = participantRole; }
    public Long getLastReadMessageId() { return lastReadMessageId; }
    public void setLastReadMessageId(Long lastReadMessageId) { this.lastReadMessageId = lastReadMessageId; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
