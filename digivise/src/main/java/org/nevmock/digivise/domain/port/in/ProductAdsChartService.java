package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.ads.ProductAdsChartWrapperDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ProductAdsChartService {
    Page<ProductAdsChartWrapperDto> findMetricsByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
}
