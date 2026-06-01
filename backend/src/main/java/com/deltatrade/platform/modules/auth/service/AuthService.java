package com.deltatrade.platform.modules.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.auth.config.PlatformSmsProperties;
import com.deltatrade.platform.modules.auth.mapper.AuthUserMapper;
import com.deltatrade.platform.modules.auth.mapper.RealNameFaceSessionMapper;
import com.deltatrade.platform.modules.auth.model.AuthUserDO;
import com.deltatrade.platform.modules.auth.model.RealNameFaceSessionDO;
import com.deltatrade.platform.modules.oss.service.OssStorageService;
import com.deltatrade.platform.modules.profile.mapper.WithdrawAccountMapper;
import com.deltatrade.platform.modules.profile.model.WithdrawAccountDO;
import com.deltatrade.platform.modules.profile.service.DistributionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int TOKEN_EXPIRE_DAYS = 7;
    private static final int VERIFY_TICKET_MINUTES = 10;
    private static final int WECHAT_QR_EXPIRE_MINUTES = 5;
    private static final int WECHAT_POLL_INTERVAL_SECONDS = 5;
    private static final int FACE_VERIFY_EXPIRE_MINUTES = 30;
    private static final DateTimeFormatter FACE_ORDER_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AuthUserMapper authUserMapper;
    private final RealNameFaceSessionMapper realNameFaceSessionMapper;
    private final AuthRedisStore authRedisStore;
    private final PnvsSmsVerificationService smsVerificationService;
    private final WechatOpenGateway wechatOpenGateway;
    private final PlatformSmsProperties smsProperties;
    private final WithdrawAccountMapper withdrawAccountMapper;
    private final OssStorageService ossStorageService;
    private final RealNameVerificationService realNameVerificationService;
    private final JuheFaceGateway juheFaceGateway;
    private final ObjectMapper objectMapper;
    private final DistributionService distributionService;
    private final boolean seedDemoUsersEnabled;

    public AuthService(
        AuthUserMapper authUserMapper,
        RealNameFaceSessionMapper realNameFaceSessionMapper,
        AuthRedisStore authRedisStore,
        PnvsSmsVerificationService smsVerificationService,
        WechatOpenGateway wechatOpenGateway,
        PlatformSmsProperties smsProperties,
        WithdrawAccountMapper withdrawAccountMapper,
        OssStorageService ossStorageService,
        RealNameVerificationService realNameVerificationService,
        JuheFaceGateway juheFaceGateway,
        ObjectMapper objectMapper,
        DistributionService distributionService,
        @Value("${platform.auth.seed-demo-users:false}") boolean seedDemoUsersEnabled
    ) {
        this.authUserMapper = authUserMapper;
        this.realNameFaceSessionMapper = realNameFaceSessionMapper;
        this.authRedisStore = authRedisStore;
        this.smsVerificationService = smsVerificationService;
        this.wechatOpenGateway = wechatOpenGateway;
        this.smsProperties = smsProperties;
        this.withdrawAccountMapper = withdrawAccountMapper;
        this.ossStorageService = ossStorageService;
        this.realNameVerificationService = realNameVerificationService;
        this.juheFaceGateway = juheFaceGateway;
        this.objectMapper = objectMapper;
        this.distributionService = distributionService;
        this.seedDemoUsersEnabled = seedDemoUsersEnabled;
    }

    @PostConstruct
    public void initDemoUsers() {
        if (!seedDemoUsersEnabled) {
            log.info("skip demo auth user seeding because platform.auth.seed-demo-users=false");
            return;
        }
        seedUserIfAbsent("13800138000", "星火买家", hashPassword("Pass123"), "wx-openid-bound-demo", true);
        seedUserIfAbsent("13900139000", "高分买家", hashPassword("Pass123"), null, false);
    }

    public SmsCodeResult sendSmsCode(String phone, String scene) {
        String normalizedScene = normalizeScene(scene);
        LocalDateTime now = LocalDateTime.now();
        validatePhoneForScene(phone, normalizedScene);

        AuthRedisStore.SmsCooldownCache cooldown = authRedisStore.getSmsCooldown(phone);
        if (cooldown != null && cooldown.getCooldownUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "验证码发送过于频繁，请稍后再试");
        }

        LocalDateTime expireAt = now.plusMinutes(smsExpireMinutes());
        LocalDateTime cooldownUntil = now.plusSeconds(smsCooldownSeconds());
        String outId = buildSmsOutId(normalizedScene);
        PnvsSmsVerificationService.SendResult sendResult = smsVerificationService.sendCode(phone, normalizedScene, outId);
        authRedisStore.saveSmsCode(phone, normalizedScene, sendResult.getVerifyCode(), outId, expireAt, cooldownUntil);
        authRedisStore.saveSmsCooldown(phone, normalizedScene, cooldownUntil);
        log.info("send sms code success scene={} phone={} expireAt={} cooldownUntil={} outId={}",
            normalizedScene, maskPhone(phone), expireAt, cooldownUntil, outId);
        return new SmsCodeResult(
            maskPhone(phone),
            normalizedScene,
            expireAt,
            smsCooldownSeconds(),
            smsProperties.isMockMode() ? "开发环境验证码固定为 246810" : "验证码已发送，请注意查收"
        );
    }

    public LoginResult smsLogin(String phone, String code) {
        verifySmsCode(phone, "LOGIN", code);
        AuthUserDO user = requireUser(phone, "该手机号尚未注册，请先注册");
        log.info("sms login success userId={} phone={}", user.getId(), maskPhone(phone));
        return buildLoginResult(user);
    }

    public LoginResult passwordLogin(String phone, String password) {
        AuthUserDO user = requireUser(phone, "账号不存在");
        if (!hasPassword(user) || !user.getPasswordHash().equals(hashPassword(password))) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "手机号或密码错误");
        }
        log.info("password login success userId={} phone={}", user.getId(), maskPhone(phone));
        return buildLoginResult(user);
    }

    public VerifyTicketResult verifyRegisterCode(String phone, String code) {
        verifySmsCode(phone, "REGISTER", code);
        if (findUserByPhone(phone) != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该手机号已注册");
        }
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(VERIFY_TICKET_MINUTES);
        String token = UUID.randomUUID().toString().replace("-", "");
        authRedisStore.saveVerifyTicket(token, phone, "REGISTER", expireAt);
        log.info("register verify success phone={} ticketExpireAt={}", maskPhone(phone), expireAt);
        return new VerifyTicketResult(token, expireAt);
    }

    public LoginResult completeRegister(String phone, String verifyToken, String password, String confirmPassword, String inviteCode) {
        AuthRedisStore.VerifyTicketCache ticket = requireVerifyTicket(phone, verifyToken, "REGISTER");
        validatePassword(password, confirmPassword);
        if (findUserByPhone(phone) != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该手机号已注册");
        }
        AuthUserDO user = new AuthUserDO();
        LocalDateTime now = LocalDateTime.now();
        user.setPhone(phone);
        user.setNickname(buildNickname(phone));
        user.setPasswordHash(hashPassword(password));
        user.setOpenId(null);
        user.setAvatarKey(null);
        user.setAccountStatus("ACTIVE");
        user.setBanReason(null);
        user.setRealNameStatus("UNVERIFIED");
        user.setRealNameRejectReason(null);
        user.setRealNameFrontKey(null);
        user.setRealNameBackKey(null);
        user.setVerified(false);
        user.setLoginAlertEnabled(true);
        user.setSecondaryVerifyEnabled(false);
        user.setDistributionEnabled(false);
        user.setDistributionOpenedAt(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        insertUser(user);
        distributionService.bindReferralOnRegister(user.getId(), inviteCode, "REGISTER");
        authRedisStore.deleteVerifyTicket(ticket.getToken());
        log.info("register success userId={} phone={}", user.getId(), maskPhone(phone));
        return buildLoginResult(user);
    }

    public VerifyTicketResult verifyPasswordResetCode(String phone, String code) {
        requireUser(phone, "该手机号尚未注册");
        verifySmsCode(phone, "RESET_PASSWORD", code);
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(VERIFY_TICKET_MINUTES);
        String token = UUID.randomUUID().toString().replace("-", "");
        authRedisStore.saveVerifyTicket(token, phone, "RESET_PASSWORD", expireAt);
        log.info("password reset verify success phone={} ticketExpireAt={}", maskPhone(phone), expireAt);
        return new VerifyTicketResult(token, expireAt);
    }

    public SimpleResult completePasswordReset(String phone, String verifyToken, String password, String confirmPassword) {
        AuthRedisStore.VerifyTicketCache ticket = requireVerifyTicket(phone, verifyToken, "RESET_PASSWORD");
        validatePassword(password, confirmPassword);
        AuthUserDO user = requireUser(phone, "该手机号尚未注册");
        user.setPasswordHash(hashPassword(password));
        user.setUpdatedAt(LocalDateTime.now());
        updateUser(user);
        authRedisStore.deleteVerifyTicket(ticket.getToken());
        log.info("password reset success userId={} phone={}", user.getId(), maskPhone(phone));
        return new SimpleResult("SUCCESS", "密码重置成功，请使用新密码登录");
    }

    public WechatQrResult createWechatQr(String redirectUri, String clientMode, String returnPath) {
        LocalDateTime now = LocalDateTime.now();
        String sceneId = "WX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        LocalDateTime expireAt = now.plusMinutes(WECHAT_QR_EXPIRE_MINUTES);
        String normalizedClientMode = normalizeWechatClientMode(clientMode);
        String normalizedReturnPath = normalizeReturnPath(returnPath);
        if (wechatOpenGateway.isMockMode()) {
            boolean boundAccount = Math.abs(sceneId.hashCode()) % 2 == 0;
            String openId = boundAccount ? "wx-openid-bound-demo" : "wx-openid-unbound-" + sceneId.substring(sceneId.length() - 4);
            authRedisStore.saveWechatQrSession(new AuthRedisStore.WechatQrCache(
                sceneId,
                openId,
                null,
                null,
                normalizedClientMode,
                normalizedReturnPath,
                boundAccount,
                expireAt,
                "WAITING",
                0,
                null,
                null
            ));
            log.info("wechat qr created sceneId={} expireAt={} mode=mock boundAccount={} clientMode={} returnPath={}",
                sceneId, expireAt, boundAccount, normalizedClientMode, normalizedReturnPath);
            return new WechatQrResult(
                sceneId,
                "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=" + sceneId,
                "https://mock.weixin.local/authorize?sceneId=" + sceneId,
                expireAt,
                WECHAT_POLL_INTERVAL_SECONDS
            );
        }

        authRedisStore.saveWechatQrSession(new AuthRedisStore.WechatQrCache(
            sceneId,
            null,
            null,
            null,
            normalizedClientMode,
            normalizedReturnPath,
            false,
            expireAt,
            "WAITING",
            0,
            null,
            null
        ));
        String authorizeUrl = wechatOpenGateway.buildQrConnectUrl(sceneId, redirectUri);
        log.info("wechat qr created sceneId={} expireAt={} mode=real redirectUri={} clientMode={} returnPath={}",
            sceneId, expireAt, redirectUri, normalizedClientMode, normalizedReturnPath);
        return new WechatQrResult(
            sceneId,
            "/api/auth/wechat/qr-page?sceneId=" + sceneId + "&redirectUri=" + encodeRedirectUri(redirectUri),
            authorizeUrl,
            expireAt,
            WECHAT_POLL_INTERVAL_SECONDS
        );
    }

    public WechatPollResult pollWechatQr(String sceneId) {
        AuthRedisStore.WechatQrCache session = requireWechatSession(sceneId);
        LocalDateTime now = LocalDateTime.now();
        if (session.getExpireAt().isBefore(now)) {
            session.setStatus("EXPIRED");
            log.warn("wechat qr expired sceneId={}", sceneId);
            return new WechatPollResult("EXPIRED", null, null, null);
        }

        session.setPollCount(session.getPollCount() + 1);
        if (wechatOpenGateway.isMockMode() && "WAITING".equals(session.getStatus()) && session.getPollCount() >= 2) {
            session.setAuthorizedAt(now);
            if (session.isBoundAccount()) {
                session.setStatus("AUTHORIZED_BOUND");
            } else {
                session.setStatus("AUTHORIZED_UNBOUND");
            }
        }

        if ("WAITING".equals(session.getStatus())) {
            authRedisStore.saveWechatQrSession(session);
            return new WechatPollResult("WAITING", null, null, null);
        }

        if ("AUTHORIZED_BOUND".equals(session.getStatus())) {
            session.setStatus("AUTHORIZED_BOUND");
            authRedisStore.saveWechatQrSession(session);
            AuthUserDO user = findUserByOpenId(session.getOpenId());
            if (user == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "微信账号绑定状态异常");
            }
            log.info("wechat login success sceneId={} userId={} openId={}", sceneId, user.getId(), maskOpenId(session.getOpenId()));
            return new WechatPollResult("AUTHORIZED_BOUND", buildLoginResult(user), null, maskPhone(user.getPhone()));
        }

        if ("AUTHORIZED_UNBOUND".equals(session.getStatus())) {
            if (session.getBindToken() == null || session.getBindToken().isEmpty()) {
                LocalDateTime expireAt = LocalDateTime.now().plusMinutes(VERIFY_TICKET_MINUTES);
                String bindToken = UUID.randomUUID().toString().replace("-", "");
                authRedisStore.saveVerifyTicket(bindToken, session.getOpenId(), "WECHAT_BIND", expireAt);
                session.setBindToken(bindToken);
            }
            authRedisStore.saveWechatQrSession(session);
            log.info("wechat login requires binding sceneId={} openId={}", sceneId, maskOpenId(session.getOpenId()));
            return new WechatPollResult("AUTHORIZED_UNBOUND", null, session.getBindToken(), null);
        }

        if ("FAILED".equals(session.getStatus())) {
            log.warn("wechat qr authorization failed sceneId={} reason={}", sceneId, session.getLastError());
            return new WechatPollResult("EXPIRED", null, null, null);
        }

        authRedisStore.saveWechatQrSession(session);
        return new WechatPollResult("WAITING", null, null, null);
    }

    public WechatCallbackPage handleWechatCallback(String sceneId, String code, String errorCode, String errorDescription) {
        AuthRedisStore.WechatQrCache session = requireWechatSession(sceneId);
        LocalDateTime now = LocalDateTime.now();
        if (session.getExpireAt().isBefore(now)) {
            session.setStatus("EXPIRED");
            authRedisStore.saveWechatQrSession(session);
            log.warn("wechat callback ignored because scene expired sceneId={}", sceneId);
            return new WechatCallbackPage(false, "二维码已过期", renderWechatCallbackExpiredDescription(session), null, null, resolveWechatReturnPath(session));
        }

        if (errorCode != null && !errorCode.trim().isEmpty()) {
            session.setStatus("FAILED");
            session.setLastError(errorDescription == null || errorDescription.trim().isEmpty() ? errorCode : errorDescription);
            authRedisStore.saveWechatQrSession(session);
            log.warn("wechat callback denied sceneId={} errorCode={} errorDescription={}", sceneId, errorCode, errorDescription);
            return new WechatCallbackPage(false, "微信授权未完成", renderWechatCallbackCancelDescription(session), null, null, resolveWechatReturnPath(session));
        }

        if (code == null || code.trim().isEmpty()) {
            session.setStatus("FAILED");
            session.setLastError("微信授权缺少 code");
            authRedisStore.saveWechatQrSession(session);
            log.warn("wechat callback missing code sceneId={}", sceneId);
            return new WechatCallbackPage(false, "微信授权失败", renderWechatCallbackMissingCodeDescription(session), null, null, resolveWechatReturnPath(session));
        }

        WechatOpenGateway.WechatAccessTokenResult tokenResult = wechatOpenGateway.exchangeCode(code);
        session.setOpenId(tokenResult.getOpenId());
        session.setUnionId(tokenResult.getUnionId());
        session.setAuthorizedAt(now);
        session.setBoundAccount(findUserByOpenId(tokenResult.getOpenId()) != null);
        session.setStatus(session.isBoundAccount() ? "AUTHORIZED_BOUND" : "AUTHORIZED_UNBOUND");
        session.setLastError(null);
        authRedisStore.saveWechatQrSession(session);
        log.info("wechat callback success sceneId={} status={} openId={}",
            sceneId, session.getStatus(), maskOpenId(tokenResult.getOpenId()));
        String returnPath = resolveWechatReturnPath(session);
        if (isWechatInternalMode(session)) {
            if (session.isBoundAccount()) {
                AuthUserDO user = findUserByOpenId(tokenResult.getOpenId());
                if (user == null) {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR, "微信账号绑定状态异常");
                }
                return new WechatCallbackPage(true, "微信授权成功", "正在为你登录并返回站内页面。", buildLoginResult(user), null, returnPath);
            }
            if (session.getBindToken() == null || session.getBindToken().isEmpty()) {
                LocalDateTime expireAt = LocalDateTime.now().plusMinutes(VERIFY_TICKET_MINUTES);
                String bindToken = UUID.randomUUID().toString().replace("-", "");
                authRedisStore.saveVerifyTicket(bindToken, session.getOpenId(), "WECHAT_BIND", expireAt);
                session.setBindToken(bindToken);
                authRedisStore.saveWechatQrSession(session);
            }
            return new WechatCallbackPage(true, "微信授权成功", "微信授权已完成，正在返回站内继续绑定手机号。", null, session.getBindToken(), returnPath);
        }
        return new WechatCallbackPage(true, "微信授权成功", "请回到电脑端，系统正在为你完成登录。", null, null, returnPath);
    }

    private boolean isWechatInternalMode(AuthRedisStore.WechatQrCache session) {
        return session != null && "WECHAT_INTERNAL".equalsIgnoreCase(trimNullable(session.getClientMode()));
    }

    private String resolveWechatReturnPath(AuthRedisStore.WechatQrCache session) {
        return normalizeReturnPath(session == null ? null : session.getReturnPath());
    }

    private String normalizeWechatClientMode(String clientMode) {
        String normalized = trimNullable(clientMode);
        if ("WECHAT_INTERNAL".equalsIgnoreCase(normalized)) {
            return "WECHAT_INTERNAL";
        }
        return "QR";
    }

    private String normalizeReturnPath(String returnPath) {
        String normalized = trimNullable(returnPath);
        if (!StringUtils.hasText(normalized)) {
            return "/";
        }
        if (!normalized.startsWith("/") || normalized.startsWith("//")) {
            return "/";
        }
        return normalized;
    }

    private String renderWechatCallbackExpiredDescription(AuthRedisStore.WechatQrCache session) {
        if (isWechatInternalMode(session)) {
            return "当前授权会话已过期，请返回站内重新发起微信登录。";
        }
        return "请回到电脑端刷新二维码后重新扫码。";
    }

    private String renderWechatCallbackCancelDescription(AuthRedisStore.WechatQrCache session) {
        if (isWechatInternalMode(session)) {
            return "你已取消本次微信授权，请返回站内重新发起登录。";
        }
        return "你已取消授权，请回到电脑端重新扫码。";
    }

    private String renderWechatCallbackMissingCodeDescription(AuthRedisStore.WechatQrCache session) {
        if (isWechatInternalMode(session)) {
            return "授权参数缺失，请返回站内重新发起微信登录。";
        }
        return "授权参数缺失，请回到电脑端重新扫码。";
    }

    public LoginResult bindWechatPhone(String bindToken, String phone, String code, String inviteCode) {
        AuthRedisStore.VerifyTicketCache ticket = requireVerifyTicket(subjectFromVerifyToken(bindToken), bindToken, "WECHAT_BIND");
        verifySmsCode(phone, "BIND_PHONE", code);

        String openId = ticket.getSubject();
        if (findUserByOpenId(openId) != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该微信账号已绑定平台用户");
        }

        AuthUserDO user = findUserByPhone(phone);
        LocalDateTime now = LocalDateTime.now();
        if (user == null) {
            user = new AuthUserDO();
            user.setPhone(phone);
            user.setNickname("微信用户" + phone.substring(phone.length() - 4));
            user.setPasswordHash(null);
            user.setOpenId(openId);
            user.setAvatarKey(null);
            user.setAccountStatus("ACTIVE");
            user.setBanReason(null);
            user.setRealNameStatus("UNVERIFIED");
            user.setRealNameRejectReason(null);
            user.setRealNameFrontKey(null);
            user.setRealNameBackKey(null);
            user.setVerified(false);
            user.setLoginAlertEnabled(true);
            user.setSecondaryVerifyEnabled(false);
            user.setDistributionEnabled(false);
            user.setDistributionOpenedAt(null);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            insertUser(user);
            distributionService.bindReferralOnRegister(user.getId(), inviteCode, "WECHAT_BIND");
        } else if (user.getOpenId() != null && !user.getOpenId().equals(openId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该手机号已绑定其他微信账号");
        } else {
            user.setOpenId(openId);
            user.setUpdatedAt(now);
            updateUser(user);
        }

        authRedisStore.deleteVerifyTicket(bindToken);
        log.info("wechat bind phone success userId={} phone={} openId={}", user.getId(), maskPhone(phone), maskOpenId(openId));
        return buildLoginResult(user);
    }

    public SimpleResult logout(String token) {
        AuthRedisStore.LoginSessionCache session = authRedisStore.getLoginSession(token);
        if (session == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        authRedisStore.deleteLoginSession(token);
        log.info("logout success userId={} phone={}", session.getUserId(), maskPhone(session.getPhone()));
        return new SimpleResult("SUCCESS", "已退出登录");
    }

    public RealNameProfile getRealNameProfile(Long userId) {
        AuthUserDO user = requireUserById(userId);
        log.info("real name profile loaded userId={} verified={} status={}",
            userId, Boolean.TRUE.equals(user.getVerified()), resolveRealNameStatus(user));
        return new RealNameProfile(
            Boolean.TRUE.equals(user.getVerified()),
            resolveRealNameStatus(user),
            safeText(user.getRealName()),
            safeText(StringUtils.hasText(user.getRealNamePhone()) ? user.getRealNamePhone() : user.getPhone()),
            maskIdCard(user.getIdCardNo()),
            safeText(user.getRealNameRejectReason()),
            safeText(user.getRealNameFrontKey()),
            safeText(user.getRealNameBackKey()),
            previewNullable(user.getRealNameFrontKey()),
            previewNullable(user.getRealNameBackKey())
        );
    }

    public RealNameProfile submitRealName(String token, Long userId, String realName, String phone, String idCardNo, String idCardFrontKey, String idCardBackKey) {
        AuthUserDO user = requireUserById(userId);
        if (Boolean.TRUE.equals(user.getVerified()) && hasRealNameProfile(user)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "实名认证已完成，暂不支持修改");
        }
        if (!hasOssObjectKey(idCardFrontKey) || !hasOssObjectKey(idCardBackKey)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请上传身份证正反面照片");
        }

        String normalizedRealName = realName.trim();
        String normalizedPhone = phone.trim();
        if (!normalizedPhone.matches("^1\\d{10}$")) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请输入 11 位手机号");
        }
        String normalizedIdCardNo = idCardNo.trim().toUpperCase();
        RealNameVerificationService.VerificationResult verificationResult = realNameVerificationService.verify(normalizedRealName, normalizedIdCardNo);

        user.setRealName(normalizedRealName);
        user.setRealNamePhone(normalizedPhone);
        user.setIdCardNo(normalizedIdCardNo);
        user.setRealNameFrontKey(idCardFrontKey.trim());
        user.setRealNameBackKey(idCardBackKey.trim());
        user.setVerified(verificationResult.isPassed());
        user.setRealNameStatus(verificationResult.isPassed() ? "APPROVED" : "REJECTED");
        user.setRealNameRejectReason(verificationResult.isPassed() ? null : verificationResult.getReason());
        user.setUpdatedAt(LocalDateTime.now());
        updateUser(user);
        refreshLoginVerified(token, user);
        log.info("real name submit success userId={} verified={} status={} provider={} realName={}",
            userId,
            verificationResult.isPassed(),
            user.getRealNameStatus(),
            verificationResult.getProvider(),
            maskRealName(user.getRealName()));
        return new RealNameProfile(
            verificationResult.isPassed(),
            user.getRealNameStatus(),
            safeText(user.getRealName()),
            safeText(user.getRealNamePhone()),
            maskIdCard(user.getIdCardNo()),
            safeText(user.getRealNameRejectReason()),
            safeText(user.getRealNameFrontKey()),
            safeText(user.getRealNameBackKey()),
            previewNullable(user.getRealNameFrontKey()),
            previewNullable(user.getRealNameBackKey())
        );
    }

    public FaceRealNameStartResult startFaceRealName(String token, Long userId, String realName, String idCardNo) {
        AuthUserDO user = requireUserById(userId);
        if (Boolean.TRUE.equals(user.getVerified())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "实名认证已完成，暂不支持修改");
        }

        String normalizedRealName = realName == null ? "" : realName.trim();
        String normalizedIdCardNo = idCardNo == null ? "" : idCardNo.trim().toUpperCase();
        RealNameVerificationService.VerificationResult localResult = realNameVerificationService.verify(normalizedRealName, normalizedIdCardNo);
        if (!localResult.isPassed()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, localResult.getReason());
        }

        String orderId = buildFaceOrderId();
        JuheFaceGateway.StartResult startResult = juheFaceGateway.startVerification(normalizedRealName, normalizedIdCardNo, orderId);
        if (startResult.getErrorCode() != 0 || !StringUtils.hasText(startResult.getJhOrderId()) || !StringUtils.hasText(startResult.getVerifyUrl())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "实名认证发起失败：" + defaultText(startResult.getReason(), "聚合服务未返回认证链接"));
        }

        LocalDateTime now = LocalDateTime.now();
        RealNameFaceSessionDO session = new RealNameFaceSessionDO();
        session.setUserId(userId);
        session.setOrderId(orderId);
        session.setJhOrderId(startResult.getJhOrderId());
        session.setRealName(normalizedRealName);
        session.setIdCardNo(normalizedIdCardNo);
        session.setStatus("PENDING");
        session.setFailReason(null);
        session.setProvider("JUHE_FACE_H5");
        session.setRawResult(startResult.getRawResult());
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        insertFaceSession(session);

        user.setRealName(normalizedRealName);
        user.setRealNamePhone(StringUtils.hasText(user.getPhone()) ? user.getPhone() : null);
        user.setIdCardNo(normalizedIdCardNo);
        user.setVerified(false);
        user.setRealNameStatus("UNVERIFIED");
        user.setRealNameRejectReason(null);
        user.setUpdatedAt(now);
        updateUser(user);
        refreshLoginVerified(token, user);
        log.info("real name face start success userId={} orderId={} jhOrderId={}",
            userId, orderId, maskFaceOrderId(startResult.getJhOrderId()));
        return new FaceRealNameStartResult(orderId, startResult.getJhOrderId(), startResult.getVerifyUrl(), now.plusMinutes(FACE_VERIFY_EXPIRE_MINUTES));
    }

    public RealNameProfile checkFaceRealNameStatus(String token, Long userId, String orderId) {
        RealNameFaceSessionDO session = findFaceSessionByOrderId(orderId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "实名认证订单不存在");
        }
        refreshFaceSessionFromProvider(session, token);
        return getRealNameProfile(userId);
    }

    public SimpleResult handleFaceNotify(Map<String, String> params, String body) {
        String jhOrderId = extractNotifyJhOrderId(params, body);
        if (!StringUtils.hasText(jhOrderId)) {
            log.warn("juhe face notify ignored because jhOrderId missing params={} body={}", params == null ? 0 : params.keySet(), body);
            return new SimpleResult("IGNORED", "缺少聚合订单号");
        }
        RealNameFaceSessionDO session = findFaceSessionByJhOrderId(jhOrderId);
        if (session == null) {
            log.warn("juhe face notify ignored because session missing jhOrderId={}", maskFaceOrderId(jhOrderId));
            return new SimpleResult("IGNORED", "认证流水不存在");
        }
        refreshFaceSessionFromProvider(session, null);
        return new SimpleResult("SUCCESS", "认证结果已同步");
    }

    public SettingsProfile getSettingsProfile(Long userId) {
        AuthUserDO user = requireUserById(userId);
        WithdrawAccountDO withdrawAccount = findWithdrawAccount(userId);
        log.info("settings profile loaded userId={} wechatBound={} withdrawAccountBound={}",
            userId, hasWechatBinding(user), withdrawAccount != null);
        return new SettingsProfile(
            safeText(user.getNickname()),
            safeText(user.getPhone()),
            hasPhoneBinding(user),
            hasWechatBinding(user),
            Boolean.TRUE.equals(user.getVerified()),
            resolveRealNameStatus(user),
            safeText(user.getRealName()),
            maskIdCard(user.getIdCardNo()),
            safeText(user.getRealNameRejectReason()),
            previewNullable(user.getAvatarKey()),
            previewNullable(user.getRealNameFrontKey()),
            previewNullable(user.getRealNameBackKey()),
            isLoginAlertEnabled(user),
            isSecondaryVerifyEnabled(user),
            withdrawAccount == null ? "未绑定" : renderChannel(withdrawAccount.getChannel()) + " · " + maskAccountNo(withdrawAccount.getAccountNo())
        );
    }

    public SettingsProfile updateAvatar(String token, Long userId, String avatarKey) {
        if (!hasOssObjectKey(avatarKey)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "头像文件不能为空");
        }
        AuthUserDO user = requireUserById(userId);
        user.setAvatarKey(avatarKey.trim());
        user.setUpdatedAt(LocalDateTime.now());
        updateUser(user);
        refreshLoginSession(token, user);
        log.info("settings avatar updated userId={} avatarKey={}", userId, user.getAvatarKey());
        return getSettingsProfile(userId);
    }

    public SettingsProfile updateNickname(String token, Long userId, String nickname) {
        AuthUserDO user = requireUserById(userId);
        String normalizedNickname = nickname == null ? "" : nickname.trim();
        if (normalizedNickname.length() < 2 || normalizedNickname.length() > 20) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "昵称长度需为 2-20 个字符");
        }
        user.setNickname(normalizedNickname);
        user.setUpdatedAt(LocalDateTime.now());
        updateUser(user);
        refreshLoginSession(token, user);
        log.info("settings nickname updated userId={} nickname={}", userId, normalizedNickname);
        return getSettingsProfile(userId);
    }

    public SimpleResult changePassword(String token, Long userId, String currentPassword, String nextPassword, String confirmPassword) {
        AuthUserDO user = requireUserById(userId);
        boolean hadPassword = hasPassword(user);
        if (hadPassword && (currentPassword == null || !user.getPasswordHash().equals(hashPassword(currentPassword)))) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "原密码不正确");
        }
        validatePassword(nextPassword, confirmPassword);
        user.setPasswordHash(hashPassword(nextPassword));
        user.setUpdatedAt(LocalDateTime.now());
        updateUser(user);
        refreshLoginSession(token, user);
        log.info("settings password updated userId={}", userId);
        return new SimpleResult("SUCCESS", hadPassword ? "登录密码修改成功" : "登录密码设置成功");
    }

    public SettingsProfile changePhone(String token, Long userId, String phone, String code) {
        verifySmsCode(phone, "BIND_PHONE", code);
        AuthUserDO existing = findUserByPhone(phone);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该手机号已绑定其他账号");
        }
        AuthUserDO user = requireUserById(userId);
        user.setPhone(phone);
        user.setUpdatedAt(LocalDateTime.now());
        updateUser(user);
        refreshLoginSession(token, user);
        log.info("settings phone updated userId={} phone={}", userId, maskPhone(phone));
        return getSettingsProfile(userId);
    }

    public SettingsProfile unbindPhone(String token, Long userId, String code) {
        AuthUserDO user = requireUserById(userId);
        if (!hasPhoneBinding(user)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号未绑定手机号");
        }
        if (!hasWechatBinding(user)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先绑定微信后再解绑手机号");
        }
        verifySmsCode(user.getPhone(), "SECURITY_VERIFY", code);
        user.setPhone(null);
        user.setUpdatedAt(LocalDateTime.now());
        updateUser(user);
        refreshLoginSession(token, user);
        log.info("settings phone unbound userId={}", userId);
        return getSettingsProfile(userId);
    }

    public SettingsProfile updateSecurity(String token, Long userId, boolean loginAlertEnabled, boolean secondaryVerifyEnabled) {
        AuthUserDO user = requireUserById(userId);
        user.setLoginAlertEnabled(loginAlertEnabled);
        user.setSecondaryVerifyEnabled(secondaryVerifyEnabled);
        user.setUpdatedAt(LocalDateTime.now());
        updateUser(user);
        refreshLoginSession(token, user);
        log.info("settings security updated userId={} loginAlertEnabled={} secondaryVerifyEnabled={}",
            userId, loginAlertEnabled, secondaryVerifyEnabled);
        return getSettingsProfile(userId);
    }

    public SettingsProfile unbindWechat(String token, Long userId) {
        AuthUserDO user = requireUserById(userId);
        if (!hasWechatBinding(user)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号未绑定微信");
        }
        user.setOpenId(null);
        user.setUpdatedAt(LocalDateTime.now());
        updateUser(user);
        refreshLoginSession(token, user);
        log.info("settings wechat unbound userId={}", userId);
        return getSettingsProfile(userId);
    }

    private void verifySmsCode(String phone, String scene, String code) {
        String normalizedScene = normalizeScene(scene);
        AuthRedisStore.SmsCodeCache session = authRedisStore.getSmsCode(phone, normalizedScene);
        if (session == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先获取验证码");
        }
        if (session.getExpireAt().isBefore(LocalDateTime.now())) {
            authRedisStore.deleteSmsCode(phone, normalizedScene);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "验证码已过期，请重新获取");
        }
        boolean verified = StringUtils.hasText(session.getCode())
            ? session.getCode().equals(code)
            : smsVerificationService.verifyCode(phone, code, session.getOutId());
        if (!verified) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "验证码不正确");
        }
        authRedisStore.deleteSmsCode(phone, normalizedScene);
        log.info("verify sms code success scene={} phone={}", normalizedScene, maskPhone(phone));
    }

    private void validatePhoneForScene(String phone, String scene) {
        if ("LOGIN".equals(scene) && findUserByPhone(phone) == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号不存在，请注册");
        }
        if ("REGISTER".equals(scene) && findUserByPhone(phone) != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该手机号已注册");
        }
        if ("RESET_PASSWORD".equals(scene) && findUserByPhone(phone) == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该手机号尚未注册");
        }
    }

    private AuthRedisStore.VerifyTicketCache requireVerifyTicket(String subject, String token, String purpose) {
        AuthRedisStore.VerifyTicketCache ticket = authRedisStore.getVerifyTicket(token);
        if (ticket == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "校验凭证不存在，请重新验证");
        }
        if (!purpose.equals(ticket.getPurpose())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "校验凭证类型不匹配");
        }
        if (ticket.getExpireAt().isBefore(LocalDateTime.now())) {
            authRedisStore.deleteVerifyTicket(token);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "校验凭证已过期，请重新验证");
        }
        if (!subject.equals(ticket.getSubject())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "微信绑定凭证无效");
        }
        return ticket;
    }

    private AuthRedisStore.WechatQrCache requireWechatSession(String sceneId) {
        AuthRedisStore.WechatQrCache session = authRedisStore.getWechatQrSession(sceneId);
        if (session == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "二维码会话不存在，请刷新二维码");
        }
        return session;
    }

    private AuthUserDO requireUser(String phone, String message) {
        AuthUserDO user = findUserByPhone(phone);
        if (user == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
        if (!isAccountActive(user)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, defaultText(user.getBanReason(), "当前账号已被禁用，请联系平台处理"));
        }
        return user;
    }

    private AuthUserDO requireUserById(Long userId) {
        long startAt = System.currentTimeMillis();
        AuthUserDO user = authUserMapper.selectById(userId);
        log.info("mysql query success target=auth_user_by_id costMs={} hit={} userId={}",
            System.currentTimeMillis() - startAt, user != null, userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录用户不存在，请重新登录");
        }
        if (!isAccountActive(user)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, defaultText(user.getBanReason(), "当前账号已被禁用，请联系平台处理"));
        }
        return user;
    }

    private LoginResult buildLoginResult(AuthUserDO user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireAt = LocalDateTime.now().plusDays(TOKEN_EXPIRE_DAYS);
        authRedisStore.saveLoginSession(token, new AuthRedisStore.LoginSessionCache(
            token,
            user.getId(),
            user.getNickname(),
            user.getPhone(),
            Boolean.TRUE.equals(user.getVerified()),
            hasPassword(user),
            expireAt
        ), TOKEN_EXPIRE_DAYS);
        return new LoginResult(token, TOKEN_EXPIRE_DAYS, new UserProfile(
            user.getId(),
            user.getNickname(),
            user.getPhone(),
            Boolean.TRUE.equals(user.getVerified()),
            hasPassword(user)
        ));
    }

    private void validatePassword(String password, String confirmPassword) {
        if (password == null || !password.matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,18}$")) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "密码需为 6-18 位字母和数字组合");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "两次输入的密码不一致");
        }
    }

    private String normalizeScene(String scene) {
        if (scene == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "验证码场景不能为空");
        }
        String normalized = scene.trim().toUpperCase();
        if (!"LOGIN".equals(normalized)
            && !"REGISTER".equals(normalized)
            && !"RESET_PASSWORD".equals(normalized)
            && !"BIND_PHONE".equals(normalized)
            && !"SECURITY_VERIFY".equals(normalized)) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "不支持的验证码场景");
        }
        return normalized;
    }

    private String buildNickname(String phone) {
        return "用户" + phone.substring(phone.length() - 4);
    }

    private int smsExpireMinutes() {
        if ("tencent".equalsIgnoreCase(smsProperties.getProvider())) {
            return Math.max(smsProperties.getTencent().getValidMinutes(), 1);
        }
        return Math.max(smsProperties.getPnvs().getValidMinutes(), 1);
    }

    private int smsCooldownSeconds() {
        if ("tencent".equalsIgnoreCase(smsProperties.getProvider())) {
            return Math.max(smsProperties.getTencent().getIntervalSeconds(), 1);
        }
        return Math.max(smsProperties.getPnvs().getIntervalSeconds(), 1);
    }

    private String buildSmsOutId(String scene) {
        String traceId = MDC.get("traceId");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        if (traceId == null || traceId.isEmpty()) {
            return "AUTH-" + scene + "-" + suffix;
        }
        return "AUTH-" + scene + "-" + traceId.substring(0, Math.min(traceId.length(), 12)) + "-" + suffix;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : encoded) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "密码摘要算法不可用");
        }
    }

    private String buildFaceOrderId() {
        return "RN" + LocalDateTime.now().format(FACE_ORDER_FORMATTER) + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String buildFaceFailReason(JuheFaceGateway.SearchResult result) {
        StringBuilder builder = new StringBuilder();
        appendReason(builder, result.getMessage());
        appendReason(builder, result.getLiveMessage());
        appendReason(builder, result.getCertifyMessage());
        appendReason(builder, result.getReason());
        return builder.length() == 0 ? "实名认证未通过，请重新发起认证" : builder.toString();
    }

    private void appendReason(StringBuilder builder, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        String trimmed = value.trim();
        if (builder.indexOf(trimmed) >= 0) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("；");
        }
        builder.append(trimmed);
    }

    private String extractNotifyJhOrderId(Map<String, String> params, String body) {
        String fromParams = extractJhOrderIdFromParams(params);
        if (StringUtils.hasText(fromParams)) {
            return fromParams;
        }
        if (!StringUtils.hasText(body)) {
            return "";
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("{")) {
            return extractJhOrderIdFromJson(trimmed);
        }
        String[] pairs = trimmed.split("&");
        for (String pair : pairs) {
            int split = pair.indexOf('=');
            if (split <= 0) {
                continue;
            }
            String key = pair.substring(0, split);
            String value = java.net.URLDecoder.decode(pair.substring(split + 1), StandardCharsets.UTF_8);
            if ("jh_order_id".equals(key) || "jhOrderId".equals(key)) {
                return value;
            }
            if ("response".equals(key)) {
                String parsed = extractJhOrderIdFromJson(value);
                if (StringUtils.hasText(parsed)) {
                    return parsed;
                }
            }
        }
        return "";
    }

    private String extractJhOrderIdFromParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        String direct = firstText(params.get("jh_order_id"), params.get("jhOrderId"));
        if (StringUtils.hasText(direct)) {
            return direct;
        }
        String response = params.get("response");
        if (StringUtils.hasText(response)) {
            return extractJhOrderIdFromJson(response);
        }
        return "";
    }

    private String extractJhOrderIdFromJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return firstText(
                root.path("jh_order_id").asText(""),
                root.path("jhOrderId").asText(""),
                root.path("result").path("jh_order_id").asText(""),
                root.path("result").path("jhOrderId").asText("")
            );
        } catch (Exception exception) {
            log.warn("juhe face notify json parse failed body={}", json);
            return "";
        }
    }

    private String firstText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private AuthUserDO findUserByPhone(String phone) {
        long startAt = System.currentTimeMillis();
        AuthUserDO user = authUserMapper.selectOne(Wrappers.<AuthUserDO>lambdaQuery().eq(AuthUserDO::getPhone, phone).last("LIMIT 1"));
        log.info("mysql query success target=auth_user_by_phone costMs={} hit={} phone={}",
            System.currentTimeMillis() - startAt, user != null, maskPhone(phone));
        return user;
    }

    private AuthUserDO findUserByOpenId(String openId) {
        long startAt = System.currentTimeMillis();
        AuthUserDO user = authUserMapper.selectOne(Wrappers.<AuthUserDO>lambdaQuery().eq(AuthUserDO::getOpenId, openId).last("LIMIT 1"));
        log.info("mysql query success target=auth_user_by_open_id costMs={} hit={} openId={}",
            System.currentTimeMillis() - startAt, user != null, maskOpenId(openId));
        return user;
    }

    private void insertUser(AuthUserDO user) {
        long startAt = System.currentTimeMillis();
        int rows = authUserMapper.insert(user);
        log.info("mysql insert success target=auth_user costMs={} rows={} userId={} phone={}",
            System.currentTimeMillis() - startAt, rows, user.getId(), maskPhone(user.getPhone()));
    }

    private void updateUser(AuthUserDO user) {
        long startAt = System.currentTimeMillis();
        int rows = authUserMapper.updateById(user);
        log.info("mysql update success target=auth_user costMs={} rows={} userId={} phone={}",
            System.currentTimeMillis() - startAt, rows, user.getId(), maskPhone(user.getPhone()));
    }

    private void insertFaceSession(RealNameFaceSessionDO session) {
        long startAt = System.currentTimeMillis();
        int rows = realNameFaceSessionMapper.insert(session);
        log.info("mysql insert success target=real_name_face_session costMs={} rows={} userId={} orderId={}",
            System.currentTimeMillis() - startAt, rows, session.getUserId(), session.getOrderId());
    }

    private void updateFaceSession(RealNameFaceSessionDO session) {
        long startAt = System.currentTimeMillis();
        int rows = realNameFaceSessionMapper.updateById(session);
        log.info("mysql update success target=real_name_face_session costMs={} rows={} userId={} orderId={} status={}",
            System.currentTimeMillis() - startAt, rows, session.getUserId(), session.getOrderId(), session.getStatus());
    }

    private RealNameFaceSessionDO findFaceSessionByOrderId(String orderId) {
        String normalized = orderId == null ? "" : orderId.trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        long startAt = System.currentTimeMillis();
        RealNameFaceSessionDO session = realNameFaceSessionMapper.selectOne(
            Wrappers.<RealNameFaceSessionDO>lambdaQuery().eq(RealNameFaceSessionDO::getOrderId, normalized).last("LIMIT 1")
        );
        log.info("mysql query success target=real_name_face_session_by_order_id costMs={} hit={} orderId={}",
            System.currentTimeMillis() - startAt, session != null, normalized);
        return session;
    }

    private RealNameFaceSessionDO findFaceSessionByJhOrderId(String jhOrderId) {
        String normalized = jhOrderId == null ? "" : jhOrderId.trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        long startAt = System.currentTimeMillis();
        RealNameFaceSessionDO session = realNameFaceSessionMapper.selectOne(
            Wrappers.<RealNameFaceSessionDO>lambdaQuery().eq(RealNameFaceSessionDO::getJhOrderId, normalized).last("LIMIT 1")
        );
        log.info("mysql query success target=real_name_face_session_by_jh_order_id costMs={} hit={} jhOrderId={}",
            System.currentTimeMillis() - startAt, session != null, maskFaceOrderId(normalized));
        return session;
    }

    private void refreshFaceSessionFromProvider(RealNameFaceSessionDO session, String token) {
        if ("APPROVED".equals(session.getStatus()) || "REJECTED".equals(session.getStatus())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (session.getCreatedAt() != null && session.getCreatedAt().plusMinutes(FACE_VERIFY_EXPIRE_MINUTES).isBefore(now)) {
            completeFaceSession(session, "EXPIRED", "认证链接已过期，请重新发起认证", null, token);
            return;
        }
        JuheFaceGateway.SearchResult searchResult = juheFaceGateway.searchVerification(session.getJhOrderId());
        if (searchResult.getErrorCode() != 0) {
            if (isFaceVerificationPending(searchResult.getErrorCode())) {
                session.setRawResult(searchResult.getRawResult());
                session.setUpdatedAt(LocalDateTime.now());
                updateFaceSession(session);
                log.info("real name face still pending userId={} orderId={} errorCode={}",
                    session.getUserId(), session.getOrderId(), searchResult.getErrorCode());
                return;
            }
            completeFaceSession(session, "REJECTED", defaultText(searchResult.getReason(), "认证结果查询失败"), searchResult.getRawResult(), token);
            return;
        }
        if (searchResult.isPassed()) {
            completeFaceSession(session, "APPROVED", null, searchResult.getRawResult(), token);
            return;
        }
        completeFaceSession(session, "REJECTED", buildFaceFailReason(searchResult), searchResult.getRawResult(), token);
    }

    private void completeFaceSession(RealNameFaceSessionDO session, String status, String failReason, String rawResult, String token) {
        LocalDateTime now = LocalDateTime.now();
        session.setStatus(status);
        session.setFailReason(failReason);
        if (StringUtils.hasText(rawResult)) {
            session.setRawResult(rawResult);
        }
        session.setUpdatedAt(now);
        if (!"PENDING".equals(status)) {
            session.setCompletedAt(now);
        }
        updateFaceSession(session);

        AuthUserDO user = requireUserById(session.getUserId());
        user.setRealName(session.getRealName());
        user.setRealNamePhone(StringUtils.hasText(user.getPhone()) ? user.getPhone() : user.getRealNamePhone());
        user.setIdCardNo(session.getIdCardNo());
        user.setVerified("APPROVED".equals(status));
        user.setRealNameStatus("APPROVED".equals(status) ? "APPROVED" : "REJECTED");
        user.setRealNameRejectReason("APPROVED".equals(status) ? null : failReason);
        user.setUpdatedAt(now);
        updateUser(user);
        if (StringUtils.hasText(token)) {
            refreshLoginVerified(token, user);
        }
        log.info("real name face status synced userId={} orderId={} status={}", user.getId(), session.getOrderId(), status);
    }

    private boolean isFaceVerificationPending(int errorCode) {
        return errorCode == 279306 || errorCode == 279307;
    }

    private boolean hasPassword(AuthUserDO user) {
        return user.getPasswordHash() != null && !user.getPasswordHash().isEmpty();
    }

    private boolean hasPhoneBinding(AuthUserDO user) {
        return user.getPhone() != null && !user.getPhone().trim().isEmpty();
    }

    private boolean hasWechatBinding(AuthUserDO user) {
        return user.getOpenId() != null && !user.getOpenId().trim().isEmpty();
    }

    private boolean hasRealNameProfile(AuthUserDO user) {
        return user.getRealName() != null && !user.getRealName().isEmpty()
            && ((user.getRealNamePhone() != null && !user.getRealNamePhone().isEmpty()) || (user.getPhone() != null && !user.getPhone().isEmpty()))
            && user.getIdCardNo() != null && !user.getIdCardNo().isEmpty()
            && user.getRealNameFrontKey() != null && !user.getRealNameFrontKey().isEmpty()
            && user.getRealNameBackKey() != null && !user.getRealNameBackKey().isEmpty();
    }

    private boolean isAccountActive(AuthUserDO user) {
        return user.getAccountStatus() == null || "ACTIVE".equalsIgnoreCase(user.getAccountStatus());
    }

    private boolean isLoginAlertEnabled(AuthUserDO user) {
        return user.getLoginAlertEnabled() == null || Boolean.TRUE.equals(user.getLoginAlertEnabled());
    }

    private boolean isSecondaryVerifyEnabled(AuthUserDO user) {
        return Boolean.TRUE.equals(user.getSecondaryVerifyEnabled());
    }

    private void refreshLoginVerified(String token, AuthUserDO user) {
        refreshLoginSession(token, user);
    }

    private void refreshLoginSession(String token, AuthUserDO user) {
        AuthRedisStore.LoginSessionCache session = authRedisStore.getLoginSession(token);
        if (session == null) {
            log.warn("auth login session missing when refreshing profile userId={}", user.getId());
            return;
        }
        session.setNickname(user.getNickname());
        session.setPhone(user.getPhone());
        session.setVerified(Boolean.TRUE.equals(user.getVerified()));
        session.setHasPassword(hasPassword(user));
        authRedisStore.refreshLoginSession(session);
        log.info("auth login session refreshed userId={} verified={} hasPassword={} phone={}",
            user.getId(), session.isVerified(), session.isHasPassword(), maskPhone(session.getPhone()));
    }

    private WithdrawAccountDO findWithdrawAccount(Long userId) {
        long startAt = System.currentTimeMillis();
        WithdrawAccountDO account = withdrawAccountMapper.selectOne(
            Wrappers.<WithdrawAccountDO>lambdaQuery().eq(WithdrawAccountDO::getUserId, userId).last("LIMIT 1")
        );
        log.info("mysql query success target=withdraw_account_by_user_id costMs={} hit={} userId={}",
            System.currentTimeMillis() - startAt, account != null, userId);
        return account;
    }

    private void seedUserIfAbsent(String phone, String nickname, String passwordHash, String openId, boolean verified) {
        AuthUserDO existing = findUserByPhone(phone);
        if (existing != null) {
            return;
        }
        AuthUserDO user = new AuthUserDO();
        LocalDateTime now = LocalDateTime.now();
        user.setPhone(phone);
        user.setNickname(nickname);
        user.setPasswordHash(passwordHash);
        user.setOpenId(openId);
        user.setAvatarKey(null);
        user.setAccountStatus("ACTIVE");
        user.setBanReason(null);
        user.setRealNameStatus(verified ? "APPROVED" : "UNVERIFIED");
        user.setRealNameRejectReason(null);
        user.setRealNameFrontKey(null);
        user.setRealNameBackKey(null);
        user.setVerified(verified);
        user.setLoginAlertEnabled(true);
        user.setSecondaryVerifyEnabled(false);
        user.setDistributionEnabled(false);
        user.setDistributionOpenedAt(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        insertUser(user);
        log.info("seed auth user success userId={} phone={}", user.getId(), maskPhone(phone));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return safeText(phone);
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String encodeRedirectUri(String redirectUri) {
        try {
            return URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信回调地址编码失败");
        }
    }

    private String maskOpenId(String openId) {
        if (openId == null || openId.length() < 8) {
            return openId;
        }
        return openId.substring(0, 4) + "****" + openId.substring(openId.length() - 4);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String resolveRealNameStatus(AuthUserDO user) {
        if (user.getRealNameStatus() != null && !user.getRealNameStatus().trim().isEmpty()) {
            String status = user.getRealNameStatus().trim().toUpperCase();
            if (Boolean.TRUE.equals(user.getVerified()) && "UNVERIFIED".equals(status)) {
                return "APPROVED";
            }
            return status;
        }
        return Boolean.TRUE.equals(user.getVerified()) ? "APPROVED" : "UNVERIFIED";
    }

    private boolean hasOssObjectKey(String objectKey) {
        return objectKey != null && !objectKey.trim().isEmpty();
    }

    private String previewNullable(String objectKey) {
        if (!hasOssObjectKey(objectKey)) {
            return "";
        }
        return ossStorageService.previewUrl(objectKey.trim());
    }

    private String maskIdCard(String idCardNo) {
        if (idCardNo == null || idCardNo.length() < 8) {
            return "";
        }
        return idCardNo.substring(0, 6) + "********" + idCardNo.substring(idCardNo.length() - 4);
    }

    private String maskFaceOrderId(String orderId) {
        if (!StringUtils.hasText(orderId) || orderId.length() < 8) {
            return safeText(orderId);
        }
        return orderId.substring(0, 4) + "****" + orderId.substring(orderId.length() - 4);
    }

    private String maskRealName(String realName) {
        if (realName == null || realName.isEmpty()) {
            return "";
        }
        if (realName.length() == 1) {
            return "*";
        }
        if (realName.length() == 2) {
            return realName.substring(0, 1) + "*";
        }
        return realName.substring(0, 1) + "*" + realName.substring(realName.length() - 1);
    }

    private String subjectFromVerifyToken(String bindToken) {
        AuthRedisStore.VerifyTicketCache ticket = authRedisStore.getVerifyTicket(bindToken);
        return ticket == null ? "" : ticket.getSubject();
    }

    private String renderChannel(String channel) {
        return "WECHAT".equals(channel) ? "微信" : "支付宝";
    }

    private String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.length() < 8) {
            return "";
        }
        return accountNo.substring(0, 4) + "****" + accountNo.substring(accountNo.length() - 4);
    }

    public static class SmsCodeResult {
        private final String phone;
        private final String scene;
        private final LocalDateTime expireAt;
        private final int cooldownSeconds;
        private final String hint;

        public SmsCodeResult(String phone, String scene, LocalDateTime expireAt, int cooldownSeconds, String hint) {
            this.phone = phone;
            this.scene = scene;
            this.expireAt = expireAt;
            this.cooldownSeconds = cooldownSeconds;
            this.hint = hint;
        }

        public String getPhone() {
            return phone;
        }

        public String getScene() {
            return scene;
        }

        public LocalDateTime getExpireAt() {
            return expireAt;
        }

        public int getCooldownSeconds() {
            return cooldownSeconds;
        }

        public String getHint() {
            return hint;
        }
    }

    public static class VerifyTicketResult {
        private final String verifyToken;
        private final LocalDateTime expireAt;

        public VerifyTicketResult(String verifyToken, LocalDateTime expireAt) {
            this.verifyToken = verifyToken;
            this.expireAt = expireAt;
        }

        public String getVerifyToken() {
            return verifyToken;
        }

        public LocalDateTime getExpireAt() {
            return expireAt;
        }
    }

    public static class WechatQrResult {
        private final String sceneId;
        private final String qrCodeUrl;
        private final String authorizeUrl;
        private final LocalDateTime expireAt;
        private final int pollIntervalSeconds;

        public WechatQrResult(String sceneId, String qrCodeUrl, String authorizeUrl, LocalDateTime expireAt, int pollIntervalSeconds) {
            this.sceneId = sceneId;
            this.qrCodeUrl = qrCodeUrl;
            this.authorizeUrl = authorizeUrl;
            this.expireAt = expireAt;
            this.pollIntervalSeconds = pollIntervalSeconds;
        }

        public String getSceneId() {
            return sceneId;
        }

        public String getQrCodeUrl() {
            return qrCodeUrl;
        }

        public String getAuthorizeUrl() {
            return authorizeUrl;
        }

        public LocalDateTime getExpireAt() {
            return expireAt;
        }

        public int getPollIntervalSeconds() {
            return pollIntervalSeconds;
        }
    }

    public static class WechatPollResult {
        private final String status;
        private final LoginResult loginResult;
        private final String bindToken;
        private final String maskedPhone;

        public WechatPollResult(String status, LoginResult loginResult, String bindToken, String maskedPhone) {
            this.status = status;
            this.loginResult = loginResult;
            this.bindToken = bindToken;
            this.maskedPhone = maskedPhone;
        }

        public String getStatus() {
            return status;
        }

        public LoginResult getLoginResult() {
            return loginResult;
        }

        public String getBindToken() {
            return bindToken;
        }

        public String getMaskedPhone() {
            return maskedPhone;
        }
    }

    public static class WechatCallbackPage {
        private final boolean success;
        private final String title;
        private final String description;
        private final LoginResult loginResult;
        private final String bindToken;
        private final String returnPath;

        public WechatCallbackPage(boolean success, String title, String description, LoginResult loginResult, String bindToken, String returnPath) {
            this.success = success;
            this.title = title;
            this.description = description;
            this.loginResult = loginResult;
            this.bindToken = bindToken;
            this.returnPath = returnPath;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public LoginResult getLoginResult() {
            return loginResult;
        }

        public String getBindToken() {
            return bindToken;
        }

        public String getReturnPath() {
            return returnPath;
        }
    }

    public static class SimpleResult {
        private final String status;
        private final String message;

        public SimpleResult(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class LoginResult {
        private final String token;
        private final int expireDays;
        private final UserProfile profile;

        public LoginResult(String token, int expireDays, UserProfile profile) {
            this.token = token;
            this.expireDays = expireDays;
            this.profile = profile;
        }

        public String getToken() {
            return token;
        }

        public int getExpireDays() {
            return expireDays;
        }

        public UserProfile getProfile() {
            return profile;
        }
    }

    public static class RealNameProfile {
        private final boolean verified;
        private final String status;
        private final String realName;
        private final String phone;
        private final String maskedIdCardNo;
        private final String rejectReason;
        private final String idCardFrontKey;
        private final String idCardBackKey;
        private final String idCardFrontUrl;
        private final String idCardBackUrl;

        public RealNameProfile(
            boolean verified,
            String status,
            String realName,
            String phone,
            String maskedIdCardNo,
            String rejectReason,
            String idCardFrontKey,
            String idCardBackKey,
            String idCardFrontUrl,
            String idCardBackUrl
        ) {
            this.verified = verified;
            this.status = status;
            this.realName = realName;
            this.phone = phone;
            this.maskedIdCardNo = maskedIdCardNo;
            this.rejectReason = rejectReason;
            this.idCardFrontKey = idCardFrontKey;
            this.idCardBackKey = idCardBackKey;
            this.idCardFrontUrl = idCardFrontUrl;
            this.idCardBackUrl = idCardBackUrl;
        }

        public boolean isVerified() {
            return verified;
        }

        public String getStatus() {
            return status;
        }

        public String getRealName() {
            return realName;
        }

        public String getPhone() {
            return phone;
        }

        public String getMaskedIdCardNo() {
            return maskedIdCardNo;
        }

        public String getRejectReason() {
            return rejectReason;
        }

        public String getIdCardFrontKey() {
            return idCardFrontKey;
        }

        public String getIdCardBackKey() {
            return idCardBackKey;
        }

        public String getIdCardFrontUrl() {
            return idCardFrontUrl;
        }

        public String getIdCardBackUrl() {
            return idCardBackUrl;
        }
    }

    public static class FaceRealNameStartResult {
        private final String orderId;
        private final String jhOrderId;
        private final String verifyUrl;
        private final LocalDateTime expireAt;

        public FaceRealNameStartResult(String orderId, String jhOrderId, String verifyUrl, LocalDateTime expireAt) {
            this.orderId = orderId;
            this.jhOrderId = jhOrderId;
            this.verifyUrl = verifyUrl;
            this.expireAt = expireAt;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getJhOrderId() {
            return jhOrderId;
        }

        public String getVerifyUrl() {
            return verifyUrl;
        }

        public LocalDateTime getExpireAt() {
            return expireAt;
        }
    }

    public static class SettingsProfile {
        private final String nickname;
        private final String phone;
        private final boolean phoneBound;
        private final boolean wechatBound;
        private final boolean verified;
        private final String realNameStatus;
        private final String realName;
        private final String maskedIdCardNo;
        private final String realNameRejectReason;
        private final String avatarUrl;
        private final String idCardFrontUrl;
        private final String idCardBackUrl;
        private final boolean loginAlertEnabled;
        private final boolean secondaryVerifyEnabled;
        private final String withdrawAccountLabel;

        public SettingsProfile(
            String nickname,
            String phone,
            boolean phoneBound,
            boolean wechatBound,
            boolean verified,
            String realNameStatus,
            String realName,
            String maskedIdCardNo,
            String realNameRejectReason,
            String avatarUrl,
            String idCardFrontUrl,
            String idCardBackUrl,
            boolean loginAlertEnabled,
            boolean secondaryVerifyEnabled,
            String withdrawAccountLabel
        ) {
            this.nickname = nickname;
            this.phone = phone;
            this.phoneBound = phoneBound;
            this.wechatBound = wechatBound;
            this.verified = verified;
            this.realNameStatus = realNameStatus;
            this.realName = realName;
            this.maskedIdCardNo = maskedIdCardNo;
            this.realNameRejectReason = realNameRejectReason;
            this.avatarUrl = avatarUrl;
            this.idCardFrontUrl = idCardFrontUrl;
            this.idCardBackUrl = idCardBackUrl;
            this.loginAlertEnabled = loginAlertEnabled;
            this.secondaryVerifyEnabled = secondaryVerifyEnabled;
            this.withdrawAccountLabel = withdrawAccountLabel;
        }

        public String getNickname() { return nickname; }
        public String getPhone() { return phone; }
        public boolean isPhoneBound() { return phoneBound; }
        public boolean isWechatBound() { return wechatBound; }
        public boolean isVerified() { return verified; }
        public String getRealNameStatus() { return realNameStatus; }
        public String getRealName() { return realName; }
        public String getMaskedIdCardNo() { return maskedIdCardNo; }
        public String getRealNameRejectReason() { return realNameRejectReason; }
        public String getAvatarUrl() { return avatarUrl; }
        public String getIdCardFrontUrl() { return idCardFrontUrl; }
        public String getIdCardBackUrl() { return idCardBackUrl; }
        public boolean isLoginAlertEnabled() { return loginAlertEnabled; }
        public boolean isSecondaryVerifyEnabled() { return secondaryVerifyEnabled; }
        public String getWithdrawAccountLabel() { return withdrawAccountLabel; }
    }

    public static class UserProfile {
        private final Long userId;
        private final String nickname;
        private final String phone;
        private final boolean verified;
        private final boolean hasPassword;

        public UserProfile(Long userId, String nickname, String phone, boolean verified, boolean hasPassword) {
            this.userId = userId;
            this.nickname = nickname;
            this.phone = phone;
            this.verified = verified;
            this.hasPassword = hasPassword;
        }

        public Long getUserId() {
            return userId;
        }

        public String getNickname() {
            return nickname;
        }

        public String getPhone() {
            return phone;
        }

        public boolean isVerified() {
            return verified;
        }

        public boolean isHasPassword() {
            return hasPassword;
        }
    }
}
