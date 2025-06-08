package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ProductStockService {
    Page<ProductAdsResponseDto> findByRangeAggTotal(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
}
