package org.nevmock.digivise.application.dto.product.keyword;

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
public class ProductKeywordResponseWrapperDto {
    private Long campaignId;
    private String shopId;
    private LocalDateTime from1;
    private LocalDateTime to1;
    private LocalDateTime from2;
    private LocalDateTime to2;
    private List<ProductKeywordResponseDto> data;
}