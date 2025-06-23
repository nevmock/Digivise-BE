// ProductPerformanceChartResponseDto.java
package org.nevmock.digivise.application.dto.product.performance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductPerformanceChartResponseDto {
    private Long productId;
    private Double pv;
    private Double addToCartUnits;
    private Double uvToAddToCartRate;
    private Double placedUnits;
    private Double placedBuyersToConfirmedBuyersRate;
    private Double uvToConfirmedBuyersRate;
    private Double uvToPlacedBuyersRate;
    private Double confirmedSales;
    private Double placedSales;
    private Long shopeeFrom;
    private Long shopeeTo;
    private LocalDateTime createdAt;
}