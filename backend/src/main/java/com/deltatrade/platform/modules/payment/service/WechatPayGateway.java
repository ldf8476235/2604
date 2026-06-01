package com.deltatrade.platform.modules.payment.service;

import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.payment.config.PlatformWechatPayProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class WechatPayGateway {

    private static final Logger log = LoggerFactory.getLogger(WechatPayGateway.class);
    private static final String API_BASE_URL = "https://api.mch.weixin.qq.com";
    private static final DateTimeFormatter WECHAT_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final PlatformWechatPayProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ResourceLoader resourceLoader;

    private PrivateKey merchantPrivateKey;

    public WechatPayGateway(
        PlatformWechatPayProperties properties,
        ObjectMapper objectMapper,
        RestTemplateBuilder restTemplateBuilder,
        ResourceLoader resourceLoader
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder.build();
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        if (!properties.isEnabled() || properties.isMockMode()) {
            log.info("skip wechat pay gateway init enabled={} mockMode={}", properties.isEnabled(), properties.isMockMode());
            return;
        }
        validateConfig();
        this.merchantPrivateKey = loadPrivateKey(properties.getPrivateKeyPath());
        log.info("wechat pay gateway initialized mchId={} serialNo={} appId={}",
            properties.getMchId(), maskSerial(properties.getMerchantSerialNumber()), properties.getAppId());
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isMockMode() {
        return properties.isMockMode();
    }

    public WechatPayResult createOrder(CreateOrderRequest request) {
        if (properties.isMockMode()) {
            LocalDateTime expireAt = request.getExpireAt() == null ? LocalDateTime.now().plusMinutes(10) : request.getExpireAt();
            return new WechatPayResult(
                request.getOutTradeNo(),
                request.getTradeType(),
                "mock://wechat/" + request.getOutTradeNo(),
                null,
                expireAt,
                null
            );
        }
        validateConfig();
        String tradeType = normalizeTradeType(request.getTradeType());
        String path = "JSAPI".equals(tradeType) ? "/v3/pay/transactions/jsapi" : "/v3/pay/transactions/native";
        try {
            JsonNode payload = objectMapper.createObjectNode()
                .put("appid", properties.getAppId())
                .put("mchid", properties.getMchId())
                .put("description", request.getDescription())
                .put("out_trade_no", request.getOutTradeNo())
                .put("notify_url", request.getNotifyUrl())
                .put("attach", defaultText(request.getAttach(), ""));
            ((com.fasterxml.jackson.databind.node.ObjectNode) payload).putObject("amount")
                .put("total", toFen(request.getAmount()))
                .put("currency", "CNY");
            if (request.getExpireAt() != null) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) payload).put(
                    "time_expire",
                    WECHAT_TIME_FORMATTER.format(request.getExpireAt().atOffset(ZoneOffset.ofHours(8)))
                );
            }
            if ("JSAPI".equals(tradeType)) {
                if (!StringUtils.hasText(request.getOpenId())) {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号未绑定微信 openId，无法发起 JSAPI 支付");
                }
                ((com.fasterxml.jackson.databind.node.ObjectNode) payload).putObject("payer").put("openid", request.getOpenId());
            }
            String requestBody = objectMapper.writeValueAsString(payload);
            String responseBody = request("POST", path, requestBody);
            JsonNode response = objectMapper.readTree(responseBody);
            LocalDateTime expireAt = request.getExpireAt() == null ? LocalDateTime.now().plusMinutes(10) : request.getExpireAt();
            if ("JSAPI".equals(tradeType)) {
                String prepayId = response.path("prepay_id").asText("");
                if (!StringUtils.hasText(prepayId)) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付返回缺少 prepay_id");
                }
                JsapiPayParams jsapiPayParams = buildJsapiParams(prepayId);
                log.info("wechat pay create success orderNo={} tradeType={} prepayId={} costScene=jsapi",
                    request.getOutTradeNo(), tradeType, maskPrepayId(prepayId));
                return new WechatPayResult(request.getOutTradeNo(), tradeType, null, prepayId, expireAt, jsapiPayParams);
            }
            String codeUrl = response.path("code_url").asText("");
            if (!StringUtils.hasText(codeUrl)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付返回缺少二维码地址");
            }
            String prepayId = response.path("prepay_id").asText(null);
            log.info("wechat pay create success orderNo={} tradeType={} prepayId={} hasCodeUrl={}",
                request.getOutTradeNo(), tradeType, maskPrepayId(prepayId), true);
            return new WechatPayResult(request.getOutTradeNo(), tradeType, codeUrl, prepayId, expireAt, null);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("wechat pay create failed orderNo={} tradeType={} amount={}",
                request.getOutTradeNo(), request.getTradeType(), request.getAmount(), exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付下单失败，请稍后重试");
        }
    }

    public TransactionResult queryOrder(String outTradeNo) {
        if (properties.isMockMode()) {
            return new TransactionResult(outTradeNo, "SUCCESS", null, "MOCK_TRANSACTION_" + outTradeNo, properties.getMchId(), properties.getAppId(), null);
        }
        validateConfig();
        String path = "/v3/pay/transactions/out-trade-no/" + outTradeNo + "?mchid=" + properties.getMchId();
        try {
            String responseBody = request("GET", path, "");
            JsonNode response = objectMapper.readTree(responseBody);
            String tradeState = response.path("trade_state").asText("");
            String attach = response.path("attach").asText(null);
            String transactionId = response.path("transaction_id").asText("");
            String mchId = response.path("mchid").asText("");
            String appId = response.path("appid").asText("");
            String successTimeText = response.path("success_time").asText("");
            LocalDateTime successTime = StringUtils.hasText(successTimeText) ? OffsetDateTime.parse(successTimeText).toLocalDateTime() : null;
            log.info("wechat pay query success orderNo={} tradeState={} transactionId={}",
                outTradeNo, tradeState, maskTransactionId(transactionId));
            return new TransactionResult(outTradeNo, tradeState, attach, transactionId, mchId, appId, successTime);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("wechat pay query failed orderNo={}", outTradeNo, exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付状态查询失败");
        }
    }

    public PaymentNotification decryptNotification(String body) {
        if (properties.isMockMode()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "测试模式不支持真实微信回调");
        }
        validateConfig();
        try {
            JsonNode payload = objectMapper.readTree(body);
            String eventType = payload.path("event_type").asText("");
            JsonNode resource = payload.path("resource");
            if (resource.isMissingNode()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信回调缺少 resource");
            }
            String associatedData = resource.path("associated_data").asText("");
            String nonce = resource.path("nonce").asText("");
            String ciphertext = resource.path("ciphertext").asText("");
            if (!StringUtils.hasText(nonce) || !StringUtils.hasText(ciphertext)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信回调密文参数缺失");
            }
            String plainText = decryptResource(associatedData, nonce, ciphertext);
            JsonNode plain = objectMapper.readTree(plainText);
            return new PaymentNotification(
                eventType,
                plain.path("out_trade_no").asText(""),
                plain.path("transaction_id").asText(""),
                plain.path("trade_state").asText(""),
                plain.path("attach").asText(null)
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("wechat pay notify decrypt failed", exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付回调解密失败");
        }
    }

    private String request(String method, String path, String body) {
        long startAt = System.currentTimeMillis();
        String normalizedBody = body == null ? "" : body;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("Authorization", buildAuthorization(method, path, normalizedBody));
        headers.set("User-Agent", "delta-trade/1.0");
        headers.set("Accept-Charset", StandardCharsets.UTF_8.name());
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                URI.create(API_BASE_URL + path),
                HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)),
                new HttpEntity<String>(normalizedBody, headers),
                String.class
            );
            log.info("wechat pay http success method={} path={} status={} costMs={}",
                method, sanitizePath(path), response.getStatusCodeValue(), System.currentTimeMillis() - startAt);
            return defaultText(response.getBody(), "");
        } catch (HttpStatusCodeException exception) {
            log.warn("wechat pay http rejected method={} path={} status={} body={} costMs={}",
                method, sanitizePath(path), exception.getRawStatusCode(), exception.getResponseBodyAsString(),
                System.currentTimeMillis() - startAt);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, parseWechatError(exception.getResponseBodyAsString()));
        } catch (RuntimeException exception) {
            log.error("wechat pay http failed method={} path={} costMs={}",
                method, sanitizePath(path), System.currentTimeMillis() - startAt, exception);
            throw exception;
        }
    }

    private String buildAuthorization(String method, String path, String body) {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String message = method.toUpperCase(Locale.ROOT) + "\n"
            + sanitizeCanonicalPath(path) + "\n"
            + timestamp + "\n"
            + nonce + "\n"
            + body + "\n";
        String signature = sign(message);
        return "WECHATPAY2-SHA256-RSA2048 mchid=\"" + properties.getMchId()
            + "\",nonce_str=\"" + nonce
            + "\",timestamp=\"" + timestamp
            + "\",serial_no=\"" + properties.getMerchantSerialNumber()
            + "\",signature=\"" + signature + "\"";
    }

    private JsapiPayParams buildJsapiParams(String prepayId) {
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String packageValue = "prepay_id=" + prepayId;
        String paySignMessage = properties.getAppId() + "\n"
            + timeStamp + "\n"
            + nonceStr + "\n"
            + packageValue + "\n";
        return new JsapiPayParams(
            properties.getAppId(),
            timeStamp,
            nonceStr,
            packageValue,
            "RSA",
            sign(paySignMessage)
        );
    }

    private String decryptResource(String associatedData, String nonce, String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec key = new SecretKeySpec(properties.getApiV3Key().getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec spec = new GCMParameterSpec(128, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            if (StringUtils.hasText(associatedData)) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付回调解密失败");
        }
    }

    private String sign(String message) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(merchantPrivateKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付签名失败");
        }
    }

    private PrivateKey loadPrivateKey(String privateKeyPath) {
        try {
            Resource resource = resourceLoader.getResource(privateKeyPath);
            byte[] bytes = readAllBytes(resource.getInputStream());
            String pem = new String(bytes, StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (IOException | GeneralSecurityException exception) {
            log.error("load wechat merchant private key failed path={}", privateKeyPath, exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付商户私钥加载失败");
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        try (InputStream in = inputStream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toByteArray();
        }
    }

    private void validateConfig() {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "微信支付未开启");
        }
        if (!StringUtils.hasText(properties.getAppId())
            || !StringUtils.hasText(properties.getMchId())
            || !StringUtils.hasText(properties.getMerchantSerialNumber())
            || !StringUtils.hasText(properties.getApiV3Key())
            || !StringUtils.hasText(properties.getPrivateKeyPath())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付配置不完整");
        }
        if (properties.getApiV3Key().trim().length() != 32) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付 APIV3 Key 长度必须为 32 位");
        }
    }

    private String parseWechatError(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            return "微信支付请求失败";
        }
        try {
            JsonNode payload = objectMapper.readTree(responseBody);
            String code = payload.path("code").asText("");
            String message = payload.path("message").asText("");
            if (StringUtils.hasText(code) || StringUtils.hasText(message)) {
                return "微信支付请求失败：" + defaultText(message, code);
            }
        } catch (IOException ignored) {
        }
        return "微信支付请求失败";
    }

    private int toFen(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "支付金额必须大于 0");
        }
        return amount.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private String normalizeTradeType(String tradeType) {
        String value = tradeType == null ? "" : tradeType.trim().toUpperCase(Locale.ROOT);
        if ("JSAPI".equals(value)) {
            return "JSAPI";
        }
        return "NATIVE";
    }

    private String sanitizeCanonicalPath(String path) {
        if (path == null) {
            return "/";
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return URI.create(path).getRawPath();
        }
        return path;
    }

    private String sanitizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        int queryIndex = path.indexOf('?');
        return queryIndex >= 0 ? path.substring(0, queryIndex) : path;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String maskPrepayId(String prepayId) {
        if (!StringUtils.hasText(prepayId) || prepayId.length() < 10) {
            return prepayId;
        }
        return prepayId.substring(0, 4) + "****" + prepayId.substring(prepayId.length() - 4);
    }

    private String maskTransactionId(String transactionId) {
        if (!StringUtils.hasText(transactionId) || transactionId.length() < 8) {
            return transactionId;
        }
        return transactionId.substring(0, 4) + "****" + transactionId.substring(transactionId.length() - 4);
    }

    private String maskSerial(String serialNo) {
        if (!StringUtils.hasText(serialNo) || serialNo.length() < 8) {
            return serialNo;
        }
        return serialNo.substring(0, 4) + "****" + serialNo.substring(serialNo.length() - 4);
    }

    public static class CreateOrderRequest {
        private final String outTradeNo;
        private final String description;
        private final BigDecimal amount;
        private final String notifyUrl;
        private final String attach;
        private final String tradeType;
        private final String openId;
        private final LocalDateTime expireAt;

        public CreateOrderRequest(String outTradeNo, String description, BigDecimal amount, String notifyUrl, String attach, String tradeType, String openId) {
            this(outTradeNo, description, amount, notifyUrl, attach, tradeType, openId, null);
        }

        public CreateOrderRequest(String outTradeNo, String description, BigDecimal amount, String notifyUrl, String attach, String tradeType, String openId, LocalDateTime expireAt) {
            this.outTradeNo = outTradeNo;
            this.description = description;
            this.amount = amount;
            this.notifyUrl = notifyUrl;
            this.attach = attach;
            this.tradeType = tradeType;
            this.openId = openId;
            this.expireAt = expireAt;
        }

        public String getOutTradeNo() {
            return outTradeNo;
        }

        public String getDescription() {
            return description;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public String getNotifyUrl() {
            return notifyUrl;
        }

        public String getAttach() {
            return attach;
        }

        public String getTradeType() {
            return tradeType;
        }

        public String getOpenId() {
            return openId;
        }

        public LocalDateTime getExpireAt() {
            return expireAt;
        }
    }

    public static class WechatPayResult {
        private final String orderNo;
        private final String tradeType;
        private final String codeUrl;
        private final String prepayId;
        private final LocalDateTime expireAt;
        private final JsapiPayParams jsapiPayParams;

        public WechatPayResult(String orderNo, String tradeType, String codeUrl, String prepayId, LocalDateTime expireAt, JsapiPayParams jsapiPayParams) {
            this.orderNo = orderNo;
            this.tradeType = tradeType;
            this.codeUrl = codeUrl;
            this.prepayId = prepayId;
            this.expireAt = expireAt;
            this.jsapiPayParams = jsapiPayParams;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public String getTradeType() {
            return tradeType;
        }

        public String getCodeUrl() {
            return codeUrl;
        }

        public String getPrepayId() {
            return prepayId;
        }

        public LocalDateTime getExpireAt() {
            return expireAt;
        }

        public JsapiPayParams getJsapiPayParams() {
            return jsapiPayParams;
        }
    }

    public static class JsapiPayParams {
        private final String appId;
        private final String timeStamp;
        private final String nonceStr;
        private final String packageValue;
        private final String signType;
        private final String paySign;

        public JsapiPayParams(String appId, String timeStamp, String nonceStr, String packageValue, String signType, String paySign) {
            this.appId = appId;
            this.timeStamp = timeStamp;
            this.nonceStr = nonceStr;
            this.packageValue = packageValue;
            this.signType = signType;
            this.paySign = paySign;
        }

        public String getAppId() {
            return appId;
        }

        public String getTimeStamp() {
            return timeStamp;
        }

        public String getNonceStr() {
            return nonceStr;
        }

        public String getPackageValue() {
            return packageValue;
        }

        public String getSignType() {
            return signType;
        }

        public String getPaySign() {
            return paySign;
        }
    }

    public static class TransactionResult {
        private final String outTradeNo;
        private final String tradeState;
        private final String attach;
        private final String transactionId;
        private final String mchId;
        private final String appId;
        private final LocalDateTime successTime;

        public TransactionResult(String outTradeNo, String tradeState, String attach, String transactionId, String mchId, String appId, LocalDateTime successTime) {
            this.outTradeNo = outTradeNo;
            this.tradeState = tradeState;
            this.attach = attach;
            this.transactionId = transactionId;
            this.mchId = mchId;
            this.appId = appId;
            this.successTime = successTime;
        }

        public String getOutTradeNo() {
            return outTradeNo;
        }

        public String getTradeState() {
            return tradeState;
        }

        public String getAttach() {
            return attach;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public String getMchId() {
            return mchId;
        }

        public String getAppId() {
            return appId;
        }

        public LocalDateTime getSuccessTime() {
            return successTime;
        }
    }

    public static class PaymentNotification {
        private final String eventType;
        private final String outTradeNo;
        private final String transactionId;
        private final String tradeState;
        private final String attach;

        public PaymentNotification(String eventType, String outTradeNo, String transactionId, String tradeState, String attach) {
            this.eventType = eventType;
            this.outTradeNo = outTradeNo;
            this.transactionId = transactionId;
            this.tradeState = tradeState;
            this.attach = attach;
        }

        public String getEventType() {
            return eventType;
        }

        public String getOutTradeNo() {
            return outTradeNo;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public String getTradeState() {
            return tradeState;
        }

        public String getAttach() {
            return attach;
        }
    }
}
