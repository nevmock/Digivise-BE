package org.nevmock.digivise.application.dto.product.keyword;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductKeywordResponseDto {
    private String id;
    private String uuid;
    private Long productId;
    private String shopId;
    private LocalDateTime createdAt;
    private String keyword;
    private Long from;
    private Long to;
    private Long campaignId;
    private String type;

    private Double acos;
    private Double broadGmv;
    private Double broadOrder;
    private Double broadOrderAmount;
    private Double broadRoi;
    private Double checkout;
    private Double checkoutRate;
    private Double click;
    private Double cost;
    private Double cpc;
    private Double cpdc;
    private Double cr;
    private Double ctr;
    private Double directCr;
    private Double directCir;
    private Double directGmv;
    private Double directOrder;
    private Double directOrderAmount;
    private Double directRoi;
    private Double impression;
    private Double avgRank;
    private Double productClick;
    private Double productImpression;
    private Double productCtr;
    private Double locationInAds;
    private Double reach;
    private Double pageViews;
    private Double uniqueVisitors;
    private Double view;
    private Double cpm;
    private Double uniqueClickUser;

    private String insight;

    private Double acosComparison;
    private Double broadGmvComparison;
    private Double broadOrderComparison;
    private Double broadOrderAmountComparison;
    private Double broadRoiComparison;
    private Double checkoutComparison;
    private Double checkoutRateComparison;
    private Double clickComparison;
    private Double costComparison;
    private Double cpcComparison;
    private Double cpdcComparison;
    private Double crComparison;
    private Double ctrComparison;
    private Double directCrComparison;
    private Double directCirComparison;
    private Double directGmvComparison;
    private Double directOrderComparison;
    private Double directOrderAmountComparison;
    private Double directRoiComparison;
    private Double impressionComparison;
    private Double avgRankComparison;
    private Double productClickComparison;
    private Double productImpressionComparison;
    private Double productCtrComparison;
    private Double locationInAdsComparison;
    private Double reachComparison;
    private Double pageViewsComparison;
    private Double uniqueVisitorsComparison;
    private Double viewComparison;
    private Double cpmComparison;
    private Double uniqueClickUserComparison;
}

//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class ProductKeywordResponseDto {
//    private String id;
//    private String uuid;
//    private Long productId;
//    private String shopId;
//    private LocalDateTime createdAt;
//
//    private String keyword;
//    private Long from;
//    private Long to;
//    private Long campaignId;
//    private String type;
//
//    private Double ratioBroadCir;
//    private Double ratioBroadGmv;
//    private Double ratioBroadOrder;
//    private Double ratioBroadOrderAmount;
//    private Double ratioBroadRoi;
//    private Double ratioCheckout;
//    private Double ratioCheckoutRate;
//    private Double ratioClick;
//    private Double ratioCost;
//    private Double ratioCpc;
//    private Double ratioCpdc;
//    private Double ratioCr;
//    private Double ratioCtr;
//    private Double ratioDirectCr;
//    private Double ratioDirectCir;
//    private Double ratioDirectGmv;
//    private Double ratioDirectOrder;
//    private Double ratioDirectOrderAmount;
//    private Double ratioDirectRoi;
//    private Double ratioImpression;
//    private Double ratioProductClick;
//    private Double ratioProductImpression;
//    private Double ratioProductCtr;
//    private Double ratioReach;
//    private Double ratioPageViews;
//    private Double ratioUniqueVisitors;
//    private Double ratioView;
//    private Double ratioCpm;
//    private Double ratioUniqueClickUser;
//
//    private Double metricsBroadCir;
//    private Long metricsBroadGmv;
//    private Integer metricsBroadOrder;
//    private Integer metricsBroadOrderAmount;
//    private Double metricsBroadRoi;
//    private Integer metricsCheckout;
//    private Double metricsCheckoutRate;
//    private Double metricsClick;
//    private Long metricsCost;
//    private Double metricsCpc;
//    private Double metricsCpdc;
//    private Double metricsCr;
//    private Double metricsCtr;
//    private Double metricsDirectCr;
//    private Double metricsDirectCir;
//    private Long metricsDirectGmv;
//    private Integer metricsDirectOrder;
//    private Double metricsDirectOrderAmount;
//    private Double metricsDirectRoi;
//    private Integer metricsImpression;
//    private Integer metricsAvgRank;
//    private Double metricsProductClick;
//    private Double metricsProductImpression;
//    private Double metricsProductCtr;
//    private Double metricsLocationInAds;
//    private Double metricsReach;
//    private Double metricsPageViews;
//    private Double metricsUniqueVisitors;
//    private Long metricsView;
//    private Double metricsCpm;
//    private Double metricsUniqueClickUser;
//
//    private String insight;
//}