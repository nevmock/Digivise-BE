package org.nevmock.digivise.application.dto.product.keyword;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductKeywordWrapperDto {
    private Long campaignId;
    private LocalDateTime from;
    private LocalDateTime to;
    private List<ProductKeywordResponseDto> data;
}