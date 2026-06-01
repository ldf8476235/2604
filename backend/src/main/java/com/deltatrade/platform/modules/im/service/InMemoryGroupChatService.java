package com.deltatrade.platform.modules.im.service;

import com.deltatrade.platform.modules.im.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryGroupChatService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryGroupChatService.class);

    private final Map<String, List<ChatMessage>> historyStore = new ConcurrentHashMap<>();
    private final Map<String, MuteInfo> muteStore = new ConcurrentHashMap<>();

    public ChatMessage appendMessage(String groupId, String senderId, String senderRole, String content, String messageType) {
        ChatMessage message = new ChatMessage(groupId, senderId, senderRole, content, messageType, LocalDateTime.now());
        historyStore.computeIfAbsent(groupId, key -> new ArrayList<>()).add(message);
        log.info("im message appended groupId={} senderId={} messageType={}", groupId, senderId, messageType);
        return message;
    }

    public List<ChatMessage> history(String groupId) {
        return historyStore.containsKey(groupId) ? historyStore.get(groupId) : Collections.<ChatMessage>emptyList();
    }

    public MuteInfo mute(String groupId, String targetUserId, String duration, String reason) {
        MuteInfo muteInfo = new MuteInfo(groupId, targetUserId, duration, reason, true);
        muteStore.put(groupId + ":" + targetUserId, muteInfo);
        log.warn("im mute applied groupId={} targetUserId={} duration={} reason={}", groupId, targetUserId, duration, reason);
        appendMessage(groupId, "system", "SYSTEM", "用户已被禁言，原因：" + reason, "SYSTEM");
        return muteInfo;
    }

    public MuteInfo unmute(String groupId, String targetUserId) {
        MuteInfo muteInfo = new MuteInfo(groupId, targetUserId, "RELEASED", "客服解除禁言", false);
        muteStore.remove(groupId + ":" + targetUserId);
        appendMessage(groupId, "system", "SYSTEM", "用户禁言已解除", "SYSTEM");
        return muteInfo;
    }

    public static class MuteInfo {
        private final String groupId;
        private final String targetUserId;
        private final String duration;
        private final String reason;
        private final boolean active;

        public MuteInfo(String groupId, String targetUserId, String duration, String reason, boolean active) {
            this.groupId = groupId;
            this.targetUserId = targetUserId;
            this.duration = duration;
            this.reason = reason;
            this.active = active;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getTargetUserId() {
            return targetUserId;
        }

        public String getDuration() {
            return duration;
        }

        public String getReason() {
            return reason;
        }

        public boolean isActive() {
            return active;
        }
    }
}
