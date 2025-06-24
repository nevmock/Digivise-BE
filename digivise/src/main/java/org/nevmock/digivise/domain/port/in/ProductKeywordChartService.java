package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordChartWrapperDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductKeywordChartService {
    List<ProductKeywordChartWrapperDto> findMetricsByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to
    );
}
