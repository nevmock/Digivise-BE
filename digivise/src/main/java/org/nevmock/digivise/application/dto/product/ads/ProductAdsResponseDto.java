package org.nevmock.digivise.application.dto.product.ads;

import lombok.*;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseDto;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class ProductAdsResponseDto {
    private Long campaignId;
    private String shopeeMerchantId;
    private Double acos;
    private Double cpc;
    private String from;
    private String to;
    private LocalDateTime createdAt;
    private String id;
    private String title;
    private String image;
    private Double dailyBudget;

    private Double roas;

    private Double click;
    private Double ctr;
    private String insight;

    private String state;
    private Double impression;

    private String convertion;

    private List<ProductKeywordResponseDto> keywords;

    private String biddingStrategy;

    private String shopId;

    private boolean hasKeywords;

    private String insightBudget;
}
