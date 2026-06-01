package com.deltatrade.platform.modules.profile.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.modules.profile.service.DistributionService;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/distribution")
public class DistributionPublicController {

    private final DistributionService distributionService;

    public DistributionPublicController(DistributionService distributionService) {
        this.distributionService = distributionService;
    }

    @GetMapping("/invite/{inviteCode}")
    public ApiResponse<DistributionService.PublicInviteLandingResult> resolveInvite(@PathVariable("inviteCode") String inviteCode) {
        return ApiResponse.success(distributionService.resolveInvite(inviteCode), MDC.get("traceId"));
    }
}
