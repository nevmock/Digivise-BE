//package org.nevmock.digivise.application.dto.product.stock;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class ProductStockResponseDto {
//    private String id;
//    private String uuid;
//    private String shopId;
//    private LocalDateTime createdAt;
//    private Long productId;
//    private String name;
//    private String coverImage;
//    private Integer status;
//    private String parentSku;
//
//    // Main stock information
//    private Integer totalAvailableStock;
//    private Integer totalSellerStock;
//    private Integer totalShopeeStock;
//    private Integer lowStockStatus;
//    private Boolean enableStockReminder;
//    private Boolean modelSellerStockSoldOut;
//    private Boolean modelShopeeStockSoldOut;
//    private Integer sellableStock;
//    private Integer inTransitStock;
//    private Integer enableStockReminderStatus;
//    private Integer soldCount;
//
//    // Model variants stock details
//    private List<ModelStockDto> modelStocks;
//}

package org.nevmock.digivise.application.dto.product.stock;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockResponseDto {
    private String id;
    private String uuid;
    private LocalDateTime createdAt;
    private String shopId;

    private Long productId;
    private String name;
    private Integer status;
    private String coverImage;
    private String parentSku;

    private Double priceMin;
    private Double priceMax;
    private Boolean hasDiscount;
    private Integer maxDiscountPercentage;
    private Integer maxDiscount;
    private Double sellingPriceMin;
    private Double sellingPriceMax;

    private Integer totalAvailableStock;
    private Integer totalSellerStock;
    private Integer totalShopeeStock;
    private Integer lowStockStatus;
    private Boolean enableStockReminder;
    private Boolean modelSellerStockSoldOut;
    private Boolean modelShopeeStockSoldOut;
    private Integer advancedSellableStock;
    private Integer advancedInTransitStock;
    private Integer enableStockReminderStatus;

    private Boolean wholesale;
    private Boolean hasBundleDeal;

    private Integer viewCount;
    private Integer likedCount;
    private Integer soldCount;

    private Boolean isVirtualSku;
    private Boolean unlist;
    private Boolean hasDiscountTag;
    private Boolean wholesaleTag;
    private Boolean hasBundleDealTag;
    private Boolean hasAddOnDeal;
    private Boolean liveSku;
    private Boolean ssp;
    private Boolean hasAmsCommission;
    private Boolean memberExclusive;
    private Boolean isIprAppealing;

    private Integer boostEntryStatus;
    private Boolean showBoostHistory;
    private Long boostCampaignId;

    private Long modifyTime;
    private Long createTime;

    private Long scheduledPublishTime;
    private Long mtskuItemId;

    private Integer appealOpt;
    private String canNotAppealTransifyKey;
    private Long referenceId;
    private Integer appealStatus;

    private List<ModelStockDto> modelStocks;
}