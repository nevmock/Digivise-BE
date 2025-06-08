package org.nevmock.digivise.application.dto.product.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPerformanceResponseDto {
    private String id;
    private String uuid;
    private String shopId;
    private LocalDateTime createdAt;
    private Long productId;
    private String name;
    private String image;
    private Integer status;

    private Long uv;
    private Long pv;
    private Long likes;
    private Long bounceVisitors;
    private Double bounceRate;
    private Long searchClicks;
    private Long addToCartUnits;
    private Long addToCartBuyers;

    private Double placedSales;
    private Long placedUnits;
    private Long placedBuyers;
    private Double paidSales;
    private Long paidUnits;
    private Long paidBuyers;
    private Double confirmedSales;
    private Long confirmedUnits;
    private Long confirmedBuyers;
}