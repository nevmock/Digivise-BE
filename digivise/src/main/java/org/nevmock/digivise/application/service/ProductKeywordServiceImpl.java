//package org.nevmock.digivise.application.service;
//
//import org.bson.Document;
//import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseDto;
//import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseWrapperDto;
//import org.nevmock.digivise.domain.model.KPI;
//import org.nevmock.digivise.domain.model.Merchant;
//import org.nevmock.digivise.domain.port.in.ProductKeywordService;
//import org.nevmock.digivise.domain.port.out.KPIRepository;
//import org.nevmock.digivise.domain.port.out.MerchantRepository;
//import org.nevmock.digivise.utils.MathKt;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.aggregation.Aggregation;
//import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
//import org.springframework.data.mongodb.core.aggregation.AggregationResults;
//import org.springframework.data.mongodb.core.aggregation.FacetOperation;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//public class ProductKeywordServiceImpl implements ProductKeywordService {
//
//    @Autowired
//    private MongoTemplate mongoTemplate;
//
//    @Autowired
//    private KPIRepository kpiRepository;
//
//    @Autowired
//    private MerchantRepository merchantRepository;
//
//    @Override
//    public Page<ProductKeywordResponseWrapperDto> findByRange(
//            String shopId,
//            LocalDateTime from,
//            LocalDateTime to,
//            String name,
//            Long campaignId,
//            Pageable pageable
//    ) {
//        Merchant merchant = merchantRepository
//                .findByShopeeMerchantId(shopId)
//                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
//        KPI kpi = kpiRepository
//                .findByMerchantId(merchant.getId())
//                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));
//
//        List<AggregationOperation> ops = new ArrayList<>();
//
//        long fromTs = from.atZone(ZoneId.systemDefault()).toEpochSecond();
//
//        ops.add(Aggregation.match(
//                Criteria.where("shop_id").is(shopId)
//                        .and("from").gte(fromTs)
//        ));
//
//        if (campaignId != null) {
//            ops.add(Aggregation.match(
//                    Criteria.where("campaign_id").is(campaignId)
//            ));
//        }
//
//        ops.add(Aggregation.unwind("data.data"));
//
//        if (name != null && !name.trim().isEmpty()) {
//            ops.add(Aggregation.match(
//                    Criteria.where("data.data.key").regex(".*" + name.trim() + ".*", "i")
//            ));
//        }
//
//        ops.add(Aggregation.project()
//                .and("uuid").as("uuid")
//                .and("shop_id").as("shopId")
//                .and("campaign_id").as("campaignId")
//                .and("type").as("type")
//                .and("createdAt").as("createdAt")
//                .and("from").as("from")
//                .and("to").as("to")
//                .and("data.data.key").as("keyword")
//                .and("data.data.ratio").as("ratio")
//                .and("data.data.metrics").as("metrics")
//        );
//
//        FacetOperation facet = Aggregation.facet(
//                        Aggregation.skip((long) pageable.getOffset()),
//                        Aggregation.limit(pageable.getPageSize())
//                ).as("pagedResults")
//                .and(Aggregation.count().as("totalCount")).as("countResult");
//        ops.add(facet);
//
//        // execute aggregation
//        AggregationResults<Document> results = mongoTemplate.aggregate(
//                Aggregation.newAggregation(ops),
//                "ProductKey",
//                Document.class
//        );
//
//        Document root = results.getMappedResults().stream().findFirst().orElse(null);
//        if (root == null) {
//            return new PageImpl<>(Collections.emptyList(), pageable, 0);
//        }
//
//        @SuppressWarnings("unchecked")
//        List<Document> docs = (List<Document>) root.get("pagedResults");
//        @SuppressWarnings("unchecked")
//        List<Document> countDocs = (List<Document>) root.get("countResult");
//        long total = countDocs.isEmpty() ? 0 : countDocs.get(0).getInteger("totalCount");
//
//        List<ProductKeywordResponseDto> dtos = docs.stream()
//                .map(doc -> {
//                    Document ratioDoc = (Document) doc.get("ratio");
//                    Document metricsDoc = (Document) doc.get("metrics");
//
//                    return ProductKeywordResponseDto.builder()
//                            .uuid(doc.getString("uuid"))
//                            .shopId(doc.getString("shopId"))
//                            .campaignId(getLong(doc, "campaignId"))
//                            .type(doc.getString("type"))
//                            .createdAt(convertDate(doc.getDate("createdAt")))
//                            .from(getLong(doc, "from"))
//                            .to(getLong(doc, "to"))
//
//                            .ratioBroadCir(getDouble(ratioDoc, "broad_cir"))
//                            .ratioBroadGmv(getDouble(ratioDoc, "broad_gmv"))
//                            .ratioBroadOrder(getDouble(ratioDoc, "broad_order"))
//                            .ratioBroadOrderAmount(getDouble(ratioDoc, "broad_order_amount"))
//                            .ratioBroadRoi(getDouble(ratioDoc, "broad_roi"))
//                            .ratioCheckout(getDouble(ratioDoc, "checkout"))
//                            .ratioCheckoutRate(getDouble(ratioDoc, "checkout_rate"))
//                            .ratioClick(getDouble(ratioDoc, "click"))
//                            .ratioCost(getDouble(ratioDoc, "cost"))
//                            .ratioCpc(getDouble(ratioDoc, "cpc"))
//                            .ratioCpdc(getDouble(ratioDoc, "cpdc"))
//                            .ratioCr(getDouble(ratioDoc, "cr"))
//                            .ratioCtr(getDouble(ratioDoc, "ctr"))
//                            .ratioDirectCr(getDouble(ratioDoc, "direct_cr"))
//                            .ratioDirectCir(getDouble(ratioDoc, "direct_cir"))
//                            .ratioDirectGmv(getDouble(ratioDoc, "direct_gmv"))
//                            .ratioDirectOrder(getDouble(ratioDoc, "direct_order"))
//                            .ratioDirectOrderAmount(getDouble(ratioDoc, "direct_order_amount"))
//                            .ratioDirectRoi(getDouble(ratioDoc, "direct_roi"))
//                            .ratioImpression(getDouble(ratioDoc, "impression"))
//                            .ratioView(getDouble(ratioDoc, "view"))
//
//                            .metricsBroadCir(getDouble(metricsDoc, "broad_cir"))
//                            .metricsBroadGmv(getLong(metricsDoc, "broad_gmv"))
//                            .metricsBroadOrder(getInteger(metricsDoc, "broad_order"))
//                            .metricsBroadOrderAmount(getInteger(metricsDoc, "broad_order_amount"))
//                            .metricsBroadRoi(getDouble(metricsDoc, "broad_roi"))
//                            .metricsCheckout(getInteger(metricsDoc, "checkout"))
//                            .metricsCheckoutRate(getDouble(metricsDoc, "checkout_rate"))
//                            .metricsClick(getDouble(metricsDoc, "click"))
//                            .metricsCost(getLong(metricsDoc, "cost"))
//                            .metricsCr(getDouble(metricsDoc, "cr"))
//                            .metricsCtr(getDouble(metricsDoc, "ctr"))
//                            .metricsDirectGmv(getLong(metricsDoc, "direct_gmv"))
//                            .metricsDirectOrder(getInteger(metricsDoc, "direct_order"))
//                            .metricsDirectRoi(getDouble(metricsDoc, "direct_roi"))
//                            .metricsImpression(getInteger(metricsDoc, "impression"))
//                            .metricsAvgRank(getInteger(metricsDoc, "avg_rank"))
//                            .metricsView(getLong(metricsDoc, "view"))
//                            .keyword(doc.getString("keyword"))
//                            .insight(MathKt.renderInsight(
//                                    MathKt.formulateRecommendation(
//                                            getDouble(metricsDoc, "cpc"), getDouble(metricsDoc, "broad_cir"), getDouble(metricsDoc, "click"), kpi, null, null
//                                    )
//
//                            ))
//                            .build();
//                })
//                .collect(Collectors.toList());
//
//        Map<String, List<ProductKeywordResponseDto>> grouped = dtos.stream()
//                .collect(Collectors.groupingBy(ProductKeywordResponseDto::getKeyword));
//
//        List<ProductKeywordResponseWrapperDto> wrappers = grouped.entrySet().stream()
//                .map(e -> ProductKeywordResponseWrapperDto.builder()
//                        .shopId(shopId)
//                        .from(from)
//                        .to(to)
//                        .data(e.getValue())
//                        .build())
//                .collect(Collectors.toList());
//
//        return new PageImpl<>(wrappers, pageable, total);
//    }
//
//    private LocalDateTime convertDate(Date date) {
//        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//    }
//
//    private Integer getInteger(Document doc, String key) {
//        if (doc == null) return null;
//        Object v = doc.get(key);
//        if (v instanceof Number) return ((Number) v).intValue();
//        return null;
//    }
//
//    private Long getLong(Document doc, String key) {
//        if (doc == null) return null;
//        Object v = doc.get(key);
//        if (v instanceof Number) return ((Number) v).longValue();
//        return null;
//    }
//
//    private Double getDouble(Document doc, String key) {
//        if (doc == null) return null;
//        Object v = doc.get(key);
//        if (v instanceof Number) return ((Number) v).doubleValue();
//        return null;
//    }
//}
package org.nevmock.digivise.application.service;

import org.bson.Document;
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

        // Get aggregated data for period 1
        List<ProductKeywordResponseDto> period1DataList = getAggregatedDataByCampaignForRange(
                shopId, from1, to1, name, campaignId
        );

        // Get aggregated data for period 2 and convert to map
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

        // Build wrapper DTOs with comparison
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

        // Pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), wrappers.size());
        List<ProductKeywordResponseWrapperDto> pagedList = wrappers.subList(start, end);

        return new PageImpl<>(pagedList, pageable, wrappers.size());
    }

    private List<ProductKeywordResponseDto> getAggregatedDataByCampaignForRange(
            String shopId, LocalDateTime from, LocalDateTime to, String keywordFilter, Long campaignIdFilter
    ) {
        long fromTs = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTs = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        List<AggregationOperation> ops = new ArrayList<>();

        // Base criteria
        Criteria criteria = Criteria.where("shop_id").is(shopId)
                .and("from").gte(fromTs).lte(toTs);
        ops.add(Aggregation.match(criteria));

        if (campaignIdFilter != null) {
            ops.add(Aggregation.match(Criteria.where("campaign_id").is(campaignIdFilter)));
        }

        ops.add(Aggregation.unwind("data"));
        ops.add(Aggregation.unwind("data.data"));

        if (keywordFilter != null && !keywordFilter.isEmpty()) {
            ops.add(Aggregation.match(Criteria.where("data.data.key").regex(keywordFilter, "i")));
        }

        // Group by campaign_id
        GroupOperation group = Aggregation.group("campaign_id")
                // Ratios: Average
                .avg("data.data.ratio.broad_cir").as("ratioBroadCir")
                .avg("data.data.ratio.broad_gmv").as("ratioBroadGmv")
                .avg("data.data.ratio.broad_order").as("ratioBroadOrder")
                .avg("data.data.ratio.broad_order_amount").as("ratioBroadOrderAmount")
                .avg("data.data.ratio.broad_roi").as("ratioBroadRoi")
                .avg("data.data.ratio.checkout").as("ratioCheckout")
                .avg("data.data.ratio.checkout_rate").as("ratioCheckoutRate")
                .avg("data.data.ratio.click").as("ratioClick")
                .avg("data.data.ratio.cost").as("ratioCost")
                .avg("data.data.ratio.cpc").as("ratioCpc")
                .avg("data.data.ratio.cpdc").as("ratioCpdc")
                .avg("data.data.ratio.cr").as("ratioCr")
                .avg("data.data.ratio.ctr").as("ratioCtr")
                .avg("data.data.ratio.direct_cr").as("ratioDirectCr")
                .avg("data.data.ratio.direct_cir").as("ratioDirectCir")
                .avg("data.data.ratio.direct_gmv").as("ratioDirectGmv")
                .avg("data.data.ratio.direct_order").as("ratioDirectOrder")
                .avg("data.data.ratio.direct_order_amount").as("ratioDirectOrderAmount")
                .avg("data.data.ratio.direct_roi").as("ratioDirectRoi")
                .avg("data.data.ratio.impression").as("ratioImpression")
                .avg("data.data.ratio.product_click").as("ratioProductClick")
                .avg("data.data.ratio.product_impression").as("ratioProductImpression")
                .avg("data.data.ratio.product_ctr").as("ratioProductCtr")
                .avg("data.data.ratio.reach").as("ratioReach")
                .avg("data.data.ratio.page_views").as("ratioPageViews")
                .avg("data.data.ratio.unique_visitors").as("ratioUniqueVisitors")
                .avg("data.data.ratio.view").as("ratioView")
                .avg("data.data.ratio.cpm").as("ratioCpm")
                .avg("data.data.ratio.unique_click_user").as("ratioUniqueClickUser")

                // Metrics: Sum for absolute values
                .sum("data.data.metrics.broad_cir").as("metricsBroadCir")
                .sum("data.data.metrics.broad_gmv").as("metricsBroadGmv")
                .sum("data.data.metrics.broad_order").as("metricsBroadOrder")
                .sum("data.data.metrics.broad_order_amount").as("metricsBroadOrderAmount")
                .sum("data.data.metrics.checkout").as("metricsCheckout")
                .sum("data.data.metrics.click").as("metricsClick")
                .sum("data.data.metrics.cost").as("metricsCost")
                .sum("data.data.metrics.direct_gmv").as("metricsDirectGmv")
                .sum("data.data.metrics.direct_order").as("metricsDirectOrder")
                .sum("data.data.metrics.direct_order_amount").as("metricsDirectOrderAmount")
                .sum("data.data.metrics.impression").as("metricsImpression")
                .sum("data.data.metrics.product_click").as("metricsProductClick")
                .sum("data.data.metrics.product_impression").as("metricsProductImpression")
                .sum("data.data.metrics.reach").as("metricsReach")
                .sum("data.data.metrics.page_views").as("metricsPageViews")
                .sum("data.data.metrics.unique_visitors").as("metricsUniqueVisitors")
                .sum("data.data.metrics.view").as("metricsView")
                .sum("data.data.metrics.unique_click_user").as("metricsUniqueClickUser")

                // Metrics: Average for ratios
                .avg("data.data.metrics.broad_roi").as("metricsBroadRoi")
                .avg("data.data.metrics.checkout_rate").as("metricsCheckoutRate")
                .avg("data.data.metrics.cpc").as("metricsCpc")
                .avg("data.data.metrics.cpdc").as("metricsCpdc")
                .avg("data.data.metrics.cr").as("metricsCr")
                .avg("data.data.metrics.ctr").as("metricsCtr")
                .avg("data.data.metrics.direct_cr").as("metricsDirectCr")
                .avg("data.data.metrics.direct_cir").as("metricsDirectCir")
                .avg("data.data.metrics.direct_roi").as("metricsDirectRoi")
                .avg("data.data.metrics.avg_rank").as("metricsAvgRank")
                .avg("data.data.metrics.product_ctr").as("metricsProductCtr")
                .avg("data.data.metrics.location_in_ads").as("metricsLocationInAds")
                .avg("data.data.metrics.cpm").as("metricsCpm");
        ops.add(group);

        // Projection
        ProjectionOperation project = Aggregation.project()
                .and("_id").as("campaignId")
                .andInclude(
                        "ratioBroadCir", "ratioBroadGmv", "ratioBroadOrder", "ratioBroadOrderAmount",
                        "ratioBroadRoi", "ratioCheckout", "ratioCheckoutRate", "ratioClick",
                        "ratioCost", "ratioCpc", "ratioCpdc", "ratioCr", "ratioCtr",
                        "ratioDirectCr", "ratioDirectCir", "ratioDirectGmv", "ratioDirectOrder",
                        "ratioDirectOrderAmount", "ratioDirectRoi", "ratioImpression",
                        "ratioProductClick", "ratioProductImpression", "ratioProductCtr",
                        "ratioReach", "ratioPageViews", "ratioUniqueVisitors", "ratioView",
                        "ratioCpm", "ratioUniqueClickUser",
                        "metricsBroadCir", "metricsBroadGmv", "metricsBroadOrder",
                        "metricsBroadOrderAmount", "metricsBroadRoi", "metricsCheckout",
                        "metricsCheckoutRate", "metricsClick", "metricsCost", "metricsCpc",
                        "metricsCpdc", "metricsCr", "metricsCtr", "metricsDirectCr",
                        "metricsDirectCir", "metricsDirectGmv", "metricsDirectOrder",
                        "metricsDirectOrderAmount", "metricsDirectRoi", "metricsImpression",
                        "metricsAvgRank", "metricsProductClick", "metricsProductImpression",
                        "metricsProductCtr", "metricsLocationInAds", "metricsReach",
                        "metricsPageViews", "metricsUniqueVisitors", "metricsView",
                        "metricsCpm", "metricsUniqueClickUser"
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

        // Ratio comparisons
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

        // Metrics comparisons
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
            return null;
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

    // Helper methods
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