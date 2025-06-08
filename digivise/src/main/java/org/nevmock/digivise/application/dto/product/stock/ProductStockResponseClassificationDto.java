package org.nevmock.digivise.application.dto.product.stock;

import lombok.*;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ProductStockResponseClassificationDto {
    private Long campaignId;
    private String id;

    private Integer soldCount;
    private String sellingPriceMax;
    private LocalDateTime createdAt;

    private Integer revenue;

}
