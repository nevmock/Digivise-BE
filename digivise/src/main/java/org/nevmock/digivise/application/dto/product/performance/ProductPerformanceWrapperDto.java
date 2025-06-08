package org.nevmock.digivise.application.dto.product.performance;

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
public class ProductPerformanceWrapperDto {
    private String shopId;
    private Long productId;
    private LocalDateTime from;
    private LocalDateTime to;
    private List<ProductPerformanceResponseDto> data;
}
