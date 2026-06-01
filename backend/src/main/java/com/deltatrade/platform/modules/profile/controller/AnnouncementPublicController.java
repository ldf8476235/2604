package com.deltatrade.platform.modules.profile.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.modules.profile.service.ProfileService;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/announcements")
public class AnnouncementPublicController {

    private final ProfileService profileService;

    public AnnouncementPublicController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<ProfileService.AnnouncementCenterResult> loadAnnouncements(
        @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResponse.success(profileService.getPublishedAnnouncements(limit), MDC.get("traceId"));
    }
}
