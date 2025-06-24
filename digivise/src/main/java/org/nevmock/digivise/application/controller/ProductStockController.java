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

//    @GetMapping("")
//    public ResponseEntity<Page<ProductStockResponseWrapperDto>> getProductStock(
//            @RequestParam
//            String shopId,
//            @RequestParam
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//            LocalDateTime from,
//            @RequestParam
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//            LocalDateTime to,
//            @RequestParam(defaultValue = "0")
//            int page,
//            @RequestParam(defaultValue = "10")
//            int limit
//    ) {
//        PageRequest pageRequest = PageRequest.of(page, limit);
//
//        return ResponseEntity.ok(productStockService.findByRange(shopId, from, to, pageRequest));
//    }

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
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from2,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to2,
            @RequestParam(required = false) String name
    ) {
        PageRequest pageRequest = PageRequest.of(page, limit);
        return ResponseEntity.ok(productStockService.findByRange(shopId, from1, to1, from2, to2, name, pageRequest));
    }
}

