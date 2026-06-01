package com.deltatrade.platform;

import com.deltatrade.platform.modules.oss.config.PlatformOssProperties;
import com.deltatrade.platform.modules.auth.config.PlatformSmsProperties;
import com.deltatrade.platform.modules.auth.config.PlatformWechatProperties;
import com.deltatrade.platform.modules.payment.config.PlatformWechatPayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({PlatformOssProperties.class, PlatformSmsProperties.class, PlatformWechatProperties.class, PlatformWechatPayProperties.class})
public class DeltaTradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeltaTradeApplication.class, args);
    }
}
