package org.nevmock.digivise.application.dto.product.ads;

import lombok.*;

@Builder
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductAdsNewestResponseDto {
    private String title;
    private Long dailyBudget;
    private String biddingType;
    private String productPlacement;
    private String adsPeriod;
    private String image;
}
