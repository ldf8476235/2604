package com.deltatrade.platform.modules.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.sms")
public class PlatformSmsProperties {

    private boolean enabled;
    private String provider = "pnvs";
    private boolean mockMode = true;
    private boolean debugReturnVerifyCode;
    private PnvsProperties pnvs = new PnvsProperties();
    private TencentProperties tencent = new TencentProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isMockMode() {
        return mockMode;
    }

    public void setMockMode(boolean mockMode) {
        this.mockMode = mockMode;
    }

    public boolean isDebugReturnVerifyCode() {
        return debugReturnVerifyCode;
    }

    public void setDebugReturnVerifyCode(boolean debugReturnVerifyCode) {
        this.debugReturnVerifyCode = debugReturnVerifyCode;
    }

    public PnvsProperties getPnvs() {
        return pnvs;
    }

    public void setPnvs(PnvsProperties pnvs) {
        this.pnvs = pnvs;
    }

    public TencentProperties getTencent() {
        return tencent;
    }

    public void setTencent(TencentProperties tencent) {
        this.tencent = tencent;
    }

    public static class PnvsProperties {
        private String endpoint = "dypnsapi.aliyuncs.com";
        private String accessKeyId;
        private String accessKeySecret;
        private String signName;
        private String templateCode;
        private String schemeName;
        private String countryCode = "86";
        private int validMinutes = 15;
        private int intervalSeconds = 60;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getAccessKeySecret() {
            return accessKeySecret;
        }

        public void setAccessKeySecret(String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
        }

        public String getSignName() {
            return signName;
        }

        public void setSignName(String signName) {
            this.signName = signName;
        }

        public String getTemplateCode() {
            return templateCode;
        }

        public void setTemplateCode(String templateCode) {
            this.templateCode = templateCode;
        }

        public String getSchemeName() {
            return schemeName;
        }

        public void setSchemeName(String schemeName) {
            this.schemeName = schemeName;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public void setCountryCode(String countryCode) {
            this.countryCode = countryCode;
        }

        public int getValidMinutes() {
            return validMinutes;
        }

        public void setValidMinutes(int validMinutes) {
            this.validMinutes = validMinutes;
        }

        public int getIntervalSeconds() {
            return intervalSeconds;
        }

        public void setIntervalSeconds(int intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
        }
    }

    public static class TencentProperties {
        private String endpoint = "sms.tencentcloudapi.com";
        private String region = "ap-guangzhou";
        private String secretId;
        private String secretKey;
        private String appId;
        private String signName;
        private String templateId;
        private int validMinutes = 15;
        private int intervalSeconds = 60;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getSecretId() {
            return secretId;
        }

        public void setSecretId(String secretId) {
            this.secretId = secretId;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getSignName() {
            return signName;
        }

        public void setSignName(String signName) {
            this.signName = signName;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
        }

        public int getValidMinutes() {
            return validMinutes;
        }

        public void setValidMinutes(int validMinutes) {
            this.validMinutes = validMinutes;
        }

        public int getIntervalSeconds() {
            return intervalSeconds;
        }

        public void setIntervalSeconds(int intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
        }
    }
}
