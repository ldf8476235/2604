package com.deltatrade.platform.modules.payment.controller;

import com.deltatrade.platform.modules.payment.service.WechatPaymentCallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/wechat")
public class WechatPaymentController {

    private static final Logger log = LoggerFactory.getLogger(WechatPaymentController.class);

    private final WechatPaymentCallbackService wechatPaymentCallbackService;

    public WechatPaymentController(WechatPaymentCallbackService wechatPaymentCallbackService) {
        this.wechatPaymentCallbackService = wechatPaymentCallbackService;
    }

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> notify(@RequestBody String requestBody) {
        try {
            wechatPaymentCallbackService.handleNotify(requestBody);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"code\":\"SUCCESS\",\"message\":\"成功\"}");
        } catch (Exception exception) {
            log.error("wechat pay notify handle failed", exception);
            return ResponseEntity.status(500)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("{\"code\":\"FAIL\",\"message\":\"处理失败\"}");
        }
    }
}
