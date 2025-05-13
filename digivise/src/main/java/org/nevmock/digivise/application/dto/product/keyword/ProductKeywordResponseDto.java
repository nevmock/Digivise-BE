package org.nevmock.digivise.application.dto.product.keyword;

import lombok.*;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class ProductKeywordResponseDto {
    private Long campaignId;

    private String id;

    private String key;

    private String shopeeMerchantId;
    private Double acos;
    private Double cpc;
    private String from;
    private String to;
    private LocalDateTime createdAt;

    private Double cost;

    private Double click;
    private Double ctr;
    private String insight;

    private Double impression;
}
