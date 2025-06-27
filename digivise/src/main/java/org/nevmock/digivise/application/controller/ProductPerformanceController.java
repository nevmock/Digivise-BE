package org.nevmock.digivise.application.controller;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
import org.nevmock.digivise.application.dto.product.performance.ProductPerformanceWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductAdsService;
import org.nevmock.digivise.domain.port.in.ProductPerformanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/product-performance")
@RequiredArgsConstructor
public class ProductPerformanceController {
    private final ProductPerformanceService productPerformanceService;

    @GetMapping("")
    public ResponseEntity<Page<ProductPerformanceWrapperDto>> getProductPerformance(
            @RequestParam
            String shopId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from1,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to1,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from2,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to2,
            @RequestParam(required = false)
            String name,
            @RequestParam(required = false)
            Integer status,
            @RequestParam(required = false)
            String salesClassification,
            @RequestParam(defaultValue = "0")
            int page,
            @RequestParam(defaultValue = "10")
            int limit
    ) {
        PageRequest pageRequest = PageRequest.of(page, limit);

        return ResponseEntity.ok(productPerformanceService.findByRange(shopId, from1, to1, from2, to2, name, status, salesClassification, pageRequest));
    }
}
