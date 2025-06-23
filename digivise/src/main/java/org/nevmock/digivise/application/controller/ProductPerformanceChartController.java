package org.nevmock.digivise.application.controller;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.product.performance.ProductPerformanceChartWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductPerformanceChartService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/product-performance/chart")
@RequiredArgsConstructor
public class ProductPerformanceChartController {
    private final ProductPerformanceChartService productPerformanceChartService;

    @GetMapping
    public Page<ProductPerformanceChartWrapperDto> getChartMetrics(
            @RequestParam String shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable
    ) {
        return productPerformanceChartService.findMetricsByRange(shopId, from, to, pageable);
    }
}