package com.deltatrade.platform.modules.admin.service;

import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.auth.config.PlatformSmsProperties;
import com.deltatrade.platform.modules.auth.config.PlatformWechatProperties;
import com.deltatrade.platform.modules.payment.config.PlatformWechatPayProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminIntegrationConfigService {

    private static final Logger log = LoggerFactory.getLogger(AdminIntegrationConfigService.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };
    private static final String GROUP_NAME = "ADMIN_INTEGRATION";
    private static final String PAYMENT_KEY = "payment";
    private static final String LOGIN_KEY = "login";
    private static final String DISTRIBUTION_KEY = "distribution";
    private static final String LISTING_PUBLISH_KEY = "listingPublish";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final PlatformWechatPayProperties wechatPayProperties;
    private final PlatformSmsProperties smsProperties;
    private final PlatformWechatProperties wechatProperties;

    public AdminIntegrationConfigService(
        JdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper,
        PlatformWechatPayProperties wechatPayProperties,
        PlatformSmsProperties smsProperties,
        PlatformWechatProperties wechatProperties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.wechatPayProperties = wechatPayProperties;
        this.smsProperties = smsProperties;
        this.wechatProperties = wechatProperties;
    }

    public Map<String, Object> loadConfigs() {
        return mapOf(
            "payment", loadPaymentConfig(),
            "login", loadLoginConfig(),
            "distribution", loadDistributionConfig(),
            "listingPublish", loadListingPublishConfig()
        );
    }

    @Transactional
    public Map<String, Object> savePaymentConfig(Map<String, Object> payload) {
        Map<String, Object> normalized = mapOf(
            "wechatEnabled", readBoolean(payload, "wechatEnabled"),
            "wechatMockMode", readBoolean(payload, "wechatMockMode"),
            "wechatAppId", readText(payload, "wechatAppId"),
            "wechatMchId", readText(payload, "wechatMchId"),
            "wechatNotifyUrl", readText(payload, "wechatNotifyUrl"),
            "alipayEnabled", readBoolean(payload, "alipayEnabled"),
            "alipayMockMode", readBoolean(payload, "alipayMockMode"),
            "alipayAppId", readText(payload, "alipayAppId"),
            "alipayMerchantNo", readText(payload, "alipayMerchantNo"),
            "alipayNotifyUrl", readText(payload, "alipayNotifyUrl")
        );
        saveConfig(PAYMENT_KEY, normalized);
        log.info("admin integration payment config saved wechatEnabled={} alipayEnabled={}",
            normalized.get("wechatEnabled"), normalized.get("alipayEnabled"));
        return mapOf("message", "支付配置已保存", "payment", normalized);
    }

    @Transactional
    public Map<String, Object> saveLoginConfig(Map<String, Object> payload) {
        Map<String, Object> normalized = mapOf(
            "smsEnabled", readBoolean(payload, "smsEnabled"),
            "smsMockMode", readBoolean(payload, "smsMockMode"),
            "smsSignName", readText(payload, "smsSignName"),
            "smsTemplateCode", readText(payload, "smsTemplateCode"),
            "wechatOpenEnabled", readBoolean(payload, "wechatOpenEnabled"),
            "wechatOpenMockMode", readBoolean(payload, "wechatOpenMockMode"),
            "wechatOpenAppId", readText(payload, "wechatOpenAppId"),
            "wechatOpenRedirectUri", readText(payload, "wechatOpenRedirectUri")
        );
        saveConfig(LOGIN_KEY, normalized);
        log.info("admin integration login config saved smsEnabled={} wechatOpenEnabled={}",
            normalized.get("smsEnabled"), normalized.get("wechatOpenEnabled"));
        return mapOf("message", "登录配置已保存", "login", normalized);
    }

    @Transactional
    public Map<String, Object> saveDistributionConfig(Map<String, Object> payload) {
        Map<String, Object> normalized = mapOf(
            "autoEnableAfterVerified", readBoolean(payload, "autoEnableAfterVerified", true),
            "defaultTradeCommissionRate", readRate(payload, "defaultTradeCommissionRate", "0.1000"),
            "defaultBoostingCommissionRate", readRate(payload, "defaultBoostingCommissionRate", "0.1000")
        );
        saveConfig(DISTRIBUTION_KEY, normalized);
        log.info("admin integration distribution config saved autoEnable={} tradeRate={} boostingRate={}",
            normalized.get("autoEnableAfterVerified"), normalized.get("defaultTradeCommissionRate"), normalized.get("defaultBoostingCommissionRate"));
        return mapOf("message", "分销配置已保存", "distribution", normalized);
    }

    @Transactional
    public Map<String, Object> saveListingPublishConfig(Map<String, Object> payload) {
        Map<String, Object> normalized = mapOf(
            "defaultExchangeRate", readExchangeRate(payload, "defaultExchangeRate", "380000"),
            "personalSellerCommissionRate", readRate(payload, "personalSellerCommissionRate", "0.1000")
        );
        saveConfig(LISTING_PUBLISH_KEY, normalized);
        log.info("admin integration listing publish config saved defaultExchangeRate={} personalSellerCommissionRate={}",
            normalized.get("defaultExchangeRate"), normalized.get("personalSellerCommissionRate"));
        return mapOf("message", "账号发布配置已保存", "listingPublish", normalized);
    }

    private Map<String, Object> loadPaymentConfig() {
        Map<String, Object> stored = readConfig(PAYMENT_KEY);
        return mapOf(
            "wechatEnabled", readBoolean(stored, "wechatEnabled", wechatPayProperties.isEnabled()),
            "wechatMockMode", readBoolean(stored, "wechatMockMode", wechatPayProperties.isMockMode()),
            "wechatAppId", readText(stored, "wechatAppId", wechatPayProperties.getAppId()),
            "wechatMchId", readText(stored, "wechatMchId", wechatPayProperties.getMchId()),
            "wechatNotifyUrl", readText(stored, "wechatNotifyUrl", wechatPayProperties.getNotifyUrl()),
            "alipayEnabled", readBoolean(stored, "alipayEnabled", false),
            "alipayMockMode", readBoolean(stored, "alipayMockMode", true),
            "alipayAppId", readText(stored, "alipayAppId", ""),
            "alipayMerchantNo", readText(stored, "alipayMerchantNo", ""),
            "alipayNotifyUrl", readText(stored, "alipayNotifyUrl", "")
        );
    }

    private Map<String, Object> loadLoginConfig() {
        Map<String, Object> stored = readConfig(LOGIN_KEY);
        return mapOf(
            "smsEnabled", readBoolean(stored, "smsEnabled", smsProperties.isEnabled()),
            "smsMockMode", readBoolean(stored, "smsMockMode", smsProperties.isMockMode()),
            "smsSignName", readText(stored, "smsSignName", defaultSmsSignName()),
            "smsTemplateCode", readText(stored, "smsTemplateCode", defaultSmsTemplateCode()),
            "wechatOpenEnabled", readBoolean(stored, "wechatOpenEnabled", wechatProperties.isEnabled()),
            "wechatOpenMockMode", readBoolean(stored, "wechatOpenMockMode", wechatProperties.isMockMode()),
            "wechatOpenAppId", readText(stored, "wechatOpenAppId", wechatProperties.getAppId()),
            "wechatOpenRedirectUri", readText(stored, "wechatOpenRedirectUri", wechatProperties.getRedirectUri())
        );
    }

    private Map<String, Object> loadDistributionConfig() {
        Map<String, Object> stored = readConfig(DISTRIBUTION_KEY);
        return mapOf(
            "autoEnableAfterVerified", readBoolean(stored, "autoEnableAfterVerified", true),
            "defaultTradeCommissionRate", readRate(stored, "defaultTradeCommissionRate", "0.1000"),
            "defaultBoostingCommissionRate", readRate(stored, "defaultBoostingCommissionRate", "0.1000")
        );
    }

    public Map<String, Object> loadListingPublishConfig() {
        Map<String, Object> stored = readConfig(LISTING_PUBLISH_KEY);
        return mapOf(
            "defaultExchangeRate", readExchangeRate(stored, "defaultExchangeRate", "380000"),
            "personalSellerCommissionRate", readRate(stored, "personalSellerCommissionRate", "0.1000")
        );
    }

    private Map<String, Object> readConfig(String configKey) {
        String payload = jdbcTemplate.query(
            "SELECT config_value FROM system_configs WHERE config_key = ? LIMIT 1",
            new Object[] { configKey },
            (resultSet) -> resultSet.next() ? resultSet.getString(1) : null
        );
        if (!StringUtils.hasText(payload)) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> result = objectMapper.readValue(payload, MAP_TYPE);
            return result == null ? Collections.<String, Object>emptyMap() : result;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "系统配置解析失败");
        }
    }

    private String defaultSmsSignName() {
        if ("tencent".equalsIgnoreCase(smsProperties.getProvider())) {
            return smsProperties.getTencent().getSignName();
        }
        return smsProperties.getPnvs().getSignName();
    }

    private String defaultSmsTemplateCode() {
        if ("tencent".equalsIgnoreCase(smsProperties.getProvider())) {
            return smsProperties.getTencent().getTemplateId();
        }
        return smsProperties.getPnvs().getTemplateCode();
    }

    private void saveConfig(String configKey, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM system_configs WHERE config_key = ?",
                new Object[] { configKey },
                Integer.class
            );
            if (count != null && count.intValue() > 0) {
                jdbcTemplate.update(
                    "UPDATE system_configs SET config_value = ?, config_group_name = ? WHERE config_key = ?",
                    json,
                    GROUP_NAME,
                    configKey
                );
                return;
            }
            jdbcTemplate.update(
                "INSERT INTO system_configs (config_key, config_value, config_group_name) VALUES (?, ?, ?)",
                configKey,
                json,
                GROUP_NAME
            );
        } catch (Exception exception) {
            log.warn("admin integration config save failed key={}", configKey, exception);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "系统配置保存失败");
        }
    }

    private boolean readBoolean(Map<String, Object> payload, String key) {
        return readBoolean(payload, key, false);
    }

    private boolean readBoolean(Map<String, Object> payload, String key, boolean fallback) {
        Object value = payload.get(key);
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        if (value instanceof String) {
            return "true".equalsIgnoreCase(((String) value).trim()) || "1".equals(((String) value).trim());
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return fallback;
    }

    private String readText(Map<String, Object> payload, String key) {
        return readText(payload, key, "");
    }

    private String readText(Map<String, Object> payload, String key, String fallback) {
        Object value = payload.get(key);
        if (value == null) {
            return fallback == null ? "" : fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? (fallback == null ? "" : fallback) : text;
    }

    private String readRate(Map<String, Object> payload, String key, String fallback) {
        String text = readText(payload, key, fallback);
        try {
            java.math.BigDecimal value = new java.math.BigDecimal(text);
            if (value.compareTo(java.math.BigDecimal.ZERO) < 0 || value.compareTo(java.math.BigDecimal.ONE) > 0) {
                return fallback;
            }
            return value.setScale(4, java.math.RoundingMode.HALF_UP).toPlainString();
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String readPositiveDecimal(Map<String, Object> payload, String key, String fallback) {
        String text = readText(payload, key, fallback);
        try {
            java.math.BigDecimal value = new java.math.BigDecimal(text);
            if (value.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                return fallback;
            }
            return value.stripTrailingZeros().toPlainString();
        } catch (Exception exception) {
            return fallback;
        }
    }

    private String readExchangeRate(Map<String, Object> payload, String key, String fallback) {
        String value = readPositiveDecimal(payload, key, fallback);
        try {
            java.math.BigDecimal decimal = new java.math.BigDecimal(value);
            if (decimal.compareTo(new java.math.BigDecimal("10000")) < 0) {
                decimal = decimal.multiply(new java.math.BigDecimal("10000"));
            }
            return decimal.stripTrailingZeros().toPlainString();
        } catch (Exception exception) {
            return fallback;
        }
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }
}
