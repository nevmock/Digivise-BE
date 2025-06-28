package org.nevmock.digivise.application.controller;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsChartWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductAdsChartService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/product-ads/chart")
@RequiredArgsConstructor
public class ProductAdsChartController {
    private final ProductAdsChartService productAdsChartService;

    @GetMapping
    public Page<ProductAdsChartWrapperDto> getChartMetrics(
            @RequestParam String shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")
            int page,
            @RequestParam(defaultValue = "10")
            int limit
    ) {
        PageRequest pageRequest = PageRequest.of(page, limit);
        return productAdsChartService.findMetricsByRange(shopId, from, to, pageRequest);
    }
}
