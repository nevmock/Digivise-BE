//package org.nevmock.digivise.application.service;
//
//import org.bson.Document;
//import org.nevmock.digivise.application.dto.product.ads.ProductAdsNewestResponseDto;
//import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
//import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
//import org.nevmock.digivise.domain.model.KPI;
//import org.nevmock.digivise.domain.model.Merchant;
//import org.nevmock.digivise.domain.port.in.ProductAdsService;
//import org.nevmock.digivise.domain.port.out.KPIRepository;
//import org.nevmock.digivise.domain.port.out.MerchantRepository;
//import org.nevmock.digivise.utils.MathKt;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.aggregation.Aggregation;
//import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
//import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext;
//import org.springframework.data.mongodb.core.aggregation.AggregationResults;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.mongodb.core.query.Update;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
//
//@Service
//public class ProductAdsServiceImpl implements ProductAdsService {
//
//    @Autowired
//    private MongoTemplate mongoTemplate;
//
//    @Autowired
//    private MerchantRepository merchantRepository;
//
//    @Autowired
//    private KPIRepository kpiRepository;
//
//    @Override
//    public Page<ProductAdsResponseWrapperDto> findByRangeAggTotal(
//            String shopId,
//            String biddingStrategy,
//            LocalDateTime from1,
//            LocalDateTime to1,
//            LocalDateTime from2,
//            LocalDateTime to2,
//            Pageable pageable,
//            String type,
//            String state,
//            String productPlacement,
//            String salesClassification,
//            String title,
//            Long campaignId
//    ) {
//        Merchant merchant = merchantRepository
//                .findByShopeeMerchantId(shopId)
//                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
//        KPI kpi = kpiRepository
//                .findByMerchantId(merchant.getId())
//                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));
//
//
//        Map<Long, Double> customRoasMap1 = getCustomRoasForPeriod(shopId, from1, to1);
//
//
//        Map<Long, Double> customRoasMap2 = getCustomRoasForPeriod(shopId, from2, to2);
//
//        List<ProductAdsResponseDto> period1DataList = getAggregatedDataByCampaignForRange(shopId, biddingStrategy, type, state, productPlacement, title, from1, to1, campaignId, kpi);
//
//        Map<Long, ProductAdsResponseDto> period2DataMap = getAggregatedDataByCampaignForRange(shopId, biddingStrategy, type, state, productPlacement, title, from2, to2, campaignId, kpi)
//                .stream()
//                .collect(Collectors.toMap(ProductAdsResponseDto::getCampaignId, Function.identity()));
//
//        if (period1DataList.isEmpty()) {
//            return new PageImpl<>(Collections.emptyList(), pageable, 0);
//        }
//
//        List<ProductAdsResponseWrapperDto> resultList = period1DataList.stream().map(period1Data -> {
//            ProductAdsResponseDto period2Data = period2DataMap.get(period1Data.getCampaignId());
//
//
//            processCustomRoas(period1Data, customRoasMap1.get(period1Data.getCampaignId()), kpi);
//
//
//            if (period2Data != null) {
//                processCustomRoas(period2Data, customRoasMap2.get(period2Data.getCampaignId()), kpi);
//            }
//
//            populateComparisonFields(period1Data, period2Data);
//
//
//            if (!period1Data.getHasCustomRoas()) {
//
//                period1Data.setInsight(
//                        MathKt.renderInsight(
//                                MathKt.formulateRecommendation(
//                                        period1Data.getCpc(),
//                                        period1Data.getAcos(),
//                                        period1Data.getClick(),
//                                        kpi,
//                                        null,
//                                        null
//                                )
//                        )
//                );
//            }
//
//
//            return ProductAdsResponseWrapperDto.builder()
//                    .campaignId(period1Data.getCampaignId())
//                    .from1(from1)
//                    .to1(to1)
//                    .from2(from2)
//                    .to2(to2)
//                    .data(Collections.singletonList(period1Data))
//                    .build();
//        }).collect(Collectors.toList());
//
//        int start = (int) pageable.getOffset();
//        int end = Math.min((start + pageable.getPageSize()), resultList.size());
//
//        if (start > resultList.size()) {
//            return new PageImpl<>(Collections.emptyList(), pageable, resultList.size());
//        }
//
//        return new PageImpl<>(resultList.subList(start, end), pageable, resultList.size());
//    }
//
//    private void processCustomRoas(ProductAdsResponseDto dto, Double customRoas, KPI kpi) {
//        if (customRoas != null) {
//            dto.setCalculatedRoas(customRoas);
//            dto.setHasCustomRoas(true);
//            dto.setRoas(MathKt.calculateRoas(customRoas, dto.getDirectRoi(), dto.getDailyBudget()));
//            dto.setInsightRoas(MathKt.renderInsight(
//                    MathKt.formulateRecommendation(
//                            dto.getCpc(), dto.getAcos(), dto.getClick(),
//                            kpi, dto.getRoas(), dto.getDailyBudget()
//                    )
//            ));
//        } else {
//            dto.setHasCustomRoas(false);
//        }
//    }
//
//    private Map<Long, Double> getCustomRoasForPeriod(String shopId, LocalDateTime from, LocalDateTime to) {
//        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
//        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();
//
//        List<AggregationOperation> ops = new ArrayList<>();
//
//        Criteria matchCriteria = Criteria.where("shop_id").is(shopId)
//                .and("from").gte(fromTimestamp).lte(toTimestamp);
//
//        ops.add(match(matchCriteria));
//        ops.add(unwind("data.entry_list"));
//
//
//        ops.add(match(Criteria.where("data.entry_list.custom_roas").exists(true)));
//
//        ops.add(Aggregation.group("data.entry_list.campaign.campaign_id")
//                .first("data.entry_list.custom_roas").as("customRoas")
//        );
//
//        ops.add(Aggregation.project()
//                .and("_id").as("campaignId")
//                .and("customRoas").as("customRoas")
//        );
//
//        AggregationResults<Document> results = mongoTemplate.aggregate(
//                newAggregation(ops), "ProductAds", Document.class
//        );
//
//        Map<Long, Double> customRoasMap = new HashMap<>();
//        for (Document doc : results.getMappedResults()) {
//            Long campaignId = getLong(doc, "campaignId");
//            Double customRoas = getDouble(doc, "customRoas");
//            if (campaignId != null && customRoas != null) {
//                customRoasMap.put(campaignId, customRoas);
//            }
//        }
//
//        return customRoasMap;
//    }
//
//    private void populateComparisonFields(ProductAdsResponseDto currentData, ProductAdsResponseDto previousData) {
//        currentData.setCostComparison(calculateComparison(currentData.getCost(), previousData != null ? previousData.getCost() : null));
//        currentData.setCpcComparison(calculateComparison(currentData.getCpc(), previousData != null ? previousData.getCpc() : null));
//        currentData.setAcosComparison(calculateComparison(currentData.getAcos(), previousData != null ? previousData.getAcos() : null));
//        currentData.setCtrComparison(calculateComparison(currentData.getCtr(), previousData != null ? previousData.getCtr() : null));
//        currentData.setImpressionComparison(calculateComparison(currentData.getImpression(), previousData != null ? previousData.getImpression() : null));
//        currentData.setClickComparison(calculateComparison(currentData.getClick(), previousData != null ? previousData.getClick() : null));
//        currentData.setBroadGmv(calculateComparison(currentData.getBroadGmv(), previousData != null ? previousData.getBroadGmv() : null));
//        currentData.setRoasComparison(calculateComparison(currentData.getRoas(), previousData != null ? previousData.getRoas() : null));
//        currentData.setCrComparison(calculateComparison(currentData.getCr(), previousData != null ? previousData.getCr() : null));
//        currentData.setDirectOrderComparison(calculateComparison(currentData.getDirectOrder(), previousData != null ? previousData.getDirectOrder() : null));
//        currentData.setDirectOrderAmountComparison(calculateComparison(currentData.getDirectOrderAmount(), previousData != null ? previousData.getDirectOrderAmount() : null));
//        currentData.setDirectGmvComparison(calculateComparison(currentData.getDirectGmv(), previousData != null ? previousData.getDirectGmv() : null));
//        currentData.setDirectRoiComparison(calculateComparison(currentData.getDirectRoi(), previousData != null ? previousData.getDirectRoi() : null));
//        currentData.setDirectCirComparison(calculateComparison(currentData.getDirectCir(), previousData != null ? previousData.getDirectCir() : null));
//        currentData.setDirectCrComparison(calculateComparison(currentData.getDirectCr(), previousData != null ? previousData.getDirectCr() : null));
//        currentData.setBroadGmvComparison(calculateComparison(currentData.getBroadGmv(), previousData != null ? previousData.getBroadGmv() : null));
//        currentData.setDailyBudgetComparison(calculateComparison(currentData.getDailyBudget(), previousData != null ? previousData.getDailyBudget() : null));
//    }
//
//    private Double calculateComparison(Double currentValue, Double previousValue) {
//        if (currentValue == null || previousValue == null) {
//            return null;
//        }
//        if (previousValue == 0) {
//            return (currentValue > 0) ? 1.0 : 0.0;
//        }
//        return (currentValue - previousValue) / previousValue;
//    }
//
//    private List<ProductAdsResponseDto> getAggregatedDataByCampaignForRange(
//            String shopId, String biddingStrategy, String type, String state,
//            String productPlacement, String title, LocalDateTime from, LocalDateTime to,
//            Long campaignId, KPI kpi) {
//
//        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
//        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();
//
//        // Step 1: Agregasi ProductStock untuk mendapatkan revenue per model per campaign
//        List<AggregationOperation> stockOps = new ArrayList<>();
//        stockOps.add(match(Criteria.where("shop_id").is(shopId)
//                .and("createdAt").gte(from).lte(to)));
//        stockOps.add(unwind("data"));
//        stockOps.add(unwind("data.model_list"));
//        stockOps.add(unwind("data.promotion_detail.ongoing_campaigns", "campaign"));
//
//        stockOps.add(project()
//                .and("data.model_list.id").as("modelId")
//                .and("data.model_list.statistics.sold_count").as("soldCount")
//                .and("data.model_list.price_detail.selling_price_max").as("sellingPriceMax")
//                .and("campaign.campaign_type").as("campaignType")
//                .and("campaign.campaign_id").as("campaignId")
//        );
//
//        stockOps.add(match(Criteria.where("campaignType").is(8)));
//
//        stockOps.add(project()
//                .and("campaignId").as("campaignId")
//                .and("modelId").as("modelId")
//                .andExpression("{$convert: {input: '$soldCount', to: 'int'}}").as("soldCount")
//                .andExpression("{$convert: {input: '$sellingPriceMax', to: 'double'}}").as("sellingPriceMax")
//                .andExpression("{$multiply: ['$sellingPriceMax', '$soldCount']}").as("revenue")
//        );
//
//        stockOps.add(group("campaignId", "modelId")
//                .sum("revenue").as("revenue")
//        );
//
//        AggregationResults<Document> stockResults = mongoTemplate.aggregate(
//                newAggregation(stockOps), "ProductStock", Document.class
//        );
//
//        Map<Long, Map<Long, Double>> campaignModelRevenue = new HashMap<>();
//        for (Document doc : stockResults) {
//            Document idDoc = doc.get("_id", Document.class);
//            if (idDoc != null) {
//                Long cId = getLong(idDoc, "campaignId");
//                Long modelId = getLong(idDoc, "modelId");
//                Double revenue = getDouble(doc, "revenue");
//
//                if (cId != null && modelId != null && revenue != null) {
//                    campaignModelRevenue
//                            .computeIfAbsent(cId, k -> new HashMap<>())
//                            .put(modelId, revenue);
//                }
//            }
//        }
//
//        List<AggregationOperation> adsOps = new ArrayList<>();
//        Criteria matchCriteria = Criteria.where("shop_id").is(shopId)
//                .and("from").gte(fromTimestamp).lte(toTimestamp);
//        adsOps.add(match(matchCriteria));
//        adsOps.add(unwind("data.entry_list"));
//
//        if (biddingStrategy != null && !biddingStrategy.trim().isEmpty()) {
//            adsOps.add(match(Criteria.where("data.entry_list.manual_product_ads.bidding_strategy").is(biddingStrategy)));
//        }
//        if (type != null && !type.trim().isEmpty()) {
//            adsOps.add(match(Criteria.where("data.entry_list.type").is(type)));
//        }
//        if (state != null && !state.trim().isEmpty()) {
//            adsOps.add(match(Criteria.where("data.entry_list.state").is(state)));
//        }
//        if (productPlacement != null && !productPlacement.trim().isEmpty()) {
//            adsOps.add(match(Criteria.where("data.entry_list.manual_product_ads.product_placement").is(productPlacement)));
//        }
//        if (title != null && !title.trim().isEmpty()) {
//            adsOps.add(match(Criteria.where("data.entry_list.title").regex(title, "i")));
//        }
//        if (campaignId != null) {
//            adsOps.add(match(Criteria.where("data.entry_list.campaign.campaign_id").is(campaignId)));
//        }
//
//        adsOps.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "from")));
//
//        // Group by campaign_id
//        adsOps.add(Aggregation.group("data.entry_list.campaign.campaign_id")
//                .sum("data.entry_list.report.cost").as("totalCost")
//                .avg("data.entry_list.campaign.daily_budget").as("dailyBudget")
//                .sum("data.entry_list.report.impression").as("totalImpression")
//                .sum("data.entry_list.report.click").as("totalClick")
//                .avg("data.entry_list.report.broad_gmv").as("totalBroadGmv")
//                .avg("data.entry_list.report.direct_gmv").as("totalDirectGmv")
//                .avg("data.entry_list.report.direct_order").as("totalDirectOrder")
//                .sum("data.entry_list.report.direct_order_amount").as("totalDirectOrderAmount")
//                .avg("data.entry_list.report.cpc").as("avgCpc")
//                .avg("data.entry_list.report.broad_cir").as("avgAcos")
//                .avg("data.entry_list.report.ctr").as("avgCtr")
//                .avg("data.entry_list.report.direct_roi").as("avgDirectRoi")
//                .avg("data.entry_list.report.direct_cir").as("avgDirectCir")
//                .avg("data.entry_list.report.direct_cr").as("avgDirectCr")
//                .avg("data.entry_list.report.broad_roi").as("avgRoas")
//                .avg("data.entry_list.report.cr").as("avgCr")
//                .avg("data.entry_list.report.broad_order").as("totalBroadOrder")
//                .sum("data.entry_list.report.broad_order_amount").as("totalBroadOrderAmount")
//                .sum("data.entry_list.report.cpdc").as("totalCpdc")
//                .first("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
//                .first("data.entry_list.manual_product_ads.product_placement").as("productPlacement")
//                .first("data.entry_list.type").as("type")
//                .first("data.entry_list.state").as("state")
//                .first("data.entry_list.title").as("title")
//                .first("data.entry_list.image").as("image")
//                .first("data.entry_list.custom_roas").as("customRoas")
//        );
//
//        adsOps.add(Aggregation.project()
//                .and("_id").as("campaignId")
//                .and("totalCost").divide(100000.0).as("cost")
//                .and("avgCpc").divide(100000.0).as("cpc")
//                .and("avgAcos").as("acos")
//                .and("avgCtr").as("ctr")
//                .and("dailyBudget").divide(100000.0).as("dailyBudget")
//                .and("totalImpression").as("impression")
//                .and("totalClick").as("click")
//                .and("totalDirectOrder").as("directOrder")
//                .and("totalDirectOrderAmount").as("directOrderAmount")
//                .and("totalDirectGmv").divide(100000.0).as("directGmv")
//                .and("avgDirectRoi").as("directRoi")
//                .and("avgDirectCir").as("directCir")
//                .and("avgDirectCr").as("directCr")
//                .and("avgRoas").as("roas")
//                .and("avgCr").as("cr")
//                .and("totalCpdc").divide(100000.0).as("cpdc")
//                .and("totalBroadGmv").divide(100000.0).as("broadGmv")
//                .andInclude("biddingStrategy", "productPlacement", "type", "state", "image", "title", "customRoas", "totalBroadOrder", "totalBroadOrderAmount")
//        );
//
//        AggregationResults<Document> adsResults = mongoTemplate.aggregate(
//                newAggregation(adsOps), "ProductAds", Document.class
//        );
//
//        // Step 3: Map hasil agregasi ProductAds dan tentukan salesClassification
//        List<ProductAdsResponseDto> result = new ArrayList<>();
//        for (Document doc : adsResults) {
//            ProductAdsResponseDto dto = mapDocumentToDto(doc);
//            Long cId = dto.getCampaignId();
//
//            // Ambil data revenue untuk campaign ini
//            Map<Long, Double> modelRevenues = campaignModelRevenue.get(cId);
//            String classification = determineSalesClassification(modelRevenues);
//            dto.setSalesClassification(classification);
//
//            result.add(dto);
//        }
//
//        return result;
//    }
//
//    private String determineSalesClassification(Map<Long, Double> modelRevenues) {
//        if (modelRevenues == null || modelRevenues.isEmpty()) {
//            return "No Data";
//        }
//
//        // Hitung total revenue untuk campaign
//        double totalRevenue = modelRevenues.values().stream()
//                .mapToDouble(Double::doubleValue)
//                .sum();
//
//        if (totalRevenue == 0) {
//            return "Slow Moving";
//        }
//
//        // Cari persentase kontribusi setiap model
//        boolean hasBestSeller = false;
//        boolean hasMiddleMoving = false;
//
//        for (Double revenue : modelRevenues.values()) {
//            double percentage = revenue / totalRevenue;
//            if (percentage >= 0.7) {
//                hasBestSeller = true;
//            } else if (percentage >= 0.2) {
//                hasMiddleMoving = true;
//            }
//        }
//
//        if (hasBestSeller) {
//            return "Best Seller";
//        } else if (hasMiddleMoving) {
//            return "Middle Moving";
//        } else {
//            return "Slow Moving";
//        }
//    }
//
//    private ProductAdsResponseDto mapDocumentToDto(Document doc) {
//        return ProductAdsResponseDto.builder()
//                .campaignId(getLong(doc, "campaignId"))
//                .shopeeFrom(getString(doc, "shopeeFrom"))
//                .shopeeTo(getString(doc, "shopeeTo"))
//                .cost(getDouble(doc, "cost"))
//                .cpc(getDouble(doc, "cpc"))
//                .acos(getDouble(doc, "acos"))
//                .ctr(getDouble(doc, "ctr"))
//                .impression(getDouble(doc, "impression"))
//                .click(getDouble(doc, "click"))
//                .broadGmv(getDouble(doc, "totalBroadGmv"))
//                .directOrder(getDouble(doc, "directOrder"))
//                .directOrderAmount(getDouble(doc, "directOrderAmount"))
//                .directGmv(getDouble(doc, "directGmv"))
//                .directRoi(getDouble(doc, "directRoi"))
//                .directCir(getDouble(doc, "directCir"))
//                .directCr(getDouble(doc, "directCr"))
//                .roas(getDouble(doc, "roas"))
//                .cr(getDouble(doc, "cr"))
//                .biddingStrategy(getString(doc, "biddingStrategy"))
//                .productPlacement(getString(doc, "productPlacement"))
//                .type(getString(doc, "type"))
//                .state(getString(doc, "state"))
//                .dailyBudget(getDouble(doc, "dailyBudget"))
//                .title(getString(doc, "title"))
//                .hasCustomRoas(false)
//                .image(getString(doc, "image"))
//                .customRoas(getDouble(doc, "customRoas"))
//                .broadOrder(getDouble(doc, "totalBroadOrder"))
//                .broadOrderAmount(getDouble(doc, "totalBroadOrderAmount"))
//                .cpdc(getDouble(doc, "totalCpdc"))
//                .build();
//    }
//
//
//    private Double getDouble(Document doc, String key) {
//        Object v = doc.get(key);
//        if (v instanceof Number) {
//            return ((Number) v).doubleValue();
//        }
//        return null;
//    }
//
//    private Long getLong(Document doc, String key) {
//        Object v = doc.get(key);
//        if (v instanceof Number) {
//            return ((Number) v).longValue();
//        }
//        return null;
//    }
//
//    private String getString(Document doc, String key) {
//        Object v = doc.get(key);
//        return v instanceof String ? (String) v : null;
//    }
//
//    @Override
//    public boolean insertCustomRoas(String shopId, Long campaignId, Double customRoas, LocalDateTime from, LocalDateTime to) {
//        try {
//            long timestampFrom = from.atZone(ZoneId.systemDefault()).toEpochSecond();
//            long timestampTo = to.atZone(ZoneId.systemDefault()).toEpochSecond();
//
//            Query query = new Query();
//            query.addCriteria(
//                    Criteria.where("shop_id").is(shopId)
//                            .and("from").gte(timestampFrom).lt(timestampTo)
//                            .and("data.entry_list.campaign.campaign_id").is(campaignId)
//            );
//
//            Update update = new Update();
//            update.set("data.entry_list.$.custom_roas", customRoas);
//            update.set("data.entry_list.$.custom_roas_updated_at", LocalDateTime.now());
//
//            var result = mongoTemplate.updateMulti(query, update, "ProductAds");
//
//            System.out.println("Custom ROAS inserted/updated: " + result.toString());
//
//            return result.getModifiedCount() > 0;
//
//        } catch (Exception e) {
//            System.err.println("Error inserting custom ROAS: " + e.getMessage());
//            return false;
//        }
//    }
//
//    @Override
//    public ProductAdsNewestResponseDto findByCampaignId(Long campaignId) {
//        Aggregation agg = newAggregation(
//                unwind("data.entry_list"),
//                match(Criteria.where("data.entry_list.campaign.campaign_id").is(campaignId)),
//                sort(Sort.Direction.DESC, "from"),
//                limit(1),
//                replaceRoot("data.entry_list")
//        );
//
//        AggregationResults<Document> results = mongoTemplate.aggregate(
//                agg,
//                "ProductAds",
//                Document.class
//        );
//
//        Document entry = results.getUniqueMappedResult();
//        if (entry == null) {
//            return null;
//        }
//
//        String title = entry.getString("title");
//        String image = entry.getString("image");
//
//        Document campaign = entry.get("campaign", Document.class);
//        Long dailyBudget = null;
//        Long startTime = null, endTime = null;
//        if (campaign != null) {
//            dailyBudget = getLong(campaign, "daily_budget");
//            startTime   = getLong(campaign, "start_time");
//            endTime     = getLong(campaign, "end_time");
//        }
//
//        Document manual = entry.get("manual_product_ads", Document.class);
//        String biddingType      = "Tidak ada";
//        String productPlacement = "Tidak ada";
//        if (manual != null) {
//            if (manual.containsKey("bidding_strategy")) {
//                biddingType = manual.getString("bidding_strategy");
//            }
//            if (manual.containsKey("product_placement")) {
//                productPlacement = manual.getString("product_placement");
//            }
//        }
//
//        String period = (startTime != null && endTime != null && endTime > 0)
//                ? "Terbatas"
//                : "Tidak Terbatas";
//
//        return ProductAdsNewestResponseDto.builder()
//                .title(title)
//                .dailyBudget(dailyBudget)
//                .biddingType(biddingType)
//                .productPlacement(productPlacement)
//                .adsPeriod(period)
//                .image(image)
//                .build();
//    }
//}

//package org.nevmock.digivise.application.service;
//
//import org.bson.Document;
//import org.nevmock.digivise.application.dto.product.ads.ProductAdsNewestResponseDto;
//import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
//import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
//import org.nevmock.digivise.domain.model.KPI;
//import org.nevmock.digivise.domain.model.Merchant;
//import org.nevmock.digivise.domain.port.in.ProductAdsService;
//import org.nevmock.digivise.domain.port.out.KPIRepository;
//import org.nevmock.digivise.domain.port.out.MerchantRepository;
//import org.nevmock.digivise.utils.MathKt;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.aggregation.Aggregation;
//import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
//import org.springframework.data.mongodb.core.aggregation.AggregationResults;
//import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.Multiply;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.mongodb.core.query.Update;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
//
//@Service
//public class ProductAdsServiceImpl implements ProductAdsService {
//
//    @Autowired
//    private MongoTemplate mongoTemplate;
//
//    @Autowired
//    private MerchantRepository merchantRepository;
//
//    @Autowired
//    private KPIRepository kpiRepository;
//
//    @Override
//    public Page<ProductAdsResponseWrapperDto> findByRangeAggTotal(
//            String shopId,
//            String biddingStrategy,
//            LocalDateTime from1,
//            LocalDateTime to1,
//            LocalDateTime from2,
//            LocalDateTime to2,
//            Pageable pageable,
//            String type,
//            String state,
//            String productPlacement,
//            String salesClassification,
//            String title,
//            Long campaignId
//    ) {
//        Merchant merchant = merchantRepository
//                .findByShopeeMerchantId(shopId)
//                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
//        KPI kpi = kpiRepository
//                .findByMerchantId(merchant.getId())
//                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));
//
//        Map<Long, Double> customRoasMap1 = getCustomRoasForPeriod(shopId, from1, to1);
//        Map<Long, Double> customRoasMap2 = getCustomRoasForPeriod(shopId, from2, to2);
//
//        List<ProductAdsResponseDto> period1DataList = getAggregatedDataByCampaignForRange(
//                shopId, biddingStrategy, type, state, productPlacement, title, from1, to1, campaignId, kpi);
//
//        Map<Long, ProductAdsResponseDto> period2DataMap = getAggregatedDataByCampaignForRange(
//                shopId, biddingStrategy, type, state, productPlacement, title, from2, to2, campaignId, kpi)
//                .stream()
//                .collect(Collectors.toMap(ProductAdsResponseDto::getCampaignId, Function.identity()));
//
//        if (period1DataList.isEmpty()) {
//            return new PageImpl<>(Collections.emptyList(), pageable, 0);
//        }
//
//        List<ProductAdsResponseWrapperDto> resultList = period1DataList.stream().map(period1Data -> {
//            ProductAdsResponseDto period2Data = period2DataMap.get(period1Data.getCampaignId());
//
//            processCustomRoas(period1Data, customRoasMap1.get(period1Data.getCampaignId()), kpi);
//            if (period2Data != null) {
//                processCustomRoas(period2Data, customRoasMap2.get(period2Data.getCampaignId()), kpi);
//            }
//
//            populateComparisonFields(period1Data, period2Data);
//
//            if (!period1Data.getHasCustomRoas()) {
//                period1Data.setInsight(
//                        MathKt.renderInsight(
//                                MathKt.formulateRecommendation(
//                                        period1Data.getCpc(),
//                                        period1Data.getAcos(),
//                                        period1Data.getClick(),
//                                        kpi,
//                                        null,
//                                        null
//                                )
//                        )
//                );
//            }
//
//            return ProductAdsResponseWrapperDto.builder()
//                    .campaignId(period1Data.getCampaignId())
//                    .from1(from1)
//                    .to1(to1)
//                    .from2(from2)
//                    .to2(to2)
//                    .data(Collections.singletonList(period1Data))
//                    .build();
//        }).collect(Collectors.toList());
//
//        int start = (int) pageable.getOffset();
//        int end = Math.min((start + pageable.getPageSize()), resultList.size());
//        if (start > resultList.size()) {
//            return new PageImpl<>(Collections.emptyList(), pageable, resultList.size());
//        }
//        return new PageImpl<>(resultList.subList(start, end), pageable, resultList.size());
//    }
//
//    private void processCustomRoas(ProductAdsResponseDto dto, Double customRoas, KPI kpi) {
//        if (customRoas != null) {
//            dto.setCalculatedRoas(customRoas);
//            dto.setHasCustomRoas(true);
//            dto.setRoas(MathKt.calculateRoas(customRoas, dto.getDirectRoi(), dto.getDailyBudget()));
//            dto.setInsightRoas(MathKt.renderInsight(
//                    MathKt.formulateRecommendation(
//                            dto.getCpc(), dto.getAcos(), dto.getClick(),
//                            kpi, dto.getRoas(), dto.getDailyBudget()
//                    )
//            ));
//        } else {
//            dto.setHasCustomRoas(false);
//        }
//    }
//
//    private Map<Long, Double> getCustomRoasForPeriod(String shopId, LocalDateTime from, LocalDateTime to) {
//        long fromTs = from.atZone(ZoneId.systemDefault()).toEpochSecond();
//        long toTs   = to.atZone(ZoneId.systemDefault()).toEpochSecond();
//
//        List<AggregationOperation> ops = new ArrayList<>();
//        ops.add(match(Criteria.where("shop_id").is(shopId)
//                .and("from").gte(fromTs).lte(toTs)));
//        ops.add(unwind("data.entry_list"));
//        ops.add(match(Criteria.where("data.entry_list.custom_roas").exists(true)));
//        ops.add(group("data.entry_list.campaign.campaign_id")
//                .first("data.entry_list.custom_roas").as("customRoas")
//        );
//        ops.add(project()
//                .and("_id").as("campaignId")
//                .and("customRoas").as("customRoas")
//        );
//
//        AggregationResults<Document> results = mongoTemplate.aggregate(newAggregation(ops), "ProductAds", Document.class);
//        Map<Long, Double> map = new HashMap<>();
//        for (Document d : results) {
//            Long id = getLong(d, "campaignId");
//            Double cr = getDouble(d, "customRoas");
//            if (id != null && cr != null) map.put(id, cr);
//        }
//        return map;
//    }
//
//    private List<ProductAdsResponseDto> getAggregatedDataByCampaignForRange(
//            String shopId, String biddingStrategy, String type, String state,
//            String productPlacement, String title, LocalDateTime from, LocalDateTime to,
//            Long campaignId, KPI kpi) {
//
//        long fromTs = from.atZone(ZoneId.systemDefault()).toEpochSecond();
//        long toTs   = to.atZone(ZoneId.systemDefault()).toEpochSecond();
//
//        List<AggregationOperation> stockOps = new ArrayList<>();
//        stockOps.add(match(Criteria.where("shop_id").is(shopId)
//                .and("createdAt").gte(from).lte(to)));
//        stockOps.add(unwind("data"));
//
//        stockOps.add(project()
//                .and("data.statistics.sold_count").as("soldCount")
//                .and("data.price_detail.selling_price_max").as("sellingPriceMax")
//                .and("data.boost_info.campaign_type").as("campaignType")
//                .and("data.boost_info.campaign_id").as("campaignId")
//        );
//        stockOps.add(match(Criteria.where("campaignId").is(campaignId)));
//
//        stockOps.add(project()
//                .and("campaignId").as("campaignId")
//                .andExpression("{ $convert: { input: '$soldCount', to: 'int' } }").as("soldCount")
//                .andExpression("{ $convert: { input: '$sellingPriceMax', to: 'double' } }").as("sellingPriceMax")
//                .and(Multiply.valueOf("sellingPriceMax").multiplyBy("soldCount")).as("revenue")
//        );
//
//        stockOps.add(group("campaignId", "modelId").sum("revenue").as("revenue"));
//
//        AggregationResults<Document> stockResults = mongoTemplate.aggregate(newAggregation(stockOps), "ProductStock", Document.class);
//        Map<Long, Map<Long, Double>> campaignModelRevenue = new HashMap<>();
//        for (Document doc : stockResults) {
//            Document idDoc = doc.get("_id", Document.class);
//            Long cId = getLong(idDoc, "campaignId");
//            Long mId = getLong(idDoc, "modelId");
//            Double rev = getDouble(doc, "revenue");
//            if (cId != null && mId != null && rev != null) {
//                campaignModelRevenue.computeIfAbsent(cId, k -> new HashMap<>()).put(mId, rev);
//            }
//        }
//
//        List<AggregationOperation> adsOps = new ArrayList<>();
//        Criteria mCrit = Criteria.where("shop_id").is(shopId)
//                .and("from").gte(fromTs).lte(toTs);
//        adsOps.add(match(mCrit));
//        adsOps.add(unwind("data.entry_list"));
//        if (biddingStrategy != null && !biddingStrategy.isBlank()) {
//            adsOps.add(match(Criteria.where("data.entry_list.manual_product_ads.bidding_strategy").is(biddingStrategy)));
//        }
//        if (type != null && !type.isBlank()) {
//            adsOps.add(match(Criteria.where("data.entry_list.type").is(type)));
//        }
//        if (state != null && !state.isBlank()) {
//            adsOps.add(match(Criteria.where("data.entry_list.state").is(state)));
//        }
//        if (productPlacement != null && !productPlacement.isBlank()) {
//            adsOps.add(match(Criteria.where("data.entry_list.manual_product_ads.product_placement").is(productPlacement)));
//        }
//        if (title != null && !title.isBlank()) {
//            adsOps.add(match(Criteria.where("data.entry_list.title").regex(title, "i")));
//        }
//        if (campaignId != null) {
//            adsOps.add(match(Criteria.where("data.entry_list.campaign.campaign_id").is(campaignId)));
//        }
//        adsOps.add(sort(Sort.by(Sort.Direction.DESC, "from")));
//
//        adsOps.add(group("data.entry_list.campaign.campaign_id")
//                .sum("data.entry_list.report.cost").as("totalCost")
//                .avg("data.entry_list.campaign.daily_budget").as("dailyBudget")
//                .sum("data.entry_list.report.impression").as("totalImpression")
//                .sum("data.entry_list.report.click").as("totalClick")
//                .avg("data.entry_list.report.broad_gmv").as("totalBroadGmv")
//                .avg("data.entry_list.report.direct_gmv").as("totalDirectGmv")
//                .avg("data.entry_list.report.direct_order").as("totalDirectOrder")
//                .sum("data.entry_list.report.direct_order_amount").as("totalDirectOrderAmount")
//                .avg("data.entry_list.report.cpc").as("avgCpc")
//                .avg("data.entry_list.report.broad_cir").as("avgAcos")
//                .avg("data.entry_list.report.ctr").as("avgCtr")
//                .avg("data.entry_list.report.direct_roi").as("avgDirectRoi")
//                .avg("data.entry_list.report.direct_cir").as("avgDirectCir")
//                .avg("data.entry_list.report.direct_cr").as("avgDirectCr")
//                .avg("data.entry_list.report.broad_roi").as("avgRoas")
//                .avg("data.entry_list.report.cr").as("avgCr")
//                .avg("data.entry_list.report.broad_order").as("totalBroadOrder")
//                .sum("data.entry_list.report.broad_order_amount").as("totalBroadOrderAmount")
//                .sum("data.entry_list.report.cpdc").as("totalCpdc")
//                .first("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
//                .first("data.entry_list.manual_product_ads.product_placement").as("productPlacement")
//                .first("data.entry_list.type").as("type")
//                .first("data.entry_list.state").as("state")
//                .first("data.entry_list.title").as("title")
//                .first("data.entry_list.image").as("image")
//                .first("data.entry_list.custom_roas").as("customRoas")
//        );
//
//        adsOps.add(project()
//                .and("_id").as("campaignId")
//                .and("totalCost").divide(100000.0).as("cost")
//                .and("avgCpc").divide(100000.0).as("cpc")
//                .and("avgAcos").as("acos")
//                .and("avgCtr").as("ctr")
//                .and("dailyBudget").divide(100000.0).as("dailyBudget")
//                .and("totalImpression").as("impression")
//                .and("totalClick").as("click")
//                .and("totalDirectOrder").as("directOrder")
//                .and("totalDirectOrderAmount").as("directOrderAmount")
//                .and("totalDirectGmv").divide(100000.0).as("directGmv")
//                .and("avgDirectRoi").as("directRoi")
//                .and("avgDirectCir").as("directCir")
//                .and("avgDirectCr").as("directCr")
//                .and("avgRoas").as("roas")
//                .and("avgCr").as("cr")
//                .and("totalCpdc").divide(100000.0).as("cpdc")
//                .and("totalBroadGmv").divide(100000.0).as("broadGmv")
//                .andInclude("biddingStrategy", "productPlacement", "type", "state", "image", "title", "customRoas", "totalBroadOrder", "totalBroadOrderAmount")
//        );
//
//        AggregationResults<Document> adsResults = mongoTemplate.aggregate(newAggregation(adsOps), "ProductAds", Document.class);
//        List<ProductAdsResponseDto> result = new ArrayList<>();
//        for (Document doc : adsResults) {
//            ProductAdsResponseDto dto = mapDocumentToDto(doc);
//            Long cId = dto.getCampaignId();
//            Map<Long, Double> modelRevenues = campaignModelRevenue.get(cId);
//            dto.setSalesClassification(determineSalesClassification(modelRevenues));
//            result.add(dto);
//        }
//        return result;
//    }
//    private void populateComparisonFields(ProductAdsResponseDto currentData, ProductAdsResponseDto previousData) {
//        currentData.setCostComparison(calculateComparison(currentData.getCost(), previousData != null ? previousData.getCost() : null));
//        currentData.setCpcComparison(calculateComparison(currentData.getCpc(), previousData != null ? previousData.getCpc() : null));
//        currentData.setAcosComparison(calculateComparison(currentData.getAcos(), previousData != null ? previousData.getAcos() : null));
//        currentData.setCtrComparison(calculateComparison(currentData.getCtr(), previousData != null ? previousData.getCtr() : null));
//        currentData.setImpressionComparison(calculateComparison(currentData.getImpression(), previousData != null ? previousData.getImpression() : null));
//        currentData.setClickComparison(calculateComparison(currentData.getClick(), previousData != null ? previousData.getClick() : null));
//        currentData.setBroadGmv(calculateComparison(currentData.getBroadGmv(), previousData != null ? previousData.getBroadGmv() : null));
//        currentData.setRoasComparison(calculateComparison(currentData.getRoas(), previousData != null ? previousData.getRoas() : null));
//        currentData.setCrComparison(calculateComparison(currentData.getCr(), previousData != null ? previousData.getCr() : null));
//        currentData.setDirectOrderComparison(calculateComparison(currentData.getDirectOrder(), previousData != null ? previousData.getDirectOrder() : null));
//        currentData.setDirectOrderAmountComparison(calculateComparison(currentData.getDirectOrderAmount(), previousData != null ? previousData.getDirectOrderAmount() : null));
//        currentData.setDirectGmvComparison(calculateComparison(currentData.getDirectGmv(), previousData != null ? previousData.getDirectGmv() : null));
//        currentData.setDirectRoiComparison(calculateComparison(currentData.getDirectRoi(), previousData != null ? previousData.getDirectRoi() : null));
//        currentData.setDirectCirComparison(calculateComparison(currentData.getDirectCir(), previousData != null ? previousData.getDirectCir() : null));
//        currentData.setDirectCrComparison(calculateComparison(currentData.getDirectCr(), previousData != null ? previousData.getDirectCr() : null));
//        currentData.setBroadGmvComparison(calculateComparison(currentData.getBroadGmv(), previousData != null ? previousData.getBroadGmv() : null));
//        currentData.setDailyBudgetComparison(calculateComparison(currentData.getDailyBudget(), previousData != null ? previousData.getDailyBudget() : null));
//    }
//
//    private Double calculateComparison(Double currentValue, Double previousValue) {
//        if (currentValue == null || previousValue == null) {
//            return null;
//        }
//        if (previousValue == 0) {
//            return (currentValue > 0) ? 1.0 : 0.0;
//        }
//        return (currentValue - previousValue) / previousValue;
//    }
//
//    private String determineSalesClassification(Map<Long, Double> modelRevenues) {
//        if (modelRevenues == null || modelRevenues.isEmpty()) return "No Data";
//        double total = modelRevenues.values().stream().mapToDouble(Double::doubleValue).sum();
//        if (total == 0) return "Slow Moving";
//        boolean best = false, middle = false;
//        for (double rev : modelRevenues.values()) {
//            double pct = rev / total;
//            if (pct >= 0.7) best = true;
//            else if (pct >= 0.2) middle = true;
//        }
//        return best ? "Best Seller" : (middle ? "Middle Moving" : "Slow Moving");
//    }
//
//    private ProductAdsResponseDto mapDocumentToDto(Document doc) {
//        return ProductAdsResponseDto.builder()
//                .campaignId(getLong(doc, "campaignId"))
//                .shopeeFrom(getString(doc, "shopeeFrom"))
//                .shopeeTo(getString(doc, "shopeeTo"))
//                .cost(getDouble(doc, "cost"))
//                .cpc(getDouble(doc, "cpc"))
//                .acos(getDouble(doc, "acos"))
//                .ctr(getDouble(doc, "ctr"))
//                .impression(getDouble(doc, "impression"))
//                .click(getDouble(doc, "click"))
//                .broadGmv(getDouble(doc, "broadGmv"))
//                .directOrder(getDouble(doc, "directOrder"))
//                .directOrderAmount(getDouble(doc, "directOrderAmount"))
//                .directGmv(getDouble(doc, "directGmv"))
//                .directRoi(getDouble(doc, "directRoi"))
//                .directCir(getDouble(doc, "directCir"))
//                .directCr(getDouble(doc, "directCr"))
//                .roas(getDouble(doc, "roas"))
//                .cr(getDouble(doc, "cr"))
//                .biddingStrategy(getString(doc, "biddingStrategy"))
//                .productPlacement(getString(doc, "productPlacement"))
//                .type(getString(doc, "type"))
//                .state(getString(doc, "state"))
//                .dailyBudget(getDouble(doc, "dailyBudget"))
//                .title(getString(doc, "title"))
//                .hasCustomRoas(false)
//                .image(getString(doc, "image"))
//                .customRoas(getDouble(doc, "customRoas"))
//                .broadOrder(getDouble(doc, "totalBroadOrder"))
//                .broadOrderAmount(getDouble(doc, "totalBroadOrderAmount"))
//                .cpdc(getDouble(doc, "cpdc"))
//                .build();
//    }
//
//    private Double getDouble(Document doc, String key) {
//        Object v = doc.get(key);
//        return (v instanceof Number) ? ((Number) v).doubleValue() : null;
//    }
//
//    private Long getLong(Document doc, String key) {
//        Object v = doc.get(key);
//        return (v instanceof Number) ? ((Number) v).longValue() : null;
//    }
//
//    private String getString(Document doc, String key) {
//        Object v = doc.get(key);
//        return (v instanceof String) ? (String) v : null;
//    }
//
//    @Override
//    public boolean insertCustomRoas(String shopId, Long campaignId, Double customRoas, LocalDateTime from, LocalDateTime to) {
//        try {
//            long tsFrom = from.atZone(ZoneId.systemDefault()).toEpochSecond();
//            long tsTo   = to.atZone(ZoneId.systemDefault()).toEpochSecond();
//
//            Query q = new Query(Criteria.where("shop_id").is(shopId)
//                    .and("from").gte(tsFrom).lt(tsTo)
//                    .and("data.entry_list.campaign.campaign_id").is(campaignId)
//            );
//            Update u = new Update()
//                    .set("data.entry_list.$.custom_roas", customRoas)
//                    .set("data.entry_list.$.custom_roas_updated_at", LocalDateTime.now());
//
//            var res = mongoTemplate.updateMulti(q, u, "ProductAds");
//            return res.getModifiedCount() > 0;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    @Override
//    public ProductAdsNewestResponseDto findByCampaignId(Long campaignId) {
//        Aggregation agg = newAggregation(
//                unwind("data.entry_list"),
//                match(Criteria.where("data.entry_list.campaign.campaign_id").is(campaignId)),
//                sort(Sort.Direction.DESC, "from"),
//                limit(1),
//                replaceRoot("data.entry_list")
//        );
//        Document entry = mongoTemplate.aggregate(agg, "ProductAds", Document.class).getUniqueMappedResult();
//        if (entry == null) return null;
//        Long db = getLong(entry.get("campaign", Document.class), "daily_budget");
//        Long start = getLong(entry.get("campaign", Document.class), "start_time");
//        Long end   = getLong(entry.get("campaign", Document.class), "end_time");
//        Document manual = entry.get("manual_product_ads", Document.class);
//        String bid = "Tidak ada", placement = "Tidak ada";
//        if (manual != null) {
//            if (manual.containsKey("bidding_strategy")) bid = manual.getString("bidding_strategy");
//            if (manual.containsKey("product_placement")) placement = manual.getString("product_placement");
//        }
//        String period = (start != null && end != null && end > 0) ? "Terbatas" : "Tidak Terbatas";
//        return ProductAdsNewestResponseDto.builder()
//                .title(entry.getString("title"))
//                .dailyBudget(db)
//                .biddingType(bid)
//                .productPlacement(placement)
//                .adsPeriod(period)
//                .image(entry.getString("image"))
//                .build();
//    }
//}
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
import org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.Multiply;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
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

        // Get total revenue for sales classification calculation
        Map<Long, Double> totalRevenueMap = getTotalRevenueForPeriod(shopId, from1, to1);
        Map<Long, Map<Long, Double>> campaignProductRevenueMap = getCampaignProductRevenueForPeriod(shopId, from1, to1);

        List<ProductAdsResponseDto> period1DataList = getAggregatedDataByCampaignForRange(
                shopId, biddingStrategy, type, state, productPlacement, title, from1, to1, campaignId, kpi);

        Map<Long, ProductAdsResponseDto> period2DataMap = getAggregatedDataByCampaignForRange(
                shopId, biddingStrategy, type, state, productPlacement, title, from2, to2, campaignId, kpi)
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

            // Set sales classification using the corrected logic
            Long cId = period1Data.getCampaignId();
            Double totalRevenue = totalRevenueMap.get(cId);
            Map<Long, Double> productRevenues = campaignProductRevenueMap.get(cId);
            period1Data.setSalesClassification(determineSalesClassification(productRevenues, totalRevenue));

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

    /**
     * Get total revenue for each campaign in the specified period
     */
    private Map<Long, Double> getTotalRevenueForPeriod(String shopId, LocalDateTime from, LocalDateTime to) {
        List<AggregationOperation> ops = new ArrayList<>();

        // Initial filtering
        ops.add(match(Criteria.where("shop_id").is(shopId)
                .and("createdAt").gte(from).lte(to)));
        ops.add(unwind("data"));

        // First projection with type-safe conversions
        ops.add(project()
                .and("data.boost_info.campaign_id").as("campaignId")
                .and(
                        ConvertOperators.valueOf("data.statistics.sold_count")
                                .convertToInt()
                ).as("soldCount")
                .and(
                        ConvertOperators.valueOf("data.price_detail.selling_price_max")
                                .convertToDouble()

                ).as("sellingPriceMax")
        );

        // Filter valid campaigns
        ops.add(match(Criteria.where("campaignId").gt(0)));

        // Calculate revenue
        ops.add(project()
                .and("campaignId").as("campaignId")
                .and(Multiply.valueOf("sellingPriceMax").multiplyBy("soldCount")).as("revenue")
        );

        // Group by campaign
        ops.add(group("campaignId").sum("revenue").as("totalRevenue"));
        ops.add(project()
                .and("_id").as("campaignId")
                .and("totalRevenue").as("totalRevenue")
                .andExclude("_id")
        );
        // Execute aggregation
        AggregationResults<Document> results = mongoTemplate.aggregate(newAggregation(ops), "ProductStock", Document.class);

        // Process results
        Map<Long, Double> totalRevenueMap = new HashMap<>();
        for (Document doc : results) {
            Long campaignId = getLong(doc, "campaignId");
            Double totalRevenue = doc.getDouble("totalRevenue");
            if (campaignId != null && totalRevenue != null) {
                totalRevenueMap.put(campaignId, totalRevenue);
            }
        }

        return totalRevenueMap;
    }

    private Map<Long, Map<Long, Double>> getCampaignProductRevenueForPeriod(String shopId, LocalDateTime from, LocalDateTime to) {
        List<AggregationOperation> ops = new ArrayList<>();

        ops.add(match(Criteria.where("shop_id").is(shopId)
                .and("createdAt").gte(from).lte(to)));
        ops.add(unwind("data"));

        ops.add(project()
                .and("data.boost_info.campaign_id").as("campaignId")
                .and("data.id").as("productId")
                .and(
                        ConvertOperators.valueOf("data.statistics.sold_count")
                                .convertToInt()
                ).as("soldCount")
                .and(
                        ConvertOperators.valueOf("data.price_detail.selling_price_max")
                                .convertToDouble()
                ).as("sellingPriceMax")
        );

        ops.add(project()
                .and("campaignId").as("campaignId")
                .and("productId").as("productId")
                .and(Multiply.valueOf("sellingPriceMax").multiplyBy("soldCount")).as("revenue")
        );

        ops.add(group("campaignId", "productId").sum("revenue").as("productRevenue"));

        AggregationResults<Document> results = mongoTemplate.aggregate(newAggregation(ops), "ProductStock", Document.class);
        Map<Long, Map<Long, Double>> campaignProductRevenueMap = new HashMap<>();

        for (Document doc : results) {
            Document idDoc = doc.get("_id", Document.class);
            Long campaignId = getLong(idDoc, "campaignId");
            Long productId = getLong(idDoc, "productId");
            Double productRevenue = getDouble(doc, "productRevenue");

            if (campaignId != null && productRevenue != null) {
                campaignProductRevenueMap.computeIfAbsent(campaignId, k -> new HashMap<>())
                        .put(productId, productRevenue);
            }
        }
        return campaignProductRevenueMap;
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
        long fromTs = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTs   = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        List<AggregationOperation> ops = new ArrayList<>();
        ops.add(match(Criteria.where("shop_id").is(shopId)
                .and("from").gte(fromTs).lte(toTs)));
        ops.add(unwind("data.entry_list"));
        ops.add(match(Criteria.where("data.entry_list.custom_roas").exists(true)));
        ops.add(group("data.entry_list.campaign.campaign_id")
                .first("data.entry_list.custom_roas").as("customRoas")
        );
        ops.add(project()
                .and("_id").as("campaignId")
                .and("customRoas").as("customRoas")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(newAggregation(ops), "ProductAds", Document.class);
        Map<Long, Double> map = new HashMap<>();
        for (Document d : results) {
            Long id = getLong(d, "campaignId");
            Double cr = getDouble(d, "customRoas");
            if (id != null && cr != null) map.put(id, cr);
        }
        return map;
    }

    private List<ProductAdsResponseDto> getAggregatedDataByCampaignForRange(
            String shopId, String biddingStrategy, String type, String state,
            String productPlacement, String title, LocalDateTime from, LocalDateTime to,
            Long campaignId, KPI kpi) {

        long fromTs = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTs   = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        List<AggregationOperation> adsOps = new ArrayList<>();
        Criteria mCrit = Criteria.where("shop_id").is(shopId)
                .and("from").gte(fromTs).lte(toTs);
        adsOps.add(match(mCrit));
        adsOps.add(unwind("data.entry_list"));
        if (biddingStrategy != null && !biddingStrategy.isBlank()) {
            adsOps.add(match(Criteria.where("data.entry_list.manual_product_ads.bidding_strategy").is(biddingStrategy)));
        }
        if (type != null && !type.isBlank()) {
            adsOps.add(match(Criteria.where("data.entry_list.type").is(type)));
        }
        if (state != null && !state.isBlank()) {
            adsOps.add(match(Criteria.where("data.entry_list.state").is(state)));
        }
        if (productPlacement != null && !productPlacement.isBlank()) {
            adsOps.add(match(Criteria.where("data.entry_list.manual_product_ads.product_placement").is(productPlacement)));
        }
        if (title != null && !title.isBlank()) {
            adsOps.add(match(Criteria.where("data.entry_list.title").regex(title, "i")));
        }
        if (campaignId != null) {
            adsOps.add(match(Criteria.where("data.entry_list.campaign.campaign_id").is(campaignId)));
        }
        adsOps.add(sort(Sort.by(Sort.Direction.DESC, "from")));

        adsOps.add(group("data.entry_list.campaign.campaign_id")
                .sum("data.entry_list.report.cost").as("totalCost")
                .avg("data.entry_list.campaign.daily_budget").as("dailyBudget")
                .sum("data.entry_list.report.impression").as("totalImpression")
                .sum("data.entry_list.report.click").as("totalClick")
                .avg("data.entry_list.report.broad_gmv").as("totalBroadGmv")
                .avg("data.entry_list.report.direct_gmv").as("totalDirectGmv")
                .avg("data.entry_list.report.direct_order").as("totalDirectOrder")
                .sum("data.entry_list.report.direct_order_amount").as("totalDirectOrderAmount")
                .avg("data.entry_list.report.cpc").as("avgCpc")
                .avg("data.entry_list.report.broad_cir").as("avgAcos")
                .avg("data.entry_list.report.ctr").as("avgCtr")
                .avg("data.entry_list.report.direct_roi").as("avgDirectRoi")
                .avg("data.entry_list.report.direct_cir").as("avgDirectCir")
                .avg("data.entry_list.report.direct_cr").as("avgDirectCr")
                .avg("data.entry_list.report.broad_roi").as("avgRoas")
                .avg("data.entry_list.report.cr").as("avgCr")
                .avg("data.entry_list.report.broad_order").as("totalBroadOrder")
                .sum("data.entry_list.report.broad_order_amount").as("totalBroadOrderAmount")
                .sum("data.entry_list.report.cpdc").as("totalCpdc")
                .first("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
                .first("data.entry_list.manual_product_ads.product_placement").as("productPlacement")
                .first("data.entry_list.type").as("type")
                .first("data.entry_list.state").as("state")
                .first("data.entry_list.title").as("title")
                .first("data.entry_list.image").as("image")
                .first("data.entry_list.custom_roas").as("customRoas")
                .first("data.entry_list.campaign.start_time").as("campaignStartTime")
                .first("data.entry_list.campaign.end_time").as("campaignEndTime")
        );

        adsOps.add(project()
                .and("_id").as("campaignId")
                .and("totalCost").divide(100000.0).as("cost")
                .and("avgCpc").divide(100000.0).as("cpc")
                .and("avgAcos").as("acos")
                .and("avgCtr").as("ctr")
                .and("dailyBudget").divide(100000.0).as("dailyBudget")
                .and("totalImpression").as("impression")
                .and("totalClick").as("click")
                .and("totalDirectOrder").as("directOrder")
                .and("totalDirectOrderAmount").as("directOrderAmount")
                .and("totalDirectGmv").divide(100000.0).as("directGmv")
                .and("avgDirectRoi").as("directRoi")
                .and("avgDirectCir").as("directCir")
                .and("avgDirectCr").as("directCr")
                .and("avgRoas").as("roas")
                .and("avgCr").as("cr")
                .and("totalCpdc").divide(100000.0).as("cpdc")
                .and("totalBroadGmv").divide(100000.0).as("broadGmv")
                .andInclude("biddingStrategy", "productPlacement", "type", "state", "image", "title", "customRoas", "totalBroadOrder", "totalBroadOrderAmount", "campaignStartTime", "campaignEndTime")
        );

        AggregationResults<Document> adsResults = mongoTemplate.aggregate(newAggregation(adsOps), "ProductAds", Document.class);
        List<ProductAdsResponseDto> result = new ArrayList<>();
        for (Document doc : adsResults) {
            ProductAdsResponseDto dto = mapDocumentToDto(doc);
            result.add(dto);
        }
        return result;
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
        currentData.setBroadGmvComparison(calculateComparison(currentData.getBroadGmv(), previousData != null ? previousData.getBroadGmv() : null));
        currentData.setDailyBudgetComparison(calculateComparison(currentData.getDailyBudget(), previousData != null ? previousData.getDailyBudget() : null));
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

    private String determineSalesClassification(Map<Long, Double> productRevenues, Double totalRevenue) {
        if (productRevenues == null || productRevenues.isEmpty() || totalRevenue == null || totalRevenue <= 0) {
            return "No Data";
        }

        // Check each product's revenue percentage
        for (double productRevenue : productRevenues.values()) {
            double percentage = productRevenue / totalRevenue;

            if (percentage >= 0.70) { // 70% or more
                return "Best Seller";
            } else if (percentage >= 0.20) { // 20% or more but less than 70%
                return "Middle Moving";
            }
        }

        // If no product reaches 20%, it's slow moving
        return "Slow Moving";
    }

    private ProductAdsResponseDto mapDocumentToDto(Document doc) {
        Long campaignStartTime = getLong(doc, "campaignStartTime");
        Long campaignEndTime = getLong(doc, "campaignEndTime");

        String period = (campaignStartTime != null && campaignEndTime != null && campaignEndTime > 0) ? "Terbatas" : "Tidak Terbatas";

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
                .broadGmv(getDouble(doc, "broadGmv"))
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
                .broadOrder(getDouble(doc, "totalBroadOrder"))
                .broadOrderAmount(getDouble(doc, "totalBroadOrderAmount"))
                .cpdc(getDouble(doc, "cpdc"))
                .campaignStartTime(campaignStartTime)
                .campaignEndTime(campaignEndTime)
                .period(period)
                .build();
    }

    private Double getDouble(Document doc, String key) {
        Object v = doc.get(key);
        return (v instanceof Number) ? ((Number) v).doubleValue() : null;
    }

    private Long getLong(Document doc, String key) {
        Object v = doc.get(key);
        return (v instanceof Number) ? ((Number) v).longValue() : null;
    }

    private String getString(Document doc, String key) {
        Object v = doc.get(key);
        return (v instanceof String) ? (String) v : null;
    }

    @Override
    public boolean insertCustomRoas(String shopId, Long campaignId, Double customRoas, LocalDateTime from, LocalDateTime to) {
        try {
            long tsFrom = from.atZone(ZoneId.systemDefault()).toEpochSecond();
            long tsTo   = to.atZone(ZoneId.systemDefault()).toEpochSecond();

            Query q = new Query(Criteria.where("shop_id").is(shopId)
                    .and("from").gte(tsFrom).lt(tsTo)
                    .and("data.entry_list.campaign.campaign_id").is(campaignId)
            );
            Update u = new Update()
                    .set("data.entry_list.$.custom_roas", customRoas)
                    .set("data.entry_list.$.custom_roas_updated_at", LocalDateTime.now());

            var res = mongoTemplate.updateMulti(q, u, "ProductAds");
            return res.getModifiedCount() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public ProductAdsNewestResponseDto findByCampaignId(Long campaignId) {
        Aggregation agg = newAggregation(
                unwind("data.entry_list"),
                match(Criteria.where("data.entry_list.campaign.campaign_id").is(campaignId)),
                sort(Sort.Direction.DESC, "from"),
                limit(1),
                replaceRoot("data.entry_list")
        );
        Document entry = mongoTemplate.aggregate(agg, "ProductAds", Document.class).getUniqueMappedResult();
        if (entry == null) return null;
        Long db = getLong(entry.get("campaign", Document.class), "daily_budget");
        Long start = getLong(entry.get("campaign", Document.class), "start_time");
        Long end   = getLong(entry.get("campaign", Document.class), "end_time");
        Document manual = entry.get("manual_product_ads", Document.class);
        String bid = "Tidak ada", placement = "Tidak ada";
        if (manual != null) {
            if (manual.containsKey("bidding_strategy")) bid = manual.getString("bidding_strategy");
            if (manual.containsKey("product_placement")) placement = manual.getString("product_placement");
        }
        String period = (start != null && end != null && end > 0) ? "Terbatas" : "Tidak Terbatas";
        return ProductAdsNewestResponseDto.builder()
                .title(entry.getString("title"))
                .dailyBudget(db)
                .biddingType(bid)
                .productPlacement(placement)
                .adsPeriod(period)
                .image(entry.getString("image"))
                .build();
    }
}