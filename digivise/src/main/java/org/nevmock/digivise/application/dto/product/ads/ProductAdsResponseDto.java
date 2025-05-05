package org.nevmock.digivise.application.dto.product.ads;

import lombok.*;

import java.time.LocalDateTime;

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

    private Double click;
    private Double ctr;
    private String insight;

    private String state;
}
