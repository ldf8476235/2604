package com.deltatrade.platform.modules.auth.service;

import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.auth.config.PlatformWechatProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

@Component
public class WechatOpenGateway {

    private static final Logger log = LoggerFactory.getLogger(WechatOpenGateway.class);
    private static final String QR_CONNECT_URL = "https://open.weixin.qq.com/connect/qrconnect";
    private static final String OAUTH_AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";

    private final PlatformWechatProperties wechatProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public WechatOpenGateway(PlatformWechatProperties wechatProperties, ObjectMapper objectMapper, RestTemplateBuilder restTemplateBuilder) {
        this.wechatProperties = wechatProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(8))
            .setReadTimeout(Duration.ofSeconds(8))
            .build();
    }

    public boolean isMockMode() {
        return wechatProperties.isMockMode();
    }

    public String buildQrConnectUrl(String sceneId, String redirectUri) {
        validateWechatConfig();
        if (!StringUtils.hasText(redirectUri)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信扫码登录回调地址未配置");
        }
        String scope = StringUtils.hasText(wechatProperties.getScope()) ? wechatProperties.getScope() : "snsapi_login";
        String baseUrl = "snsapi_login".equals(scope) ? QR_CONNECT_URL : OAUTH_AUTHORIZE_URL;
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParam("appid", wechatProperties.getAppId())
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", scope)
            .queryParam("state", sceneId)
            .build(true)
            .toUriString() + "#wechat_redirect";
    }

    public WechatAccessTokenResult exchangeCode(String code) {
        validateWechatConfig();
        URI uri = UriComponentsBuilder.fromHttpUrl(ACCESS_TOKEN_URL)
            .queryParam("appid", wechatProperties.getAppId())
            .queryParam("secret", wechatProperties.getAppSecret())
            .queryParam("code", code)
            .queryParam("grant_type", "authorization_code")
            .build(true)
            .toUri();
        long startAt = System.currentTimeMillis();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            String responseBody = response.getBody();
            if (!StringUtils.hasText(responseBody)) {
                log.error("wechat access token response empty costMs={}", System.currentTimeMillis() - startAt);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信登录服务返回为空");
            }
            WechatAccessTokenResponse body;
            try {
                body = objectMapper.readValue(responseBody, WechatAccessTokenResponse.class);
            } catch (Exception parseException) {
                log.error("wechat access token parse failed contentType={} body={} costMs={}",
                    response.getHeaders().getContentType(), responseBody, System.currentTimeMillis() - startAt);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信登录服务返回格式异常");
            }
            if (body.errCode != null && body.errCode != 0) {
                log.warn("wechat access token rejected errCode={} errMsg={} costMs={}",
                    body.errCode, body.errMsg, System.currentTimeMillis() - startAt);
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "微信扫码授权失败：" + safeErrorMessage(body.errMsg));
            }
            if (!StringUtils.hasText(body.openId)) {
                log.error("wechat access token missing openId costMs={}", System.currentTimeMillis() - startAt);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信登录返回缺少 openId");
            }
            log.info("wechat access token success costMs={} openId={}",
                System.currentTimeMillis() - startAt, maskOpenId(body.openId));
            return new WechatAccessTokenResult(body.openId, body.unionId);
        } catch (RestClientException exception) {
            log.error("wechat access token request failed costMs={}", System.currentTimeMillis() - startAt, exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信登录服务暂时不可用");
        }
    }

    private void validateWechatConfig() {
        if (!wechatProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "微信扫码登录未开启");
        }
        if (!StringUtils.hasText(wechatProperties.getAppId()) || !StringUtils.hasText(wechatProperties.getAppSecret())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信扫码登录配置不完整");
        }
    }

    private String safeErrorMessage(String errMsg) {
        return StringUtils.hasText(errMsg) ? errMsg : "未知错误";
    }

    private String maskOpenId(String openId) {
        if (!StringUtils.hasText(openId) || openId.length() < 8) {
            return openId;
        }
        return openId.substring(0, 4) + "****" + openId.substring(openId.length() - 4);
    }

    public static class WechatAccessTokenResult {
        private final String openId;
        private final String unionId;

        public WechatAccessTokenResult(String openId, String unionId) {
            this.openId = openId;
            this.unionId = unionId;
        }

        public String getOpenId() {
            return openId;
        }

        public String getUnionId() {
            return unionId;
        }
    }

    public static class WechatAccessTokenResponse {
        @JsonProperty("openid")
        private String openId;

        @JsonProperty("unionid")
        private String unionId;

        @JsonProperty("errcode")
        private Integer errCode;

        @JsonProperty("errmsg")
        private String errMsg;
    }
}
