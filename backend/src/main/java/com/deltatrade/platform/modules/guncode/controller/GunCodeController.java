package com.deltatrade.platform.modules.guncode.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.modules.guncode.service.GunCodeService;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/gun-codes")
public class GunCodeController {

    private final GunCodeService gunCodeService;

    public GunCodeController(GunCodeService gunCodeService) {
        this.gunCodeService = gunCodeService;
    }

    @GetMapping
    public ApiResponse<GunCodeService.GunCodePayload> loadGunCodes() {
        return ApiResponse.success(gunCodeService.loadGunCodes(), MDC.get("traceId"));
    }
}
