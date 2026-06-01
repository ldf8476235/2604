package com.deltatrade.platform.modules.guncode.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.common.auth.AuthContext;
import com.deltatrade.platform.common.auth.AuthPrincipal;
import com.deltatrade.platform.modules.guncode.service.GunCodeService;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gun-codes")
public class GunCodeInteractionController {

    private final GunCodeService gunCodeService;

    public GunCodeInteractionController(GunCodeService gunCodeService) {
        this.gunCodeService = gunCodeService;
    }

    @GetMapping("/votes")
    public ApiResponse<Map<String, String>> loadVotes(@RequestParam(required = false) String entryCodes) {
        AuthPrincipal principal = AuthContext.requirePrincipal();
        return ApiResponse.success(
            gunCodeService.loadUserVotes(principal, parseCsv(entryCodes)),
            MDC.get("traceId")
        );
    }

    @PostMapping("/{entryCode}/vote")
    public ApiResponse<GunCodeService.VoteResult> vote(@PathVariable String entryCode, @RequestParam String type) {
        AuthPrincipal principal = AuthContext.requirePrincipal();
        return ApiResponse.success(gunCodeService.vote(principal, entryCode, type), MDC.get("traceId"));
    }

    private List<String> parseCsv(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<String>();
        }
        List<String> result = new ArrayList<String>();
        for (String item : Arrays.asList(raw.split(","))) {
            String normalized = item == null ? "" : item.trim();
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return result;
    }
}
