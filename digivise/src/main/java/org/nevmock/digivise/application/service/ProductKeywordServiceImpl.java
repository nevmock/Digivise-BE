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
                .avg("metrics.broad_cir").as("acos")
                .avg("metrics.broad_gmv").as("broadGmv")
                .avg("metrics.broad_order").as("broadOrder")
                .sum("metrics.broad_order_amount").as("broadOrderAmount")
                .avg("metrics.checkout").as("checkout")
                .sum("metrics.click").as("click")
                .sum("metrics.cost").as("cost")
                .avg("metrics.direct_gmv").as("directGmv")
                .avg("metrics.direct_order").as("directOrder")
                .sum("metrics.direct_order_amount").as("directOrderAmount")
                .sum("metrics.impression").as("impression")
                .avg("metrics.product_click").as("productClick")
                .avg("metrics.product_impression").as("productImpression")
                .avg("metrics.reach").as("reach")
                .avg("metrics.page_views").as("pageViews")
                .avg("metrics.unique_visitors").as("uniqueVisitors")
                .avg("metrics.view").as("view")
                .avg("metrics.unique_click_user").as("uniqueClickUser")

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
                .and("broadGmv").divide(100000.0).as("broadGmv")
                .and("cost").divide(100000.0).as("cost")
                .and("cpc").divide(100000.0).as("cpc")
                .and("cpdc").divide(100000.0).as("cpdc")
                .and("directGmv").divide(100000.0).as("directGmv")
                .and(ArithmeticOperators.Round.roundValueOf("acos").place(2)).as("acos")
                .and(ArithmeticOperators.Round.roundValueOf("broadOrder").place(2)).as("broadOrder")
                .and("broadOrderAmount").as("broadOrderAmount")
                .and(ArithmeticOperators.Round.roundValueOf("broadRoi").place(2)).as("broadRoi")
                .and(ArithmeticOperators.Round.roundValueOf("checkout").place(2)).as("checkout")
                .and(ArithmeticOperators.Round.roundValueOf("checkoutRate").place(2)).as("checkoutRate")
                .and("click").as("click")
                .and(ArithmeticOperators.Round.roundValueOf("cr").place(2)).as("cr")
                .and(ArithmeticOperators.Round.roundValueOf("ctr").place(2)).as("ctr")
                .and(ArithmeticOperators.Round.roundValueOf("directCr").place(2)).as("directCr")
                .and(ArithmeticOperators.Round.roundValueOf("directCir").place(2)).as("directCir")
                .and(ArithmeticOperators.Round.roundValueOf("directOrder").place(2)).as("directOrder")
                .and("directOrderAmount").as("directOrderAmount")
                .and(ArithmeticOperators.Round.roundValueOf("directRoi").place(2)).as("directRoi")
                .and("impression").as("impression")
                .and(ArithmeticOperators.Round.roundValueOf("avgRank").place(2)).as("avgRank")
                .and(ArithmeticOperators.Round.roundValueOf("productClick").place(2)).as("productClick")
                .and(ArithmeticOperators.Round.roundValueOf("productImpression").place(2)).as("productImpression")
                .and(ArithmeticOperators.Round.roundValueOf("productCtr").place(2)).as("productCtr")
                .and("locationInAds").as("locationInAds")
                .and(ArithmeticOperators.Round.roundValueOf("reach").place(2)).as("reach")
                .and(ArithmeticOperators.Round.roundValueOf("pageViews").place(2)).as("pageViews")
                .and(ArithmeticOperators.Round.roundValueOf("uniqueVisitors").place(2)).as("uniqueVisitors")
                .and(ArithmeticOperators.Round.roundValueOf("view").place(2)).as("view")
                .and(ArithmeticOperators.Round.roundValueOf("cpm").place(2)).as("cpm")
                .and(ArithmeticOperators.Round.roundValueOf("uniqueClickUser").place(2)).as("uniqueClickUser");

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

        current.setAcos(roundDouble(calculateChange(current.getAcos(), previous.getAcos())));
        current.setBroadGmvComparison(roundDouble(calculateChange(current.getBroadGmv(), previous.getBroadGmv())));
        current.setBroadOrderComparison(roundDouble(calculateChange(current.getBroadOrder(), previous.getBroadOrder())));
        current.setBroadOrderAmountComparison(roundDouble(calculateChange(current.getBroadOrderAmount(), previous.getBroadOrderAmount())));
        current.setBroadRoiComparison(roundDouble(calculateChange(current.getBroadRoi(), previous.getBroadRoi())));
        current.setCheckoutComparison(roundDouble(calculateChange(current.getCheckout(), previous.getCheckout())));
        current.setCheckoutRateComparison(roundDouble(calculateChange(current.getCheckoutRate(), previous.getCheckoutRate())));
        current.setClickComparison(roundDouble(calculateChange(current.getClick(), previous.getClick())));
        current.setCostComparison(roundDouble(calculateChange(current.getCost(), previous.getCost())));
        current.setCpcComparison(roundDouble(calculateChange(current.getCpc(), previous.getCpc())));
        current.setCpdcComparison(roundDouble(calculateChange(current.getCpdc(), previous.getCpdc())));
        current.setCrComparison(roundDouble(calculateChange(current.getCr(), previous.getCr())));
        current.setCtrComparison(roundDouble(calculateChange(current.getCtr(), previous.getCtr())));
        current.setDirectCrComparison(roundDouble(calculateChange(current.getDirectCr(), previous.getDirectCr())));
        current.setDirectCirComparison(roundDouble(calculateChange(current.getDirectCir(), previous.getDirectCir())));
        current.setDirectGmvComparison(roundDouble(calculateChange(current.getDirectGmv(), previous.getDirectGmv())));
        current.setDirectOrderComparison(roundDouble(calculateChange(current.getDirectOrder(), previous.getDirectOrder())));
        current.setDirectOrderAmountComparison(roundDouble(calculateChange(current.getDirectOrderAmount(), previous.getDirectOrderAmount())));
        current.setDirectRoiComparison(roundDouble(calculateChange(current.getDirectRoi(), previous.getDirectRoi())));
        current.setImpressionComparison(roundDouble(calculateChange(current.getImpression(), previous.getImpression())));
        current.setAvgRankComparison(roundDouble(calculateChange(current.getAvgRank(), previous.getAvgRank())));
        current.setProductClickComparison(roundDouble(calculateChange(current.getProductClick(), previous.getProductClick())));
        current.setProductImpressionComparison(roundDouble(calculateChange(current.getProductImpression(), previous.getProductImpression())));
        current.setProductCtrComparison(roundDouble(calculateChange(current.getProductCtr(), previous.getProductCtr())));
        current.setLocationInAdsComparison(roundDouble(calculateChange(current.getLocationInAds(), previous.getLocationInAds())));
        current.setReachComparison(roundDouble(calculateChange(current.getReach(), previous.getReach())));
        current.setPageViewsComparison(roundDouble(calculateChange(current.getPageViews(), previous.getPageViews())));
        current.setUniqueVisitorsComparison(roundDouble(calculateChange(current.getUniqueVisitors(), previous.getUniqueVisitors())));
        current.setViewComparison(roundDouble(calculateChange(current.getView(), previous.getView())));
        current.setCpmComparison(roundDouble(calculateChange(current.getCpm(), previous.getCpm())));
        current.setUniqueClickUserComparison(roundDouble(calculateChange(current.getUniqueClickUser(), previous.getUniqueClickUser())));
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
                        dto.getAcos(),
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

    private Double roundDouble(Double value) {
        if (value == null) return null;
        return Math.round(value * 100.0) / 100.0;
    }
}