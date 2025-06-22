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

    private Integer ratioBroadCir;
    private Integer ratioBroadGmv;
    private Integer ratioBroadOrder;
    private Integer ratioBroadOrderAmount;
    private Integer ratioBroadRoi;
    private Integer ratioCheckout;
    private Double ratioCheckoutRate;
    private Integer ratioClick;
    private Integer ratioCost;
    private Integer ratioCpc;
    private Integer ratioCpdc;
    private Integer ratioCr;
    private Integer ratioCtr;
    private Integer ratioDirectCr;
    private Integer ratioDirectCir;
    private Integer ratioDirectGmv;
    private Integer ratioDirectOrder;
    private Integer ratioDirectOrderAmount;
    private Integer ratioDirectRoi;
    private Integer ratioImpression;
    private Integer ratioProductClick;
    private Integer ratioProductImpression;
    private Integer ratioProductCtr;
    private Integer ratioReach;
    private Integer ratioPageViews;
    private Integer ratioUniqueVisitors;
    private Integer ratioView;
    private Integer ratioCpm;
    private Integer ratioUniqueClickUser;

    private Integer metricsBroadCir;
    private Integer metricsBroadGmv;
    private Integer metricsBroadOrder;
    private Integer metricsBroadOrderAmount;
    private Integer metricsBroadRoi;
    private Integer metricsCheckout;
    private Double metricsCheckoutRate;
    private Integer metricsClick;
    private Long metricsCost;
    private Integer metricsCpc;
    private Integer metricsCpdc;
    private Integer metricsCr;
    private Double metricsCtr;
    private Integer metricsDirectCr;
    private Integer metricsDirectCir;
    private Integer metricsDirectGmv;
    private Integer metricsDirectOrder;
    private Integer metricsDirectOrderAmount;
    private Integer metricsDirectRoi;
    private Integer metricsImpression;
    private Integer metricsAvgRank;
    private Integer metricsProductClick;
    private Integer metricsProductImpression;
    private Integer metricsProductCtr;
    private Integer metricsLocationInAds;
    private Integer metricsReach;
    private Integer metricsPageViews;
    private Integer metricsUniqueVisitors;
    private Integer metricsView;
    private Integer metricsCpm;
    private Integer metricsUniqueClickUser;
}