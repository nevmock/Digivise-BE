package org.nevmock.digivise.application.dto.product.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class ModelStockDto {
//    private Long id;
//    private String name;
//    private String sku;
//    private Boolean isDefault;
//    private String image;
//    private Integer totalAvailableStock;
//    private Integer totalSellerStock;
//    private Integer totalShopeeStock;
//    private Integer sellableStock;
//    private Integer inTransitStock;
//    private Integer soldCount;
//}
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelStockDto {
    private Long id;
    private String name;
    private String sku;
    private Boolean isDefault;
    private String image;
    private Integer totalAvailableStock;
    private Integer totalSellerStock;
    private Integer totalShopeeStock;
    private Integer sellableStock;
    private Integer inTransitStock;
    private Integer soldCount;
    private LocalDateTime createdAt;
}