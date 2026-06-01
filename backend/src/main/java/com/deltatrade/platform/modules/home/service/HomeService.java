package com.deltatrade.platform.modules.home.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class HomeService {

    private final JdbcTemplate jdbcTemplate;

    public HomeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HomePayload loadHome() {
        return new HomePayload(
            Arrays.asList(
                "海量优质账号",
                "平台审核验真",
                "IM 监管交接",
                "安全上号",
                "公正售后处理",
                "守护未成年"
            ),
            Arrays.asList(
                new HomeMetric("在售账号", formatCount(count("SELECT COUNT(*) FROM account_listing WHERE status = 'PUBLISHED'"))),
                new HomeMetric(
                    "已完成订单",
                    formatCount(
                        count("SELECT COUNT(*) FROM trade_order WHERE status = 'COMPLETED'") +
                            count("SELECT COUNT(*) FROM boosting_order WHERE status = 'COMPLETED'")
                    )
                ),
                new HomeMetric("入驻工作室", formatCount(count("SELECT COUNT(*) FROM studio_profile WHERE active = TRUE")))
            ),
            Arrays.asList(
                new FooterLink("关于我们", "#footer"),
                new FooterLink("隐私保护", "#footer"),
                new FooterLink("免责声明", "#footer")
            ),
            "鄂ICP备20号-1",
            "15811502982",
            "版权所有 ©2026 Delta Trade Platform"
        );
    }

    private long count(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private String formatCount(long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    public static class HomePayload {
        private final List<String> siteAssurances;
        private final List<HomeMetric> metrics;
        private final List<FooterLink> footerLinks;
        private final String icpNo;
        private final String contactPhone;
        private final String copyright;

        public HomePayload(
            List<String> siteAssurances,
            List<HomeMetric> metrics,
            List<FooterLink> footerLinks,
            String icpNo,
            String contactPhone,
            String copyright
        ) {
            this.siteAssurances = siteAssurances;
            this.metrics = metrics;
            this.footerLinks = footerLinks;
            this.icpNo = icpNo;
            this.contactPhone = contactPhone;
            this.copyright = copyright;
        }

        public List<String> getSiteAssurances() {
            return siteAssurances;
        }

        public List<HomeMetric> getMetrics() {
            return metrics;
        }

        public List<FooterLink> getFooterLinks() {
            return footerLinks;
        }

        public String getIcpNo() {
            return icpNo;
        }

        public String getContactPhone() {
            return contactPhone;
        }

        public String getCopyright() {
            return copyright;
        }
    }

    public static class HomeMetric {
        private final String label;
        private final String value;

        public HomeMetric(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }
    }

    public static class FooterLink {
        private final String label;
        private final String href;

        public FooterLink(String label, String href) {
            this.label = label;
            this.href = href;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }
    }
}
