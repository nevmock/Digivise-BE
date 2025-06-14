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
public class ProductStockResponseWrapperDto {
    private String shopId;
    private Long productId;
    private LocalDateTime from;
    private LocalDateTime to;
    private List<ProductStockResponseDto> data;
}