package org.nevmock.digivise.application.controller;


import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
import org.nevmock.digivise.application.dto.product.performance.ProductPerformanceWrapperDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductAdsService;
import org.nevmock.digivise.domain.port.in.ProductPerformanceService;
import org.nevmock.digivise.domain.port.in.ProductStockService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/product-stock")
@RequiredArgsConstructor
public class ProductStockController {
    private final ProductStockService productStockService;

    @GetMapping("/by-shop")
    public ResponseEntity<Page<ProductStockResponseWrapperDto>> getProductStockByShop(
            @RequestParam String shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from1,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to1,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String state
    ) {
        PageRequest pageRequest = PageRequest.of(page, limit);
        return ResponseEntity.ok(productStockService.findByRange(shopId, from1, to1, name, state, pageRequest));
    }

    @GetMapping("/live")
    public ResponseEntity<ProductStockResponseWrapperDto> fetchStockLive(
            //@RequestParam String username,
            @RequestParam String shopId,
            @RequestParam String type,
            @RequestParam(defaultValue = "1") int targetPage,
            @RequestParam(defaultValue = "true") boolean isAsc,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String searchKeyword
    ) {
        try {
            ResponseEntity<ProductStockResponseWrapperDto> stuff = ResponseEntity.ok(productStockService.fetchStockLive(shopId, type, isAsc, pageSize, targetPage, searchKeyword));

            return stuff;
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/newest")
    public ResponseEntity<Page<ProductStockResponseWrapperDto>> getNewestProductStock(
            @RequestParam String shopId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String state
    ) {
        PageRequest pageRequest = PageRequest.of(page, limit);
        return ResponseEntity.ok(productStockService.findNewest(shopId, name, state, pageRequest));
    }
}

