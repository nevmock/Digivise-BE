package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseWrapperDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ProductKeywordService {
    Page<ProductKeywordResponseWrapperDto> findByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            String name,
            Long campaignId,
            Pageable pageable
    );
}
