package com.deltatrade.platform.modules.listing.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.common.auth.AuthContext;
import com.deltatrade.platform.modules.listing.service.ListingPublishService;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/listings/mine")
public class ListingOwnerController {

    private final ListingPublishService listingPublishService;

    public ListingOwnerController(ListingPublishService listingPublishService) {
        this.listingPublishService = listingPublishService;
    }

    @GetMapping
    public ApiResponse<ListingPublishService.MyListingListResult> list(
        @RequestParam(required = false, defaultValue = "ALL") String status
    ) {
        return ApiResponse.success(listingPublishService.loadMyListings(AuthContext.requirePrincipal(), status), MDC.get("traceId"));
    }

    @GetMapping("/{listingNo}")
    public ApiResponse<ListingPublishService.MyListingDetail> detail(@PathVariable String listingNo) {
        return ApiResponse.success(listingPublishService.loadMyListingDetail(AuthContext.requirePrincipal(), listingNo), MDC.get("traceId"));
    }

    @PutMapping("/{listingNo}")
    public ApiResponse<ListingPublishService.PublishSubmitResult> update(
        @PathVariable String listingNo,
        @Valid @RequestBody ListingPublishService.PublishCommand request
    ) {
        return ApiResponse.success(listingPublishService.updateMyListing(AuthContext.requirePrincipal(), listingNo, request), MDC.get("traceId"));
    }

    @PostMapping("/{listingNo}/withdraw")
    public ApiResponse<ListingPublishService.ActionResult> withdraw(@PathVariable String listingNo) {
        return ApiResponse.success(listingPublishService.withdrawMyListing(AuthContext.requirePrincipal(), listingNo), MDC.get("traceId"));
    }

    @PostMapping("/{listingNo}/resubmit")
    public ApiResponse<ListingPublishService.ActionResult> resubmit(@PathVariable String listingNo) {
        return ApiResponse.success(listingPublishService.resubmitMyListing(AuthContext.requirePrincipal(), listingNo), MDC.get("traceId"));
    }
}
