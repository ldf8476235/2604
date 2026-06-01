package com.deltatrade.platform.modules.im.model;

import java.time.LocalDateTime;

public class ChatMessage {

    private final String groupId;
    private final String senderId;
    private final String senderRole;
    private final String content;
    private final String messageType;
    private final LocalDateTime sentAt;

    public ChatMessage(String groupId, String senderId, String senderRole, String content, String messageType, LocalDateTime sentAt) {
        this.groupId = groupId;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.content = content;
        this.messageType = messageType;
        this.sentAt = sentAt;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public String getContent() {
        return content;
    }

    public String getMessageType() {
        return messageType;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }
}
