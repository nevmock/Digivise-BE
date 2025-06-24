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

    private LocalDateTime from1;
    private LocalDateTime to1;

    private LocalDateTime from2;
    private LocalDateTime to2;

    private List<ProductStockResponseDto> data;
}