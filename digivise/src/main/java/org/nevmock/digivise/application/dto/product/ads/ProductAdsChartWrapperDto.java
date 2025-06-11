package org.nevmock.digivise.application.dto.product.ads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ProductAdsChartWrapperDto {
    private Long campaignId;
    private LocalDateTime from;
    private LocalDateTime to;
    private List<ProductAdsChartResponseDto> data;
}