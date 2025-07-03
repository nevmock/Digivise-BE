package org.nevmock.digivise.application.dto.product.ads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductAdsChartResponseDto {
    private Long campaignId;
    private Double impression;
    private Double click;
    private Double ctr;
    private Double broadOrderAmount;
    private Double broadGmv;
    private Double dailyBudget;
    private Double roas;
    private Double cost;
    private Double checkout;
    private Long shopeeFrom;
    private Long shopeeTo;
    private LocalDateTime createdAt;
}