package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.performance.ProductPerformanceWrapperDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ProductPerformanceService {
    public Page<ProductPerformanceWrapperDto> findByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            String name,
            Pageable pageable
    );
}
