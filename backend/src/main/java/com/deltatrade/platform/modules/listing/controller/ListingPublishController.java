package com.deltatrade.platform.modules.listing.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.common.auth.AuthContext;
import com.deltatrade.platform.modules.listing.service.ListingPublishService;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/listings/publish")
public class ListingPublishController {

    private final ListingPublishService listingPublishService;

    public ListingPublishController(ListingPublishService listingPublishService) {
        this.listingPublishService = listingPublishService;
    }

    @GetMapping("/meta")
    public ApiResponse<ListingPublishService.PublishMetaResult> meta() {
        return ApiResponse.success(listingPublishService.loadMeta(AuthContext.requirePrincipal()), MDC.get("traceId"));
    }

    @PostMapping
    public ApiResponse<ListingPublishService.PublishSubmitResult> submit(@Valid @RequestBody ListingPublishService.PublishCommand request) {
        return ApiResponse.success(listingPublishService.submit(AuthContext.requirePrincipal(), request), MDC.get("traceId"));
    }
}
