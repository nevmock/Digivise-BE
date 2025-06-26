//package org.nevmock.digivise.application.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
//import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
//import org.nevmock.digivise.domain.port.in.ProductAdsService;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/product-ads")
//@RequiredArgsConstructor
//public class ProductAdsController {
//    private final ProductAdsService productAdsService;
//
//    @GetMapping("/daily")
//    public ResponseEntity<Page<ProductAdsResponseWrapperDto>> getProductAdsByShopIdAndFromAndToTotal(
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
//            int limit,
//            @RequestParam(required = false)
//            String biddingStrategy,
//            @RequestParam(required = false)
//            String type,
//            @RequestParam(required = false)
//            String state,
//            @RequestParam(required = false)
//            String productPlacement,
//            @RequestParam(required = false)
//            String salesClassification,
//            @RequestParam(required = false)
//            String title,
//            @RequestParam(required = false)
//            Boolean hasKeywords
//            ) {
//        PageRequest pageRequest = PageRequest.of(page, limit);
//
//        return ResponseEntity.ok(productAdsService.findByRangeAggTotal(shopId, biddingStrategy, from, to, pageRequest, type, state, productPlacement, salesClassification, title, hasKeywords));
//    }
//
//    @PostMapping("/custom-roas")
//    public ResponseEntity<Map<String, Object>> insertCustomRoas(
//            @RequestParam String shopId,
//            @RequestParam Long campaignId,
//            @RequestParam Double customRoas
//    ) {
//        boolean success = productAdsService.insertCustomRoasForToday(shopId, campaignId, customRoas);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", success);
//        response.put("message", success ? "Custom ROAS berhasil disimpan" : "Gagal menyimpan Custom ROAS");
//        response.put("shopId", shopId);
//        response.put("campaignId", campaignId);
//        response.put("customRoas", customRoas);
//
//        return ResponseEntity.ok(response);
//    }
//}
package org.nevmock.digivise.application.controller;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsNewestResponseDto;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductAdsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/product-ads")
@RequiredArgsConstructor
public class ProductAdsController {
    private final ProductAdsService productAdsService;

    @GetMapping("")
    public ResponseEntity<Page<ProductAdsResponseWrapperDto>> getProductAdsComparison(
            @RequestParam
            String shopId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from1,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to1,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from2,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to2,
            @RequestParam(defaultValue = "0")
            int page,
            @RequestParam(defaultValue = "10")
            int limit,
            @RequestParam(required = false)
            String biddingStrategy,
            @RequestParam(required = false)
            String type,
            @RequestParam(required = false)
            String state,
            @RequestParam(required = false)
            String productPlacement,
            @RequestParam(required = false)
            String salesClassification,
            @RequestParam(required = false)
            String title,
            @RequestParam(required = false)
            Long campaignId
    ) {
        PageRequest pageRequest = PageRequest.of(page, limit);

        return ResponseEntity.ok(productAdsService.findByRangeAggTotal(
                shopId,
                biddingStrategy,
                from1,
                to1,
                from2,
                to2,
                pageRequest,
                type,
                state,
                productPlacement,
                salesClassification,
                title,
                campaignId
        ));
    }

    @PostMapping("/custom-roas")
    public ResponseEntity<Map<String, Object>> insertCustomRoas(
            @RequestParam String shopId,
            @RequestParam Long campaignId,
            @RequestParam Double customRoas,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to
    ) {
        boolean success = productAdsService.insertCustomRoas(shopId, campaignId, customRoas, from, to);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Custom ROAS berhasil disimpan" : "Gagal menyimpan Custom ROAS");
        response.put("shopId", shopId);
        response.put("campaignId", campaignId);
        response.put("customRoas", customRoas);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @RequestMapping("/newest")
    public ResponseEntity<ProductAdsNewestResponseDto> getNewestProductAds(
            @RequestParam Long campaignId
    ) {
        ProductAdsNewestResponseDto response = productAdsService.findByCampaignId(campaignId);
        return ResponseEntity.ok(response);
    }
}
