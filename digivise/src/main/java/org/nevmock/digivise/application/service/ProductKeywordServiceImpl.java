package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseDto;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseWrapperDto;
import org.nevmock.digivise.domain.model.KPI;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.port.in.ProductKeywordService;
import org.nevmock.digivise.domain.port.out.KPIRepository;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.utils.MathKt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductKeywordServiceImpl implements ProductKeywordService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private KPIRepository kpiRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Override
    public Page<ProductKeywordResponseWrapperDto> findByRange(
            String shopId,
            LocalDateTime from1,
            LocalDateTime to1,
            LocalDateTime from2,
            LocalDateTime to2,
            String name,
            Long campaignId,
            Pageable pageable
    ) {
        Merchant merchant = merchantRepository.findByShopeeMerchantId(shopId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
        KPI kpi = kpiRepository.findByMerchantId(merchant.getId())
                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));

        List<ProductKeywordResponseDto> period1DataList = getAggregatedDataByCampaignForRange(
                shopId, from1, to1, name, campaignId
        );

        Map<Long, ProductKeywordResponseDto> period2DataMap = getAggregatedDataByCampaignForRange(
                shopId, from2, to2, name, campaignId
        ).stream().collect(Collectors.toMap(
                ProductKeywordResponseDto::getCampaignId,
                Function.identity(),
                (existing, replacement) -> existing
        ));


        if (period1DataList.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<ProductKeywordResponseWrapperDto> wrappers = period1DataList.stream()
                .map(dto -> {
                    ProductKeywordResponseDto prevDto = period2DataMap.get(dto.getCampaignId());
                    populateComparisonFields(dto, prevDto);
                    dto.setInsight(generateInsight(dto, kpi));

                    return ProductKeywordResponseWrapperDto.builder()
                            .campaignId(dto.getCampaignId())
                            .shopId(shopId)
                            .from1(from1)
                            .to1(to1)
                            .from2(from2)
                            .to2(to2)
                            .data(Collections.singletonList(dto))
                            .build();
                })
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), wrappers.size());
        List<ProductKeywordResponseWrapperDto> pagedList = wrappers.subList(start, end);

        return new PageImpl<>(pagedList, pageable, wrappers.size());
    }

    private List<ProductKeywordResponseDto> getAggregatedDataByCampaignForRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            String keywordFilter,
            Long campaignIdFilter
    ) {
        long fromTs = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTs   = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        List<AggregationOperation> ops = new ArrayList<>();

        Criteria baseCriteria = Criteria.where("shop_id").is(shopId)
                .and("from").gte(fromTs).lte(toTs);
        ops.add(Aggregation.match(baseCriteria));

        if (campaignIdFilter != null) {
            ops.add(Aggregation.match(Criteria.where("campaign_id").is(campaignIdFilter)));
        }

        ops.add(Aggregation.unwind("data"));
        ops.add(Aggregation.unwind("data.data"));

        if (keywordFilter != null && !keywordFilter.isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.data.key").regex(keywordFilter, "i")
            ));
        }

        ops.add(Aggregation.project()
                .and("campaign_id").as("campaignId")
                .and("data.data.key").as("keyword")
                .and("data.data.ratio").as("ratio")
                .and("data.data.metrics").as("metrics")
        );

        GroupOperation group = Aggregation.group("campaignId", "keyword")
                .avg("ratio.broad_cir").as("ratioBroadCir")
                .avg("ratio.broad_gmv").as("ratioBroadGmv")
                .avg("ratio.broad_order").as("ratioBroadOrder")
                .avg("ratio.broad_order_amount").as("ratioBroadOrderAmount")
                .avg("ratio.broad_roi").as("ratioBroadRoi")
                .avg("ratio.checkout").as("ratioCheckout")
                .avg("ratio.checkout_rate").as("ratioCheckoutRate")
                .avg("ratio.click").as("ratioClick")
                .avg("ratio.cost").as("ratioCost")
                .avg("ratio.cpc").as("ratioCpc")
                .avg("ratio.cpdc").as("ratioCpdc")
                .avg("ratio.cr").as("ratioCr")
                .avg("ratio.ctr").as("ratioCtr")
                .avg("ratio.direct_cr").as("ratioDirectCr")
                .avg("ratio.direct_cir").as("ratioDirectCir")
                .avg("ratio.direct_gmv").as("ratioDirectGmv")
                .avg("ratio.direct_order").as("ratioDirectOrder")
                .avg("ratio.direct_order_amount").as("ratioDirectOrderAmount")
                .avg("ratio.direct_roi").as("ratioDirectRoi")
                .avg("ratio.impression").as("ratioImpression")
                .avg("ratio.product_click").as("ratioProductClick")
                .avg("ratio.product_impression").as("ratioProductImpression")
                .avg("ratio.product_ctr").as("ratioProductCtr")
                .avg("ratio.reach").as("ratioReach")
                .avg("ratio.page_views").as("ratioPageViews")
                .avg("ratio.unique_visitors").as("ratioUniqueVisitors")
                .avg("ratio.view").as("ratioView")
                .avg("ratio.cpm").as("ratioCpm")
                .avg("ratio.unique_click_user").as("ratioUniqueClickUser")

                .sum("metrics.broad_cir").as("metricsBroadCir")
                .sum("metrics.broad_gmv").as("metricsBroadGmv")
                .sum("metrics.broad_order").as("metricsBroadOrder")
                .sum("metrics.broad_order_amount").as("metricsBroadOrderAmount")
                .sum("metrics.checkout").as("metricsCheckout")
                .sum("metrics.click").as("metricsClick")
                .sum("metrics.cost").as("metricsCost")
                .sum("metrics.direct_gmv").as("metricsDirectGmv")
                .sum("metrics.direct_order").as("metricsDirectOrder")
                .sum("metrics.direct_order_amount").as("metricsDirectOrderAmount")
                .sum("metrics.impression").as("metricsImpression")
                .sum("metrics.product_click").as("metricsProductClick")
                .sum("metrics.product_impression").as("metricsProductImpression")
                .sum("metrics.reach").as("metricsReach")
                .sum("metrics.page_views").as("metricsPageViews")
                .sum("metrics.unique_visitors").as("metricsUniqueVisitors")
                .sum("metrics.view").as("metricsView")
                .sum("metrics.unique_click_user").as("metricsUniqueClickUser")

                .avg("metrics.broad_roi").as("metricsBroadRoi")
                .avg("metrics.checkout_rate").as("metricsCheckoutRate")
                .avg("metrics.cpc").as("metricsCpc")
                .avg("metrics.cpdc").as("metricsCpdc")
                .avg("metrics.cr").as("metricsCr")
                .avg("metrics.ctr").as("metricsCtr")
                .avg("metrics.direct_cr").as("metricsDirectCr")
                .avg("metrics.direct_cir").as("metricsDirectCir")
                .avg("metrics.direct_roi").as("metricsDirectRoi")
                .avg("metrics.avg_rank").as("metricsAvgRank")
                .avg("metrics.product_ctr").as("metricsProductCtr")
                .avg("metrics.location_in_ads").as("metricsLocationInAds")
                .avg("metrics.cpm").as("metricsCpm");
        ops.add(group);

        ProjectionOperation project = Aggregation.project()
                .and("_id.campaignId").as("campaignId")
                .and("_id.keyword").as("keyword")
                .andInclude(
                        "ratioBroadCir", "ratioBroadGmv", "ratioBroadOrder", "ratioBroadOrderAmount",
                        "ratioBroadRoi", "ratioCheckout", "ratioCheckoutRate", "ratioClick",
                        "ratioCost", "ratioCpc", "ratioCpdc", "ratioCr", "ratioCtr",
                        "ratioDirectCr", "ratioDirectCir", "ratioDirectGmv", "ratioDirectOrder",
                        "ratioDirectOrderAmount", "ratioDirectRoi", "ratioImpression",
                        "ratioProductClick", "ratioProductImpression", "ratioProductCtr",
                        "ratioReach", "ratioPageViews", "ratioUniqueVisitors", "ratioView",
                        "ratioCpm", "ratioUniqueClickUser",
                        "metricsBroadCir", "metricsBroadGmv", "metricsBroadOrder", "metricsBroadOrderAmount",
                        "metricsCheckout", "metricsClick", "metricsCost", "metricsCpm",
                        "metricsDirectGmv", "metricsDirectOrder", "metricsDirectOrderAmount",
                        "metricsImpression", "metricsProductClick", "metricsProductImpression",
                        "metricsReach", "metricsPageViews", "metricsUniqueVisitors", "metricsView",
                        "metricsUniqueClickUser", "metricsBroadRoi", "metricsCheckoutRate",
                        "metricsCpc", "metricsCpdc", "metricsCr", "metricsCtr",
                        "metricsDirectCr", "metricsDirectCir", "metricsDirectRoi",
                        "metricsAvgRank", "metricsProductCtr", "metricsLocationInAds"
                );
        ops.add(project);

        AggregationResults<ProductKeywordResponseDto> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops),
                "ProductKey",
                ProductKeywordResponseDto.class
        );

        return results.getMappedResults();
    }

    private void populateComparisonFields(ProductKeywordResponseDto current, ProductKeywordResponseDto previous) {
        if (previous == null) return;

        current.setRatioBroadCirComparison(calculateChange(current.getRatioBroadCir(), previous.getRatioBroadCir()));
        current.setRatioBroadGmvComparison(calculateChange(current.getRatioBroadGmv(), previous.getRatioBroadGmv()));
        current.setRatioBroadOrderComparison(calculateChange(current.getRatioBroadOrder(), previous.getRatioBroadOrder()));
        current.setRatioBroadOrderAmountComparison(calculateChange(current.getRatioBroadOrderAmount(), previous.getRatioBroadOrderAmount()));
        current.setRatioBroadRoiComparison(calculateChange(current.getRatioBroadRoi(), previous.getRatioBroadRoi()));
        current.setRatioCheckoutComparison(calculateChange(current.getRatioCheckout(), previous.getRatioCheckout()));
        current.setRatioCheckoutRateComparison(calculateChange(current.getRatioCheckoutRate(), previous.getRatioCheckoutRate()));
        current.setRatioClickComparison(calculateChange(current.getRatioClick(), previous.getRatioClick()));
        current.setRatioCostComparison(calculateChange(current.getRatioCost(), previous.getRatioCost()));
        current.setRatioCpcComparison(calculateChange(current.getRatioCpc(), previous.getRatioCpc()));
        current.setRatioCpdcComparison(calculateChange(current.getRatioCpdc(), previous.getRatioCpdc()));
        current.setRatioCrComparison(calculateChange(current.getRatioCr(), previous.getRatioCr()));
        current.setRatioCtrComparison(calculateChange(current.getRatioCtr(), previous.getRatioCtr()));
        current.setRatioDirectCrComparison(calculateChange(current.getRatioDirectCr(), previous.getRatioDirectCr()));
        current.setRatioDirectCirComparison(calculateChange(current.getRatioDirectCir(), previous.getRatioDirectCir()));
        current.setRatioDirectGmvComparison(calculateChange(current.getRatioDirectGmv(), previous.getRatioDirectGmv()));
        current.setRatioDirectOrderComparison(calculateChange(current.getRatioDirectOrder(), previous.getRatioDirectOrder()));
        current.setRatioDirectOrderAmountComparison(calculateChange(current.getRatioDirectOrderAmount(), previous.getRatioDirectOrderAmount()));
        current.setRatioDirectRoiComparison(calculateChange(current.getRatioDirectRoi(), previous.getRatioDirectRoi()));
        current.setRatioImpressionComparison(calculateChange(current.getRatioImpression(), previous.getRatioImpression()));
        current.setRatioProductClickComparison(calculateChange(current.getRatioProductClick(), previous.getRatioProductClick()));
        current.setRatioProductImpressionComparison(calculateChange(current.getRatioProductImpression(), previous.getRatioProductImpression()));
        current.setRatioProductCtrComparison(calculateChange(current.getRatioProductCtr(), previous.getRatioProductCtr()));
        current.setRatioReachComparison(calculateChange(current.getRatioReach(), previous.getRatioReach()));
        current.setRatioPageViewsComparison(calculateChange(current.getRatioPageViews(), previous.getRatioPageViews()));
        current.setRatioUniqueVisitorsComparison(calculateChange(current.getRatioUniqueVisitors(), previous.getRatioUniqueVisitors()));
        current.setRatioViewComparison(calculateChange(current.getRatioView(), previous.getRatioView()));
        current.setRatioCpmComparison(calculateChange(current.getRatioCpm(), previous.getRatioCpm()));
        current.setRatioUniqueClickUserComparison(calculateChange(current.getRatioUniqueClickUser(), previous.getRatioUniqueClickUser()));

        current.setMetricsBroadCirComparison(calculateChange(current.getMetricsBroadCir(), previous.getMetricsBroadCir()));
        current.setMetricsBroadGmvComparison(calculateChange(current.getMetricsBroadGmv(), previous.getMetricsBroadGmv()));
        current.setMetricsBroadOrderComparison(calculateChange(current.getMetricsBroadOrder(), previous.getMetricsBroadOrder()));
        current.setMetricsBroadOrderAmountComparison(calculateChange(current.getMetricsBroadOrderAmount(), previous.getMetricsBroadOrderAmount()));
        current.setMetricsBroadRoiComparison(calculateChange(current.getMetricsBroadRoi(), previous.getMetricsBroadRoi()));
        current.setMetricsCheckoutComparison(calculateChange(current.getMetricsCheckout(), previous.getMetricsCheckout()));
        current.setMetricsCheckoutRateComparison(calculateChange(current.getMetricsCheckoutRate(), previous.getMetricsCheckoutRate()));
        current.setMetricsClickComparison(calculateChange(current.getMetricsClick(), previous.getMetricsClick()));
        current.setMetricsCostComparison(calculateChange(current.getMetricsCost(), previous.getMetricsCost()));
        current.setMetricsCpcComparison(calculateChange(current.getMetricsCpc(), previous.getMetricsCpc()));
        current.setMetricsCpdcComparison(calculateChange(current.getMetricsCpdc(), previous.getMetricsCpdc()));
        current.setMetricsCrComparison(calculateChange(current.getMetricsCr(), previous.getMetricsCr()));
        current.setMetricsCtrComparison(calculateChange(current.getMetricsCtr(), previous.getMetricsCtr()));
        current.setMetricsDirectCrComparison(calculateChange(current.getMetricsDirectCr(), previous.getMetricsDirectCr()));
        current.setMetricsDirectCirComparison(calculateChange(current.getMetricsDirectCir(), previous.getMetricsDirectCir()));
        current.setMetricsDirectGmvComparison(calculateChange(current.getMetricsDirectGmv(), previous.getMetricsDirectGmv()));
        current.setMetricsDirectOrderComparison(calculateChange(current.getMetricsDirectOrder(), previous.getMetricsDirectOrder()));
        current.setMetricsDirectOrderAmountComparison(calculateChange(current.getMetricsDirectOrderAmount(), previous.getMetricsDirectOrderAmount()));
        current.setMetricsDirectRoiComparison(calculateChange(current.getMetricsDirectRoi(), previous.getMetricsDirectRoi()));
        current.setMetricsImpressionComparison(calculateChange(current.getMetricsImpression(), previous.getMetricsImpression()));
        current.setMetricsAvgRankComparison(calculateChange(current.getMetricsAvgRank(), previous.getMetricsAvgRank()));
        current.setMetricsProductClickComparison(calculateChange(current.getMetricsProductClick(), previous.getMetricsProductClick()));
        current.setMetricsProductImpressionComparison(calculateChange(current.getMetricsProductImpression(), previous.getMetricsProductImpression()));
        current.setMetricsProductCtrComparison(calculateChange(current.getMetricsProductCtr(), previous.getMetricsProductCtr()));
        current.setMetricsLocationInAdsComparison(calculateChange(current.getMetricsLocationInAds(), previous.getMetricsLocationInAds()));
        current.setMetricsReachComparison(calculateChange(current.getMetricsReach(), previous.getMetricsReach()));
        current.setMetricsPageViewsComparison(calculateChange(current.getMetricsPageViews(), previous.getMetricsPageViews()));
        current.setMetricsUniqueVisitorsComparison(calculateChange(current.getMetricsUniqueVisitors(), previous.getMetricsUniqueVisitors()));
        current.setMetricsViewComparison(calculateChange(current.getMetricsView(), previous.getMetricsView()));
        current.setMetricsCpmComparison(calculateChange(current.getMetricsCpm(), previous.getMetricsCpm()));
        current.setMetricsUniqueClickUserComparison(calculateChange(current.getMetricsUniqueClickUser(), previous.getMetricsUniqueClickUser()));
    }

    private Double calculateChange(Double current, Double previous) {
        if (current == null || previous == null || previous == 0) {
            return 0.0;
        }
        return (current - previous) / previous;
    }

    private String generateInsight(ProductKeywordResponseDto dto, KPI kpi) {
        return MathKt.renderInsight(
                MathKt.formulateRecommendation(
                        dto.getMetricsCpc(),
                        dto.getMetricsBroadCir(),
                        dto.getMetricsClick(),
                        kpi,
                        null,
                        null
                )
        );
    }

    private Long getLong(Document doc, String key) {
        if (doc == null) return null;
        Object v = doc.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        return null;
    }

    private Double getDouble(Document doc, String key) {
        if (doc == null) return null;
        Object v = doc.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return null;
    }

    private Integer getInteger(Document doc, String key) {
        if (doc == null) return null;
        Object v = doc.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return null;
    }
}