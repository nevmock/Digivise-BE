package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.performance.ProductPerformanceWrapperDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ProductPerformanceService {
    Page<ProductPerformanceWrapperDto> findByRange(
            String shopId,
            LocalDateTime from1,
            LocalDateTime to1,
            LocalDateTime from2,
            LocalDateTime to2,
            String name,
            Integer status,
            String salesClassification,
            Pageable pageable
    );
}
