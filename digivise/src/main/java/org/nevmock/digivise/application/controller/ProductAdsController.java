package org.nevmock.digivise.application.controller;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductAdsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/product-ads")
@RequiredArgsConstructor
public class ProductAdsController {
    private final ProductAdsService productAdsService;


    @GetMapping("")
    public ResponseEntity<Page<ProductAdsResponseDto>> getProductAdsByShopIdAndFromAndTo(
            @RequestParam
            String shopId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(defaultValue = "0")
            int page,
            @RequestParam(defaultValue = "10")
            int limit,
            @RequestParam(required = false)
            String biddingStrategy
    ) {
        PageRequest pageRequest = PageRequest.of(page, limit);

        return ResponseEntity.ok(productAdsService.findByRangeAgg(shopId, biddingStrategy, from, to, pageRequest));
    }

    @GetMapping("/daily")
    public ResponseEntity<Page<ProductAdsResponseWrapperDto>> getProductAdsByShopIdAndFromAndToTotal(
            @RequestParam
            String shopId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(defaultValue = "0")
            int page,
            @RequestParam(defaultValue = "10")
            int limit,
            @RequestParam(required = false)
            String biddingStrategy,
            @RequestParam(required = false)
            String type
    ) {
        PageRequest pageRequest = PageRequest.of(page, limit);

        return ResponseEntity.ok(productAdsService.findByRangeAggTotal(shopId, biddingStrategy, from, to, pageRequest, type));
    }

    @GetMapping("/old")
    public ResponseEntity<Page<ProductAdsResponseDto>> getProductAdsByShopIdAndFromAndToOld(
            @RequestParam
            String shopId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(defaultValue = "0")
            int page,
            @RequestParam(defaultValue = "10")
            int limit,
            @RequestParam(required = false)
            String biddingStrategy
    ) {
        PageRequest pageRequest = PageRequest.of(page, limit);

        return ResponseEntity.ok(productAdsService.findByRange(shopId, biddingStrategy, from, to, pageRequest));
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductAdsResponseDto>> getAllProducts(
    ) {
        return ResponseEntity.ok(productAdsService.findAll());
    }
}
