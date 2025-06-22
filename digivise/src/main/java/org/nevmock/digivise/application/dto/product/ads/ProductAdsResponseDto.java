package org.nevmock.digivise.application.dto.product.ads;

import lombok.*;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordAdsResponseDto;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordAdsResponseWrapperDto;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseWrapperDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseClassificationDto;

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
    private Long shopeeFrom;
    private Long shopeeTo;
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
    private Double broadGmv;
    private Double broadOrder;
    private Double checkout;

    private Double directOrder;
    private Double directOrderAmount;
    private Double directGmv;
    private Double directRoi;
    private Double directCir;
    private Double directCr;
    private Double cpdc;
    private String insightBudget;
    private Double broadRoi;
    private Double cost;
    private Double broadOrderAmount;
    private Double cr;

    private List<ProductKeywordAdsResponseDto> keywords;

    private String biddingStrategy;

    private String shopId;

    private boolean hasKeywords;

    private Double acosRatio;
    private Double cpcRatio;
    private Double clickRatio;
    private Double impressionRatio;
    private Double costRatio;
    private Double ctrRatio;

    private Double broadGmvRatio;
    private Double broadOrderRatio;
    private Double checkoutRatio;
    private Double directOrderRatio;
    private Double directOrderAmountRatio;
    private Double directGmvRatio;
    private Double directRoiRatio;
    private Double directCirRatio;
    private Double directCrRatio;
    private Double cpdcRatio;

    private Double broadRoiRatio;
    private Double crRatio;
    private Double broadOrderAmountRatio;

    private String type;
    private Double customRoas;
    private Boolean hasCustomRoas;

    private Double dailyBudgetAvg;
    private Double roasAvg;
    private Double clickAvg;
    private Double ctrAvg;
    private Double impressionAvg;
    private Double broadGmvAvg;
    private Double broadOrderAvg;
    private Double checkoutAvg;
    private Double directOrderAvg;
    private Double directOrderAmountAvg;
    private Double directGmvAvg;
    private Double directRoiAvg;
    private Double directCirAvg;
    private Double directCrAvg;
    private Double cpdcAvg;
    private Double broadRoiAvg;
    private Double costAvg;
    private Double broadOrderAmountAvg;
    private Double crAvg;

    private String salesClassification;

    private List<ProductStockResponseClassificationDto> productStocks;
    private Boolean hasProductStock;

    private String productPlacement;

    private ProductKeywordAdsResponseWrapperDto keywordWrapper;
}
