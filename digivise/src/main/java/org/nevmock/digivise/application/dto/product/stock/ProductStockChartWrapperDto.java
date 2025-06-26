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
public class ProductStockChartWrapperDto {
    private Long productId;
    private String productName;
    private LocalDateTime from;
    private LocalDateTime to;
    private List<ProductStockChartResponseDto> data;

    // Constructor untuk single product
    public ProductStockChartWrapperDto(Long productId, LocalDateTime from, LocalDateTime to, List<ProductStockChartResponseDto> data) {
        this.productId = productId;
        this.from = from;
        this.to = to;
        this.data = data;
    }

    // Constructor dengan product name
    public ProductStockChartWrapperDto(Long productId, LocalDateTime from, LocalDateTime to, String productName, List<ProductStockChartResponseDto> data) {
        this.productId = productId;
        this.productName = productName;
        this.from = from;
        this.to = to;
        this.data = data;
    }
}
