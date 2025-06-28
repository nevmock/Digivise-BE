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
                .and("data.data.metrics").as("metrics")
        );

        GroupOperation group = Aggregation.group("campaignId", "keyword")
                .sum("metrics.broad_cir").as("broadCir")
                .sum("metrics.broad_gmv").as("broadGmv")
                .sum("metrics.broad_order").as("broadOrder")
                .sum("metrics.broad_order_amount").as("broadOrderAmount")
                .sum("metrics.checkout").as("checkout")
                .sum("metrics.click").as("click")
                .sum("metrics.cost").as("cost")
                .sum("metrics.direct_gmv").as("directGmv")
                .sum("metrics.direct_order").as("directOrder")
                .sum("metrics.direct_order_amount").as("directOrderAmount")
                .sum("metrics.impression").as("impression")
                .sum("metrics.product_click").as("productClick")
                .sum("metrics.product_impression").as("productImpression")
                .sum("metrics.reach").as("reach")
                .sum("metrics.page_views").as("pageViews")
                .sum("metrics.unique_visitors").as("uniqueVisitors")
                .sum("metrics.view").as("view")
                .sum("metrics.unique_click_user").as("uniqueClickUser")

                .avg("metrics.broad_roi").as("broadRoi")
                .avg("metrics.checkout_rate").as("checkoutRate")
                .avg("metrics.cpc").as("cpc")
                .avg("metrics.cpdc").as("cpdc")
                .avg("metrics.cr").as("cr")
                .avg("metrics.ctr").as("ctr")
                .avg("metrics.direct_cr").as("directCr")
                .avg("metrics.direct_cir").as("directCir")
                .avg("metrics.direct_roi").as("directRoi")
                .avg("metrics.avg_rank").as("avgRank")
                .avg("metrics.product_ctr").as("productCtr")
                .avg("metrics.location_in_ads").as("locationInAds")
                .avg("metrics.cpm").as("cpm");
        ops.add(group);

        ProjectionOperation project = Aggregation.project()
                .and("_id.campaignId").as("campaignId")
                .and("_id.keyword").as("keyword")
                .andInclude(
                        "broadCir", "broadGmv", "broadOrder", "broadOrderAmount",
                        "broadRoi", "checkout", "checkoutRate", "click", "cost",
                        "cpc", "cpdc", "cr", "ctr", "directCr", "directCir",
                        "directGmv", "directOrder", "directOrderAmount", "directRoi",
                        "impression", "avgRank", "productClick", "productImpression",
                        "productCtr", "locationInAds", "reach", "pageViews",
                        "uniqueVisitors", "view", "cpm", "uniqueClickUser"
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

        current.setBroadCirComparison(calculateChange(current.getBroadCir(), previous.getBroadCir()));
        current.setBroadGmvComparison(calculateChange(current.getBroadGmv(), previous.getBroadGmv()));
        current.setBroadOrderComparison(calculateChange(current.getBroadOrder(), previous.getBroadOrder()));
        current.setBroadOrderAmountComparison(calculateChange(current.getBroadOrderAmount(), previous.getBroadOrderAmount()));
        current.setBroadRoiComparison(calculateChange(current.getBroadRoi(), previous.getBroadRoi()));
        current.setCheckoutComparison(calculateChange(current.getCheckout(), previous.getCheckout()));
        current.setCheckoutRateComparison(calculateChange(current.getCheckoutRate(), previous.getCheckoutRate()));
        current.setClickComparison(calculateChange(current.getClick(), previous.getClick()));
        current.setCostComparison(calculateChange(current.getCost(), previous.getCost()));
        current.setCpcComparison(calculateChange(current.getCpc(), previous.getCpc()));
        current.setCpdcComparison(calculateChange(current.getCpdc(), previous.getCpdc()));
        current.setCrComparison(calculateChange(current.getCr(), previous.getCr()));
        current.setCtrComparison(calculateChange(current.getCtr(), previous.getCtr()));
        current.setDirectCrComparison(calculateChange(current.getDirectCr(), previous.getDirectCr()));
        current.setDirectCirComparison(calculateChange(current.getDirectCir(), previous.getDirectCir()));
        current.setDirectGmvComparison(calculateChange(current.getDirectGmv(), previous.getDirectGmv()));
        current.setDirectOrderComparison(calculateChange(current.getDirectOrder(), previous.getDirectOrder()));
        current.setDirectOrderAmountComparison(calculateChange(current.getDirectOrderAmount(), previous.getDirectOrderAmount()));
        current.setDirectRoiComparison(calculateChange(current.getDirectRoi(), previous.getDirectRoi()));
        current.setImpressionComparison(calculateChange(current.getImpression(), previous.getImpression()));
        current.setAvgRankComparison(calculateChange(current.getAvgRank(), previous.getAvgRank()));
        current.setProductClickComparison(calculateChange(current.getProductClick(), previous.getProductClick()));
        current.setProductImpressionComparison(calculateChange(current.getProductImpression(), previous.getProductImpression()));
        current.setProductCtrComparison(calculateChange(current.getProductCtr(), previous.getProductCtr()));
        current.setLocationInAdsComparison(calculateChange(current.getLocationInAds(), previous.getLocationInAds()));
        current.setReachComparison(calculateChange(current.getReach(), previous.getReach()));
        current.setPageViewsComparison(calculateChange(current.getPageViews(), previous.getPageViews()));
        current.setUniqueVisitorsComparison(calculateChange(current.getUniqueVisitors(), previous.getUniqueVisitors()));
        current.setViewComparison(calculateChange(current.getView(), previous.getView()));
        current.setCpmComparison(calculateChange(current.getCpm(), previous.getCpm()));
        current.setUniqueClickUserComparison(calculateChange(current.getUniqueClickUser(), previous.getUniqueClickUser()));
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
                        dto.getCpc(),
                        dto.getBroadCir(),
                        dto.getClick(),
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