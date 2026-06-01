package com.deltatrade.platform.modules.listing.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.modules.listing.service.ListingService;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/public/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public ApiResponse<ListingService.MarketplaceListResult> loadListings(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) String depositRange,
        @RequestParam(required = false) Long maxHafCurrency,
        @RequestParam(required = false) Integer minLevel,
        @RequestParam(required = false) Integer maxLevel,
        @RequestParam(required = false) String regionCodes,
        @RequestParam(required = false) String weaponCodes,
        @RequestParam(required = false) String knifeSkins,
        @RequestParam(required = false) String redSkins,
        @RequestParam(required = false) String awmBulletRange,
        @RequestParam(required = false) String rank,
        @RequestParam(required = false) Integer safeBoxLevel,
        @RequestParam(required = false) Integer staminaLevel,
        @RequestParam(required = false) Integer carryLevel,
        @RequestParam(required = false) String deliveryMethod,
        @RequestParam(required = false) String sellerType,
        @RequestParam(required = false) String exchangeRateType,
        @RequestParam(required = false) Boolean negotiable,
        @RequestParam(required = false) Boolean alwaysOnline,
        @RequestParam(required = false) Integer publishedWithinDays,
        @RequestParam(required = false, defaultValue = "newest") String sort,
        @RequestParam(required = false, defaultValue = "1") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer pageSize
    ) {
        ListingService.MarketplaceQuery query = new ListingService.MarketplaceQuery();
        query.setKeyword(keyword);
        query.setMinPrice(minPrice);
        query.setMaxPrice(maxPrice);
        query.setDepositRange(depositRange);
        query.setMaxHafCurrency(maxHafCurrency);
        query.setMinAccountLevel(minLevel);
        query.setMaxAccountLevel(maxLevel);
        query.setRegionCodes(parseCsv(regionCodes));
        query.setWeaponCodes(parseCsv(weaponCodes));
        query.setKnifeSkins(parseCsv(knifeSkins));
        query.setRedSkins(parseCsv(redSkins));
        query.setAwmBulletRange(awmBulletRange);
        query.setRank(rank);
        query.setSafeBoxLevel(safeBoxLevel);
        query.setStaminaLevel(staminaLevel);
        query.setCarryLevel(carryLevel);
        query.setDeliveryMethod(deliveryMethod);
        query.setSellerType(sellerType);
        query.setExchangeRateType(exchangeRateType);
        query.setNegotiable(negotiable);
        query.setAlwaysOnline(alwaysOnline);
        query.setPublishedWithinDays(publishedWithinDays);
        query.setSort(sort);
        query.setPage(page);
        query.setPageSize(pageSize);
        return ApiResponse.success(listingService.loadMarketplace(query), MDC.get("traceId"));
    }

    @GetMapping("/meta")
    public ApiResponse<ListingService.MarketplaceMeta> loadMeta() {
        return ApiResponse.success(listingService.loadMarketplaceMeta(), MDC.get("traceId"));
    }

    @GetMapping("/{listingNo}")
    public ApiResponse<ListingService.ListingDetail> loadDetail(@PathVariable String listingNo) {
        return ApiResponse.success(listingService.loadDetail(listingNo), MDC.get("traceId"));
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
