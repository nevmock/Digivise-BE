package org.nevmock.digivise.application.dto.product.stock;

import lombok.*;

@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ProductStockResponseDto {
    private Long campaignId;
    private String id;

    private Integer soldCount;
    private Integer sellingPriceMax;

    private Integer revenue;
}
