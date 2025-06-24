package org.nevmock.digivise.application.dto.product.keyword;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductKeywordChartResponseDto {
    private String key;
    private Long campaignId;
    private String type;
    private Double impression;
    private Double click;
    private Double ctr;
    private Double checkout;
    private Double broadOrderAmount;
    private Double broadGmv;
    private Double dailyBudget;
    private Double roas;
    private Long shopeeFrom;
    private Long shopeeTo;
    private LocalDateTime createdAt;
}
