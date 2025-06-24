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
    private String key;
    private List<ProductKeywordChartResponseDto> data;

    public ProductKeywordChartWrapperDto(Long campaignId, LocalDateTime from, LocalDateTime to,
                                         List<ProductKeywordChartResponseDto> data) {
        this.campaignId = campaignId;
        this.from = from;
        this.to = to;
        this.data = data;
    }
}