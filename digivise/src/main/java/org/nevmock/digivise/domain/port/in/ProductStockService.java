package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseWrapperDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface ProductStockService {
//    Page<ProductStockResponseWrapperDto> findByRange(
//            String shopId,
//            LocalDateTime from,
//            LocalDateTime to,
//            Pageable pageable
//    );

    Page<ProductStockResponseDto> findByShopId(
            String shopId,
            Pageable pageable
    );
}
