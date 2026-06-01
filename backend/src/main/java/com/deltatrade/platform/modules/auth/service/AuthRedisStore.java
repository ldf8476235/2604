package com.deltatrade.platform.modules.auth.service;

import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
public class AuthRedisStore {

    private static final Logger log = LoggerFactory.getLogger(AuthRedisStore.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AuthRedisStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void saveSmsCode(String phone, String scene, String code, String outId, LocalDateTime expireAt, LocalDateTime cooldownUntil) {
        saveValue(
            buildSmsKey(phone, scene),
            new SmsCodeCache(phone, scene, code, outId, expireAt, cooldownUntil),
            remainingDuration(expireAt),
            "auth:sms"
        );
    }

    public SmsCodeCache getSmsCode(String phone, String scene) {
        return readValue(buildSmsKey(phone, scene), SmsCodeCache.class, "auth:sms");
    }

    public void deleteSmsCode(String phone, String scene) {
        deleteValue(buildSmsKey(phone, scene), "auth:sms");
    }

    public void saveSmsCooldown(String phone, String scene, LocalDateTime cooldownUntil) {
        saveValue(
            buildSmsCooldownKey(phone),
            new SmsCooldownCache(phone, scene, cooldownUntil),
            remainingDuration(cooldownUntil),
            "auth:sms:cooldown"
        );
    }

    public SmsCooldownCache getSmsCooldown(String phone) {
        return readValue(buildSmsCooldownKey(phone), SmsCooldownCache.class, "auth:sms:cooldown");
    }

    public void saveVerifyTicket(String token, String subject, String purpose, LocalDateTime expireAt) {
        saveValue(
            buildVerifyKey(token),
            new VerifyTicketCache(token, subject, purpose, expireAt),
            remainingDuration(expireAt),
            "auth:verify"
        );
    }

    public VerifyTicketCache getVerifyTicket(String token) {
        return readValue(buildVerifyKey(token), VerifyTicketCache.class, "auth:verify");
    }

    public void deleteVerifyTicket(String token) {
        deleteValue(buildVerifyKey(token), "auth:verify");
    }

    public void saveWechatQrSession(WechatQrCache cache) {
        saveValue(buildWechatKey(cache.getSceneId()), cache, remainingDuration(cache.getExpireAt()), "auth:wechat:scene");
    }

    public WechatQrCache getWechatQrSession(String sceneId) {
        return readValue(buildWechatKey(sceneId), WechatQrCache.class, "auth:wechat:scene");
    }

    public void deleteWechatQrSession(String sceneId) {
        deleteValue(buildWechatKey(sceneId), "auth:wechat:scene");
    }

    public void saveLoginSession(String token, LoginSessionCache cache, int expireDays) {
        saveValue(buildLoginKey(token), cache, Duration.ofDays(expireDays), "auth:login");
    }

    public LoginSessionCache getLoginSession(String token) {
        return readValue(buildLoginKey(token), LoginSessionCache.class, "auth:login");
    }

    public void deleteLoginSession(String token) {
        deleteValue(buildLoginKey(token), "auth:login");
    }

    public void refreshLoginSession(LoginSessionCache cache) {
        saveValue(buildLoginKey(cache.getToken()), cache, remainingDuration(cache.getExpireAt()), "auth:login");
    }

    private <T> void saveValue(String key, T value, Duration ttl, String target) {
        long startAt = System.currentTimeMillis();
        try {
            redisTemplate.opsForValue().set(key, writeJson(value), ttl.getSeconds(), TimeUnit.SECONDS);
            log.info("redis write success target={} costMs={} ttlSeconds={}", target, System.currentTimeMillis() - startAt, ttl.getSeconds());
        } catch (RuntimeException exception) {
            log.error("redis write failed target={} costMs={}", target, System.currentTimeMillis() - startAt, exception);
            throw exception;
        }
    }

    private <T> T readValue(String key, Class<T> type, String target) {
        long startAt = System.currentTimeMillis();
        try {
            String raw = redisTemplate.opsForValue().get(key);
            boolean hit = raw != null;
            log.info("redis read success target={} costMs={} hit={}", target, System.currentTimeMillis() - startAt, hit);
            if (!hit) {
                return null;
            }
            return readJson(raw, type);
        } catch (RuntimeException exception) {
            log.error("redis read failed target={} costMs={}", target, System.currentTimeMillis() - startAt, exception);
            throw exception;
        }
    }

    private void deleteValue(String key, String target) {
        long startAt = System.currentTimeMillis();
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("redis delete success target={} costMs={} deleted={}", target, System.currentTimeMillis() - startAt, Boolean.TRUE.equals(deleted));
        } catch (RuntimeException exception) {
            log.error("redis delete failed target={} costMs={}", target, System.currentTimeMillis() - startAt, exception);
            throw exception;
        }
    }

    private Duration remainingDuration(LocalDateTime expireAt) {
        long seconds = Duration.between(LocalDateTime.now(), expireAt).getSeconds();
        return Duration.ofSeconds(Math.max(seconds, 1));
    }

    private String buildSmsKey(String phone, String scene) {
        return "auth:sms:" + scene + ":" + phone;
    }

    private String buildSmsCooldownKey(String phone) {
        return "auth:sms:cooldown:" + phone;
    }

    private String buildVerifyKey(String token) {
        return "auth:verify:" + token;
    }

    private String buildWechatKey(String sceneId) {
        return "auth:wechat:scene:" + sceneId;
    }

    private String buildLoginKey(String token) {
        return "auth:login:" + token;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "认证缓存序列化失败");
        }
    }

    private <T> T readJson(String raw, Class<T> type) {
        try {
            return objectMapper.readValue(raw, type);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "认证缓存反序列化失败");
        }
    }

    public static class SmsCodeCache {
        private String phone;
        private String scene;
        private String code;
        private String outId;
        private LocalDateTime expireAt;
        private LocalDateTime cooldownUntil;

        public SmsCodeCache() {
        }

        public SmsCodeCache(String phone, String scene, String code, String outId, LocalDateTime expireAt, LocalDateTime cooldownUntil) {
            this.phone = phone;
            this.scene = scene;
            this.code = code;
            this.outId = outId;
            this.expireAt = expireAt;
            this.cooldownUntil = cooldownUntil;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getScene() {
            return scene;
        }

        public void setScene(String scene) {
            this.scene = scene;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getOutId() {
            return outId;
        }

        public void setOutId(String outId) {
            this.outId = outId;
        }

        public LocalDateTime getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(LocalDateTime expireAt) {
            this.expireAt = expireAt;
        }

        public LocalDateTime getCooldownUntil() {
            return cooldownUntil;
        }

        public void setCooldownUntil(LocalDateTime cooldownUntil) {
            this.cooldownUntil = cooldownUntil;
        }
    }

    public static class VerifyTicketCache {
        private String token;
        private String subject;
        private String purpose;
        private LocalDateTime expireAt;

        public VerifyTicketCache() {
        }

        public VerifyTicketCache(String token, String subject, String purpose, LocalDateTime expireAt) {
            this.token = token;
            this.subject = subject;
            this.purpose = purpose;
            this.expireAt = expireAt;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getPurpose() {
            return purpose;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }

        public LocalDateTime getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(LocalDateTime expireAt) {
            this.expireAt = expireAt;
        }
    }

    public static class SmsCooldownCache {
        private String phone;
        private String scene;
        private LocalDateTime cooldownUntil;

        public SmsCooldownCache() {
        }

        public SmsCooldownCache(String phone, String scene, LocalDateTime cooldownUntil) {
            this.phone = phone;
            this.scene = scene;
            this.cooldownUntil = cooldownUntil;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getScene() {
            return scene;
        }

        public void setScene(String scene) {
            this.scene = scene;
        }

        public LocalDateTime getCooldownUntil() {
            return cooldownUntil;
        }

        public void setCooldownUntil(LocalDateTime cooldownUntil) {
            this.cooldownUntil = cooldownUntil;
        }
    }

    public static class WechatQrCache {
        private String sceneId;
        private String openId;
        private String unionId;
        private String bindToken;
        private String clientMode;
        private String returnPath;
        private boolean boundAccount;
        private LocalDateTime expireAt;
        private String status;
        private int pollCount;
        private LocalDateTime authorizedAt;
        private String lastError;

        public WechatQrCache() {
        }

        public WechatQrCache(
            String sceneId,
            String openId,
            String unionId,
            String bindToken,
            String clientMode,
            String returnPath,
            boolean boundAccount,
            LocalDateTime expireAt,
            String status,
            int pollCount,
            LocalDateTime authorizedAt,
            String lastError
        ) {
            this.sceneId = sceneId;
            this.openId = openId;
            this.unionId = unionId;
            this.bindToken = bindToken;
            this.clientMode = clientMode;
            this.returnPath = returnPath;
            this.boundAccount = boundAccount;
            this.expireAt = expireAt;
            this.status = status;
            this.pollCount = pollCount;
            this.authorizedAt = authorizedAt;
            this.lastError = lastError;
        }

        public String getSceneId() {
            return sceneId;
        }

        public void setSceneId(String sceneId) {
            this.sceneId = sceneId;
        }

        public String getOpenId() {
            return openId;
        }

        public void setOpenId(String openId) {
            this.openId = openId;
        }

        public String getUnionId() {
            return unionId;
        }

        public void setUnionId(String unionId) {
            this.unionId = unionId;
        }

        public String getBindToken() {
            return bindToken;
        }

        public void setBindToken(String bindToken) {
            this.bindToken = bindToken;
        }

        public String getClientMode() {
            return clientMode;
        }

        public void setClientMode(String clientMode) {
            this.clientMode = clientMode;
        }

        public String getReturnPath() {
            return returnPath;
        }

        public void setReturnPath(String returnPath) {
            this.returnPath = returnPath;
        }

        public boolean isBoundAccount() {
            return boundAccount;
        }

        public void setBoundAccount(boolean boundAccount) {
            this.boundAccount = boundAccount;
        }

        public LocalDateTime getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(LocalDateTime expireAt) {
            this.expireAt = expireAt;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getPollCount() {
            return pollCount;
        }

        public void setPollCount(int pollCount) {
            this.pollCount = pollCount;
        }

        public LocalDateTime getAuthorizedAt() {
            return authorizedAt;
        }

        public void setAuthorizedAt(LocalDateTime authorizedAt) {
            this.authorizedAt = authorizedAt;
        }

        public String getLastError() {
            return lastError;
        }

        public void setLastError(String lastError) {
            this.lastError = lastError;
        }
    }

    public static class LoginSessionCache {
        private String token;
        private Long userId;
        private String nickname;
        private String phone;
        private boolean verified;
        private boolean hasPassword;
        private LocalDateTime expireAt;

        public LoginSessionCache() {
        }

        public LoginSessionCache(String token, Long userId, String nickname, String phone, boolean verified, boolean hasPassword, LocalDateTime expireAt) {
            this.token = token;
            this.userId = userId;
            this.nickname = nickname;
            this.phone = phone;
            this.verified = verified;
            this.hasPassword = hasPassword;
            this.expireAt = expireAt;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public boolean isVerified() {
            return verified;
        }

        public void setVerified(boolean verified) {
            this.verified = verified;
        }

        public boolean isHasPassword() {
            return hasPassword;
        }

        public void setHasPassword(boolean hasPassword) {
            this.hasPassword = hasPassword;
        }

        public LocalDateTime getExpireAt() {
            return expireAt;
        }

        public void setExpireAt(LocalDateTime expireAt) {
            this.expireAt = expireAt;
        }
    }
}
