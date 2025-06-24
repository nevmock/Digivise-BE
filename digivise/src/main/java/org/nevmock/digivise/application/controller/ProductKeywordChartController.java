package org.nevmock.digivise.application.controller;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordChartWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductKeywordChartService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/product-keyword/chart")
@RequiredArgsConstructor
public class ProductKeywordChartController {
    private final ProductKeywordChartService productKeywordChartService;

    @GetMapping
    public List<ProductKeywordChartWrapperDto> getKeywordChartMetrics(
            @RequestParam String shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return productKeywordChartService.findMetricsByRange(shopId, from, to);
    }
}

