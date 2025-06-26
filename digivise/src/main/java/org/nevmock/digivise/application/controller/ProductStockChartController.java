package org.nevmock.digivise.application.controller;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
import org.nevmock.digivise.application.dto.product.performance.ProductPerformanceWrapperDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockChartWrapperDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductAdsService;
import org.nevmock.digivise.domain.port.in.ProductPerformanceService;
import org.nevmock.digivise.domain.port.in.ProductStockChartService;
import org.nevmock.digivise.domain.port.in.ProductStockService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product-stock")
@RequiredArgsConstructor
public class ProductStockChartController {
    private final ProductStockChartService productStockService;

    @GetMapping("/chart")
    public ResponseEntity<List<ProductStockChartWrapperDto>> getProductStockByShop(
            @RequestParam String shopId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from1,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to1,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Long productId
    ) {
        return ResponseEntity.ok(productStockService.findStockChartByRange(shopId, from1, to1, productId));
    }
}