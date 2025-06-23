package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.performance.ProductPerformanceChartWrapperDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ProductPerformanceChartService {
    Page<ProductPerformanceChartWrapperDto> findMetricsByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );
}