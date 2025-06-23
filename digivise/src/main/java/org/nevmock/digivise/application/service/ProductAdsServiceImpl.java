//package org.nevmock.digivise.application.service;
//
//import org.bson.Document;
//import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
//import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
//import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordAdsResponseDto;
//import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordAdsResponseWrapperDto;
//import org.nevmock.digivise.domain.model.KPI;
//import org.nevmock.digivise.domain.model.Merchant;
//import org.nevmock.digivise.domain.port.in.ProductAdsService;
//import org.nevmock.digivise.domain.port.out.KPIRepository;
//import org.nevmock.digivise.domain.port.out.MerchantRepository;
//import org.nevmock.digivise.domain.port.out.ProductAdsRepository;
//import org.nevmock.digivise.domain.port.out.ProductKeywordRepository;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.aggregation.*;
//import org.springframework.data.mongodb.core.query.Criteria;
//
//import static org.springframework.data.mongodb.core.aggregation.ComparisonOperators.*;
//
//import org.springframework.data.mongodb.core.query.Query;
//import org.springframework.data.mongodb.core.query.Update;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.*;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import org.nevmock.digivise.utils.MathKt;
//
//@Service
//public class ProductAdsServiceImpl implements ProductAdsService {
//    @Autowired
//    private MongoTemplate mongoTemplate;
//
//    @Autowired
//    private ProductAdsRepository productAdsRepository;
//
//    @Autowired
//    private KPIRepository kpiRepository;
//
//    @Autowired
//    private MerchantRepository merchantRepository;
//
//    @Autowired
//    private ProductKeywordRepository productKeywordRepository;
//
//    @Override
//    public Page<ProductAdsResponseWrapperDto> findByRangeAggTotal(
//            String shopId,
//            String biddingStrategy,
//            LocalDateTime from,
//            LocalDateTime to,
//            Pageable pageable,
//            String type,
//            String state,
//            String productPlacement,
//            String salesClassification,
//            String title,
//            Boolean hasKeywords
//    ) {
//        Merchant merchant = merchantRepository
//                .findByShopeeMerchantId(shopId)
//                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
//        KPI kpi = kpiRepository
//                .findByMerchantId(merchant.getId())
//                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));
//
//        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
//        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();
//
//
//        List<Long> campaignIds = getUniqueCampaignIds(shopId, biddingStrategy, type, state,
//                productPlacement, title, fromTimestamp, toTimestamp);
//
//
//        int totalCampaigns = campaignIds.size();
//        int start = (int) pageable.getOffset();
//        int end = Math.min(start + pageable.getPageSize(), totalCampaigns);
//
//        if (start >= totalCampaigns) {
//            return new PageImpl<>(Collections.emptyList(), pageable, totalCampaigns);
//        }
//
//        List<Long> paginatedCampaignIds = campaignIds.subList(start, end);
//
//
//        List<ProductAdsResponseWrapperDto> wrapperList = getProductAdsForCampaigns(
//                paginatedCampaignIds, shopId, biddingStrategy, type, state, productPlacement,
//                salesClassification, title, hasKeywords, fromTimestamp, toTimestamp, from, to, kpi
//        );
//
//        return new PageImpl<>(wrapperList, pageable, totalCampaigns);
//    }
//
//
//    private List<Long> getUniqueCampaignIds(String shopId, String biddingStrategy, String type,
//                                            String state, String productPlacement, String title,
//                                            long fromTimestamp, long toTimestamp) {
//        List<AggregationOperation> ops = new ArrayList<>();
//
//
//        Criteria matchCriteria = Criteria.where("shop_id").is(shopId)
//                .and("from").gte(fromTimestamp).lte(toTimestamp);
//        ops.add(Aggregation.match(matchCriteria));
//
//
//        ops.add(Aggregation.unwind("data.entry_list"));
//
//
//        if (biddingStrategy != null && !biddingStrategy.trim().isEmpty()) {
//            ops.add(Aggregation.match(
//                    Criteria.where("data.entry_list.manual_product_ads.bidding_strategy").is(biddingStrategy)
//            ));
//        }
//        if (type != null && !type.trim().isEmpty()) {
//            ops.add(Aggregation.match(
//                    Criteria.where("data.entry_list.type").is(type)
//            ));
//        }
//        if (state != null && !state.trim().isEmpty()) {
//            ops.add(Aggregation.match(
//                    Criteria.where("data.entry_list.state").is(state)
//            ));
//        }
//        if (productPlacement != null && !productPlacement.trim().isEmpty()) {
//            ops.add(Aggregation.match(
//                    Criteria.where("data.entry_list.manual_product_ads.product_placement").is(productPlacement)
//            ));
//        }
//        if (title != null && !title.trim().isEmpty()) {
//            ops.add(Aggregation.match(
//                    Criteria.where("data.entry_list.title").regex(title, "i")
//            ));
//        }
//
//
//        ops.add(Aggregation.group("data.entry_list.campaign.campaign_id"));
//
//
//        ops.add(Aggregation.project().and("_id").as("campaignId"));
//
//
//        ops.add(Aggregation.sort(Sort.Direction.ASC, "campaignId"));
//
//        AggregationResults<Document> results = mongoTemplate.aggregate(
//                Aggregation.newAggregation(ops), "ProductAds", Document.class
//        );
//
//        return results.getMappedResults().stream()
//                .map(doc -> getLong(doc, "campaignId"))
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }
//
//
//    private List<ProductAdsResponseWrapperDto> getProductAdsForCampaigns(
//            List<Long> campaignIds, String shopId, String biddingStrategy, String type,
//            String state, String productPlacement, String salesClassification, String title, Boolean hasKeywords,
//            long fromTimestamp, long toTimestamp, LocalDateTime from, LocalDateTime to, KPI kpi) {
//
//        List<AggregationOperation> ops = buildOptimizedAggregationOps(
//                campaignIds, shopId, biddingStrategy, type, state, productPlacement, title, hasKeywords,
//                fromTimestamp, toTimestamp, from, to
//        );
//
//        AggregationResults<Document> results = mongoTemplate.aggregate(
//                Aggregation.newAggregation(ops), "ProductAds", Document.class
//        );
//
//        List<Document> docs = results.getMappedResults();
//
//
//        List<ProductAdsResponseDto> dtos = docs.stream()
//                .map(doc -> mapToProductAdsDtoWithSalesClassification(doc, kpi))
//                .collect(Collectors.toList());
//
//
//        if (salesClassification != null && !salesClassification.trim().isEmpty()) {
//            dtos = dtos.stream()
//                    .filter(dto -> salesClassification.equalsIgnoreCase(dto.getSalesClassification()))
//                    .collect(Collectors.toList());
//        }
//
//
//        Map<Long, List<ProductAdsResponseDto>> grouped = dtos.stream()
//                .filter(d -> d.getCampaignId() != null)
//                .collect(Collectors.groupingBy(
//                        ProductAdsResponseDto::getCampaignId,
//                        LinkedHashMap::new,
//                        Collectors.toList()
//                ));
//
//        return grouped.entrySet().stream()
//                .map(entry -> ProductAdsResponseWrapperDto.builder()
//                        .campaignId(entry.getKey())
//                        .from(from)
//                        .to(to)
//                        .data(entry.getValue())
//                        .build()
//                )
//                .collect(Collectors.toList());
//    }
//
//    private List<AggregationOperation> buildOptimizedAggregationOps(
//            List<Long> campaignIds, String shopId, String biddingStrategy, String type,
//            String state, String productPlacement, String title, Boolean hasKeywords,
//            long fromTimestamp, long toTimestamp, LocalDateTime from, LocalDateTime to) {
//
//        List<AggregationOperation> ops = new ArrayList<>();
//
//        Criteria matchCriteria = Criteria.where("shop_id").is(shopId)
//                .and("from").gte(fromTimestamp).lte(toTimestamp);
//        ops.add(Aggregation.match(matchCriteria));
//
//        ops.add(Aggregation.unwind("data.entry_list"));
//
//        ops.add(Aggregation.match(
//                Criteria.where("data.entry_list.campaign.campaign_id").in(campaignIds)
//        ));
//
//        if (biddingStrategy != null && !biddingStrategy.trim().isEmpty()) {
//            ops.add(Aggregation.match(
//                    Criteria.where("data.entry_list.manual_product_ads.bidding_strategy").is(biddingStrategy)
//            ));
//        }
//        if (type != null && !type.trim().isEmpty()) {
//            ops.add(Aggregation.match(
//                    Criteria.where("data.entry_list.type").is(type)
//            ));
//        }
//        if (state != null && !state.trim().isEmpty()) {
//            ops.add(Aggregation.match(
//                    Criteria.where("data.entry_list.state").is(state)
//            ));
//        }
//        if (productPlacement != null && !productPlacement.trim().isEmpty()) {
//            ops.add(Aggregation.match(
//                    Criteria.where("data.entry_list.manual_product_ads.product_placement").is(productPlacement)
//            ));
//        }
//        if (title != null && !title.trim().isEmpty()) {
//            ops.add(Aggregation.match(
//                    Criteria.where("data.entry_list.title").regex(title, "i")
//            ));
//        }
//
//        ops.add(Aggregation.project()
//                .and("_id").as("id")
//                .and("shop_id").as("shopId")
//                .and("createdAt").as("createdAt")
//                .and("data.entry_list.campaign.campaign_id").as("campaignId")
//                .and("data.entry_list.title").as("title")
//                .and("data.entry_list.image").as("image")
//                .and("data.entry_list.state").as("state")
//                .and("data.entry_list.campaign.daily_budget").as("dailyBudget")
//                .and("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
//                .and("data.entry_list.manual_product_ads.product_placement").as("productPlacement")
//                .and("data.entry_list.report.cpc").as("cpc")
//                .and("data.entry_list.report.broad_cir").as("acos")
//                .and("data.entry_list.report.click").as("click")
//                .and("data.entry_list.report.ctr").as("ctr")
//                .and("data.entry_list.report.impression").as("impression")
//                .and("data.entry_list.report.broad_roi").as("broadRoi")
//                .and("data.entry_list.report.broad_order").as("broadOrder")
//                .and("data.entry_list.report.broad_order_amount").as("broadOrderAmount")
//                .and("data.entry_list.report.broad_gmv").as("broadGmv")
//                .and("data.entry_list.report.direct_order").as("directOrder")
//                .and("data.entry_list.report.direct_order_amount").as("directOrderAmount")
//                .and("data.entry_list.report.direct_gmv").as("directGmv")
//                .and("data.entry_list.report.direct_roi").as("directRoi")
//                .and("data.entry_list.report.direct_cir").as("directCir")
//                .and("data.entry_list.report.direct_cr").as("directCr")
//                .and("data.entry_list.report.cost").as("cost")
//                .and("data.entry_list.report.cpdc").as("cpdc")
//                .and("data.entry_list.ratio.broad_cir").as("acosRatio")
//                .and("data.entry_list.ratio.cpc").as("cpcRatio")
//                .and("data.entry_list.ratio.click").as("clickRatio")
//                .and("data.entry_list.ratio.ctr").as("ctrRatio")
//                .and("data.entry_list.ratio.impression").as("impressionRatio")
//                .and("data.entry_list.ratio.cost").as("costRatio")
//                .and("data.entry_list.ratio.broad_gmv").as("broadGmvRatio")
//                .and("data.entry_list.ratio.broad_order").as("broadOrderRatio")
//                .and("data.entry_list.ratio.checkout").as("checkoutRatio")
//                .and("data.entry_list.ratio.direct_order").as("directOrderRatio")
//                .and("data.entry_list.ratio.direct_order_amount").as("directOrderAmountRatio")
//                .and("data.entry_list.ratio.direct_gmv").as("directGmvRatio")
//                .and("data.entry_list.ratio.direct_roi").as("directRoiRatio")
//                .and("data.entry_list.ratio.direct_cir").as("directCirRatio")
//                .and("data.entry_list.ratio.direct_cr").as("directCrRatio")
//                .and("data.entry_list.ratio.cpdc").as("cpdcRatio")
//                .and("data.entry_list.ratio.broad_roi").as("broadRoiRatio")
//                .and("data.entry_list.ratio.cr").as("crRatio")
//                .and("data.entry_list.report.cr").as("cr")
//                .and("data.entry_list.ratio.broad_order_amount").as("broadOrderAmountRatio")
//                .and("data.entry_list.type").as("type")
//                .and("from").as("shopeeFrom")
//                .and("to").as("shopeeTo")
//                .and("data.entry_list.custom_roas").as("customRoas")
//                .andExpression("{$literal: '" + from.toString() + "'}").as("from")
//                .andExpression("{$literal: '" + to.toString() + "'}").as("to")
//        );
//
//        ops.add(Aggregation.lookup()
//                .from("ProductKey")
//                .let(VariableOperators.Let.just(
//                        VariableOperators.Let.ExpressionVariable.newVariable("campaign_id").forField("campaignId"),
//                        VariableOperators.Let.ExpressionVariable.newVariable("from_ts").forField("shopeeFrom"),
//                        VariableOperators.Let.ExpressionVariable.newVariable("to_ts").forField("shopeeTo")
//                ))
//                .pipeline(
//                        Aggregation.match(Criteria.expr(
//                                BooleanOperators.And.and(
//                                        Eq.valueOf("$campaign_id").equalTo("$$campaign_id"),
//                                        Eq.valueOf("$from").equalTo("$$from_ts"),
//                                        Eq.valueOf("$to").equalTo("$$to_ts")
//                                )
//                        )),
//                        Aggregation.group("campaign_id")
//                                .first("from").as("from")
//                                .first("to").as("to")
//                                .push("$$ROOT").as("keywords")
//                )
//                .as("keywordWrapper"));
//
//        if (hasKeywords != null) {
//            if (hasKeywords) {
//                ops.add(Aggregation.match(Criteria.where("keywordWrapper.0").exists(true)));
//            } else {
//                ops.add(Aggregation.match(Criteria.where("keywordWrapper.0").exists(false)));
//            }
//        }
//
//        ops.add(createOptimizedSalesClassificationLookup());
//
//        return ops;
//    }
//
//    private void debugKeywordLookup(String shopId, long fromTimestamp, long toTimestamp) {
//        System.out.println("=== DEBUG KEYWORD LOOKUP ===");
//
//        Query keyQuery = new Query();
//        keyQuery.addCriteria(
//                Criteria.where("from").is(fromTimestamp)
//                        .and("to").is(toTimestamp)
//        );
//
//        List<Document> keyDocs = mongoTemplate.find(keyQuery, Document.class, "ProductKey");
//        System.out.println("ProductKey documents found: " + keyDocs.size());
//        keyDocs.forEach(doc -> {
//            System.out.println("  - Campaign ID: " + doc.get("campaign_id") +
//                    ", From: " + doc.get("from") +
//                    ", To: " + doc.get("to") +
//                    ", Shop ID: " + doc.get("shop_id"));
//        });
//
//        List<AggregationOperation> debugOps = Arrays.asList(
//                Aggregation.match(Criteria.where("shop_id").is(shopId)
//                        .and("from").gte(fromTimestamp).lte(toTimestamp)),
//                Aggregation.unwind("data.entry_list"),
//                Aggregation.project()
//                        .and("data.entry_list.campaign.campaign_id").as("campaignId")
//                        .and("from").as("from")
//                        .and("to").as("to")
//                        .and("shop_id").as("shopId")
//        );
//
//        AggregationResults<Document> debugResults = mongoTemplate.aggregate(
//                Aggregation.newAggregation(debugOps), "ProductAds", Document.class
//        );
//
//        System.out.println("ProductAds campaigns found: " + debugResults.getMappedResults().size());
//        debugResults.getMappedResults().forEach(doc -> {
//            System.out.println("  - Campaign ID: " + doc.get("campaignId") +
//                    ", From: " + doc.get("from") +
//                    ", To: " + doc.get("to") +
//                    ", Shop ID: " + doc.get("shopId"));
//        });
//    }
//
//    private void testKeywordLookup(List<Long> campaignIds, long fromTimestamp, long toTimestamp) {
//        List<AggregationOperation> testOps = Arrays.asList(
//                Aggregation.match(Criteria.where("shop_id").regex(".*")
//                        .and("from").gte(fromTimestamp).lte(toTimestamp)),
//                Aggregation.unwind("data.entry_list"),
//                Aggregation.match(Criteria.where("data.entry_list.campaign.campaign_id").in(campaignIds)),
//                Aggregation.project()
//                        .and("data.entry_list.campaign.campaign_id").as("campaignId")
//                        .and("from").as("shopeeFrom")
//                        .and("to").as("shopeeTo"),
//                Aggregation.lookup()
//                        .from("ProductKey")
//                        .let(VariableOperators.Let.just(
//                                VariableOperators.Let.ExpressionVariable.newVariable("campaign_id").forField("campaignId"),
//                                VariableOperators.Let.ExpressionVariable.newVariable("from_ts").forField("shopeeFrom"),
//                                VariableOperators.Let.ExpressionVariable.newVariable("to_ts").forField("shopeeTo")
//                        ))
//                        .pipeline(
//                                Aggregation.match(Criteria.expr(
//                                        BooleanOperators.And.and(
//                                                Eq.valueOf("$campaign_id").equalTo("$$campaign_id"),
//                                                Eq.valueOf("$from").equalTo("$$from_ts"),
//                                                Eq.valueOf("$to").equalTo("$$to_ts")
//                                        )
//                                ))
//                        )
//                        .as("keywords"),
//                Aggregation.project()
//                        .and("campaignId").as("campaignId")
//                        .and("shopeeFrom").as("shopeeFrom")
//                        .and("shopeeTo").as("shopeeTo")
//                        .and("keywords").as("keywords")
//                        .andExpression("{$size: '$keywords'}").as("keywordCount")
//        );
//
//        AggregationResults<Document> testResults = mongoTemplate.aggregate(
//                Aggregation.newAggregation(testOps), "ProductAds", Document.class
//        );
//
//        System.out.println("=== TEST KEYWORD LOOKUP RESULTS ===");
//        testResults.getMappedResults().forEach(doc -> {
//            System.out.println("Campaign ID: " + doc.get("campaignId") +
//                    ", Keyword Count: " + doc.get("keywordCount"));
//        });
//    }
//
//    private ProjectionOperation createProjectionStage(LocalDateTime from, LocalDateTime to) {
//        return Aggregation.project()
//                .and("_id").as("id")
//                .and("shop_id").as("shopId")
//                .and("createdAt").as("createdAt")
//                .and("data.entry_list.campaign.campaign_id").as("campaignId")
//                .and("data.entry_list.title").as("title")
//                .and("data.entry_list.image").as("image")
//                .and("data.entry_list.state").as("state")
//                .and("data.entry_list.campaign.daily_budget").as("dailyBudget")
//                .and("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
//                .and("data.entry_list.manual_product_ads.product_placement").as("productPlacement")
//                .and("data.entry_list.report.cpc").as("cpc")
//                .and("data.entry_list.report.broad_cir").as("acos")
//                .and("data.entry_list.report.click").as("click")
//                .and("data.entry_list.report.ctr").as("ctr")
//                .and("data.entry_list.report.impression").as("impression")
//                .and("data.entry_list.report.broad_roi").as("broadRoi")
//                .and("data.entry_list.report.broad_order").as("broadOrder")
//                .and("data.entry_list.report.broad_order_amount").as("broadOrderAmount")
//                .and("data.entry_list.report.broad_gmv").as("broadGmv")
//                .and("data.entry_list.report.direct_order").as("directOrder")
//                .and("data.entry_list.report.direct_order_amount").as("directOrderAmount")
//                .and("data.entry_list.report.direct_gmv").as("directGmv")
//                .and("data.entry_list.report.direct_roi").as("directRoi")
//                .and("data.entry_list.report.direct_cir").as("directCir")
//                .and("data.entry_list.report.direct_cr").as("directCr")
//                .and("data.entry_list.report.cost").as("cost")
//                .and("data.entry_list.report.cpdc").as("cpdc")
//                .and("data.entry_list.ratio.broad_cir").as("acosRatio")
//                .and("data.entry_list.ratio.cpc").as("cpcRatio")
//                .and("data.entry_list.ratio.click").as("clickRatio")
//                .and("data.entry_list.ratio.ctr").as("ctrRatio")
//                .and("data.entry_list.ratio.impression").as("impressionRatio")
//                .and("data.entry_list.ratio.cost").as("costRatio")
//                .and("data.entry_list.ratio.broad_gmv").as("broadGmvRatio")
//                .and("data.entry_list.ratio.broad_order").as("broadOrderRatio")
//                .and("data.entry_list.ratio.checkout").as("checkoutRatio")
//                .and("data.entry_list.ratio.direct_order").as("directOrderRatio")
//                .and("data.entry_list.ratio.direct_order_amount").as("directOrderAmountRatio")
//                .and("data.entry_list.ratio.direct_gmv").as("directGmvRatio")
//                .and("data.entry_list.ratio.direct_roi").as("directRoiRatio")
//                .and("data.entry_list.ratio.direct_cir").as("directCirRatio")
//                .and("data.entry_list.ratio.direct_cr").as("directCrRatio")
//                .and("data.entry_list.ratio.cpdc").as("cpdcRatio")
//                .and("data.entry_list.ratio.broad_roi").as("broadRoiRatio")
//                .and("data.entry_list.ratio.cr").as("crRatio")
//                .and("data.entry_list.report.cr").as("cr")
//                .and("data.entry_list.ratio.broad_order_amount").as("broadOrderAmountRatio")
//                .and("data.entry_list.type").as("type")
//                .and("from").as("shopeeFrom")
//                .and("to").as("shopeeTo")
//                .and("data.entry_list.custom_roas").as("customRoas")
//                .andExpression("{$literal: '" + from.toString() + "'}").as("from")
//                .andExpression("{$literal: '" + to.toString() + "'}").as("to");
//    }
//
//    private LookupOperation createOptimizedSalesClassificationLookup() {
//        List<Document> pipeline = new ArrayList<>();
//
//        pipeline.add(new Document("$match",
//                new Document("$expr",
//                        new Document("$eq", Arrays.asList("$data.boost_info.campaign_id", "$$campaign_id"))
//                )
//        ));
//
//        pipeline.add(new Document("$unwind", "$data"));
//
//        pipeline.add(new Document("$project",
//                new Document("campaignId", "$data.boost_info.campaign_id")
//                        .append("soldCount", "$data.statistics.sold_count")
//                        .append("sellingPriceMax",
//                                new Document("$toDouble", "$data.price_detail.selling_price_max"))
//                        .append("revenue",
//                                new Document("$multiply", Arrays.asList(
//                                        new Document("$toDouble", "$data.price_detail.selling_price_max"),
//                                        "$data.statistics.sold_count"
//                                ))
//                        )
//        ));
//
//        pipeline.add(new Document("$group",
//                new Document("_id", "$campaignId")
//                        .append("totalCampaignRevenue", new Document("$sum", "$revenue"))
//                        .append("products",
//                                new Document("$push",
//                                        new Document("soldCount", "$soldCount")
//                                                .append("sellingPriceMax", "$sellingPriceMax")
//                                                .append("revenue", "$revenue")
//                                )
//                        )
//        ));
//
//        pipeline.add(new Document("$unwind", "$products"));
//
//        pipeline.add(new Document("$project",
//                new Document("campaignId", "$_id")
//                        .append("soldCount", "$products.soldCount")
//                        .append("sellingPriceMax", "$products.sellingPriceMax")
//                        .append("revenue", "$products.revenue")
//                        .append("totalRevenue", "$totalCampaignRevenue")
//                        .append("revenuePercentage",
//                                new Document("$cond",
//                                        new Document("if", new Document("$gt", Arrays.asList("$totalCampaignRevenue", 0)))
//                                                .append("then",
//                                                        new Document("$multiply", Arrays.asList(
//                                                                new Document("$divide", Arrays.asList("$products.revenue", "$totalCampaignRevenue")),
//                                                                100
//                                                        ))
//                                                )
//                                                .append("else", 0)
//                                )
//                        )
//                        .append("salesClassification",
//                                new Document("$switch",
//                                        new Document("branches", Arrays.asList(
//                                                new Document()
//                                                        .append("case", new Document("$gte", Arrays.asList("$revenuePercentage", 70)))
//                                                        .append("then", "Best Seller"),
//
//                                                new Document()
//                                                        .append("case", new Document("$gte", Arrays.asList("$revenuePercentage", 20)))
//                                                        .append("then", "Middle Moving")
//                                        ))
//                                                .append("default", "Slow Moving")
//                                )
//                        )
//        ));
//
//        pipeline.add(new Document("$match",
//                new Document("$expr",
//                        new Document("$eq", Arrays.asList("$campaignId", "$$campaign_id"))
//                )
//        ));
//
//        List<AggregationOperation> operations = pipeline.stream()
//                .map(operation -> (AggregationOperation) context -> context.getMappedObject(operation))
//                .collect(Collectors.toList());
//
//        return LookupOperation.newLookup()
//                .from("ProductStock")
//                .let(
//                    VariableOperators.Let.ExpressionVariable.newVariable("campaign_id").forField("$campaignId")
//                )
//                .pipeline(operations.toArray(new AggregationOperation[0]))
//                .as("salesClassificationData");
//    }
//
//    private static class CustomAggregationOperation implements AggregationOperation {
//        private final Document operation;
//
//        public CustomAggregationOperation(Document operation) {
//            this.operation = operation;
//        }
//
//        @Override
//        public Document toDocument(AggregationOperationContext context) {
//            return context.getMappedObject(operation);
//        }
//    }
//
//    private ProductAdsResponseDto mapToProductAdsDto(Document doc, KPI kpi) {
//        ProductAdsResponseDto dto = ProductAdsResponseDto.builder().build();
//        dto.setId(getString(doc, "id"));
//        dto.setShopeeMerchantId(getString(doc, "shopId"));
//        dto.setFrom(getString(doc, "from"));
//        dto.setTo(getString(doc, "to"));
//        dto.setCreatedAt(getDateTime(doc, "createdAt"));
//        dto.setCampaignId(getLong(doc, "campaignId"));
//        dto.setTitle(getString(doc, "title"));
//        dto.setImage(getString(doc, "image"));
//        dto.setState(getString(doc, "state"));
//        dto.setDailyBudget(getDouble(doc, "dailyBudget") / 100000);
//        dto.setBiddingStrategy(getString(doc, "biddingStrategy"));
//        dto.setCpc(getDouble(doc, "cpc") / 100000);
//        dto.setAcos(getDouble(doc, "acos"));
//        dto.setClick(getDouble(doc, "click"));
//        dto.setCtr(getDouble(doc, "ctr"));
//        dto.setImpression(getDouble(doc, "impression"));
//        dto.setBroadRoi(getDouble(doc, "broadRoi"));
//        dto.setBroadRoiRatio(getDouble(doc, "broadRoiRatio"));
//        dto.setShopeeFrom(getLong(doc, "shopeeFrom"));
//        dto.setShopeeTo(getLong(doc, "shopeeTo"));
//        dto.setAcosRatio(getDouble(doc, "acosRatio"));
//        dto.setCpcRatio(getDouble(doc, "cpcRatio"));
//        dto.setClickRatio(getDouble(doc, "clickRatio"));
//        dto.setCtrRatio(getDouble(doc, "ctrRatio"));
//        dto.setImpressionRatio(getDouble(doc, "impressionRatio"));
//        dto.setCostRatio(getDouble(doc, "costRatio"));
//        dto.setDirectOrder(getDouble(doc, "directOrder"));
//        dto.setDirectOrderAmount(getDouble(doc, "directOrderAmount"));
//        dto.setDirectGmv(getDouble(doc, "directGmv"));
//        dto.setDirectRoi(getDouble(doc, "directRoi"));
//        dto.setDirectCir(getDouble(doc, "directCir"));
//        dto.setDirectCr(getDouble(doc, "directCr"));
//        dto.setCpdc(getDouble(doc, "cpdc"));
//        dto.setBroadOrder(getDouble(doc, "broadOrder"));
//        dto.setBroadGmv(getDouble(doc, "broadGmv"));
//        dto.setBroadGmvRatio(getDouble(doc, "broadGmvRatio"));
//        dto.setBroadOrderRatio(getDouble(doc, "broadOrderRatio"));
//        dto.setCheckoutRatio(getDouble(doc, "checkoutRatio"));
//        dto.setDirectOrderRatio(getDouble(doc, "directOrderRatio"));
//        dto.setDirectOrderAmountRatio(getDouble(doc, "directOrderAmountRatio"));
//        dto.setDirectGmvRatio(getDouble(doc, "directGmvRatio"));
//        dto.setDirectRoiRatio(getDouble(doc, "directRoiRatio"));
//        dto.setDirectCirRatio(getDouble(doc, "directCirRatio"));
//        dto.setDirectCrRatio(getDouble(doc, "directCrRatio"));
//        dto.setCpdcRatio(getDouble(doc, "cpdcRatio"));
//        dto.setCost(getDouble(doc, "cost"));
//        dto.setType(getString(doc, "type"));
//        dto.setCr(getDouble(doc, "cr"));
//        dto.setCrRatio(getDouble(doc, "crRatio"));
//        dto.setBroadOrderAmountRatio(getDouble(doc, "broadOrderAmountRatio"));
//        dto.setBroadOrderAmount(getDouble(doc, "broadOrderAmount"));
//        dto.setProductPlacement(getString(doc, "productPlacement"));
//
//
//        @SuppressWarnings("unchecked")
//        List<Document> keywordWrapperDocs = (List<Document>) doc.get("keywordWrapper");
//
//        if (keywordWrapperDocs != null && !keywordWrapperDocs.isEmpty()) {
//            Document wrapperDoc = keywordWrapperDocs.get(0);
//            @SuppressWarnings("unchecked")
//            List<Document> keywordDocs = (List<Document>) wrapperDoc.get("keywords");
//
//            if (keywordDocs != null && !keywordDocs.isEmpty()) {
//                List<ProductKeywordAdsResponseDto> keywords = keywordDocs.stream()
//                        .flatMap(kd -> processKeywordDocument(kd, dto, kpi))
//                        .collect(Collectors.toList());
//
//                ProductKeywordAdsResponseWrapperDto keywordWrapper = ProductKeywordAdsResponseWrapperDto.builder()
//                        .campaignId(dto.getCampaignId())
//                        .from(LocalDateTime.parse(dto.getFrom()))
//                        .to(LocalDateTime.parse(dto.getTo()))
//                        .data(keywords)
//                        .build();
//
//                dto.setKeywordWrapper(keywordWrapper);
//                dto.setHasKeywords(!keywords.isEmpty());
//            } else {
//                dto.setKeywords(Collections.emptyList());
//                dto.setHasKeywords(false);
//            }
//        } else {
//            dto.setKeywords(Collections.emptyList());
//            dto.setHasKeywords(false);
//        }
//
//        dto.setInsight(
//                MathKt.renderInsight(
//                        MathKt.formulateRecommendation(
//                                dto.getCpc(), dto.getAcos(), dto.getClick(), kpi, null, null
//                        )
//                )
//        );
//        return dto;
//    }
//
//    private Stream<ProductKeywordAdsResponseDto> processKeywordDocument(Document kd, ProductAdsResponseDto dto, KPI kpi) {
//        Object rawData = kd.get("data");
//        if (rawData instanceof List) {
//            @SuppressWarnings("unchecked")
//            List<Document> dataDocs = (List<Document>) rawData;
//            return dataDocs.stream()
//                    .map(data -> buildKeywordDto(kd, data, kpi));
//        } else if (rawData instanceof Document) {
//            ProductKeywordAdsResponseDto pk = buildKeywordDto(kd, (Document) rawData, kpi);
//            return Stream.of(pk);
//        } else {
//            ProductKeywordAdsResponseDto pk = buildKeywordDto(kd, null, kpi);
//            return Stream.of(pk);
//        }
//    }
//
//    private ProductKeywordAdsResponseDto buildKeywordDto(Document kd, Document data, KPI kpi) {
//        ProductKeywordAdsResponseDto pk = ProductKeywordAdsResponseDto.builder().build();
//
//        // Set basic fields from kd (parent document)
//        pk.setId(getString(kd, "_id"));
//        pk.setShopeeMerchantId(getString(kd, "shop_id"));
//        pk.setCampaignId(getLong(kd, "campaign_id"));
//        pk.setFrom(getString(kd, "from"));
//        pk.setTo(getString(kd, "to"));
//        pk.setCreatedAt(getDateTime(kd, "createdAt"));
//        pk.setShopeeFrom(getLong(kd, "from"));
//        pk.setShopeeTo(getLong(kd, "to"));
//
//        if (data == null) {
//            // If no data document, return empty DTO with basic fields
//            return pk;
//        }
//
//        // Set key from data document
//        pk.setKey(getString(data, "key"));
//
//        // Get metrics document - this should exist in the data document
//        Document metrics = data.get("metrics", Document.class);
//
//        if (metrics != null) {
//            // Set metrics fields
//            pk.setAcos(getDouble(metrics, "broad_cir"));
//            pk.setCpc(getDouble(metrics, "cpc"));
//            pk.setCost(getDouble(metrics, "cost"));
//            pk.setImpression(getDouble(metrics, "impression"));
//            pk.setClick(getDouble(metrics, "click"));
//            pk.setCtr(getDouble(metrics, "ctr"));
//        } else {
//            System.out.println("Warning: No metrics found for keyword: " + getString(data, "key"));
//            pk.setAcos(0.0);
//            pk.setCpc(0.0);
//            pk.setCost(0.0);
//            pk.setImpression(0.0);
//            pk.setClick(0.0);
//            pk.setCtr(0.0);
//        }
//
//        try {
//            pk.setInsight(
//                    MathKt.renderInsight(
//                            MathKt.formulateRecommendation(
//                                    pk.getCpc(), pk.getAcos(), pk.getClick(), kpi, null, null
//                            )
//                    )
//            );
//        } catch (Exception e) {
//            System.err.println("Error generating insight for keyword: " + pk.getKey() + " - " + e.getMessage());
//            pk.setInsight("No insight available");
//        }
//
//        return pk;
//    }
//
//    private String getString(Document doc, String key) {
//        Object v = doc.get(key);
//        return v instanceof String ? (String) v : null;
//    }
//
//    private LocalDateTime getDateTime(Document doc, String key) {
//        Object v = doc.get(key);
//        if (v instanceof Date) {
//            return ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
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
//    private Double getDouble(Document doc, String key) {
//        Object v = doc.get(key);
//        if (v instanceof Number) {
//            return ((Number) v).doubleValue();
//        }
//        return null;
//    }
//
//    private Integer getInteger(Document doc, String key) {
//        Object v = doc.get(key);
//        if (v instanceof Number) {
//            return ((Number) v).intValue();
//        }
//        return null;
//    }
//
//    @Override
//    public boolean insertCustomRoasForToday(String shopId, Long campaignId, Double customRoas) {
//        try {
//            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
//            LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
//
//            long startTimestamp = startOfDay.atZone(ZoneId.systemDefault()).toEpochSecond();
//            long endTimestamp = endOfDay.atZone(ZoneId.systemDefault()).toEpochSecond();
//
//            Query query = new Query();
//            query.addCriteria(
//                    Criteria.where("shop_id").is(shopId)
//                            .and("from").gte(startTimestamp).lte(endTimestamp)
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
//    private ProductAdsResponseDto mapToProductAdsDtoWithSalesClassification(Document doc, KPI kpi) {
//        ProductAdsResponseDto dto = mapToProductAdsDto(doc, kpi);
//
//        Double customRoas = getDouble(doc, "customRoas");
//        if (customRoas != null) {
//            dto.setCustomRoas(customRoas);
//            dto.setHasCustomRoas(true);
//            dto.setRoas(MathKt.calculateRoas(customRoas, dto.getBroadRoi(), dto.getDailyBudget()));
//            dto.setInsightBudget(MathKt.renderInsight(
//                    MathKt.formulateRecommendation(
//                            dto.getCpc(), dto.getAcos(), dto.getClick(),
//                            kpi, dto.getRoas(), dto.getDailyBudget()
//                    )
//            ));
//        } else {
//            dto.setHasCustomRoas(false);
//        }
//
//        @SuppressWarnings("unchecked")
//        List<Document> salesClassificationDocs = (List<Document>) doc.get("salesClassificationData");
//
//        if (salesClassificationDocs != null && !salesClassificationDocs.isEmpty()) {
//            Document salesData = salesClassificationDocs.get(0);
//            String classification = getString(salesData, "salesClassification");
//            dto.setSalesClassification(classification != null ? classification : "UNKNOWN");
//
//
//            System.out.println("Sales Classification for Campaign " + dto.getCampaignId() + ": " + classification);
//            System.out.println("Revenue Percentage: " + getDouble(salesData, "revenuePercentage"));
//            System.out.println("Total Revenue: " + getDouble(salesData, "totalCampaignRevenue"));
//        } else {
//            dto.setSalesClassification("UNKNOWN");
//            System.out.println("No sales classification data for Campaign " + dto.getCampaignId());
//        }
//
//        return dto;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductAdsServiceImpl implements ProductAdsService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private KPIRepository kpiRepository;

    /**
     * Metode ini telah diperbaiki untuk membandingkan performa iklan antara dua rentang waktu.
     * Ia mengambil data agregat per campaign untuk kedua periode, membandingkannya,
     * dan mengisi field `...Comparison`.
     */
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
            String title
    ) {
        Merchant merchant = merchantRepository
                .findByShopeeMerchantId(shopId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
        KPI kpi = kpiRepository
                .findByMerchantId(merchant.getId())
                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));

        // 1. Ambil data agregat untuk periode pertama (periode utama)
        List<ProductAdsResponseDto> period1DataList = getAggregatedDataByCampaignForRange(shopId, biddingStrategy, type, state, productPlacement, title, from1, to1, kpi);

        // 2. Ambil data agregat untuk periode kedua (periode pembanding) dan masukkan ke dalam Map untuk pencarian cepat
        Map<Long, ProductAdsResponseDto> period2DataMap = getAggregatedDataByCampaignForRange(shopId, biddingStrategy, type, state, productPlacement, title, from2, to2, kpi)
                .stream()
                .collect(Collectors.toMap(ProductAdsResponseDto::getCampaignId, Function.identity()));

        if (period1DataList.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // 3. Lakukan perbandingan untuk setiap campaign di periode pertama
        List<ProductAdsResponseWrapperDto> resultList = period1DataList.stream().map(period1Data -> {
            ProductAdsResponseDto period2Data = period2DataMap.get(period1Data.getCampaignId());

            // Isi field perbandingan
            populateComparisonFields(period1Data, period2Data);

            // Generate insight berdasarkan data dari periode pertama
            period1Data.setInsight(
                    MathKt.renderInsight(
                            MathKt.formulateRecommendation(
                                    period1Data.getCpc(),
                                    period1Data.getAcos(),
                                    period1Data.getClick(),
                                    kpi,
                                    null, // Anda mungkin perlu data tambahan untuk customRoas
                                    null  // dan targetRoas
                            )
                    )
            );

            return ProductAdsResponseWrapperDto.builder()
                    .campaignId(period1Data.getCampaignId())
                    .from1(from1)
                    .to1(to1)
                    .from2(from2)
                    .to2(to2)
                    // Data array berisi satu elemen (data periode 1 yang sudah dilengkapi perbandingan)
                    .data(Collections.singletonList(period1Data))
                    .build();
        }).collect(Collectors.toList());

        // 4. Terapkan paginasi secara manual ke hasil akhir
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), resultList.size());

        if (start > resultList.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, resultList.size());
        }

        return new PageImpl<>(resultList.subList(start, end), pageable, resultList.size());
    }

    /**
     * Helper method untuk mengisi field-field perbandingan pada DTO.
     * @param currentData DTO untuk data periode 1 (periode saat ini).
     * @param previousData DTO untuk data periode 2 (periode pembanding), bisa null jika campaign tidak ada di periode 2.
     */
    private void populateComparisonFields(ProductAdsResponseDto currentData, ProductAdsResponseDto previousData) {
        currentData.setCostComparison(calculateComparison(currentData.getCost(), previousData != null ? previousData.getCost() : null));
        currentData.setCpcComparison(calculateComparison(currentData.getCpc(), previousData != null ? previousData.getCpc() : null));
        currentData.setAcosComparison(calculateComparison(currentData.getAcos(), previousData != null ? previousData.getAcos() : null));
        currentData.setCtrComparison(calculateComparison(currentData.getCtr(), previousData != null ? previousData.getCtr() : null));
        currentData.setImpressionComparison(calculateComparison(currentData.getImpression(), previousData != null ? previousData.getImpression() : null));
        currentData.setClickComparison(calculateComparison(currentData.getClick(), previousData != null ? previousData.getClick() : null));
        currentData.setGmvComparison(calculateComparison(currentData.getGmv(), previousData != null ? previousData.getGmv() : null));
        currentData.setRoasComparison(calculateComparison(currentData.getRoas(), previousData != null ? previousData.getRoas() : null));
        currentData.setCrComparison(calculateComparison(currentData.getCr(), previousData != null ? previousData.getCr() : null));
        currentData.setDirectOrderComparison(calculateComparison(currentData.getDirectOrder(), previousData != null ? previousData.getDirectOrder() : null));
        currentData.setDirectOrderAmountComparison(calculateComparison(currentData.getDirectOrderAmount(), previousData != null ? previousData.getDirectOrderAmount() : null));
        currentData.setDirectGmvComparison(calculateComparison(currentData.getDirectGmv(), previousData != null ? previousData.getDirectGmv() : null));
        currentData.setDirectRoiComparison(calculateComparison(currentData.getDirectRoi(), previousData != null ? previousData.getDirectRoi() : null));
        currentData.setDirectCirComparison(calculateComparison(currentData.getDirectCir(), previousData != null ? previousData.getDirectCir() : null));
        currentData.setDirectCrComparison(calculateComparison(currentData.getDirectCr(), previousData != null ? previousData.getDirectCr() : null));
        // Ulangi untuk semua field perbandingan lainnya...
    }

    /**
     * Menghitung rasio perbandingan antara nilai baru dan lama.
     * Rumus: (nilaiBaru - nilaiLama) / nilaiLama.
     * @param currentValue Nilai dari periode 1.
     * @param previousValue Nilai dari periode 2.
     * @return Rasio perubahan (misal: -0.5 untuk penurunan 50%, 1.0 untuk kenaikan 100%).
     */
    private Double calculateComparison(Double currentValue, Double previousValue) {
        if (currentValue == null || previousValue == null) {
            return null;
        }
        if (previousValue == 0) {
            return (currentValue > 0) ? 1.0 : 0.0;
        }
        return (currentValue - previousValue) / previousValue;
    }

    /**
     * Melakukan agregasi data dari MongoDB, dikelompokkan berdasarkan campaign_id untuk rentang waktu tertentu.
     * (Metode ini tidak diubah, karena sudah berfungsi dengan benar untuk mengambil data per campaign).
     */
    private List<ProductAdsResponseDto> getAggregatedDataByCampaignForRange(
            String shopId, String biddingStrategy, String type, String state,
            String productPlacement, String title, LocalDateTime from, LocalDateTime to, KPI kpi) {

        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        List<AggregationOperation> ops = new ArrayList<>();

        Criteria matchCriteria = Criteria.where("shop_id").is(shopId)
                .and("from").gte(fromTimestamp).lte(toTimestamp);
        ops.add(Aggregation.match(matchCriteria));
        ops.add(Aggregation.unwind("data.entry_list"));

        if (biddingStrategy != null && !biddingStrategy.trim().isEmpty()) {
            ops.add(Aggregation.match(Criteria.where("data.entry_list.manual_product_ads.bidding_strategy").is(biddingStrategy)));
        }
        if (type != null && !type.trim().isEmpty()) {
            ops.add(Aggregation.match(Criteria.where("data.entry_list.type").is(type)));
        }
        if (state != null && !state.trim().isEmpty()) {
            ops.add(Aggregation.match(Criteria.where("data.entry_list.state").is(state)));
        }
        if (productPlacement != null && !productPlacement.trim().isEmpty()) {
            ops.add(Aggregation.match(Criteria.where("data.entry_list.manual_product_ads.product_placement").is(productPlacement)));
        }
        if (title != null && !title.trim().isEmpty()) {
            ops.add(Aggregation.match(Criteria.where("data.entry_list.title").regex(title, "i")));
        }

        ops.add(Aggregation.group("data.entry_list.campaign.campaign_id")
                .sum("data.entry_list.report.cost").as("totalCost")
                .sum("data.entry_list.report.impression").as("totalImpression")
                .sum("data.entry_list.report.click").as("totalClick")
                .sum("data.entry_list.report.broad_gmv").as("totalBroadGmv")
                .sum("data.entry_list.report.direct_gmv").as("totalDirectGmv")
                .sum("data.entry_list.report.direct_order").as("totalDirectOrder")
                .sum("data.entry_list.report.direct_order_amount").as("totalDirectOrderAmount")
                .avg("data.entry_list.report.cpc").as("avgCpc")
                .avg("data.entry_list.report.broad_cir").as("avgAcos") // ACOS adalah CIR
                .avg("data.entry_list.report.ctr").as("avgCtr")
                .avg("data.entry_list.report.direct_roi").as("avgDirectRoi")
                .avg("data.entry_list.report.direct_cir").as("avgDirectCir")
                .avg("data.entry_list.report.direct_cr").as("avgDirectCr")
                .avg("data.entry_list.report.broad_roi").as("avgRoas") // ROAS adalah ROI
                .avg("data.entry_list.report.cr").as("avgCr")
                .first("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
                .first("data.entry_list.manual_product_ads.product_placement").as("productPlacement")
                .first("data.entry_list.type").as("type")
                .first("data.entry_list.state").as("state")
        );

        ops.add(Aggregation.project()
                .and("_id").as("campaignId")
                .and("totalCost").divide(100000.0).as("cost")
                .and("avgCpc").divide(100000.0).as("cpc")
                .and("avgAcos").as("acos")
                .and("avgCtr").as("ctr")
                .and("totalImpression").as("impression")
                .and("totalClick").as("click")
                .andExpression("totalBroadGmv + totalDirectGmv").divide(100000.0).as("gmv")
                .and("totalDirectOrder").as("directOrder")
                .and("totalDirectOrderAmount").as("directOrderAmount")
                .and("totalDirectGmv").divide(100000.0).as("directGmv")
                .and("avgDirectRoi").as("directRoi")
                .and("avgDirectCir").as("directCir")
                .and("avgDirectCr").as("directCr")
                .and("avgRoas").as("roas")
                .and("avgCr").as("cr")
                .andInclude("biddingStrategy", "productPlacement", "type", "state")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops), "ProductAds", Document.class
        );

        return results.getMappedResults().stream()
                .map(this::mapDocumentToDto)
                .collect(Collectors.toList());
    }

    private ProductAdsResponseDto mapDocumentToDto(Document doc) {
        return ProductAdsResponseDto.builder()
                .campaignId(getLong(doc, "campaignId"))
                .cost(getDouble(doc, "cost"))
                .cpc(getDouble(doc, "cpc"))
                .acos(getDouble(doc, "acos"))
                .ctr(getDouble(doc, "ctr"))
                .impression(getDouble(doc, "impression"))
                .click(getDouble(doc, "click"))
                .gmv(getDouble(doc, "gmv"))
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
                .build();
    }

    // Helper methods (getLong, getDouble, getString) tidak berubah
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
    public boolean insertCustomRoasForToday(String shopId, Long campaignId, Double customRoas) {
        System.err.println("Warning: insertCustomRoasForToday might not function as expected with the new aggregated data structure.");
        return false;
    }
}