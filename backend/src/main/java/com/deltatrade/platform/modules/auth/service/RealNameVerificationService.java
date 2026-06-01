package com.deltatrade.platform.modules.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RealNameVerificationService {

    private static final Logger log = LoggerFactory.getLogger(RealNameVerificationService.class);
    private static final int[] ID_CARD_WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] ID_CARD_CODE = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    public VerificationResult verify(String realName, String idCardNo) {
        long startAt = System.currentTimeMillis();
        String normalizedName = realName == null ? "" : realName.trim();
        String normalizedIdCard = idCardNo == null ? "" : idCardNo.trim().toUpperCase();

        if (!normalizedName.matches("^[\\u4e00-\\u9fa5·]{2,20}$")) {
            log.warn("real name verification rejected provider=LOCAL reason=name_format");
            return new VerificationResult(false, "认证姓名需为 2-20 位中文字符", "LOCAL");
        }
        if (!isValidChineseIdCard(normalizedIdCard)) {
            log.warn("real name verification rejected provider=LOCAL reason=id_card_format");
            return new VerificationResult(false, "身份证号校验未通过，请核对后重新提交", "LOCAL");
        }

        log.info("real name verification success provider=LOCAL costMs={}", System.currentTimeMillis() - startAt);
        return new VerificationResult(true, "", "LOCAL");
    }

    private boolean isValidChineseIdCard(String idCardNo) {
        if (!idCardNo.matches("^\\d{17}[0-9X]$")) {
            return false;
        }
        int sum = 0;
        for (int index = 0; index < 17; index++) {
            sum += (idCardNo.charAt(index) - '0') * ID_CARD_WEIGHT[index];
        }
        return ID_CARD_CODE[sum % 11] == idCardNo.charAt(17);
    }

    public static class VerificationResult {
        private final boolean passed;
        private final String reason;
        private final String provider;

        public VerificationResult(boolean passed, String reason, String provider) {
            this.passed = passed;
            this.reason = reason;
            this.provider = provider;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getReason() {
            return reason;
        }

        public String getProvider() {
            return provider;
        }
    }
}
