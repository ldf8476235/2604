package com.deltatrade.platform.modules.auth.service;

import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.auth.config.PlatformJuheFaceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Component
public class JuheFaceGateway {

    private static final Logger log = LoggerFactory.getLogger(JuheFaceGateway.class);
    private static final String START_URL = "https://apis.juhe.cn/faceid/query";
    private static final String SEARCH_URL = "https://apis.juhe.cn/faceid/search";

    private final PlatformJuheFaceProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public JuheFaceGateway(PlatformJuheFaceProperties properties, ObjectMapper objectMapper, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(8))
            .setReadTimeout(Duration.ofSeconds(12))
            .build();
    }

    public StartResult startVerification(String realName, String idCardNo, String orderId) {
        validateConfig();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("key", properties.getAppKey());
        form.add("certName", realName);
        form.add("certNo", idCardNo);
        form.add("orderId", orderId);
        form.add("model", defaultText(properties.getModel(), "1"));
        form.add("returnUrl", properties.getReturnUrl());
        if (StringUtils.hasText(properties.getNotifyUrl())) {
            form.add("notifyUrl", properties.getNotifyUrl());
        }
        form.add("deviceType", defaultText(properties.getDeviceType(), "H5"));

        String body = postForm(START_URL, form, "juhe face start");
        JsonNode root = parseBody(body, "聚合实名认证发起返回格式异常");
        int errorCode = root.path("error_code").asInt(-1);
        String reason = root.path("reason").asText("");
        JsonNode result = root.path("result");
        String jhOrderId = result.path("jh_order_id").asText("");
        String verifyUrl = result.path("verify_url").asText("");
        log.info("juhe face start result orderId={} errorCode={} jhOrderId={}",
            orderId, errorCode, maskOrderId(jhOrderId));
        return new StartResult(errorCode, reason, jhOrderId, verifyUrl, removeSensitiveImage(body));
    }

    public SearchResult searchVerification(String jhOrderId) {
        validateConfig();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("key", properties.getAppKey());
        form.add("jhOrderId", jhOrderId);

        String body = postForm(SEARCH_URL, form, "juhe face search");
        JsonNode root = parseBody(body, "聚合实名认证查询返回格式异常");
        int errorCode = root.path("error_code").asInt(-1);
        String reason = root.path("reason").asText("");
        JsonNode result = root.path("result");
        JsonNode detail = result.path("detail");
        String message = result.path("message").asText("");
        String liveMessage = detail.path("live_message").asText("");
        String certifyMessage = detail.path("certify_message").asText("");
        boolean passed = "T".equalsIgnoreCase(result.path("passed").asText(""))
            && "T".equalsIgnoreCase(detail.path("live_passed").asText(""))
            && "T".equalsIgnoreCase(detail.path("certify_passed").asText(""));
        log.info("juhe face search result jhOrderId={} errorCode={} passed={}",
            maskOrderId(jhOrderId), errorCode, passed);
        return new SearchResult(errorCode, reason, passed, message, liveMessage, certifyMessage, removeSensitiveImage(body));
    }

    private String postForm(String url, MultiValueMap<String, String> form, String action) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        long startAt = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
            String body = response.getBody();
            if (!StringUtils.hasText(body)) {
                log.error("{} response empty costMs={}", action, System.currentTimeMillis() - startAt);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "聚合实名认证服务返回为空");
            }
            log.info("{} request success costMs={}", action, System.currentTimeMillis() - startAt);
            return body;
        } catch (RestClientException exception) {
            log.error("{} request failed costMs={}", action, System.currentTimeMillis() - startAt, exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "聚合实名认证服务暂时不可用");
        }
    }

    private JsonNode parseBody(String body, String message) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception exception) {
            log.error("juhe face response parse failed body={}", body, exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, message);
        }
    }

    private String removeSensitiveImage(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode detail = root.path("result").path("detail");
            if (detail instanceof ObjectNode && detail.has("img")) {
                ((ObjectNode) detail).remove("img");
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            return body;
        }
    }

    private void validateConfig() {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "聚合人脸实名认证未开启");
        }
        if (!StringUtils.hasText(properties.getAppKey())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "聚合人脸实名认证 AppKey 未配置");
        }
        if (!StringUtils.hasText(properties.getReturnUrl())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "聚合人脸实名认证回跳地址未配置");
        }
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String maskOrderId(String value) {
        if (!StringUtils.hasText(value) || value.length() < 8) {
            return value;
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    public static class StartResult {
        private final int errorCode;
        private final String reason;
        private final String jhOrderId;
        private final String verifyUrl;
        private final String rawResult;

        public StartResult(int errorCode, String reason, String jhOrderId, String verifyUrl, String rawResult) {
            this.errorCode = errorCode;
            this.reason = reason;
            this.jhOrderId = jhOrderId;
            this.verifyUrl = verifyUrl;
            this.rawResult = rawResult;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public String getReason() {
            return reason;
        }

        public String getJhOrderId() {
            return jhOrderId;
        }

        public String getVerifyUrl() {
            return verifyUrl;
        }

        public String getRawResult() {
            return rawResult;
        }
    }

    public static class SearchResult {
        private final int errorCode;
        private final String reason;
        private final boolean passed;
        private final String message;
        private final String liveMessage;
        private final String certifyMessage;
        private final String rawResult;

        public SearchResult(int errorCode, String reason, boolean passed, String message, String liveMessage, String certifyMessage, String rawResult) {
            this.errorCode = errorCode;
            this.reason = reason;
            this.passed = passed;
            this.message = message;
            this.liveMessage = liveMessage;
            this.certifyMessage = certifyMessage;
            this.rawResult = rawResult;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public String getReason() {
            return reason;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getMessage() {
            return message;
        }

        public String getLiveMessage() {
            return liveMessage;
        }

        public String getCertifyMessage() {
            return certifyMessage;
        }

        public String getRawResult() {
            return rawResult;
        }
    }
}
