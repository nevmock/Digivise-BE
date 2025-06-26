package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsNewestResponseDto;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
import org.nevmock.digivise.domain.model.KPI;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.port.in.ProductAdsService;
import org.nevmock.digivise.domain.port.out.KPIRepository;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.utils.MathKt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class ProductAdsServiceImpl implements ProductAdsService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private KPIRepository kpiRepository;

    @Override
    public Page<ProductAdsResponseWrapperDto> findByRangeAggTotal(
            String shopId,
            String biddingStrategy,
            LocalDateTime from1,
            LocalDateTime to1,
            LocalDateTime from2,
            LocalDateTime to2,
            Pageable pageable,
            String type,
            String state,
            String productPlacement,
            String salesClassification,
            String title,
            Long campaignId
    ) {
        Merchant merchant = merchantRepository
                .findByShopeeMerchantId(shopId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
        KPI kpi = kpiRepository
                .findByMerchantId(merchant.getId())
                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));


        Map<Long, Double> customRoasMap1 = getCustomRoasForPeriod(shopId, from1, to1);


        Map<Long, Double> customRoasMap2 = getCustomRoasForPeriod(shopId, from2, to2);

        List<ProductAdsResponseDto> period1DataList = getAggregatedDataByCampaignForRange(shopId, biddingStrategy, type, state, productPlacement, title, from1, to1, campaignId, kpi);

        Map<Long, ProductAdsResponseDto> period2DataMap = getAggregatedDataByCampaignForRange(shopId, biddingStrategy, type, state, productPlacement, title, from2, to2, campaignId, kpi)
                .stream()
                .collect(Collectors.toMap(ProductAdsResponseDto::getCampaignId, Function.identity()));

        if (period1DataList.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<ProductAdsResponseWrapperDto> resultList = period1DataList.stream().map(period1Data -> {
            ProductAdsResponseDto period2Data = period2DataMap.get(period1Data.getCampaignId());


            processCustomRoas(period1Data, customRoasMap1.get(period1Data.getCampaignId()), kpi);


            if (period2Data != null) {
                processCustomRoas(period2Data, customRoasMap2.get(period2Data.getCampaignId()), kpi);
            }

            populateComparisonFields(period1Data, period2Data);


            if (!period1Data.getHasCustomRoas()) {

                period1Data.setInsight(
                        MathKt.renderInsight(
                                MathKt.formulateRecommendation(
                                        period1Data.getCpc(),
                                        period1Data.getAcos(),
                                        period1Data.getClick(),
                                        kpi,
                                        null,
                                        null
                                )
                        )
                );
            }


            return ProductAdsResponseWrapperDto.builder()
                    .campaignId(period1Data.getCampaignId())
                    .from1(from1)
                    .to1(to1)
                    .from2(from2)
                    .to2(to2)
                    .data(Collections.singletonList(period1Data))
                    .build();
        }).collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), resultList.size());

        if (start > resultList.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, resultList.size());
        }

        return new PageImpl<>(resultList.subList(start, end), pageable, resultList.size());
    }

    private void processCustomRoas(ProductAdsResponseDto dto, Double customRoas, KPI kpi) {
        if (customRoas != null) {
            dto.setCalculatedRoas(customRoas);
            dto.setHasCustomRoas(true);
            dto.setRoas(MathKt.calculateRoas(customRoas, dto.getDirectRoi(), dto.getDailyBudget()));
            dto.setInsightRoas(MathKt.renderInsight(
                    MathKt.formulateRecommendation(
                            dto.getCpc(), dto.getAcos(), dto.getClick(),
                            kpi, dto.getRoas(), dto.getDailyBudget()
                    )
            ));
        } else {
            dto.setHasCustomRoas(false);
        }
    }

    private Map<Long, Double> getCustomRoasForPeriod(String shopId, LocalDateTime from, LocalDateTime to) {
        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        List<AggregationOperation> ops = new ArrayList<>();

        Criteria matchCriteria = Criteria.where("shop_id").is(shopId)
                .and("from").gte(fromTimestamp).lte(toTimestamp);

        ops.add(match(matchCriteria));
        ops.add(unwind("data.entry_list"));


        ops.add(match(Criteria.where("data.entry_list.custom_roas").exists(true)));

        ops.add(Aggregation.group("data.entry_list.campaign.campaign_id")
                .first("data.entry_list.custom_roas").as("customRoas")
        );

        ops.add(Aggregation.project()
                .and("_id").as("campaignId")
                .and("customRoas").as("customRoas")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                newAggregation(ops), "ProductAds", Document.class
        );

        Map<Long, Double> customRoasMap = new HashMap<>();
        for (Document doc : results.getMappedResults()) {
            Long campaignId = getLong(doc, "campaignId");
            Double customRoas = getDouble(doc, "customRoas");
            if (campaignId != null && customRoas != null) {
                customRoasMap.put(campaignId, customRoas);
            }
        }

        return customRoasMap;
    }

    private void populateComparisonFields(ProductAdsResponseDto currentData, ProductAdsResponseDto previousData) {
        currentData.setCostComparison(calculateComparison(currentData.getCost(), previousData != null ? previousData.getCost() : null));
        currentData.setCpcComparison(calculateComparison(currentData.getCpc(), previousData != null ? previousData.getCpc() : null));
        currentData.setAcosComparison(calculateComparison(currentData.getAcos(), previousData != null ? previousData.getAcos() : null));
        currentData.setCtrComparison(calculateComparison(currentData.getCtr(), previousData != null ? previousData.getCtr() : null));
        currentData.setImpressionComparison(calculateComparison(currentData.getImpression(), previousData != null ? previousData.getImpression() : null));
        currentData.setClickComparison(calculateComparison(currentData.getClick(), previousData != null ? previousData.getClick() : null));
        currentData.setBroadGmv(calculateComparison(currentData.getBroadGmv(), previousData != null ? previousData.getBroadGmv() : null));
        currentData.setRoasComparison(calculateComparison(currentData.getRoas(), previousData != null ? previousData.getRoas() : null));
        currentData.setCrComparison(calculateComparison(currentData.getCr(), previousData != null ? previousData.getCr() : null));
        currentData.setDirectOrderComparison(calculateComparison(currentData.getDirectOrder(), previousData != null ? previousData.getDirectOrder() : null));
        currentData.setDirectOrderAmountComparison(calculateComparison(currentData.getDirectOrderAmount(), previousData != null ? previousData.getDirectOrderAmount() : null));
        currentData.setDirectGmvComparison(calculateComparison(currentData.getDirectGmv(), previousData != null ? previousData.getDirectGmv() : null));
        currentData.setDirectRoiComparison(calculateComparison(currentData.getDirectRoi(), previousData != null ? previousData.getDirectRoi() : null));
        currentData.setDirectCirComparison(calculateComparison(currentData.getDirectCir(), previousData != null ? previousData.getDirectCir() : null));
        currentData.setDirectCrComparison(calculateComparison(currentData.getDirectCr(), previousData != null ? previousData.getDirectCr() : null));
    }

    private Double calculateComparison(Double currentValue, Double previousValue) {
        if (currentValue == null || previousValue == null) {
            return null;
        }
        if (previousValue == 0) {
            return (currentValue > 0) ? 1.0 : 0.0;
        }
        return (currentValue - previousValue) / previousValue;
    }

    private List<ProductAdsResponseDto> getAggregatedDataByCampaignForRange(
            String shopId, String biddingStrategy, String type, String state,
            String productPlacement, String title, LocalDateTime from, LocalDateTime to, Long campaignId, KPI kpi) {

        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        List<AggregationOperation> ops = new ArrayList<>();

        Criteria matchCriteria = Criteria.where("shop_id").is(shopId)
                .and("from").gte(fromTimestamp).lte(toTimestamp);
        ops.add(match(matchCriteria));
        ops.add(unwind("data.entry_list"));

        if (biddingStrategy != null && !biddingStrategy.trim().isEmpty()) {
            ops.add(match(Criteria.where("data.entry_list.manual_product_ads.bidding_strategy").is(biddingStrategy)));
        }
        if (type != null && !type.trim().isEmpty()) {
            ops.add(match(Criteria.where("data.entry_list.type").is(type)));
        }
        if (state != null && !state.trim().isEmpty()) {
            ops.add(match(Criteria.where("data.entry_list.state").is(state)));
        }
        if (productPlacement != null && !productPlacement.trim().isEmpty()) {
            ops.add(match(Criteria.where("data.entry_list.manual_product_ads.product_placement").is(productPlacement)));
        }
        if (title != null && !title.trim().isEmpty()) {
            ops.add(match(Criteria.where("data.entry_list.title").regex(title, "i")));
        }
        if (campaignId != null) {
            ops.add(match(Criteria.where("data.entry_list.campaign.campaign_id").is(campaignId)));
        }

        ops.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "from")));

        ops.add(Aggregation.group("data.entry_list.campaign.campaign_id")
                .sum("data.entry_list.report.cost").as("totalCost")
                .sum("data.entry_list.campaign.daily_budget").as("dailyBudget")
                .avg("data.entry_list.report.impression").as("totalImpression")
                .avg("data.entry_list.report.click").as("totalClick")
                .avg("data.entry_list.report.broad_gmv").as("totalBroadGmv")
                .sum("data.entry_list.report.direct_gmv").as("totalDirectGmv")
                .avg("data.entry_list.report.direct_order").as("totalDirectOrder")
                .avg("data.entry_list.report.direct_order_amount").as("totalDirectOrderAmount")
                .avg("data.entry_list.report.cpc").as("avgCpc")
                .avg("data.entry_list.report.broad_cir").as("avgAcos")
                .avg("data.entry_list.report.ctr").as("avgCtr")
                .avg("data.entry_list.report.direct_roi").as("avgDirectRoi")
                .avg("data.entry_list.report.direct_cir").as("avgDirectCir")
                .avg("data.entry_list.report.direct_cr").as("avgDirectCr")
                .avg("data.entry_list.report.broad_roi").as("avgRoas")
                .avg("data.entry_list.report.cr").as("avgCr")
                .first("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
                .first("data.entry_list.manual_product_ads.product_placement").as("productPlacement")
                .first("data.entry_list.type").as("type")
                .first("data.entry_list.state").as("state")
                .first("data.entry_list.title").as("title")
                .first("data.entry_list.image").as("image")
                .first("data.entry_list.custom_roas").as("customRoas")
        );

        ops.add(Aggregation.project()
                .and("_id").as("campaignId")
                .and("totalCost").divide(10.0).as("cost")
                .and("avgCpc").divide(100000.0).as("cpc")
                .and("avgAcos").as("acos")
                .and("avgCtr").as("ctr")
                .and("dailyBudget").divide(100000.0).as("dailyBudget")
                .and("totalImpression").as("impression")
                .and("totalClick").as("click")
                .and("data.entry_list.title").as("title")
                .and("totalDirectOrder").as("directOrder")
                .and("totalDirectOrderAmount").as("directOrderAmount")
                .and("totalDirectGmv").divide(100000.0).as("directGmv")
                .and("avgDirectRoi").as("directRoi")
                .and("avgDirectCir").as("directCir")
                .and("avgDirectCr").as("directCr")
                .and("avgRoas").as("roas")
                .and("avgCr").as("cr")
                .andInclude("biddingStrategy", "productPlacement", "type", "state", "image", "title", "customRoas")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                newAggregation(ops), "ProductAds", Document.class
        );

        return results.getMappedResults().stream()
                .map(this::mapDocumentToDto)
                .collect(Collectors.toList());
    }

    private ProductAdsResponseDto mapDocumentToDto(Document doc) {
        return ProductAdsResponseDto.builder()
                .campaignId(getLong(doc, "campaignId"))
                .shopeeFrom(getString(doc, "shopeeFrom"))
                .shopeeTo(getString(doc, "shopeeTo"))
                .cost(getDouble(doc, "cost"))
                .cpc(getDouble(doc, "cpc"))
                .acos(getDouble(doc, "acos"))
                .ctr(getDouble(doc, "ctr"))
                .impression(getDouble(doc, "impression"))
                .click(getDouble(doc, "click"))
                .broadGmv(getDouble(doc, "totalBroadGmv"))
                .directOrder(getDouble(doc, "directOrder"))
                .directOrderAmount(getDouble(doc, "directOrderAmount"))
                .directGmv(getDouble(doc, "directGmv"))
                .directRoi(getDouble(doc, "directRoi"))
                .directCir(getDouble(doc, "directCir"))
                .directCr(getDouble(doc, "directCr"))
                .roas(getDouble(doc, "roas"))
                .cr(getDouble(doc, "cr"))
                .biddingStrategy(getString(doc, "biddingStrategy"))
                .productPlacement(getString(doc, "productPlacement"))
                .type(getString(doc, "type"))
                .state(getString(doc, "state"))
                .dailyBudget(getDouble(doc, "dailyBudget"))
                .title(getString(doc, "title"))
                .hasCustomRoas(false)
                .image(getString(doc, "image"))
                .customRoas(getDouble(doc, "customRoas"))
                .build();
    }


    private Double getDouble(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return null;
    }

    private Long getLong(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return null;
    }

    private String getString(Document doc, String key) {
        Object v = doc.get(key);
        return v instanceof String ? (String) v : null;
    }

    @Override
    public boolean insertCustomRoas(String shopId, Long campaignId, Double customRoas, LocalDateTime from, LocalDateTime to) {
        try {
            long timestampFrom = from.atZone(ZoneId.systemDefault()).toEpochSecond();
            long timestampTo = to.atZone(ZoneId.systemDefault()).toEpochSecond();

            Query query = new Query();
            query.addCriteria(
                    Criteria.where("shop_id").is(shopId)
                            .and("from").gte(timestampFrom)
                            .and("to").lte(timestampTo)
                            .and("data.entry_list.campaign.campaign_id").is(campaignId)
            );

            Update update = new Update();
            update.set("data.entry_list.$.custom_roas", customRoas);
            update.set("data.entry_list.$.custom_roas_updated_at", LocalDateTime.now());

            var result = mongoTemplate.updateMulti(query, update, "ProductAds");

            System.out.println("Custom ROAS inserted/updated: " + result.toString());

            return result.getModifiedCount() > 0;

        } catch (Exception e) {
            System.err.println("Error inserting custom ROAS: " + e.getMessage());
            return false;
        }
    }

    @Override
    public ProductAdsNewestResponseDto findByCampaignId(Long campaignId) {
        // 1. Unwind array entry_list
        // 2. Filter berdasarkan campaign_id
        // 3. Sort descending by "from"
        // 4. Ambil 1 hasil
        // 5. Jadikan root entry_list
        Aggregation agg = newAggregation(
                unwind("data.entry_list"),
                match(Criteria.where("data.entry_list.campaign.campaign_id").is(campaignId)),
                sort(Sort.Direction.DESC, "from"),
                limit(1),
                replaceRoot("data.entry_list")
        );

        // Jalankan agregasi
        AggregationResults<Document> results = mongoTemplate.aggregate(
                agg,
                "ProductAds",       // nama koleksi
                Document.class      // tipe hasil (Dokumen BSON)
        );

        Document entry = results.getUniqueMappedResult();
        if (entry == null) {
            return null;
        }

        // --- Ekstrak field dari entry ---
        String title = entry.getString("title");

        // Campaign
        Document campaign = entry.get("campaign", Document.class);
        Long dailyBudget = null;
        Long startTime = null, endTime = null;
        if (campaign != null) {
            dailyBudget = getLong(campaign, "daily_budget");
            startTime   = getLong(campaign, "start_time");
            endTime     = getLong(campaign, "end_time");
        }

        // Manual product ads
        Document manual = entry.get("manual_product_ads", Document.class);
        String biddingType      = "Tidak ada";
        String productPlacement = "Tidak ada";
        if (manual != null) {
            if (manual.containsKey("bidding_strategy")) {
                biddingType = manual.getString("bidding_strategy");
            }
            if (manual.containsKey("product_placement")) {
                productPlacement = manual.getString("product_placement");
            }
        }

        // Tentukan periode
        String period = (startTime != null && endTime != null && endTime > 0)
                ? "Terbatas"
                : "Tidak Terbatas";

        // Bangun DTO hasil
        return ProductAdsNewestResponseDto.builder()
                .title(title)
                .dailyBudget(dailyBudget)
                .biddingType(biddingType)
                .productPlacement(productPlacement)
                .adsPeriod(period)
                .build();
    }
}