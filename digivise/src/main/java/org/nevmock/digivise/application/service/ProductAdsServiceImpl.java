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
import java.util.*;
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

        Long totalRevenue = getTotalRevenueForPeriod(shopId, from1, to1);
        Map<Long, Map<Long, Long>> campaignProductRevenueMap = getCampaignProductRevenueForPeriod(shopId, from1, to1);

        List<ProductAdsResponseDto> period1DataList = getAggregatedDataByCampaignForRange(
                shopId, biddingStrategy, type, state, productPlacement, title, from1, to1, campaignId, kpi);

        Map<Long, ProductAdsResponseDto> period2DataMap = getAggregatedDataByCampaignForRange(
                shopId, biddingStrategy, type, state, productPlacement, title, from2, to2, campaignId, kpi)
                .stream()
                .collect(Collectors.toMap(ProductAdsResponseDto::getCampaignId, Function.identity()));

        if (period1DataList.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<ProductAdsResponseWrapperDto> resultList = period1DataList.stream()
                .map(period1Data -> {
                    ProductAdsResponseDto period2Data = period2DataMap.get(period1Data.getCampaignId());

                    processCustomRoas(period1Data, customRoasMap1.get(period1Data.getCampaignId()), kpi);
                    if (period2Data != null) {
                        processCustomRoas(period2Data, customRoasMap2.get(period2Data.getCampaignId()), kpi);
                    }

                    Long cId = period1Data.getCampaignId();
                    Map<Long, Long> productRevenues = campaignProductRevenueMap.get(cId);
                    String calculatedSalesClassification = determineSalesClassification(productRevenues, totalRevenue);
                    period1Data.setSalesClassification(calculatedSalesClassification);

                    populateComparisonFields(period1Data, period2Data);

                    return ProductAdsResponseWrapperDto.builder()
                            .campaignId(period1Data.getCampaignId())
                            .from1(from1)
                            .to1(to1)
                            .from2(from2)
                            .to2(to2)
                            .data(Collections.singletonList(period1Data))
                            .build();
                })
                .filter(wrapper -> {
                    if (salesClassification == null || salesClassification.isBlank()) {
                        return true; // Tidak ada filter, tampilkan semua
                    }

                    String actualSalesClassification = wrapper.getData().get(0).getSalesClassification();

                    // Case-insensitive comparison
                    return salesClassification.equalsIgnoreCase(actualSalesClassification);
                })
                .collect(Collectors.toList());

        // Pagination setelah filtering
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), resultList.size());
        if (start > resultList.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, resultList.size());
        }
        return new PageImpl<>(resultList.subList(start, end), pageable, resultList.size());
    }

    // ALTERNATIVE APPROACH: Separate filter method untuk reusability
    private List<ProductAdsResponseWrapperDto> applySalesClassificationFilter(
            List<ProductAdsResponseWrapperDto> dataList,
            String salesClassification) {

        if (salesClassification == null || salesClassification.isBlank()) {
            return dataList;
        }

        return dataList.stream()
                .filter(wrapper -> {
                    String actualSalesClassification = wrapper.getData().get(0).getSalesClassification();
                    return salesClassification.equalsIgnoreCase(actualSalesClassification);
                })
                .collect(Collectors.toList());
    }

    // ALTERNATIVE APPROACH: Multiple filter support
    private List<ProductAdsResponseWrapperDto> applyMultipleSalesClassificationFilter(
            List<ProductAdsResponseWrapperDto> dataList,
            List<String> salesClassifications) {

        if (salesClassifications == null || salesClassifications.isEmpty()) {
            return dataList;
        }

        Set<String> filterSet = salesClassifications.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return dataList.stream()
                .filter(wrapper -> {
                    String actualSalesClassification = wrapper.getData().get(0).getSalesClassification();
                    return filterSet.contains(actualSalesClassification.toLowerCase());
                })
                .collect(Collectors.toList());
    }

    // OPTIMIZED APPROACH: Early filtering dengan pre-calculation
    public Page<ProductAdsResponseWrapperDto> findByRangeAggTotalOptimized(
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
        // Get required data
        Merchant merchant = merchantRepository
                .findByShopeeMerchantId(shopId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
        KPI kpi = kpiRepository
                .findByMerchantId(merchant.getId())
                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));

        // Pre-calculate revenue data for classification
        Long totalRevenue = getTotalRevenueForPeriod(shopId, from1, to1);
        Map<Long, Map<Long, Long>> campaignProductRevenueMap = getCampaignProductRevenueForPeriod(shopId, from1, to1);

        // Pre-calculate sales classification for all campaigns
        Map<Long, String> salesClassificationMap = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Long>> entry : campaignProductRevenueMap.entrySet()) {
            Long campaignId2 = entry.getKey();
            Map<Long, Long> productRevenues = entry.getValue();
            String classification = determineSalesClassification(productRevenues, totalRevenue);
            salesClassificationMap.put(campaignId2, classification);
        }

        // Get data for both periods
        List<ProductAdsResponseDto> period1DataList = getAggregatedDataByCampaignForRange(
                shopId, biddingStrategy, type, state, productPlacement, title, from1, to1, campaignId, kpi);

        // Early filter berdasarkan sales classification jika diperlukan
        if (salesClassification != null && !salesClassification.isBlank()) {
            period1DataList = period1DataList.stream()
                    .filter(data -> {
                        String actualClassification = salesClassificationMap.get(data.getCampaignId());
                        return salesClassification.equalsIgnoreCase(actualClassification);
                    })
                    .collect(Collectors.toList());
        }

        if (period1DataList.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // Get period 2 data hanya untuk campaign yang sudah difilter
        Set<Long> filteredCampaignIds = period1DataList.stream()
                .map(ProductAdsResponseDto::getCampaignId)
                .collect(Collectors.toSet());

        Map<Long, ProductAdsResponseDto> period2DataMap = getAggregatedDataByCampaignForRange(
                shopId, biddingStrategy, type, state, productPlacement, title, from2, to2, campaignId, kpi)
                .stream()
                .filter(data -> filteredCampaignIds.contains(data.getCampaignId()))
                .collect(Collectors.toMap(ProductAdsResponseDto::getCampaignId, Function.identity()));

        // Process remaining data
        Map<Long, Double> customRoasMap1 = getCustomRoasForPeriod(shopId, from1, to1);
        Map<Long, Double> customRoasMap2 = getCustomRoasForPeriod(shopId, from2, to2);

        List<ProductAdsResponseWrapperDto> resultList = period1DataList.stream()
                .map(period1Data -> {
                    ProductAdsResponseDto period2Data = period2DataMap.get(period1Data.getCampaignId());

                    processCustomRoas(period1Data, customRoasMap1.get(period1Data.getCampaignId()), kpi);
                    if (period2Data != null) {
                        processCustomRoas(period2Data, customRoasMap2.get(period2Data.getCampaignId()), kpi);
                    }

                    // Set sales classification dari pre-calculated map
                    String calculatedSalesClassification = salesClassificationMap.get(period1Data.getCampaignId());
                    period1Data.setSalesClassification(calculatedSalesClassification);

                    populateComparisonFields(period1Data, period2Data);

                    return ProductAdsResponseWrapperDto.builder()
                            .campaignId(period1Data.getCampaignId())
                            .from1(from1)
                            .to1(to1)
                            .from2(from2)
                            .to2(to2)
                            .data(Collections.singletonList(period1Data))
                            .build();
                })
                .collect(Collectors.toList());

        // Pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), resultList.size());
        if (start > resultList.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, resultList.size());
        }
        return new PageImpl<>(resultList.subList(start, end), pageable, resultList.size());
    }
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
//
//        Long totalRevenue = getTotalRevenueForPeriod(shopId, from1, to1);
//        Map<Long, Map<Long, Long>> campaignProductRevenueMap = getCampaignProductRevenueForPeriod(shopId, from1, to1);
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
//            Long cId = period1Data.getCampaignId();
//            Map<Long, Long> productRevenues = campaignProductRevenueMap.get(cId);
//            period1Data.setSalesClassification(determineSalesClassification(productRevenues, totalRevenue));
//
//            populateComparisonFields(period1Data, period2Data);
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

    private Long getTotalRevenueForPeriod(String shopId, LocalDateTime from, LocalDateTime to) {
        List<AggregationOperation> ops = new ArrayList<>();

        ops.add(match(Criteria.where("shop_id").is(shopId)
                .and("createdAt").gte(from).lte(to)));
        ops.add(unwind("data"));

        ops.add(project()
                .and("data.boost_info.campaign_id").as("campaignId")
                .and(ConvertOperators.valueOf("data.statistics.sold_count")
                        .convertToInt()).as("soldCount")
                .and(ConvertOperators.valueOf("data.price_detail.selling_price_max")
                        .convertToDouble()).as("sellingPriceMax")
        );

        ops.add(match(Criteria.where("campaignId").gt(0)));

        ops.add(project()
                .and("campaignId").as("campaignId")
                .and(Multiply.valueOf("sellingPriceMax").multiplyBy("soldCount")).as("revenue")
        );

        ops.add(group("campaignId").sum("revenue").as("totalRevenue"));
        ops.add(project()
                .and("_id").as("campaignId")
                .and("totalRevenue").as("totalRevenue")
                .andExclude("_id")
        );

        AggregationResults<Document> results =
                mongoTemplate.aggregate(newAggregation(ops), "ProductStock", Document.class);

        Map<Long, Double> totalRevenueMap = new HashMap<>();
        for (Document doc : results) {
            Long campaignId = getLong(doc, "campaignId");
            Double totalRevenue = getDouble(doc, "totalRevenue");
            if (campaignId != null && totalRevenue != null) {
                totalRevenueMap.put(campaignId, totalRevenue);
            }
        }

        long grandTotal = totalRevenueMap.values()
                .stream()
                .mapToLong(d -> d != null ? d.longValue() : 0L)
                .sum();

        return grandTotal;
    }

    private Map<Long, Map<Long, Long>> getCampaignProductRevenueForPeriod(String shopId, LocalDateTime from, LocalDateTime to) {
        List<AggregationOperation> ops = new ArrayList<>();

        ops.add(match(Criteria.where("shop_id").is(shopId)
                .and("createdAt").gte(from).lte(to)));
        ops.add(unwind("data"));

        ops.add(project()
                .and("data.boost_info.campaign_id").as("campaignId")
                .and("data.id").as("productId")
                .and(ConvertOperators.valueOf("data.statistics.sold_count")
                        .convertToInt()).as("soldCount")
                .and(ConvertOperators.valueOf("data.price_detail.selling_price_max")
                        .convertToDouble()).as("sellingPriceMax")
        );

        ops.add(project()
                .and("campaignId").as("campaignId")
                .and("productId").as("productId")
                .and(Multiply.valueOf("sellingPriceMax").multiplyBy("soldCount")).as("revenue")
        );

        ops.add(group("campaignId", "productId").sum("revenue").as("productRevenue"));

        AggregationResults<Document> results = mongoTemplate.aggregate(newAggregation(ops), "ProductStock", Document.class);

        Map<Long, Map<Long, Long>> campaignProductRevenueMap = new HashMap<>();

        for (Document doc : results) {
            Document idDoc = doc.get("_id", Document.class);
            Long campaignId = getLong(idDoc, "campaignId");
            Long productId = getLong(idDoc, "productId");
            Double productRevenue = getDouble(doc, "productRevenue");

            if (campaignId != null && productId != null && productRevenue != null) {
                long roundedRevenue = productRevenue.longValue(); 
                campaignProductRevenueMap
                        .computeIfAbsent(campaignId, k -> new HashMap<>())
                        .put(productId, roundedRevenue);
            }
        }

        return campaignProductRevenueMap;
    }

    private void processCustomRoas(ProductAdsResponseDto dto, Double customRoas, KPI kpi) {
        if (customRoas != null) {
            dto.setCalculatedRoas(roundDouble(customRoas)); 
            dto.setHasCustomRoas(true);
            Double newRoas = MathKt.calculateRoas(customRoas, dto.getDirectRoi(), dto.getDailyBudget());
            dto.setRoas(roundDouble(newRoas)); 
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

        // Ini buat debugging
        //adsOps.add(match(Criteria.where("data.entry_list.report.cpc").gt(0)));


        adsOps.add(sort(Sort.by(Sort.Direction.DESC, "from")));

        adsOps.add(group("data.entry_list.campaign.campaign_id")
                .sum("data.entry_list.report.cost").as("totalCost")
                .avg("data.entry_list.campaign.daily_budget").as("dailyBudget")
                .sum("data.entry_list.report.impression").as("totalImpression")
                .sum("data.entry_list.report.click").as("totalClick")
                .sum("data.entry_list.report.broad_gmv").as("totalBroadGmv")
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
                .and("avgAcos").multiply(100).as("acos")
                .and("avgCtr").as("ctr")
                .and("dailyBudget").divide(100000.0).as("dailyBudget")
                .and("totalImpression").as("impression")
                .and("totalClick").as("click")
                .and("totalDirectOrder").as("directOrder")
                .and("totalDirectOrderAmount").as("directOrderAmount")
                .and("totalDirectGmv").divide(100000.0).as("directGmv")
                .and("avgDirectRoi").as("directRoi")
                .and("avgDirectCir").multiply(100).as("directCir")
                .and("avgDirectCr").multiply(100).as("directCr")
                .and("avgRoas").as("roas")
                .and("avgCr").as("cr")
                .and("totalCpdc").divide(100000.0).as("cpdc")
                .and("totalBroadGmv").as("broadGmv")
                .andInclude("biddingStrategy", "productPlacement", "type", "state", "image", "title", "customRoas", "totalBroadOrder", "totalBroadOrderAmount", "campaignStartTime", "campaignEndTime")
        );

        AggregationResults<Document> adsResults = mongoTemplate.aggregate(newAggregation(adsOps), "ProductAds", Document.class);
        List<ProductAdsResponseDto> result = new ArrayList<>();
        for (Document doc : adsResults) {
            ProductAdsResponseDto dto = mapDocumentToDto(doc, kpi);
            result.add(dto);
        }
        return result;
    }

    private void populateComparisonFields(ProductAdsResponseDto currentData, ProductAdsResponseDto previousData) {
        currentData.setCostComparison(roundDouble(calculateComparison(currentData.getCost(), previousData != null ? previousData.getCost() : null)));
        currentData.setCpcComparison(roundDouble(calculateComparison(currentData.getCpc(), previousData != null ? previousData.getCpc() : null)));
        currentData.setAcosComparison(roundDouble(calculateComparison(currentData.getAcos(), previousData != null ? previousData.getAcos() : null)));
        currentData.setCtrComparison(roundDouble(calculateComparison(currentData.getCtr(), previousData != null ? previousData.getCtr() : null)));
        currentData.setImpressionComparison(roundDouble(calculateComparison(currentData.getImpression(), previousData != null ? previousData.getImpression() : null)));
        currentData.setClickComparison(roundDouble(calculateComparison(currentData.getClick(), previousData != null ? previousData.getClick() : null)));
        currentData.setBroadGmv(roundDouble(calculateComparison(currentData.getBroadGmv(), previousData != null ? previousData.getBroadGmv() : null)));
        currentData.setRoasComparison(roundDouble(calculateComparison(currentData.getRoas(), previousData != null ? previousData.getRoas() : null)));
        currentData.setCrComparison(roundDouble(calculateComparison(currentData.getCr(), previousData != null ? previousData.getCr() : null)));
        currentData.setDirectOrderComparison(roundDouble(calculateComparison(currentData.getDirectOrder(), previousData != null ? previousData.getDirectOrder() : null)));
        currentData.setDirectOrderAmountComparison(roundDouble(calculateComparison(currentData.getDirectOrderAmount(), previousData != null ? previousData.getDirectOrderAmount() : null)));
        currentData.setDirectGmvComparison(roundDouble(calculateComparison(currentData.getDirectGmv(), previousData != null ? previousData.getDirectGmv() : null)));
        currentData.setDirectRoiComparison(roundDouble(calculateComparison(currentData.getDirectRoi(), previousData != null ? previousData.getDirectRoi() : null)));
        currentData.setDirectCirComparison(roundDouble(calculateComparison(currentData.getDirectCir(), previousData != null ? previousData.getDirectCir() : null)));
        currentData.setDirectCrComparison(roundDouble(calculateComparison(currentData.getDirectCr(), previousData != null ? previousData.getDirectCr() : null)));
        currentData.setBroadGmvComparison(roundDouble(calculateComparison(currentData.getBroadGmv(), previousData != null ? previousData.getBroadGmv() : null)));
        currentData.setDailyBudgetComparison(roundDouble(calculateComparison(currentData.getDailyBudget(), previousData != null ? previousData.getDailyBudget() : null)));
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

    private String determineSalesClassification(Map<Long, Long> productRevenues, Long totalRevenue) {
        if (productRevenues == null || productRevenues.isEmpty() || totalRevenue == null || totalRevenue <= 0) {
            return "No Data";
        }

        for (double productRevenue : productRevenues.values()) {
            double percentage = ((productRevenue) / totalRevenue);

            if (percentage >= 0.20) {
                return "Best Seller";
            } else if (percentage >= 0.10) {
                return "Middle Moving";
            }
        }

        return "Slow Moving";
    }

    private ProductAdsResponseDto mapDocumentToDto(Document doc, KPI kpi) {
        Long campaignStartTime = getLong(doc, "campaignStartTime");
        Long campaignEndTime = getLong(doc, "campaignEndTime");

        String period = (campaignStartTime != null && campaignEndTime != null && campaignEndTime > 0) ? "Terbatas" : "Tidak Terbatas";

        return ProductAdsResponseDto.builder()
                .campaignId(getLong(doc, "campaignId"))
                .shopeeFrom(getString(doc, "shopeeFrom"))
                .shopeeTo(getString(doc, "shopeeTo"))
                .cost(roundDouble(getDouble(doc, "cost")))
                .cpc(roundDouble(getDouble(doc, "cpc")))
                .acos(roundDouble(getDouble(doc, "acos")))
                .ctr(roundDouble(getDouble(doc, "ctr")))
                .impression(roundDouble(getDouble(doc, "impression")))
                .click(roundDouble(getDouble(doc, "click")))
                .broadGmv(roundDouble(getDouble(doc, "broadGmv")))
                .directOrder(roundDouble(getDouble(doc, "directOrder")))
                .directOrderAmount(roundDouble(getDouble(doc, "directOrderAmount")))
                .directGmv(roundDouble(getDouble(doc, "directGmv")))
                .directRoi(roundDouble(getDouble(doc, "directRoi")))
                .directCir(roundDouble(getDouble(doc, "directCir")))
                .directCr(roundDouble(getDouble(doc, "directCr")))
                .roas(roundDouble(getDouble(doc, "roas")))
                .cr(roundDouble(getDouble(doc, "cr")))
                .biddingStrategy(getString(doc, "biddingStrategy"))
                .productPlacement(getString(doc, "productPlacement"))
                .type(getString(doc, "type"))
                .state(getString(doc, "state"))
                .dailyBudget(roundDouble(getDouble(doc, "dailyBudget")))
                .title(getString(doc, "title"))
                .hasCustomRoas(false)
                .image(getString(doc, "image"))
                .customRoas(roundDouble(getDouble(doc, "customRoas")))
                .broadOrder(roundDouble(getDouble(doc, "totalBroadOrder")))
                .broadOrderAmount(roundDouble(getDouble(doc, "totalBroadOrderAmount")))
                .cpdc(roundDouble(getDouble(doc, "cpdc")))
                .campaignStartTime(campaignStartTime)
                .campaignEndTime(campaignEndTime)
                .period(period)
                .insight(
                        MathKt.renderInsight(
                                MathKt.formulateRecommendation(
                                        getDouble(doc, "cpc"),
                                        getDouble(doc, "acos"),
                                        getDouble(doc, "click"),
                                        kpi,
                                        null,
                                        null
                                )
                        )
                )
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
                .dailyBudget(db / 100000)
                .biddingType(bid)
                .productPlacement(placement)
                .adsPeriod(period)
                .image(entry.getString("image"))
                .build();
    }

    private Double roundDouble(Double value) {
        if (value == null) return null;
        return Math.round(value * 100.0) / 100.0;
    }
}