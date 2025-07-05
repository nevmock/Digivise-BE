package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseWrapperDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.time.LocalDateTime;

public interface ProductStockService {
//    Page<ProductStockResponseWrapperDto> findByRange(
//            String shopId,
//            LocalDateTime from,
//            LocalDateTime to,
//            Pageable pageable
//    );

//    Page<ProductStockResponseWrapperDto> findByShopId(
//            String shopId,
//            LocalDateTime from,
//            LocalDateTime to,
//            String name,
//            Pageable pageable
//    );

//    Page<ProductStockResponseWrapperDto> findByRange(
//            String shopId,
//            LocalDateTime from1,
//            LocalDateTime to1,
//            LocalDateTime from2,
//            LocalDateTime to2,
//            String name,
//            String state,
//            Pageable pageable
//    );
Page<ProductStockResponseWrapperDto> findByRange(
        String shopId,
        LocalDateTime from,
        LocalDateTime to,
        String name,
        String state,
        Pageable pageable
);

ProductStockResponseWrapperDto fetchStockLive(
        String username,
        String type,
        boolean isAsc,
        int pageSize
) throws IOException, InterruptedException;
}
