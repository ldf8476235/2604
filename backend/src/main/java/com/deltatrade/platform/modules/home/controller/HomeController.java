package com.deltatrade.platform.modules.home.controller;

import com.deltatrade.platform.common.api.ApiResponse;
import com.deltatrade.platform.modules.home.service.HomeService;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping
    public ApiResponse<HomeService.HomePayload> loadHome() {
        return ApiResponse.success(homeService.loadHome(), MDC.get("traceId"));
    }
}

