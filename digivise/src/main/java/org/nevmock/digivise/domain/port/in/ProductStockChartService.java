package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.stock.ProductStockChartWrapperDto;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductStockChartService {
    List<ProductStockChartWrapperDto> findStockChartByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            Long productId    // nullable: jika null atau kosong, tampilkan semua products
    );
}
