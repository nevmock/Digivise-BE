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
    private String shopeeFrom;
    private String shopeeTo;
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

    private List<ProductKeywordResponseDto> keywords;

    private String biddingStrategy;

    private String shopId;

    private boolean hasKeywords;

    private String insightBudget;

    private Double cost;

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

    private String type;

    private Double customRoas;

    private Boolean hasCustomRoas;

    private Double broadRoi;
    private Double broadRoiRatio;

    private Double cr;
    private Double crRatio;

    private Double broadOrderAmount;
    private Double broadOrderAmountRatio;
}
