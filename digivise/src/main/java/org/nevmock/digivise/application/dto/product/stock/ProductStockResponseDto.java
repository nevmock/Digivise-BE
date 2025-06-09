package org.nevmock.digivise.application.dto.product.stock;

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
public class ProductStockResponseDto {
    private String id;
    private String uuid;
    private String shopId;
    private LocalDateTime createdAt;
    private Long productId;
    private String name;
    private String coverImage;
    private Integer status;
    private String parentSku;

    // Main stock information
    private Integer totalAvailableStock;
    private Integer totalSellerStock;
    private Integer totalShopeeStock;
    private Integer lowStockStatus;
    private Boolean enableStockReminder;
    private Boolean modelSellerStockSoldOut;
    private Boolean modelShopeeStockSoldOut;
    private Integer sellableStock;
    private Integer inTransitStock;
    private Integer enableStockReminderStatus;
    private Integer soldCount;

    // Model variants stock details
    private List<ModelStockDto> modelStocks;
}