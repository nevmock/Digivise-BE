package org.nevmock.digivise.application.dto.product.keyword;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductKeywordResponseWrapperDto {
    private Long campaignId;
    private LocalDateTime from;
    private LocalDateTime to;
    private List<ProductKeywordResponseDto> data;
}
