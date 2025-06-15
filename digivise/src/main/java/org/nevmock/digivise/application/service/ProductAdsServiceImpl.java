package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseDto;
import org.nevmock.digivise.domain.model.KPI;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.port.in.ProductAdsService;
import org.nevmock.digivise.domain.port.out.KPIRepository;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.domain.port.out.ProductAdsRepository;
import org.nevmock.digivise.domain.port.out.ProductKeywordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import static org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.*;
import static org.springframework.data.mongodb.core.aggregation.ConditionalOperators.*;
import static org.springframework.data.mongodb.core.aggregation.ComparisonOperators.*;
import static org.springframework.data.mongodb.core.aggregation.ConvertOperators.*;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.nevmock.digivise.utils.MathKt;

@Service
public class ProductAdsServiceImpl implements ProductAdsService {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ProductAdsRepository productAdsRepository;

    @Autowired
    private KPIRepository kpiRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private ProductKeywordRepository productKeywordRepository;

    @Override
    public Page<ProductAdsResponseWrapperDto> findByRangeAggTotal(
            String shopId,
            String biddingStrategy,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable,
            String type,
            String state,
            String productPlacement,
            String salesClassification,
            String title,
            Boolean hasKeywords
    ) {
        Merchant merchant = merchantRepository
                .findByShopeeMerchantId(shopId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
        KPI kpi = kpiRepository
                .findByMerchantId(merchant.getId())
                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));

        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        
        List<Long> campaignIds = getUniqueCampaignIds(shopId, biddingStrategy, type, state,
                productPlacement, title, fromTimestamp, toTimestamp);

        
        int totalCampaigns = campaignIds.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), totalCampaigns);

        if (start >= totalCampaigns) {
            return new PageImpl<>(Collections.emptyList(), pageable, totalCampaigns);
        }

        List<Long> paginatedCampaignIds = campaignIds.subList(start, end);

        
        List<ProductAdsResponseWrapperDto> wrapperList = getProductAdsForCampaigns(
                paginatedCampaignIds, shopId, biddingStrategy, type, state, productPlacement,
                salesClassification, title, hasKeywords, fromTimestamp, toTimestamp, from, to, kpi
        );

        return new PageImpl<>(wrapperList, pageable, totalCampaigns);
    }

    
    private List<Long> getUniqueCampaignIds(String shopId, String biddingStrategy, String type,
                                            String state, String productPlacement, String title,
                                            long fromTimestamp, long toTimestamp) {
        List<AggregationOperation> ops = new ArrayList<>();

        
        Criteria matchCriteria = Criteria.where("shop_id").is(shopId)
                .and("from").gte(fromTimestamp).lte(toTimestamp);
        ops.add(Aggregation.match(matchCriteria));

        
        ops.add(Aggregation.unwind("data.entry_list"));

        
        if (biddingStrategy != null && !biddingStrategy.trim().isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.entry_list.manual_product_ads.bidding_strategy").is(biddingStrategy)
            ));
        }
        if (type != null && !type.trim().isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.entry_list.type").is(type)
            ));
        }
        if (state != null && !state.trim().isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.entry_list.state").is(state)
            ));
        }
        if (productPlacement != null && !productPlacement.trim().isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.entry_list.manual_product_ads.product_placement").is(productPlacement)
            ));
        }
        if (title != null && !title.trim().isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.entry_list.title").regex(title, "i") 
            ));
        }

        
        ops.add(Aggregation.group("data.entry_list.campaign.campaign_id"));

        
        ops.add(Aggregation.project().and("_id").as("campaignId"));

        
        ops.add(Aggregation.sort(Sort.Direction.ASC, "campaignId"));

        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops), "ProductAds", Document.class
        );

        return results.getMappedResults().stream()
                .map(doc -> getLong(doc, "campaignId"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    
    private List<ProductAdsResponseWrapperDto> getProductAdsForCampaigns(
            List<Long> campaignIds, String shopId, String biddingStrategy, String type,
            String state, String productPlacement, String salesClassification, String title, Boolean hasKeywords,
            long fromTimestamp, long toTimestamp, LocalDateTime from, LocalDateTime to, KPI kpi) {

        List<AggregationOperation> ops = buildOptimizedAggregationOps(
                campaignIds, shopId, biddingStrategy, type, state, productPlacement, title, hasKeywords,
                fromTimestamp, toTimestamp, from, to
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops), "ProductAds", Document.class
        );

        List<Document> docs = results.getMappedResults();

        
        List<ProductAdsResponseDto> dtos = docs.stream()
                .map(doc -> mapToProductAdsDtoWithSalesClassification(doc, kpi))
                .collect(Collectors.toList());

        
        if (salesClassification != null && !salesClassification.trim().isEmpty()) {
            dtos = dtos.stream()
                    .filter(dto -> salesClassification.equalsIgnoreCase(dto.getSalesClassification()))
                    .collect(Collectors.toList());
        }

        
        Map<Long, List<ProductAdsResponseDto>> grouped = dtos.stream()
                .filter(d -> d.getCampaignId() != null)
                .collect(Collectors.groupingBy(
                        ProductAdsResponseDto::getCampaignId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(entry -> ProductAdsResponseWrapperDto.builder()
                        .campaignId(entry.getKey())
                        .from(from)
                        .to(to)
                        .data(entry.getValue())
                        .build()
                )
                .collect(Collectors.toList());
    }


    private List<AggregationOperation> buildOptimizedAggregationOps(
            List<Long> campaignIds, String shopId, String biddingStrategy, String type,
            String state, String productPlacement, String title, Boolean hasKeywords,
            long fromTimestamp, long toTimestamp, LocalDateTime from, LocalDateTime to) {

        List<AggregationOperation> ops = new ArrayList<>();

        // Match criteria
        Criteria matchCriteria = Criteria.where("shop_id").is(shopId)
                .and("from").gte(fromTimestamp).lte(toTimestamp);
        ops.add(Aggregation.match(matchCriteria));

        // Unwind entry list
        ops.add(Aggregation.unwind("data.entry_list"));

        // Match campaign IDs
        ops.add(Aggregation.match(
                Criteria.where("data.entry_list.campaign.campaign_id").in(campaignIds)
        ));

        // Apply filters
        if (biddingStrategy != null && !biddingStrategy.trim().isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.entry_list.manual_product_ads.bidding_strategy").is(biddingStrategy)
            ));
        }
        if (type != null && !type.trim().isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.entry_list.type").is(type)
            ));
        }
        if (state != null && !state.trim().isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.entry_list.state").is(state)
            ));
        }
        if (productPlacement != null && !productPlacement.trim().isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.entry_list.manual_product_ads.product_placement").is(productPlacement)
            ));
        }
        if (title != null && !title.trim().isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.entry_list.title").regex(title, "i")
            ));
        }

        if (hasKeywords != null) {
            if (hasKeywords) {
                // hanya yang keywords tidak kosong
                ops.add(Aggregation.match(Criteria.where("keywords.0").exists(true)));
            } else {
                // hanya yang keywords kosong
                ops.add(Aggregation.match(Criteria.where("keywords.0").exists(false)));
            }
        }

        // Project stage
        ops.add(createProjectionStage(from, to));

        // PERBAIKAN: Ganti "ProductKey" menjadi "ProductKeyword"
        ops.add(Aggregation.lookup()
                .from("ProductKeyword")  // <- Ini yang diubah
                .let(VariableOperators.Let.just(
                        VariableOperators.Let.ExpressionVariable.newVariable("campaign_id").forField("campaignId"),
                        VariableOperators.Let.ExpressionVariable.newVariable("shopee_from").forField("shopeeFrom"),
                        VariableOperators.Let.ExpressionVariable.newVariable("shopee_to").forField("shopeeTo")
                ))
                .pipeline(
                        Aggregation.match(Criteria.expr(
                                BooleanOperators.And.and(
                                        Eq.valueOf("$campaign_id").equalTo("$$campaign_id"),
                                        Eq.valueOf("$from").equalTo("$$shopee_from"),
                                        Eq.valueOf("$to").equalTo("$$shopee_to")
                                )
                        ))
                )
                .as("keywords"));

        // Sales classification lookup
        ops.add(createOptimizedSalesClassificationLookup());

        return ops;
    }

    private List<AggregationOperation> buildOptimizedAggregationOps(
            List<Long> campaignIds, String shopId, String biddingStrategy, String type,
            long fromTimestamp, long toTimestamp, LocalDateTime from, LocalDateTime to) {

        List<AggregationOperation> ops = new ArrayList<>();

        
        Criteria matchCriteria = Criteria.where("shop_id").is(shopId)
                .and("from").gte(fromTimestamp).lte(toTimestamp);
        ops.add(Aggregation.match(matchCriteria));

        
        ops.add(Aggregation.unwind("data.entry_list"));

        
        ops.add(Aggregation.match(
                Criteria.where("data.entry_list.campaign.campaign_id").in(campaignIds)
        ));

        
        if (biddingStrategy != null) {
            ops.add(Aggregation.match(
                    Criteria.where("data.entry_list.manual_product_ads.bidding_strategy").is(biddingStrategy)
            ));
        }
        if (type != null) {
            ops.add(Aggregation.match(
                    Criteria.where("data.entry_list.type").is(type)
            ));
        }

        
        ops.add(createProjectionStage(from, to));

        
        ops.add(Aggregation.lookup()
                .from("ProductKey")
                .let(VariableOperators.Let.just( 
                        VariableOperators.Let.ExpressionVariable.newVariable("campaign_id").forField("campaignId"),
                        VariableOperators.Let.ExpressionVariable.newVariable("shopee_from").forField("shopeeFrom"),
                        VariableOperators.Let.ExpressionVariable.newVariable("shopee_to").forField("shopeeTo")
                ))
                .pipeline(
                        Aggregation.match(Criteria.expr(
                                BooleanOperators.And.and(
                                        Eq.valueOf("$campaign_id").equalTo("$$campaign_id"),
                                        Eq.valueOf("$from").equalTo("$$shopee_from"),
                                        Eq.valueOf("$to").equalTo("$$shopee_to")
                                )
                        ))
                )
                .as("keywords"));

        
        ops.add(createOptimizedSalesClassificationLookup());

        return ops;
    }

    private ProjectionOperation createProjectionStage(LocalDateTime from, LocalDateTime to) {
        return Aggregation.project()
                .and("_id").as("id")
                .and("shop_id").as("shopId")
                .and("createdAt").as("createdAt")
                .and("data.entry_list.campaign.campaign_id").as("campaignId")
                .and("data.entry_list.title").as("title")
                .and("data.entry_list.image").as("image")
                .and("data.entry_list.state").as("state")
                .and("data.entry_list.campaign.daily_budget").as("dailyBudget")
                .and("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
                .and("data.entry_list.manual_product_ads.product_placement").as("productPlacement")
                .and("data.entry_list.report.cpc").as("cpc")
                .and("data.entry_list.report.broad_cir").as("acos")
                .and("data.entry_list.report.click").as("click")
                .and("data.entry_list.report.ctr").as("ctr")
                .and("data.entry_list.report.impression").as("impression")
                .and("data.entry_list.report.broad_roi").as("broadRoi")
                .and("data.entry_list.report.broad_order").as("broadOrder")
                .and("data.entry_list.report.broad_order_amount").as("broadOrderAmount")
                .and("data.entry_list.report.broad_gmv").as("broadGmv")
                .and("data.entry_list.report.direct_order").as("directOrder")
                .and("data.entry_list.report.direct_order_amount").as("directOrderAmount")
                .and("data.entry_list.report.direct_gmv").as("directGmv")
                .and("data.entry_list.report.direct_roi").as("directRoi")
                .and("data.entry_list.report.direct_cir").as("directCir")
                .and("data.entry_list.report.direct_cr").as("directCr")
                .and("data.entry_list.report.cost").as("cost")
                .and("data.entry_list.report.cpdc").as("cpdc")
                .and("data.entry_list.ratio.broad_cir").as("acosRatio")
                .and("data.entry_list.ratio.cpc").as("cpcRatio")
                .and("data.entry_list.ratio.click").as("clickRatio")
                .and("data.entry_list.ratio.ctr").as("ctrRatio")
                .and("data.entry_list.ratio.impression").as("impressionRatio")
                .and("data.entry_list.ratio.cost").as("costRatio")
                .and("data.entry_list.ratio.broad_gmv").as("broadGmvRatio")
                .and("data.entry_list.ratio.broad_order").as("broadOrderRatio")
                .and("data.entry_list.ratio.checkout").as("checkoutRatio")
                .and("data.entry_list.ratio.direct_order").as("directOrderRatio")
                .and("data.entry_list.ratio.direct_order_amount").as("directOrderAmountRatio")
                .and("data.entry_list.ratio.direct_gmv").as("directGmvRatio")
                .and("data.entry_list.ratio.direct_roi").as("directRoiRatio")
                .and("data.entry_list.ratio.direct_cir").as("directCirRatio")
                .and("data.entry_list.ratio.direct_cr").as("directCrRatio")
                .and("data.entry_list.ratio.cpdc").as("cpdcRatio")
                .and("data.entry_list.ratio.broad_roi").as("broadRoiRatio")
                .and("data.entry_list.ratio.cr").as("crRatio")
                .and("data.entry_list.report.cr").as("cr")
                .and("data.entry_list.ratio.broad_order_amount").as("broadOrderAmountRatio")
                .and("data.entry_list.type").as("type")
                .and("from").as("shopeeFrom")
                .and("to").as("shopeeTo")
                .and("data.entry_list.custom_roas").as("customRoas")
                .andExpression("{$literal: '" + from.toString() + "'}").as("from")
                .andExpression("{$literal: '" + to.toString() + "'}").as("to");
    }

    private LookupOperation createOptimizedSalesClassificationLookup() {
        return Aggregation.lookup()
                .from("ProductStock")
                .let(VariableOperators.Let.ExpressionVariable
                        .newVariable("campaign_id").forField("campaignId"))
                .pipeline(
                        
                        Aggregation.match(Criteria.expr(
                                Eq.valueOf("$data.boost_info.campaign_id").equalTo("$$campaign_id")
                        )),
                        Aggregation.unwind("data"),
                        Aggregation.match(Criteria.expr(
                                Eq.valueOf("$data.boost_info.campaign_id").equalTo("$$campaign_id")
                        )),
                        
                        Aggregation.project()
                                .and("data.boost_info.campaign_id").as("campaignId")
                                .and("data.statistics.sold_count").as("soldCount")
                                .and("data.price_detail.selling_price_max").as("sellingPriceMax")
                                .and(Multiply.valueOf(
                                        IfNull.ifNull("$data.statistics.sold_count").then(0)
                                ).multiplyBy(
                                        IfNull.ifNull(
                                                ToDouble.toDouble("$data.price_detail.selling_price_max")
                                        ).then(0.0)
                                )).as("revenue"),
                        
                        Aggregation.group("campaignId")
                                .sum("revenue").as("totalRevenue")
                                .push(Document.parse("{ soldCount: '$soldCount', sellingPriceMax: '$sellingPriceMax', revenue: '$revenue' }"))
                                .as("products"),
                        
                        Aggregation.unwind("products"),
                        
                        Aggregation.project()
                                .and("_id").as("campaignId")
                                .and("products.soldCount").as("soldCount")
                                .and("products.sellingPriceMax").as("sellingPriceMax")
                                .and("products.revenue").as("revenue")
                                .and("totalRevenue").as("totalRevenue")
                                .and(Cond.when(Gt.valueOf("$totalRevenue").greaterThanValue(0))
                                        .then(Multiply.valueOf(
                                                Divide.valueOf("$products.revenue").divideBy("$totalRevenue")
                                        ).multiplyBy(100))
                                        .otherwise(0)).as("revenuePercentage")
                                .and(Cond.when(
                                                Gte.valueOf(
                                                        Cond.when(Gt.valueOf("$totalRevenue").greaterThanValue(0))
                                                                .then(Multiply.valueOf(
                                                                        Divide.valueOf("$products.revenue").divideBy("$totalRevenue")
                                                                ).multiplyBy(100))
                                                                .otherwise(0)
                                                ).greaterThanEqualToValue(30)
                                        ).then("Best Seller")
                                        .otherwise(
                                                Cond.when(
                                                                Gte.valueOf(
                                                                        Cond.when(Gt.valueOf("$totalRevenue").greaterThanValue(0))
                                                                                .then(Multiply.valueOf(
                                                                                        Divide.valueOf("$products.revenue").divideBy("$totalRevenue")
                                                                                ).multiplyBy(100))
                                                                                .otherwise(0)
                                                                ).greaterThanEqualToValue(10)
                                                        ).then("Middle Moving")
                                                        .otherwise("Slow Moving")
                                        )).as("salesClassification"),
                        
                        Aggregation.match(Criteria.expr(
                                Eq.valueOf("$campaignId").equalTo("$$campaign_id")
                        )),
                        
                        Aggregation.limit(1)
                )
                .as("salesClassificationData");
    }

    private ProductAdsResponseDto mapToProductAdsDto(Document doc, KPI kpi) {
        ProductAdsResponseDto dto = ProductAdsResponseDto.builder().build();
        dto.setId(getString(doc, "id"));
        dto.setShopeeMerchantId(getString(doc, "shopId"));
        dto.setFrom(getString(doc, "from"));
        dto.setTo(getString(doc, "to"));
        dto.setCreatedAt(getDateTime(doc, "createdAt"));
        dto.setCampaignId(getLong(doc, "campaignId"));
        dto.setTitle(getString(doc, "title"));
        dto.setImage(getString(doc, "image"));
        dto.setState(getString(doc, "state"));
        dto.setDailyBudget(getDouble(doc, "dailyBudget") / 100000);
        dto.setBiddingStrategy(getString(doc, "biddingStrategy"));
        dto.setCpc(getDouble(doc, "cpc") / 100000);
        dto.setAcos(getDouble(doc, "acos"));
        dto.setClick(getDouble(doc, "click"));
        dto.setCtr(getDouble(doc, "ctr"));
        dto.setImpression(getDouble(doc, "impression"));
        dto.setBroadRoi(getDouble(doc, "broadRoi"));
        dto.setBroadRoiRatio(getDouble(doc, "broadRoiRatio"));
        dto.setShopeeFrom(getLong(doc, "shopeeFrom"));
        dto.setShopeeTo(getLong(doc, "shopeeTo"));
        dto.setAcosRatio(getDouble(doc, "acosRatio"));
        dto.setCpcRatio(getDouble(doc, "cpcRatio"));
        dto.setClickRatio(getDouble(doc, "clickRatio"));
        dto.setCtrRatio(getDouble(doc, "ctrRatio"));
        dto.setImpressionRatio(getDouble(doc, "impressionRatio"));
        dto.setCostRatio(getDouble(doc, "costRatio"));
        dto.setDirectOrder(getDouble(doc, "directOrder"));
        dto.setDirectOrderAmount(getDouble(doc, "directOrderAmount"));
        dto.setDirectGmv(getDouble(doc, "directGmv"));
        dto.setDirectRoi(getDouble(doc, "directRoi"));
        dto.setDirectCir(getDouble(doc, "directCir"));
        dto.setDirectCr(getDouble(doc, "directCr"));
        dto.setCpdc(getDouble(doc, "cpdc"));
        dto.setBroadOrder(getDouble(doc, "broadOrder"));
        dto.setBroadGmv(getDouble(doc, "broadGmv"));
        dto.setBroadGmvRatio(getDouble(doc, "broadGmvRatio"));
        dto.setBroadOrderRatio(getDouble(doc, "broadOrderRatio"));
        dto.setCheckoutRatio(getDouble(doc, "checkoutRatio"));
        dto.setDirectOrderRatio(getDouble(doc, "directOrderRatio"));
        dto.setDirectOrderAmountRatio(getDouble(doc, "directOrderAmountRatio"));
        dto.setDirectGmvRatio(getDouble(doc, "directGmvRatio"));
        dto.setDirectRoiRatio(getDouble(doc, "directRoiRatio"));
        dto.setDirectCirRatio(getDouble(doc, "directCirRatio"));
        dto.setDirectCrRatio(getDouble(doc, "directCrRatio"));
        dto.setCpdcRatio(getDouble(doc, "cpdcRatio"));
        dto.setCost(getDouble(doc, "cost"));
        dto.setType(getString(doc, "type"));
        dto.setCr(getDouble(doc, "cr"));
        dto.setCrRatio(getDouble(doc, "crRatio"));
        dto.setBroadOrderAmountRatio(getDouble(doc, "broadOrderAmountRatio"));
        dto.setBroadOrderAmount(getDouble(doc, "broadOrderAmount"));
        dto.setProductPlacement(getString(doc, "productPlacement"));

        
        @SuppressWarnings("unchecked")
        List<Document> kwDocs = (List<Document>) doc.get("keywords");
        List<ProductKeywordResponseDto> kws = Collections.emptyList();

        if (kwDocs != null && !kwDocs.isEmpty()) {
            kws = kwDocs.stream()
                    .flatMap(kd -> processKeywordDocument(kd, dto, kpi))
                    .collect(Collectors.toList());
        }

        dto.setKeywords(kws);
        dto.setHasKeywords(!kws.isEmpty());

        dto.setInsight(
                MathKt.renderInsight(
                        MathKt.formulateRecommendation(
                                dto.getCpc(), dto.getAcos(), dto.getClick(), kpi, null, null
                        )
                )
        );
        return dto;
    }

    private Stream<ProductKeywordResponseDto> processKeywordDocument(Document kd, ProductAdsResponseDto dto, KPI kpi) {
        Object rawData = kd.get("data");
        if (rawData instanceof List) {
            @SuppressWarnings("unchecked")
            List<Document> dataDocs = (List<Document>) rawData;
            return dataDocs.stream()
                    .map(data -> buildKeywordDto(kd, data, kpi))
                    .filter(pk -> Objects.equals(pk.getCampaignId(), dto.getCampaignId())
                            && Objects.equals(pk.getShopeeFrom(), dto.getShopeeFrom())
                            && Objects.equals(pk.getShopeeTo(), dto.getShopeeTo()));
        } else if (rawData instanceof Document) {
            ProductKeywordResponseDto pk = buildKeywordDto(kd, (Document) rawData, kpi);
            return Stream.of(pk)
                    .filter(p -> Objects.equals(p.getCampaignId(), dto.getCampaignId())
                            && Objects.equals(p.getShopeeFrom(), dto.getShopeeFrom())
                            && Objects.equals(p.getShopeeTo(), dto.getShopeeTo()));
        } else {
            ProductKeywordResponseDto pk = buildKeywordDto(kd, null, kpi);
            return Stream.of(pk)
                    .filter(p -> Objects.equals(p.getCampaignId(), dto.getCampaignId())
                            && Objects.equals(p.getShopeeFrom(), dto.getShopeeFrom())
                            && Objects.equals(p.getShopeeTo(), dto.getShopeeTo()));
        }
    }

    private ProductKeywordResponseDto buildKeywordDto(Document kd, Document data, KPI kpi) {
        if (data == null) {
            return ProductKeywordResponseDto.builder().build();
        }

        ProductKeywordResponseDto pk = ProductKeywordResponseDto.builder().build();
        pk.setId(getString(kd, "_id"));
        pk.setShopeeMerchantId(getString(kd, "shop_id"));
        pk.setCampaignId(getLong(kd, "campaign_id"));
        pk.setFrom(getString(kd, "from"));
        pk.setTo(getString(kd, "to"));
        pk.setCreatedAt(getDateTime(kd, "createdAt"));
        pk.setKey(data != null ? getString(data, "key") : getString(kd, "key"));
        Document metrics = data != null ? data.get("metrics", Document.class) : kd;
        pk.setAcos(getDouble(metrics, "broad_cir"));
        pk.setCpc(getDouble(metrics, "cpc"));
        pk.setCost(getDouble(metrics, "cost"));
        pk.setImpression(getDouble(metrics, "impression"));
        pk.setClick(getDouble(metrics, "click"));
        pk.setCtr(getDouble(metrics, "ctr"));
        pk.setShopeeFrom(getString(kd, "from"));
        pk.setShopeeTo(getString(kd, "to"));
        pk.setInsight(
                MathKt.renderInsight(
                        MathKt.formulateRecommendation(
                                pk.getCpc(), pk.getAcos(), pk.getClick(), kpi, null, null
                        )
                )
        );
        return pk;
    }

    private String getString(Document doc, String key) {
        Object v = doc.get(key);
        return v instanceof String ? (String) v : null;
    }

    private LocalDateTime getDateTime(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Date) {
            return ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
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

    private Double getDouble(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return null;
    }

    private Integer getInteger(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return null;
    }

    @Override
    public boolean insertCustomRoasForToday(String shopId, Long campaignId, Double customRoas) {
        try {
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);

            long startTimestamp = startOfDay.atZone(ZoneId.systemDefault()).toEpochSecond();
            long endTimestamp = endOfDay.atZone(ZoneId.systemDefault()).toEpochSecond();

            Query query = new Query();
            query.addCriteria(
                    Criteria.where("shop_id").is(shopId)
                            .and("from").gte(startTimestamp).lte(endTimestamp)
                            .and("data.entry_list.campaign.campaign_id").is(campaignId)
            );

            Update update = new Update();
            update.set("data.entry_list.$.custom_roas", customRoas);
            update.set("data.entry_list.$.custom_roas_updated_at", LocalDateTime.now());

            var result = mongoTemplate.updateMulti(query, update, "ProductAds");

            return result.getModifiedCount() > 0;

        } catch (Exception e) {
            System.err.println("Error inserting custom ROAS: " + e.getMessage());
            return false;
        }
    }

    private ProductAdsResponseDto mapToProductAdsDtoWithSalesClassification(Document doc, KPI kpi) {
        ProductAdsResponseDto dto = mapToProductAdsDto(doc, kpi);

        
        Double customRoas = getDouble(doc, "customRoas");
        if (customRoas != null) {
            dto.setCustomRoas(customRoas);
            dto.setHasCustomRoas(true);

            dto.setRoas(
                    MathKt.calculateRoas(
                            customRoas,
                            dto.getBroadRoi(),
                            dto.getDailyBudget()
                    )
            );

            dto.setInsightBudget(
                    MathKt.renderInsight(
                            MathKt.formulateRecommendation(
                                    dto.getCpc(), dto.getAcos(), dto.getClick(), kpi, dto.getRoas(), dto.getDailyBudget()
                            )
                    )
            );
        } else {
            dto.setHasCustomRoas(false);
        }

        @SuppressWarnings("unchecked")
        List<Document> salesClassificationDocs = (List<Document>) doc.get("salesClassificationData");

        if (salesClassificationDocs != null && !salesClassificationDocs.isEmpty()) {
            Document salesData = salesClassificationDocs.get(0);
            String salesClassification = getString(salesData, "salesClassification");
            dto.setSalesClassification(salesClassification != null ? salesClassification : "UNKNOWN");
        } else {
            dto.setSalesClassification("UNKNOWN");
        }

        return dto;
    }
}