package org.nevmock.digivise.application.dto.product.keyword;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductKeywordResponseDto {
    private String id;
    private String uuid;
    private Long productId;
    private String shopId;
    private LocalDateTime createdAt;

    // Product basic info
    private String name;
    private String coverImage;
    private String parentSku;
    private Integer status;

    // Price details
    private BigDecimal priceMin;
    private BigDecimal priceMax;
    private BigDecimal sellingPriceMin;
    private BigDecimal sellingPriceMax;
    private Boolean hasDiscount;
    private Integer maxDiscountPercentage;

    // Stock details
    private Integer totalAvailableStock;
    private Integer totalSellerStock;

    // Statistics
    private Integer viewCount;
    private Integer likedCount;
    private Integer soldCount;

    // Promotion info
    private Boolean wholesale;
    private Boolean hasBundleDeal;

    // Timestamps
    private Long modifyTime;
    private Long createTime;

    private String keyword;
    private Long from;
    private Long to;
}
