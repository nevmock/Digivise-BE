package org.nevmock.digivise.application.dto.product.performance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductPerformanceChartWrapperDto {
    private Long productId;
    private LocalDateTime from;
    private LocalDateTime to;
    private List<ProductPerformanceChartResponseDto> data;
}