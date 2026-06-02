package com.deltatrade.platform.modules.auth.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.common.auth.AuthContext;
import com.deltatrade.platform.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.deltatrade.platform.modules.auth.config.PlatformWechatProperties;
import com.deltatrade.platform.modules.auth.service.AuthService;
import com.deltatrade.platform.modules.auth.service.WechatOpenGateway;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final int WECHAT_QR_SIZE = 280;

    private final AuthService authService;
    private final PlatformWechatProperties wechatProperties;
    private final WechatOpenGateway wechatOpenGateway;
    private final ObjectMapper objectMapper;

    public AuthController(AuthService authService, PlatformWechatProperties wechatProperties, WechatOpenGateway wechatOpenGateway, ObjectMapper objectMapper) {
        this.authService = authService;
        this.wechatProperties = wechatProperties;
        this.wechatOpenGateway = wechatOpenGateway;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/sms-code")
    public ApiResponse<AuthService.SmsCodeResult> sendSmsCode(@Valid @RequestBody SendSmsCodeRequest request) {
        return ApiResponse.success(authService.sendSmsCode(request.getPhone(), request.getScene()), MDC.get("traceId"));
    }

    @PostMapping("/sms-login")
    public ApiResponse<AuthService.LoginResult> smsLogin(@Valid @RequestBody SmsLoginRequest request) {
        return ApiResponse.success(authService.smsLogin(request.getPhone(), request.getCode()), MDC.get("traceId"));
    }

    @PostMapping("/password-login")
    public ApiResponse<AuthService.LoginResult> passwordLogin(@Valid @RequestBody PasswordLoginRequest request) {
        return ApiResponse.success(authService.passwordLogin(request.getPhone(), request.getPassword()), MDC.get("traceId"));
    }

    @PostMapping("/register/verify-code")
    public ApiResponse<AuthService.VerifyTicketResult> verifyRegisterCode(@Valid @RequestBody VerifyCodeRequest request) {
        return ApiResponse.success(authService.verifyRegisterCode(request.getPhone(), request.getCode()), MDC.get("traceId"));
    }

    @PostMapping("/register/complete")
    public ApiResponse<AuthService.LoginResult> completeRegister(@Valid @RequestBody CompleteRegisterRequest request) {
        return ApiResponse.success(
            authService.completeRegister(
                request.getPhone(),
                request.getVerifyToken(),
                request.getPassword(),
                request.getConfirmPassword(),
                request.getInviteCode()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/password-reset/verify-code")
    public ApiResponse<AuthService.VerifyTicketResult> verifyPasswordResetCode(@Valid @RequestBody VerifyCodeRequest request) {
        return ApiResponse.success(authService.verifyPasswordResetCode(request.getPhone(), request.getCode()), MDC.get("traceId"));
    }

    @PostMapping("/password-reset/complete")
    public ApiResponse<AuthService.SimpleResult> completePasswordReset(@Valid @RequestBody CompleteRegisterRequest request) {
        return ApiResponse.success(
            authService.completePasswordReset(request.getPhone(), request.getVerifyToken(), request.getPassword(), request.getConfirmPassword()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/wechat/qr-code")
    public ApiResponse<AuthService.WechatQrResult> createWechatQr(HttpServletRequest request, @RequestBody(required = false) WechatQrCreateRequest requestBody) {
        return ApiResponse.success(
            authService.createWechatQr(
                resolveWechatRedirectUri(request),
                requestBody == null ? null : requestBody.getClientMode(),
                requestBody == null ? null : requestBody.getReturnPath()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/wechat/poll")
    public ApiResponse<AuthService.WechatPollResult> pollWechatQr(@Valid @RequestBody WechatPollRequest request) {
        return ApiResponse.success(authService.pollWechatQr(request.getSceneId()), MDC.get("traceId"));
    }

    @PostMapping("/wechat/bind-phone")
    public ApiResponse<AuthService.LoginResult> bindWechatPhone(@Valid @RequestBody WechatBindPhoneRequest request) {
        return ApiResponse.success(
            authService.bindWechatPhone(request.getBindToken(), request.getPhone(), request.getCode(), request.getInviteCode()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/logout")
    public ApiResponse<AuthService.SimpleResult> logout() {
        return ApiResponse.success(authService.logout(AuthContext.requirePrincipal().getToken()), MDC.get("traceId"));
    }

    @GetMapping("/real-name")
    public ApiResponse<AuthService.RealNameProfile> realNameProfile() {
        return ApiResponse.success(authService.getRealNameProfile(AuthContext.requirePrincipal().getUserId()), MDC.get("traceId"));
    }

    @PostMapping("/real-name")
    public ApiResponse<AuthService.RealNameProfile> submitRealName(@Valid @RequestBody SubmitRealNameRequest request) {
        return ApiResponse.success(
            authService.submitRealName(
                AuthContext.requirePrincipal().getToken(),
                AuthContext.requirePrincipal().getUserId(),
                request.getRealName(),
                request.getPhone(),
                request.getIdCardNo(),
                request.getIdCardFrontKey(),
                request.getIdCardBackKey()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/real-name/face/start")
    public ApiResponse<AuthService.FaceRealNameStartResult> startFaceRealName(@Valid @RequestBody StartFaceRealNameRequest request) {
        return ApiResponse.success(
            authService.startFaceRealName(
                AuthContext.requirePrincipal().getToken(),
                AuthContext.requirePrincipal().getUserId(),
                request.getRealName(),
                request.getIdCardNo()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/real-name/face/status")
    public ApiResponse<AuthService.RealNameProfile> checkFaceRealNameStatus(@Valid @RequestBody FaceRealNameStatusRequest request) {
        return ApiResponse.success(
            authService.checkFaceRealNameStatus(
                AuthContext.requirePrincipal().getToken(),
                AuthContext.requirePrincipal().getUserId(),
                request.getOrderId()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/real-name/face/notify")
    public ApiResponse<AuthService.SimpleResult> notifyFaceRealName(
        @RequestParam Map<String, String> params,
        @RequestBody(required = false) String body
    ) {
        return ApiResponse.success(authService.handleFaceNotify(params, body), MDC.get("traceId"));
    }

    @GetMapping("/settings")
    public ApiResponse<AuthService.SettingsProfile> settingsProfile() {
        return ApiResponse.success(authService.getSettingsProfile(AuthContext.requirePrincipal().getUserId()), MDC.get("traceId"));
    }

    @PostMapping("/settings/nickname")
    public ApiResponse<AuthService.SettingsProfile> updateNickname(@Valid @RequestBody UpdateNicknameRequest request) {
        return ApiResponse.success(
            authService.updateNickname(AuthContext.requirePrincipal().getToken(), AuthContext.requirePrincipal().getUserId(), request.getNickname()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/settings/avatar")
    public ApiResponse<AuthService.SettingsProfile> updateAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        return ApiResponse.success(
            authService.updateAvatar(AuthContext.requirePrincipal().getToken(), AuthContext.requirePrincipal().getUserId(), request.getAvatarKey()),
            MDC.get("traceId")
        );
    }

    @PostMapping("/settings/password")
    public ApiResponse<AuthService.SimpleResult> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ApiResponse.success(
            authService.changePassword(
                AuthContext.requirePrincipal().getToken(),
                AuthContext.requirePrincipal().getUserId(),
                request.getCurrentPassword(),
                request.getNextPassword(),
                request.getConfirmPassword()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/settings/phone")
    public ApiResponse<AuthService.SettingsProfile> changePhone(@Valid @RequestBody ChangePhoneRequest request) {
        return ApiResponse.success(
            authService.changePhone(
                AuthContext.requirePrincipal().getToken(),
                AuthContext.requirePrincipal().getUserId(),
                request.getPhone(),
                request.getCode()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/settings/phone/unbind")
    public ApiResponse<AuthService.SettingsProfile> unbindPhone(@Valid @RequestBody SecurityVerifyRequest request) {
        return ApiResponse.success(
            authService.unbindPhone(
                AuthContext.requirePrincipal().getToken(),
                AuthContext.requirePrincipal().getUserId(),
                request.getCode()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/settings/security")
    public ApiResponse<AuthService.SettingsProfile> updateSecurity(@Valid @RequestBody UpdateSecurityRequest request) {
        return ApiResponse.success(
            authService.updateSecurity(
                AuthContext.requirePrincipal().getToken(),
                AuthContext.requirePrincipal().getUserId(),
                request.isLoginAlertEnabled(),
                request.isSecondaryVerifyEnabled()
            ),
            MDC.get("traceId")
        );
    }

    @PostMapping("/settings/wechat/unbind")
    public ApiResponse<AuthService.SettingsProfile> unbindWechat() {
        return ApiResponse.success(
            authService.unbindWechat(AuthContext.requirePrincipal().getToken(), AuthContext.requirePrincipal().getUserId()),
            MDC.get("traceId")
        );
    }

    @GetMapping(value = "/wechat/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> wechatCallback(
        HttpServletRequest request,
        @RequestParam(value = "code", required = false) String code,
        @RequestParam(value = "state", required = false) String state,
        @RequestParam(value = "error", required = false) String error,
        @RequestParam(value = "error_description", required = false) String error_description
    ) {
        try {
            AuthService.WechatCallbackPage page = authService.handleWechatCallback(state, code, error, error_description);
            return htmlResponse(renderWechatCallbackPage(page));
        } catch (BusinessException exception) {
            log.warn("wechat callback business failure state={} message={} path={}", state, exception.getMessage(), request.getRequestURI());
            return htmlResponse(renderWechatCallbackPage(new AuthService.WechatCallbackPage(
                false,
                "微信授权失败",
                exception.getMessage(),
                null,
                null,
                "/"
            )));
        } catch (Exception exception) {
            log.error("wechat callback unexpected failure state={} path={}", state, request.getRequestURI(), exception);
            return htmlResponse(renderWechatCallbackPage(new AuthService.WechatCallbackPage(
                false,
                "微信授权失败",
                "系统处理微信回调时发生异常，请稍后重新发起微信登录。",
                null,
                null,
                "/"
            )));
        }
    }

    @GetMapping(value = "/wechat/qr-page", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> wechatQrPage(
        @RequestParam("sceneId") String sceneId,
        @RequestParam("redirectUri") String redirectUri
    ) {
        String authorizeUrl = wechatOpenGateway.buildQrConnectUrl(sceneId, redirectUri);
        String qrImageUrl = "/api/auth/wechat/qr-image?sceneId=" + encode(sceneId) + "&redirectUri=" + encode(redirectUri);
        String html = "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            + "<title>微信扫码登录</title>"
            + "<style>"
            + "body{margin:0;display:grid;place-items:center;min-height:100vh;background:#fff;overflow:hidden;}"
            + ".code{display:block;width:min(100vw,304px);height:min(100vw,304px);object-fit:contain;}"
            + "</style></head><body>"
            + "<a href=\"" + authorizeUrl + "\" target=\"_blank\" rel=\"noopener noreferrer\" aria-label=\"新窗口打开微信授权页\">"
            + "<img class=\"code\" src=\"" + qrImageUrl + "\" alt=\"微信扫码登录二维码\" />"
            + "</a>"
            + "</body></html>";
        return htmlResponse(html);
    }

    @GetMapping(value = "/wechat/qr-image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> wechatQrImage(
        @RequestParam("sceneId") String sceneId,
        @RequestParam("redirectUri") String redirectUri
    ) {
        String authorizeUrl = wechatOpenGateway.buildQrConnectUrl(sceneId, redirectUri);
        try {
            byte[] image = renderQrPng(authorizeUrl);
            return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, max-age=0")
                .body(image);
        } catch (Exception exception) {
            log.error("wechat qr image generate failed sceneId={} traceId={}", sceneId, MDC.get("traceId"), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_PLAIN)
                .body("wechat qr image generate failed".getBytes(StandardCharsets.UTF_8));
        }
    }

    private ResponseEntity<String> htmlResponse(String html) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
            .body(html);
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException exception) {
            throw new BusinessException(com.deltatrade.platform.common.exception.ErrorCode.SYSTEM_ERROR, "微信二维码地址编码失败");
        }
    }

    private byte[] renderQrPng(String content) throws WriterException, java.io.IOException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, WECHAT_QR_SIZE, WECHAT_QR_SIZE, hints);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    private String resolveWechatRedirectUri(HttpServletRequest request) {
        if (StringUtils.hasText(wechatProperties.getRedirectUri())) {
            return wechatProperties.getRedirectUri().trim();
        }
        String proto = Optional.ofNullable(request.getHeader("X-Forwarded-Proto"))
            .filter(StringUtils::hasText)
            .orElse(request.getScheme());
        String host = Optional.ofNullable(request.getHeader("X-Forwarded-Host"))
            .filter(StringUtils::hasText)
            .orElseGet(() -> Optional.ofNullable(request.getHeader("Host")).orElse(request.getServerName() + ":" + request.getServerPort()));
        return proto + "://" + host + request.getContextPath() + "/api/auth/wechat/callback";
    }

    private String renderWechatCallbackPage(AuthService.WechatCallbackPage page) {
        String title = escapeHtml(page.getTitle());
        String description = escapeHtml(page.getDescription());
        String badge = page.isSuccess() ? "授权成功" : "授权失败";
        String badgeClass = page.isSuccess() ? "success" : "error";
        String script = buildWechatCallbackScript(page);
        return "<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
            + "<title>" + title + "</title>"
            + "<style>"
            + "body{margin:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#f5f7fb;color:#172033;display:grid;place-items:center;min-height:100vh;}"
            + ".card{width:min(92vw,420px);background:#fff;border-radius:24px;padding:32px 28px;box-shadow:0 18px 50px rgba(18,31,53,.12);text-align:center;}"
            + ".badge{display:inline-flex;padding:6px 14px;border-radius:999px;font-size:12px;font-weight:700;margin-bottom:18px;}"
            + ".badge.success{background:#e9f9ef;color:#12803c;}"
            + ".badge.error{background:#fff1f0;color:#c0352b;}"
            + "h1{margin:0 0 12px;font-size:24px;line-height:1.3;}"
            + "p{margin:0;color:#5c667a;line-height:1.8;font-size:14px;}"
            + "</style></head><body><div class=\"card\">"
            + "<span class=\"badge " + badgeClass + "\">" + badge + "</span>"
            + "<h1>" + title + "</h1>"
            + "<p>" + description + "</p>"
            + "</div>"
            + script
            + "</body></html>";
    }

    private String buildWechatCallbackScript(AuthService.WechatCallbackPage page) {
        AuthService.LoginResult loginResult = page.getLoginResult();
        String returnPath = page.getReturnPath();
        if (loginResult == null && !StringUtils.hasText(page.getBindToken())) {
            return "";
        }
        String normalizedReturnPath = StringUtils.hasText(returnPath) ? returnPath : "/";
        StringBuilder script = new StringBuilder();
        script.append("<script>(function(){");
        script.append("var targetPath=").append(toJsString(normalizedReturnPath)).append(";");
        script.append("function buildTargetUrl(path){try{return new URL(path, window.location.origin);}catch(_){return new URL('/', window.location.origin);}}");
        if (loginResult != null) {
            script.append("var session=").append(toJson(loginResult)).append(";");
            script.append("window.localStorage.setItem('delta_trade_session', JSON.stringify(session));");
            script.append("window.localStorage.setItem('delta_trade_token', session.token);");
            script.append("document.cookie='delta_trade_token='+encodeURIComponent(session.token)+'; Max-Age='+(session.expireDays*24*60*60)+'; Path=/; SameSite=Lax';");
            script.append("window.setTimeout(function(){window.location.replace(buildTargetUrl(targetPath).toString());}, 600);");
        } else {
            script.append("var targetUrl=buildTargetUrl(targetPath);");
            script.append("window.localStorage.removeItem('delta_trade_session');");
            script.append("window.localStorage.removeItem('delta_trade_token');");
            script.append("document.cookie='delta_trade_token=; Max-Age=0; Path=/; SameSite=Lax';");
            script.append("targetUrl.searchParams.set('wechat_bind','1');");
            script.append("targetUrl.searchParams.set('bind_token',").append(toJsString(page.getBindToken())).append(");");
            script.append("window.setTimeout(function(){window.location.replace(targetUrl.toString());}, 600);");
        }
        script.append("})();</script>");
        return script.toString();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(com.deltatrade.platform.common.exception.ErrorCode.SYSTEM_ERROR, "微信授权结果序列化失败");
        }
    }

    private String toJsString(String value) {
        return toJson(value == null ? "" : value);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
        return new String(escaped.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    public static class SendSmsCodeRequest {
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        private String phone;
        @NotBlank(message = "验证码场景不能为空")
        private String scene;

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
    }

    public static class WechatQrCreateRequest {
        private String clientMode;
        private String returnPath;

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
    }

    public static class SmsLoginRequest {
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        private String phone;
        @Pattern(regexp = "^\\d{6}$", message = "验证码需为 6 位数字")
        private String code;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public static class PasswordLoginRequest {
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        private String phone;
        @NotBlank(message = "密码不能为空")
        private String password;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class VerifyCodeRequest {
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        private String phone;
        @Pattern(regexp = "^\\d{6}$", message = "验证码需为 6 位数字")
        private String code;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public static class CompleteRegisterRequest {
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        private String phone;
        @NotBlank(message = "校验凭证不能为空")
        private String verifyToken;
        @NotBlank(message = "密码不能为空")
        private String password;
        @NotBlank(message = "确认密码不能为空")
        private String confirmPassword;
        private String inviteCode;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getVerifyToken() {
            return verifyToken;
        }

        public void setVerifyToken(String verifyToken) {
            this.verifyToken = verifyToken;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }

        public String getInviteCode() {
            return inviteCode;
        }

        public void setInviteCode(String inviteCode) {
            this.inviteCode = inviteCode;
        }
    }

    public static class WechatPollRequest {
        @NotBlank(message = "二维码会话不能为空")
        private String sceneId;

        public String getSceneId() {
            return sceneId;
        }

        public void setSceneId(String sceneId) {
            this.sceneId = sceneId;
        }
    }

    public static class WechatBindPhoneRequest {
        @NotBlank(message = "绑定凭证不能为空")
        private String bindToken;
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        private String phone;
        @Pattern(regexp = "^\\d{6}$", message = "验证码需为 6 位数字")
        private String code;
        private String inviteCode;

        public String getBindToken() {
            return bindToken;
        }

        public void setBindToken(String bindToken) {
            this.bindToken = bindToken;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getInviteCode() {
            return inviteCode;
        }

        public void setInviteCode(String inviteCode) {
            this.inviteCode = inviteCode;
        }
    }

    public static class SubmitRealNameRequest {
        @NotBlank(message = "姓名不能为空")
        private String realName;

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "请输入 11 位手机号")
        private String phone;

        @Pattern(regexp = "^\\d{17}[0-9Xx]$", message = "请输入 18 位身份证号")
        private String idCardNo;

        @NotBlank(message = "身份证人像面不能为空")
        private String idCardFrontKey;

        @NotBlank(message = "身份证国徽面不能为空")
        private String idCardBackKey;

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getIdCardNo() {
            return idCardNo;
        }

        public void setIdCardNo(String idCardNo) {
            this.idCardNo = idCardNo;
        }

        public String getIdCardFrontKey() {
            return idCardFrontKey;
        }

        public void setIdCardFrontKey(String idCardFrontKey) {
            this.idCardFrontKey = idCardFrontKey;
        }

        public String getIdCardBackKey() {
            return idCardBackKey;
        }

        public void setIdCardBackKey(String idCardBackKey) {
            this.idCardBackKey = idCardBackKey;
        }
    }

    public static class StartFaceRealNameRequest {
        @NotBlank(message = "姓名不能为空")
        private String realName;

        @Pattern(regexp = "^\\d{17}[0-9Xx]$", message = "请输入 18 位身份证号")
        private String idCardNo;

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getIdCardNo() {
            return idCardNo;
        }

        public void setIdCardNo(String idCardNo) {
            this.idCardNo = idCardNo;
        }
    }

    public static class FaceRealNameStatusRequest {
        @Pattern(regexp = "^[A-Za-z0-9]{8,32}$", message = "认证订单号格式不正确")
        private String orderId;

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }
    }

    public static class UpdateNicknameRequest {
        @NotBlank(message = "昵称不能为空")
        private String nickname;

        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
    }

    public static class UpdateAvatarRequest {
        @NotBlank(message = "头像文件不能为空")
        private String avatarKey;

        public String getAvatarKey() { return avatarKey; }
        public void setAvatarKey(String avatarKey) { this.avatarKey = avatarKey; }
    }

    public static class ChangePasswordRequest {
        private String currentPassword;
        @NotBlank(message = "新密码不能为空")
        private String nextPassword;
        @NotBlank(message = "确认密码不能为空")
        private String confirmPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNextPassword() { return nextPassword; }
        public void setNextPassword(String nextPassword) { this.nextPassword = nextPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }

    public static class ChangePhoneRequest {
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        private String phone;
        @Pattern(regexp = "^\\d{6}$", message = "验证码需为 6 位数字")
        private String code;

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class SecurityVerifyRequest {
        @Pattern(regexp = "^\\d{6}$", message = "验证码需为 6 位数字")
        private String code;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class UpdateSecurityRequest {
        private boolean loginAlertEnabled;
        private boolean secondaryVerifyEnabled;

        public boolean isLoginAlertEnabled() { return loginAlertEnabled; }
        public void setLoginAlertEnabled(boolean loginAlertEnabled) { this.loginAlertEnabled = loginAlertEnabled; }
        public boolean isSecondaryVerifyEnabled() { return secondaryVerifyEnabled; }
        public void setSecondaryVerifyEnabled(boolean secondaryVerifyEnabled) { this.secondaryVerifyEnabled = secondaryVerifyEnabled; }
    }
}
