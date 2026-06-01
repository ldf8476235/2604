package com.deltatrade.platform.modules.auth.service;

import com.aliyun.dypnsapi20170525.Client;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.CheckSmsVerifyCodeResponse;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import com.aliyun.teaopenapi.models.Config;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.auth.config.PlatformSmsProperties;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PnvsSmsVerificationService {

    private static final Logger log = LoggerFactory.getLogger(PnvsSmsVerificationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PlatformSmsProperties smsProperties;

    public PnvsSmsVerificationService(PlatformSmsProperties smsProperties) {
        this.smsProperties = smsProperties;
    }

    public SendResult sendCode(String phone, String scene, String outId) {
        if (!smsProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "短信服务未开启");
        }
        if (smsProperties.isMockMode()) {
            log.info("sms mock send success provider={} scene={} phone={} outId={}",
                smsProperties.getProvider(), scene, maskPhone(phone), outId);
            return new SendResult(outId, "246810");
        }
        if (isTencentProvider()) {
            return sendTencentCode(phone, scene, outId);
        }

        PlatformSmsProperties.PnvsProperties pnvs = smsProperties.getPnvs();
        validatePnvsConfig(pnvs);
        long startAt = System.currentTimeMillis();
        try {
            Client client = createClient(pnvs);
            SendSmsVerifyCodeRequest request = new SendSmsVerifyCodeRequest()
                .setPhoneNumber(phone)
                .setCountryCode(pnvs.getCountryCode())
                .setSignName(pnvs.getSignName())
                .setTemplateCode(pnvs.getTemplateCode())
                .setTemplateParam(buildTemplateParam(pnvs.getValidMinutes()))
                .setInterval((long) pnvs.getIntervalSeconds())
                .setValidTime((long) pnvs.getValidMinutes() * 60L)
                .setCodeLength(6L)
                .setCodeType(1L)
                .setAutoRetry(1L)
                .setReturnVerifyCode(smsProperties.isDebugReturnVerifyCode())
                .setOutId(outId);
            String schemeName = normalizedSchemeName(pnvs.getSchemeName());
            if (schemeName != null) {
                request.setSchemeName(schemeName);
            }

            SendSmsVerifyCodeResponse response = client.sendSmsVerifyCode(request);
            SendSmsVerifyCodeResponseBodyGuard body = new SendSmsVerifyCodeResponseBodyGuard(response);
            log.info("pnvs send success scene={} phone={} outId={} providerCode={} requestId={} costMs={}",
                scene,
                maskPhone(phone),
                outId,
                body.code,
                body.requestId,
                System.currentTimeMillis() - startAt);
            if (!body.success) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "短信发送失败：" + body.message);
            }
            return new SendResult(outId, smsProperties.isDebugReturnVerifyCode() ? body.verifyCode : null);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("pnvs send failed scene={} phone={} outId={} costMs={}",
                scene,
                maskPhone(phone),
                outId,
                System.currentTimeMillis() - startAt,
                exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "短信服务暂时不可用");
        }
    }

    public boolean verifyCode(String phone, String code, String outId) {
        if (!smsProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "短信服务未开启");
        }
        if (smsProperties.isMockMode()) {
            return "246810".equals(code);
        }
        if (isTencentProvider()) {
            return true;
        }

        PlatformSmsProperties.PnvsProperties pnvs = smsProperties.getPnvs();
        validatePnvsConfig(pnvs);
        long startAt = System.currentTimeMillis();
        try {
            Client client = createClient(pnvs);
            CheckSmsVerifyCodeRequest request = new CheckSmsVerifyCodeRequest()
                .setPhoneNumber(phone)
                .setCountryCode(pnvs.getCountryCode())
                .setOutId(outId)
                .setVerifyCode(code);
            String schemeName = normalizedSchemeName(pnvs.getSchemeName());
            if (schemeName != null) {
                request.setSchemeName(schemeName);
            }
            CheckSmsVerifyCodeResponse response = client.checkSmsVerifyCode(request);
            CheckSmsVerifyCodeBodyGuard body = new CheckSmsVerifyCodeBodyGuard(response);
            boolean pass = body.success && "PASS".equalsIgnoreCase(body.verifyResult);
            log.info("pnvs verify result phone={} outId={} providerCode={} verifyResult={} costMs={}",
                maskPhone(phone),
                outId,
                body.code,
                body.verifyResult,
                System.currentTimeMillis() - startAt);
            return pass;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("pnvs verify failed phone={} outId={} costMs={}",
                maskPhone(phone),
                outId,
                System.currentTimeMillis() - startAt,
                exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "短信校验服务暂时不可用");
        }
    }

    private Client createClient(PlatformSmsProperties.PnvsProperties pnvs) throws Exception {
        Config config = new Config()
            .setAccessKeyId(pnvs.getAccessKeyId())
            .setAccessKeySecret(pnvs.getAccessKeySecret());
        config.setEndpoint(pnvs.getEndpoint());
        return new Client(config);
    }

    private SendResult sendTencentCode(String phone, String scene, String outId) {
        PlatformSmsProperties.TencentProperties tencent = smsProperties.getTencent();
        validateTencentConfig(tencent);
        String verifyCode = generateCode();
        long startAt = System.currentTimeMillis();
        try {
            SmsClient client = createTencentClient(tencent);
            SendSmsRequest request = new SendSmsRequest();
            request.setSmsSdkAppId(tencent.getAppId());
            request.setSignName(tencent.getSignName());
            request.setTemplateId(tencent.getTemplateId());
            request.setTemplateParamSet(new String[] { verifyCode });
            request.setPhoneNumberSet(new String[] { "+86" + phone });
            request.setSessionContext(outId);

            SendSmsResponse response = client.SendSms(request);
            SendStatus status = firstTencentStatus(response);
            String providerCode = status == null ? "" : status.getCode();
            log.info("tencent sms send result scene={} phone={} outId={} providerCode={} serialNo={} costMs={}",
                scene,
                maskPhone(phone),
                outId,
                providerCode,
                status == null ? "" : status.getSerialNo(),
                System.currentTimeMillis() - startAt);
            if (status == null || !"Ok".equalsIgnoreCase(providerCode)) {
                String message = status == null ? "响应为空" : status.getMessage();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "短信发送失败：" + message);
            }
            return new SendResult(outId, verifyCode);
        } catch (BusinessException exception) {
            throw exception;
        } catch (TencentCloudSDKException exception) {
            log.error("tencent sms send failed scene={} phone={} outId={} costMs={}",
                scene,
                maskPhone(phone),
                outId,
                System.currentTimeMillis() - startAt,
                exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "腾讯云短信发送失败：" + safeMessage(exception.getMessage()));
        }
    }

    private SmsClient createTencentClient(PlatformSmsProperties.TencentProperties tencent) {
        Credential credential = new Credential(tencent.getSecretId(), tencent.getSecretKey());
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(tencent.getEndpoint());
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new SmsClient(credential, tencent.getRegion(), clientProfile);
    }

    private SendStatus firstTencentStatus(SendSmsResponse response) {
        if (response == null || response.getSendStatusSet() == null || response.getSendStatusSet().length == 0) {
            return null;
        }
        return response.getSendStatusSet()[0];
    }

    private void validateTencentConfig(PlatformSmsProperties.TencentProperties tencent) {
        if (!StringUtils.hasText(tencent.getSecretId())
            || !StringUtils.hasText(tencent.getSecretKey())
            || !StringUtils.hasText(tencent.getAppId())
            || !StringUtils.hasText(tencent.getSignName())
            || !StringUtils.hasText(tencent.getTemplateId())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "腾讯云短信配置不完整");
        }
    }

    private void validatePnvsConfig(PlatformSmsProperties.PnvsProperties pnvs) {
        if (!StringUtils.hasText(pnvs.getAccessKeyId())
            || !StringUtils.hasText(pnvs.getAccessKeySecret())
            || !StringUtils.hasText(pnvs.getSignName())
            || !StringUtils.hasText(pnvs.getTemplateCode())
            || isPlaceholderValue(pnvs.getTemplateCode())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "短信配置不完整，请补充真实的 template-code");
        }
    }

    private boolean isPlaceholderValue(String value) {
        return value != null && value.startsWith("你的");
    }

    private String normalizedSchemeName(String value) {
        if (!StringUtils.hasText(value) || isPlaceholderValue(value)) {
            return null;
        }
        return value;
    }

    private String buildTemplateParam(int validMinutes) {
        return "{\"code\":\"##code##\",\"min\":\"" + validMinutes + "\"}";
    }

    private boolean isTencentProvider() {
        return "tencent".equalsIgnoreCase(smsProperties.getProvider());
    }

    private String generateCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1000000));
    }

    private String safeMessage(String message) {
        return StringUtils.hasText(message) ? message : "未知错误";
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static class SendResult {
        private final String outId;
        private final String verifyCode;

        public SendResult(String outId, String verifyCode) {
            this.outId = outId;
            this.verifyCode = verifyCode;
        }

        public String getOutId() {
            return outId;
        }

        public String getVerifyCode() {
            return verifyCode;
        }
    }

    private static class SendSmsVerifyCodeResponseBodyGuard {
        private final boolean success;
        private final String code;
        private final String message;
        private final String requestId;
        private final String verifyCode;

        private SendSmsVerifyCodeResponseBodyGuard(SendSmsVerifyCodeResponse response) {
            if (response == null || response.getBody() == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "短信发送响应为空");
            }
            this.success = Boolean.TRUE.equals(response.getBody().getSuccess());
            this.code = response.getBody().getCode();
            this.message = response.getBody().getMessage();
            this.requestId = response.getBody().getRequestId();
            this.verifyCode = response.getBody().getModel() == null ? null : response.getBody().getModel().getVerifyCode();
        }
    }

    private static class CheckSmsVerifyCodeBodyGuard {
        private final boolean success;
        private final String code;
        private final String verifyResult;

        private CheckSmsVerifyCodeBodyGuard(CheckSmsVerifyCodeResponse response) {
            if (response == null || response.getBody() == null) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "短信校验响应为空");
            }
            this.success = Boolean.TRUE.equals(response.getBody().getSuccess());
            this.code = response.getBody().getCode();
            this.verifyResult = response.getBody().getModel() == null ? null : response.getBody().getModel().getVerifyResult();
        }
    }
}
