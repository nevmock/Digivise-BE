package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductAdsService {
    List<ProductAdsResponseDto> findAll();

    Optional<ProductAdsResponseDto> findById(String id);

    List<ProductAdsResponseDto> findByShopId(String shopId);

    List<ProductAdsResponseDto> findByCreatedAtBetween(String from, String to);

    List<ProductAdsResponseDto> findByCreatedAtGreaterThanEqual(String from);

    List<ProductAdsResponseDto> findByCreatedAtLessThanEqual(String to);

    List<ProductAdsResponseDto> findByFromAndTo(String from, String to);

    Page<ProductAdsResponseDto> findByRange(String shopId, String biddingStrategy, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<ProductAdsResponseDto> findByRangeAgg(String shopId, String biddingStrategy, LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<ProductAdsResponseWrapperDto> findByRangeAggTotal(
            String shopId,
            String biddingStrategy,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable,
            String type
    );

    Page<ProductAdsResponseDto> findTodayData(
            String shopId,
            String biddingStrategy,
            Pageable pageable
    );

    boolean insertCustomRoasForToday(String shopId, Long campaignId, Double customRoas);
}
