//package org.nevmock.digivise.application.service;
//
//import org.bson.Document;
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
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.mongodb.core.query.Update;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.*;
//import java.util.function.Function;
//import java.util.stream.Collectors;
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
//        for (ProductAdsResponseWrapperDto wrapper : resultList) {
//            ProductAdsResponseDto dto = wrapper.getData().get(0);
//            String classification = calculateSalesClassification(shopId, dto.getCampaignId());
//            dto.setSalesClassification(classification);
//        }
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
//        ops.add(Aggregation.match(matchCriteria));
//        ops.add(Aggregation.unwind("data.entry_list"));
//
//
//        ops.add(Aggregation.match(Criteria.where("data.entry_list.custom_roas").exists(true)));
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
//                Aggregation.newAggregation(ops), "ProductAds", Document.class
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
//            String productPlacement, String title, LocalDateTime from, LocalDateTime to, Long campaignId, KPI kpi) {
//
//        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
//        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();
//
//        List<AggregationOperation> ops = new ArrayList<>();
//
//        Criteria matchCriteria = Criteria.where("shop_id").is(shopId)
//                .and("from").gte(fromTimestamp).lte(toTimestamp);
//        ops.add(Aggregation.match(matchCriteria));
//        ops.add(Aggregation.unwind("data.entry_list"));
//
//        if (biddingStrategy != null && !biddingStrategy.trim().isEmpty()) {
//            ops.add(Aggregation.match(Criteria.where("data.entry_list.manual_product_ads.bidding_strategy").is(biddingStrategy)));
//        }
//        if (type != null && !type.trim().isEmpty()) {
//            ops.add(Aggregation.match(Criteria.where("data.entry_list.type").is(type)));
//        }
//        if (state != null && !state.trim().isEmpty()) {
//            ops.add(Aggregation.match(Criteria.where("data.entry_list.state").is(state)));
//        }
//        if (productPlacement != null && !productPlacement.trim().isEmpty()) {
//            ops.add(Aggregation.match(Criteria.where("data.entry_list.manual_product_ads.product_placement").is(productPlacement)));
//        }
//        if (title != null && !title.trim().isEmpty()) {
//            ops.add(Aggregation.match(Criteria.where("data.entry_list.title").regex(title, "i")));
//        }
//        if (campaignId != null) {
//            ops.add(Aggregation.match(Criteria.where("data.entry_list.campaign.campaign_id").is(campaignId)));
//        }
//
//        ops.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "from")));
//
//        ops.add(Aggregation.group("data.entry_list.campaign.campaign_id")
//                .sum("data.entry_list.report.cost").as("totalCost")
//                .sum("data.entry_list.campaign.daily_budget").as("dailyBudget")
//                .avg("data.entry_list.report.impression").as("totalImpression")
//                .avg("data.entry_list.report.click").as("totalClick")
//                .avg("data.entry_list.report.broad_gmv").as("totalBroadGmv")
//                .sum("data.entry_list.report.direct_gmv").as("totalDirectGmv")
//                .avg("data.entry_list.report.direct_order").as("totalDirectOrder")
//                .avg("data.entry_list.report.direct_order_amount").as("totalDirectOrderAmount")
//                .avg("data.entry_list.report.cpc").as("avgCpc")
//                .avg("data.entry_list.report.broad_cir").as("avgAcos")
//                .avg("data.entry_list.report.ctr").as("avgCtr")
//                .avg("data.entry_list.report.direct_roi").as("avgDirectRoi")
//                .avg("data.entry_list.report.direct_cir").as("avgDirectCir")
//                .avg("data.entry_list.report.direct_cr").as("avgDirectCr")
//                .avg("data.entry_list.report.broad_roi").as("avgRoas")
//                .avg("data.entry_list.report.cr").as("avgCr")
//                .first("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
//                .first("data.entry_list.manual_product_ads.product_placement").as("productPlacement")
//                .first("data.entry_list.type").as("type")
//                .first("data.entry_list.state").as("state")
//                .first("data.entry_list.title").as("title")
//                .first("data.entry_list.image").as("image")
//        );
//
//        ops.add(Aggregation.project()
//                .and("_id").as("campaignId")
//                .and("totalCost").divide(10.0).as("cost")
//                .and("avgCpc").divide(100000.0).as("cpc")
//                .and("avgAcos").as("acos")
//                .and("avgCtr").as("ctr")
//                .and("dailyBudget").divide(100000.0).as("dailyBudget")
//                .and("totalImpression").as("impression")
//                .and("totalClick").as("click")
//                .and("data.entry_list.title").as("title")
//                .and("totalDirectOrder").as("directOrder")
//                .and("totalDirectOrderAmount").as("directOrderAmount")
//                .and("totalDirectGmv").divide(100000.0).as("directGmv")
//                .and("avgDirectRoi").as("directRoi")
//                .and("avgDirectCir").as("directCir")
//                .and("avgDirectCr").as("directCr")
//                .and("avgRoas").as("roas")
//                .and("avgCr").as("cr")
//                .andInclude("biddingStrategy", "productPlacement", "type", "state", "image", "title")
//        );
//
//        AggregationResults<Document> results = mongoTemplate.aggregate(
//                Aggregation.newAggregation(ops), "ProductAds", Document.class
//        );
//
//        return results.getMappedResults().stream()
//                .map(this::mapDocumentToDto)
//                .collect(Collectors.toList());
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
//    public boolean insertCustomRoas(String shopId, Long campaignId, Double customRoas, Long from, Long to) {
//        try {
//            Query query = new Query();
//            query.addCriteria(
//                    Criteria.where("shop_id").is(shopId)
//                            .and("from").is(from)
//                            .and("to").is(to)
//                            .and("data.entry_list.campaign.campaign_id").is(campaignId)
//            );
//
//            Update update = new Update();
//            update.set("data.entry_list.$.custom_roas", customRoas);
//            update.set("data.entry_list.$.custom_roas_updated_at", LocalDateTime.now());
//
//            var result = mongoTemplate.updateMulti(query, update, "ProductAds");
//
//            return result.getModifiedCount() > 0;
//
//        } catch (Exception e) {
//            System.err.println("Error inserting custom ROAS: " + e.getMessage());
//            return false;
//        }
//    }
//
//    private String calculateSalesClassification(String shopId, Long campaignId) {
//        // 1. Dapatkan product IDs dari campaign terbaru
//        List<Long> productIds = getProductIdsForCampaign(shopId, campaignId);
//        if (productIds.isEmpty()) return "Slow Moving";
//
//        // 2. Ambil data stok produk terbaru
//        List<ProductStockData> stockDataList = getLatestProductStockData(shopId, productIds);
//        if (stockDataList.isEmpty()) return "Slow Moving";
//
//        // 3. Hitung revenue per produk dan total revenue
//        double totalRevenue = 0;
//        List<Double> revenues = new ArrayList<>();
//        for (ProductStockData data : stockDataList) {
//            double revenue = data.sellingPriceMax * data.soldCount;
//            revenues.add(revenue);
//            totalRevenue += revenue;
//        }
//
//        // 4. Klasifikasi jika revenue = 0
//        if (totalRevenue == 0) return "Slow Moving";
//
//        // 5. Hitung kontribusi revenue per kategori
//        double revenueBest = 0, revenueMiddle = 0, revenueSlow = 0;
//        for (Double revenue : revenues) {
//            if (revenue >= 0.7 * totalRevenue) revenueBest += revenue;
//            else if (revenue >= 0.2 * totalRevenue) revenueMiddle += revenue;
//            else revenueSlow += revenue;
//        }
//
//        // 6. Tentukan klasifikasi campaign
//        if (revenueBest >= revenueMiddle && revenueBest >= revenueSlow) return "Best Seller";
//        else if (revenueMiddle >= revenueSlow) return "Middle Moving";
//        else return "Slow Moving";
//    }
//
//    // Helper class untuk data stok produk
//    private static class ProductStockData {
//        Long productId;
//        Double sellingPriceMax;
//        Integer soldCount;
//    }
//
//    private List<Long> getProductIdsForCampaign(String shopId, Long campaignId) {
//        Query query = new Query();
//        query.addCriteria(Criteria.where("shop_id").is(shopId)
//                .and("data.entry_list.campaign.campaign_id").is(campaignId));
//        query.with(Sort.by(Sort.Direction.DESC, "from")).limit(1);
//
//        Document doc = mongoTemplate.findOne(query, Document.class, "ProductAds");
//        if (doc == null) return Collections.emptyList();
//
//        Object dataField = doc.get("data");
//        if (dataField == null) return Collections.emptyList();
//
//        List<Long> productIds = new ArrayList<>();
//
//        try {
//            if (dataField instanceof Document) {
//                Document dataDoc = (Document) dataField;
//                List<Document> entryList = dataDoc.get("entry_list", List.class);
//                if (entryList != null) {
//                    entryList.forEach(entry -> {
//                        Document campaign = entry.get("campaign", Document.class);
//                        if (campaign != null && campaignId.equals(campaign.getLong("campaign_id"))) {
//                            Long productId = entry.getLong("product_id");
//                            if (productId != null) {
//                                productIds.add(productId);
//                            }
//                        }
//                    });
//                }
//            }
//            else if (dataField instanceof List) {
//                List<Document> dataList = (List<Document>) dataField;
//                for (Document dataItem : dataList) {
//                    List<Document> entryList = dataItem.get("entry_list", List.class);
//                    if (entryList != null) {
//                        entryList.forEach(entry -> {
//                            Document campaign = entry.get("campaign", Document.class);
//                            if (campaign != null && campaignId.equals(campaign.getLong("campaign_id"))) {
//                                Long productId = entry.getLong("product_id");
//                                if (productId != null) {
//                                    productIds.add(productId);
//                                }
//                            }
//                        });
//                    }
//                }
//            }
//        } catch (Exception e) {
//        }
//
//        return productIds;
//    }
//
//    private List<ProductStockData> getLatestProductStockData(String shopId, List<Long> productIds) {
//        Aggregation agg = Aggregation.newAggregation(
//                Aggregation.match(Criteria.where("shop_id").is(shopId)
//                        .and("data.id").in(productIds)),
//                Aggregation.sort(Sort.Direction.DESC, "createdAt"),
//                Aggregation.group("data.id").first("$$ROOT").as("latestDoc")
//        );
//
//        return mongoTemplate.aggregate(agg, "ProductStock", Document.class)
//                .getMappedResults().stream()
//                .map(doc -> {
//                    Document latestDoc = (Document) doc.get("latestDoc");
//                    Document product = latestDoc.get("data", Document.class);
//                    try {
//                        ProductStockData data = new ProductStockData();
//                        data.productId = product.getLong("id");
//
//                        // Access nested fields correctly
//                        Document statistics = product.get("statistics", Document.class);
//                        data.soldCount = statistics != null ? statistics.getInteger("sold_count") : 0;
//
//                        Document priceDetail = product.get("price_detail", Document.class);
//                        if (priceDetail != null) {
//                            String priceStr = priceDetail.getString("selling_price_max");
//                            data.sellingPriceMax = priceStr != null ? Double.parseDouble(priceStr) : 0.0;
//                        } else {
//                            data.sellingPriceMax = 0.0;
//                        }
//                        return data;
//                    } catch (Exception e) {
//                        return null;
//                    }
//                })
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }
//}
package org.nevmock.digivise.application.service;

import org.bson.Document;
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
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductAdsServiceImpl implements ProductAdsService {

    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private MerchantRepository merchantRepository;
    @Autowired private KPIRepository kpiRepository;

    @Override
    public Page<ProductAdsResponseWrapperDto> findByRangeAggTotal(
            String shopId, String biddingStrategy, LocalDateTime from1, LocalDateTime to1,
            LocalDateTime from2, LocalDateTime to2, Pageable pageable, String type, String state,
            String productPlacement, String salesClassification, String title, Long campaignId) {

        Merchant merchant = merchantRepository.findByShopeeMerchantId(shopId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
        KPI kpi = kpiRepository.findByMerchantId(merchant.getId())
                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));

        List<ProductAdsResponseDto> period1DataList = getAggregatedDataByCampaignForRange(
                shopId, biddingStrategy, type, state, productPlacement, title, from1, to1, campaignId, kpi);

        if (period1DataList.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Long> campaignIds = period1DataList.stream()
                .map(ProductAdsResponseDto::getCampaignId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Long, Double> customRoasMap1 = getCustomRoasForPeriodBatch(shopId, from1, to1, campaignIds);
        Map<Long, Double> customRoasMap2 = getCustomRoasForPeriodBatch(shopId, from2, to2, campaignIds);
        Map<Long, ProductAdsResponseDto> period2DataMap = getPeriod2DataBatch(
                shopId, biddingStrategy, type, state, productPlacement, title, from2, to2, campaignId, kpi, campaignIds);
        Map<Long, String> salesClassificationMap = calculateSalesClassificationBatch(shopId, campaignIds);

        List<ProductAdsResponseWrapperDto> resultList = new ArrayList<>();
        for (ProductAdsResponseDto period1Data : period1DataList) {
            ProductAdsResponseDto period2Data = period2DataMap.get(period1Data.getCampaignId());

            processCustomRoas(period1Data, customRoasMap1.get(period1Data.getCampaignId()), kpi);
            if (period2Data != null) {
                processCustomRoas(period2Data, customRoasMap2.get(period2Data.getCampaignId()), kpi);
            }

            populateComparisonFields(period1Data, period2Data);

            if (!period1Data.getHasCustomRoas()) {
                period1Data.setInsight(MathKt.renderInsight(
                        MathKt.formulateRecommendation(
                                period1Data.getCpc(),
                                period1Data.getAcos(),
                                period1Data.getClick(),
                                kpi, null, null)));
            }

            period1Data.setSalesClassification(salesClassificationMap.get(period1Data.getCampaignId()));

            resultList.add(ProductAdsResponseWrapperDto.builder()
                    .campaignId(period1Data.getCampaignId())
                    .from1(from1).to1(to1)
                    .from2(from2).to2(to2)
                    .data(Collections.singletonList(period1Data))
                    .build());
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), resultList.size());
        return new PageImpl<>(resultList.subList(start, end), pageable, resultList.size());
    }

    private List<ProductAdsResponseDto> getAggregatedDataByCampaignForRange(
            String shopId, String biddingStrategy, String type, String state,
            String productPlacement, String title, LocalDateTime from, LocalDateTime to,
            Long campaignId, KPI kpi) {

        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        List<AggregationOperation> ops = new ArrayList<>();
        ops.add(Aggregation.match(Criteria.where("shop_id").is(shopId).and("from").gte(fromTimestamp).lte(toTimestamp)));
        ops.add(Aggregation.unwind("data.entry_list"));

        if (biddingStrategy != null) ops.add(Aggregation.match(Criteria.where("data.entry_list.manual_product_ads.bidding_strategy").is(biddingStrategy)));
        if (type != null) ops.add(Aggregation.match(Criteria.where("data.entry_list.type").is(type)));
        if (state != null) ops.add(Aggregation.match(Criteria.where("data.entry_list.state").is(state)));
        if (productPlacement != null) ops.add(Aggregation.match(Criteria.where("data.entry_list.manual_product_ads.product_placement").is(productPlacement)));
        if (title != null) ops.add(Aggregation.match(Criteria.where("data.entry_list.title").regex(title, "i")));
        if (campaignId != null) ops.add(Aggregation.match(Criteria.where("data.entry_list.campaign.campaign_id").is(campaignId)));

        GroupOperation groupOp = Aggregation.group("data.entry_list.campaign.campaign_id")
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
                .first("data.entry_list.image").as("image");
        ops.add(groupOp);

        ProjectionOperation projectOp = Aggregation.project()
                .and("_id").as("campaignId")
                .and("totalCost").divide(10.0).as("cost")
                .and("avgCpc").divide(100000.0).as("cpc")
                .and("avgAcos").as("acos")
                .and("avgCtr").as("ctr")
                .and("dailyBudget").divide(100000.0).as("dailyBudget")
                .and("totalImpression").as("impression")
                .and("totalClick").as("click")
                .and("title").as("title")
                .and("totalDirectOrder").as("directOrder")
                .and("totalDirectOrderAmount").as("directOrderAmount")
                .and("totalDirectGmv").divide(100000.0).as("directGmv")
                .and("avgDirectRoi").as("directRoi")
                .and("avgDirectCir").as("directCir")
                .and("avgDirectCr").as("directCr")
                .and("avgRoas").as("roas")
                .and("avgCr").as("cr")
                .and("totalBroadGmv").as("broadGmv")
                .andInclude("biddingStrategy", "productPlacement", "type", "state", "image");
        ops.add(projectOp);

        Aggregation aggregation = Aggregation.newAggregation(ops);
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "ProductAds", Document.class);

        return results.getMappedResults().stream()
                .map(this::mapDocumentToDto)
                .collect(Collectors.toList());
    }

    private Map<Long, ProductAdsResponseDto> getPeriod2DataBatch(
            String shopId, String biddingStrategy, String type, String state,
            String productPlacement, String title, LocalDateTime from, LocalDateTime to,
            Long campaignId, KPI kpi, List<Long> campaignIds) {

        if (campaignIds.isEmpty()) return Collections.emptyMap();
        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        List<AggregationOperation> ops = new ArrayList<>();
        ops.add(Aggregation.match(Criteria.where("shop_id").is(shopId)
                .and("from").gte(fromTimestamp).lte(toTimestamp)
                .and("data.entry_list.campaign.campaign_id").in(campaignIds)));
        ops.add(Aggregation.unwind("data.entry_list"));
        ops.add(Aggregation.match(Criteria.where("data.entry_list.campaign.campaign_id").in(campaignIds)));

        GroupOperation groupOp = Aggregation.group("data.entry_list.campaign.campaign_id")
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
                .first("data.entry_list.image").as("image");
        ops.add(groupOp);

        ProjectionOperation projectOp = Aggregation.project()
                .and("_id").as("campaignId")
                .and("totalCost").divide(10.0).as("cost")
                .and("avgCpc").divide(100000.0).as("cpc")
                .and("avgAcos").as("acos")
                .and("avgCtr").as("ctr")
                .and("dailyBudget").divide(100000.0).as("dailyBudget")
                .and("totalImpression").as("impression")
                .and("totalClick").as("click")
                .and("title").as("title")
                .and("totalDirectOrder").as("directOrder")
                .and("totalDirectOrderAmount").as("directOrderAmount")
                .and("totalDirectGmv").divide(100000.0).as("directGmv")
                .and("avgDirectRoi").as("directRoi")
                .and("avgDirectCir").as("directCir")
                .and("avgDirectCr").as("directCr")
                .and("avgRoas").as("roas")
                .and("avgCr").as("cr")
                .and("totalBroadGmv").as("broadGmv")
                .andInclude("biddingStrategy", "productPlacement", "type", "state", "image");
        ops.add(projectOp);

        Aggregation aggregation = Aggregation.newAggregation(ops);
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "ProductAds", Document.class);

        return results.getMappedResults().stream()
                .map(this::mapDocumentToDto)
                .filter(dto -> dto.getCampaignId() != null)
                .collect(Collectors.toMap(ProductAdsResponseDto::getCampaignId, Function.identity()));
    }

    private Map<Long, Double> getCustomRoasForPeriodBatch(String shopId, LocalDateTime from, LocalDateTime to, List<Long> campaignIds) {
        if (campaignIds.isEmpty()) return Collections.emptyMap();
        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        List<AggregationOperation> ops = new ArrayList<>();
        ops.add(Aggregation.match(Criteria.where("shop_id").is(shopId)
                .and("from").gte(fromTimestamp).lte(toTimestamp)
                .and("data.entry_list.campaign.campaign_id").in(campaignIds)));
        ops.add(Aggregation.unwind("data.entry_list"));
        ops.add(Aggregation.match(Criteria.where("data.entry_list.custom_roas").exists(true)));
        ops.add(Aggregation.group("data.entry_list.campaign.campaign_id").first("data.entry_list.custom_roas").as("customRoas"));
        ops.add(Aggregation.project().and("_id").as("campaignId").and("customRoas").as("customRoas"));

        AggregationResults<Document> results = mongoTemplate.aggregate(Aggregation.newAggregation(ops), "ProductAds", Document.class);
        return results.getMappedResults().stream()
                .filter(doc -> doc.getLong("campaignId") != null)
                .collect(Collectors.toMap(
                        doc -> doc.getLong("campaignId"),
                        doc -> doc.getDouble("customRoas"),
                        (existing, replacement) -> existing));
    }

    private Map<Long, String> calculateSalesClassificationBatch(String shopId, List<Long> campaignIds) {
        Map<Long, String> result = new HashMap<>();
        if (campaignIds.isEmpty()) return result;
        Map<Long, List<Long>> campaignProducts = new HashMap<>();
        for (Long campaignId : campaignIds) {
            campaignProducts.put(campaignId, getProductIdsForCampaign(shopId, campaignId));
        }
        List<Long> allProductIds = campaignProducts.values().stream().flatMap(List::stream).distinct().collect(Collectors.toList());
        Map<Long, ProductStockData> stockDataMap = getLatestProductStockData(shopId, allProductIds).stream()
                .collect(Collectors.toMap(d -> d.productId, Function.identity()));

        for (Map.Entry<Long, List<Long>> entry : campaignProducts.entrySet()) {
            Long campaignId = entry.getKey();
            List<Long> productIds = entry.getValue();
            if (productIds.isEmpty()) {
                result.put(campaignId, "Slow Moving");
                continue;
            }
            double totalRevenue = 0;
            List<Double> revenues = new ArrayList<>();
            for (Long productId : productIds) {
                ProductStockData data = stockDataMap.get(productId);
                if (data == null) continue;
                double revenue = data.sellingPriceMax * data.soldCount;
                revenues.add(revenue);
                totalRevenue += revenue;
            }
            if (totalRevenue == 0) {
                result.put(campaignId, "Slow Moving");
                continue;
            }
            double revenueBest = 0, revenueMiddle = 0, revenueSlow = 0;
            for (Double revenue : revenues) {
                double percentage = revenue / totalRevenue;
                if (percentage >= 0.7) revenueBest += revenue;
                else if (percentage >= 0.2) revenueMiddle += revenue;
                else revenueSlow += revenue;
            }
            if (revenueBest >= revenueMiddle && revenueBest >= revenueSlow) {
                result.put(campaignId, "Best Seller");
            } else if (revenueMiddle >= revenueSlow) {
                result.put(campaignId, "Middle Moving");
            } else {
                result.put(campaignId, "Slow Moving");
            }
        }
        return result;
    }

    private void processCustomRoas(ProductAdsResponseDto dto, Double customRoas, KPI kpi) {
        if (customRoas != null) {
            dto.setCalculatedRoas(customRoas);
            dto.setHasCustomRoas(true);
            dto.setRoas(MathKt.calculateRoas(customRoas, dto.getDirectRoi(), dto.getDailyBudget()));
            dto.setInsightRoas(MathKt.renderInsight(
                    MathKt.formulateRecommendation(
                            dto.getCpc(), dto.getAcos(), dto.getClick(),
                            kpi, dto.getRoas(), dto.getDailyBudget())));
        } else {
            dto.setHasCustomRoas(false);
        }
    }

    private void populateComparisonFields(ProductAdsResponseDto current, ProductAdsResponseDto previous) {
        current.setCostComparison(calculateComparison(current.getCost(), previous != null ? previous.getCost() : null));
        current.setCpcComparison(calculateComparison(current.getCpc(), previous != null ? previous.getCpc() : null));
        current.setAcosComparison(calculateComparison(current.getAcos(), previous != null ? previous.getAcos() : null));
        current.setCtrComparison(calculateComparison(current.getCtr(), previous != null ? previous.getCtr() : null));
        current.setImpressionComparison(calculateComparison(current.getImpression(), previous != null ? previous.getImpression() : null));
        current.setClickComparison(calculateComparison(current.getClick(), previous != null ? previous.getClick() : null));
        current.setBroadGmv(calculateComparison(current.getBroadGmv(), previous != null ? previous.getBroadGmv() : null));
        current.setRoasComparison(calculateComparison(current.getRoas(), previous != null ? previous.getRoas() : null));
        current.setCrComparison(calculateComparison(current.getCr(), previous != null ? previous.getCr() : null));
        current.setDirectOrderComparison(calculateComparison(current.getDirectOrder(), previous != null ? previous.getDirectOrder() : null));
        current.setDirectOrderAmountComparison(calculateComparison(current.getDirectOrderAmount(), previous != null ? previous.getDirectOrderAmount() : null));
        current.setDirectGmvComparison(calculateComparison(current.getDirectGmv(), previous != null ? previous.getDirectGmv() : null));
        current.setDirectRoiComparison(calculateComparison(current.getDirectRoi(), previous != null ? previous.getDirectRoi() : null));
        current.setDirectCirComparison(calculateComparison(current.getDirectCir(), previous != null ? previous.getDirectCir() : null));
        current.setDirectCrComparison(calculateComparison(current.getDirectCr(), previous != null ? previous.getDirectCr() : null));
    }

    private Double calculateComparison(Double current, Double previous) {
        if (current == null || previous == null || previous == 0) return null;
        return (current - previous) / previous;
    }

    private ProductAdsResponseDto mapDocumentToDto(Document doc) {
        return ProductAdsResponseDto.builder()
                .campaignId(getLong(doc, "campaignId"))
                .cost(getDouble(doc, "cost"))
                .cpc(getDouble(doc, "cpc"))
                .acos(getDouble(doc, "acos"))
                .ctr(getDouble(doc, "ctr"))
                .dailyBudget(getDouble(doc, "dailyBudget"))
                .impression(getDouble(doc, "impression"))
                .click(getDouble(doc, "click"))
                .title(getString(doc, "title"))
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
                .image(getString(doc, "image"))
                .broadGmv(getDouble(doc, "broadGmv"))
                .hasCustomRoas(false)
                .build();
    }

    private List<Long> getProductIdsForCampaign(String shopId, Long campaignId) {
        Query query = new Query(Criteria.where("shop_id").is(shopId)
                .and("data.entry_list.campaign.campaign_id").is(campaignId));
        query.fields().include("data.entry_list.$");
        query.with(Sort.by(Sort.Direction.DESC, "from")).limit(1);
        Document doc = mongoTemplate.findOne(query, Document.class, "ProductAds");
        if (doc == null) return Collections.emptyList();
        List<Long> productIds = new ArrayList<>();
        List<Document> entries = doc.get("data", Document.class).get("entry_list", List.class);
        for (Document entry : entries) {
            Long productId = entry.getLong("product_id");
            if (productId != null) productIds.add(productId);
        }
        return productIds;
    }

    private List<ProductStockData> getLatestProductStockData(String shopId, List<Long> productIds) {
        if (productIds.isEmpty()) return Collections.emptyList();
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("shop_id").is(shopId).and("data.id").in(productIds)),
                Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt")),
                Aggregation.group("data.id").first("$$ROOT").as("latestDoc"));
        return mongoTemplate.aggregate(agg, "ProductStock", Document.class)
                .getMappedResults().stream()
                .map(doc -> {
                    Document product = ((Document) doc.get("latestDoc")).get("data", Document.class);
                    ProductStockData data = new ProductStockData();
                    data.productId = product.getLong("id");
                    Document statistics = product.get("statistics", Document.class);
                    data.soldCount = statistics != null ? statistics.getInteger("sold_count") : 0;
                    Document priceDetail = product.get("price_detail", Document.class);
                    if (priceDetail != null) {
                        String priceStr = priceDetail.getString("selling_price_max");
                        data.sellingPriceMax = priceStr != null ? Double.parseDouble(priceStr) : 0.0;
                    } else data.sellingPriceMax = 0.0;
                    return data;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public boolean insertCustomRoas(String shopId, Long campaignId, Double customRoas, Long from, Long to) {
        try {
            Query query = new Query(Criteria.where("shop_id").is(shopId)
                    .and("from").is(from).and("to").is(to)
                    .and("data.entry_list.campaign.campaign_id").is(campaignId));
            Update update = new Update()
                    .set("data.entry_list.$.custom_roas", customRoas)
                    .set("data.entry_list.$.custom_roas_updated_at", LocalDateTime.now());
            return mongoTemplate.updateMulti(query, update, "ProductAds").getModifiedCount() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String getString(Document d, String key) {
        Object v = d.get(key);
        return v instanceof String ? (String) v : null;
    }

    private Long getLong(Document d, String key) {
        Object v = d.get(key);
        return v instanceof Number ? ((Number) v).longValue() : null;
    }

    private Double getDouble(Document d, String key) {
        Object v = d.get(key);
        return v instanceof Number ? ((Number) v).doubleValue() : null;
    }

    private static class ProductStockData {
        Long productId;
        Double sellingPriceMax;
        Integer soldCount;
    }
}