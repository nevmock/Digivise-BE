package org.nevmock.digivise.application.dto.product.stock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockChartResponseDto {
    private Long productId;
    private String name;
    private Integer totalAvailableStock;
    private Long shopeeFrom;
    private Long shopeeTo;
    private LocalDateTime createdAt;
    private List<ModelStockDto> modelStock;
}