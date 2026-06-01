package com.deltatrade.platform.modules.listing.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.common.auth.AuthContext;
import com.deltatrade.platform.common.auth.AuthPrincipal;
import com.deltatrade.platform.modules.listing.service.ListingService;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/listings")
public class ListingInteractionController {

    private final ListingService listingService;

    public ListingInteractionController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping("/favorites")
    public ApiResponse<Set<String>> loadFavorites(@RequestParam(required = false) String listingNos) {
        AuthPrincipal principal = AuthContext.requirePrincipal();
        return ApiResponse.success(
            listingService.loadFavoriteListingNos(principal, parseCsv(listingNos)),
            MDC.get("traceId")
        );
    }

    @PostMapping("/{listingNo}/favorite")
    public ApiResponse<ListingService.FavoriteToggleResult> favorite(@PathVariable String listingNo) {
        AuthPrincipal principal = AuthContext.requirePrincipal();
        return ApiResponse.success(listingService.favoriteListing(principal, listingNo), MDC.get("traceId"));
    }

    @DeleteMapping("/{listingNo}/favorite")
    public ApiResponse<ListingService.FavoriteToggleResult> unfavorite(@PathVariable String listingNo) {
        AuthPrincipal principal = AuthContext.requirePrincipal();
        return ApiResponse.success(listingService.unfavoriteListing(principal, listingNo), MDC.get("traceId"));
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
