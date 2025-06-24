package org.nevmock.digivise.application.dto.product.keyword;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductKeywordChartWrapperDto {
    private Long campaignId;
    private LocalDateTime from;
    private LocalDateTime to;
    private List<ProductKeywordChartResponseDto> data;
}