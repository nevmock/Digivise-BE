package org.nevmock.digivise.application.dto.product.ads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAdsResponseWrapperDto {
    private Long campaignId;
    private LocalDateTime from;
    private LocalDateTime to;
    private List<ProductAdsResponseDto> data;
}