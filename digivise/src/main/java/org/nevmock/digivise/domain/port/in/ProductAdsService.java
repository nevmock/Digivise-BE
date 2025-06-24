package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductAdsService {
//    Page<ProductAdsResponseWrapperDto> findByRangeAggTotal(
//            String shopId,
//            String biddingStrategy,
//            LocalDateTime from,
//            LocalDateTime to,
//            Pageable pageable,
//            String type,
//            String state,
//            String productPlacement,
//            String salesClassification,
//            String title,
//            Boolean hasKeywords
//    );
    Page<ProductAdsResponseWrapperDto> findByRangeAggTotal(
        String shopId,
        String biddingStrategy,
        LocalDateTime from1,
        LocalDateTime to1,
        LocalDateTime from2,
        LocalDateTime to2,
        Pageable pageable,
        String type,
        String state,
        String productPlacement,
        String salesClassification, // This filter can be applied post-aggregation if needed
        String title,
        Long campaignId
        // hasKeywords is removed
    );

    boolean insertCustomRoas(String shopId, Long campaignId, Double customRoas, Long from, Long to);
}
