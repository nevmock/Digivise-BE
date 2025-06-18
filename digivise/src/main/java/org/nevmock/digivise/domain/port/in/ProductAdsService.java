package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductAdsService {
    Page<ProductAdsResponseWrapperDto> findByRangeAggTotal(
            String shopId,
            String biddingStrategy,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable,
            String type,
            String state,
            String productPlacement,
            String salesClassification,
            String title,
            Boolean hasKeywords
    );

    boolean insertCustomRoasForToday(String shopId, Long campaignId, Double customRoas);
}
