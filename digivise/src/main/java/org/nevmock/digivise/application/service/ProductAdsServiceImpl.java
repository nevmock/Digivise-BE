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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    ProductAdsRepository productAdsRepository;

    @Autowired
    KPIRepository kpiRepository;

    @Autowired
    MerchantRepository merchantRepository;

    @Autowired
    ProductKeywordRepository productKeywordRepository;

    public List<ProductAdsResponseDto> findAll() {
        return List.of();
    }

    @Override
    public Optional<ProductAdsResponseDto> findById(String id) {
        return Optional.empty();
    }

    @Override
    public List<ProductAdsResponseDto> findByShopId(String shopId) {
        return List.of();
    }

    @Override
    public List<ProductAdsResponseDto> findByCreatedAtBetween(String from, String to) {
        return List.of();
    }

    @Override
    public List<ProductAdsResponseDto> findByCreatedAtGreaterThanEqual(String from) {
        return List.of();
    }

    @Override
    public List<ProductAdsResponseDto> findByCreatedAtLessThanEqual(String to) {
        return List.of();
    }

    @Override
    public List<ProductAdsResponseDto> findByFromAndTo(String from, String to) {
        return List.of();
    }

    @Override
    public Page<ProductAdsResponseDto> findByRangeAgg(
            String shopId,
            String biddingStrategy,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        Merchant merchant = merchantRepository
                .findByShopeeMerchantId(shopId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
        KPI kpi = kpiRepository
                .findByMerchantId(merchant.getId())
                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));

        List<AggregationOperation> baseOps = new ArrayList<>();

        // Match by shop_id and date range
        baseOps.add(Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("createdAt").gte(from).lte(to)
        ));

        // Unwind entry list
        baseOps.add(Aggregation.unwind("data.entry_list"));

        // Filter by bidding strategy if provided
        if (biddingStrategy != null) {
            baseOps.add(Aggregation.match(
                    Criteria.where("data.entry_list.manual_product_ads.bidding_strategy")
                            .is(biddingStrategy)
            ));
        }

        // Group by campaign_id with aggregations (avg for rates, sum for amounts)
        baseOps.add(Aggregation.group("data.entry_list.campaign.campaign_id")
                .first("_id").as("id")
                .first("shop_id").as("shopId")
                .first("createdAt").as("createdAt")
                .first("data.entry_list.title").as("title")
                .first("data.entry_list.image").as("image")
                .first("data.entry_list.state").as("state")
                .sum("data.entry_list.campaign.daily_budget").as("dailyBudget") // Changed to avg
                .first("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
                .avg("data.entry_list.report.cpc").as("cpc")
                .avg("data.entry_list.report.broad_cir").as("acos")
                .avg("data.entry_list.report.click").as("click")
                .avg("data.entry_list.report.ctr").as("ctr")
                .avg("data.entry_list.report.impression").as("impression")
                // Added missing fields with avg aggregation
                .avg("data.entry_list.report.broad_roi").as("roas")
                .avg("data.entry_list.report.broad_order").as("broadOrder")
                .avg("data.entry_list.report.broad_gmv").as("broadGmv")
                .avg("data.entry_list.report.direct_order").as("directOrder")
                .avg("data.entry_list.report.direct_order_amount").as("directOrderAmount")
                .avg("data.entry_list.report.direct_gmv").as("directGmv")
                .avg("data.entry_list.report.direct_roi").as("directRoi")
                .avg("data.entry_list.report.direct_cir").as("directCir")
                .avg("data.entry_list.report.direct_cr").as("directCr")
                .sum("data.entry_list.report.cost").as("cost")
                .avg("data.entry_list.report.cpdc").as("cpdc")
                // Added ratio fields with avg aggregation
                .avg("data.entry_list.ratio.broad_cir").as("acosRatio")
                .avg("data.entry_list.ratio.cpc").as("cpcRatio")
                .avg("data.entry_list.ratio.click").as("clickRatio")
                .avg("data.entry_list.ratio.ctr").as("ctrRatio")
                .avg("data.entry_list.ratio.impression").as("impressionRatio")
                .avg("data.entry_list.ratio.cost").as("costRatio")
                .avg("data.entry_list.ratio.broad_gmv").as("broadGmvRatio")
                .avg("data.entry_list.ratio.broad_order").as("broadOrderRatio")
                .avg("data.entry_list.ratio.checkout").as("checkoutRatio")
                .avg("data.entry_list.ratio.direct_order").as("directOrderRatio")
                .avg("data.entry_list.ratio.direct_order_amount").as("directOrderAmountRatio")
                .avg("data.entry_list.ratio.direct_gmv").as("directGmvRatio")
                .avg("data.entry_list.ratio.direct_roi").as("directRoiRatio")
                .avg("data.entry_list.ratio.direct_cir").as("directCirRatio")
                .avg("data.entry_list.ratio.direct_cr").as("directCrRatio")
                .avg("data.entry_list.ratio.cpdc").as("cpdcRatio")
                // Added type field
                .first("data.entry_list.type").as("type")
                .first("from").as("shopeeFrom")
                .first("to").as("shopeeTo")
        );

        baseOps.add(Aggregation.project()
                .andExpression("_id").as("campaignId")
                .andInclude("id", "shopId", "createdAt", "title", "image", "state",
                        "dailyBudget", "biddingStrategy", "cpc", "acos", "click", "ctr", "impression",
                        "broadRoi", "broadOrder", "broadGmv", "directOrder", "directOrderAmount",
                        "directGmv", "directRoi", "directCir", "directCr", "cost", "cpdc",
                        "acosRatio", "cpcRatio", "clickRatio", "ctrRatio", "impressionRatio",
                        "costRatio", "broadGmvRatio", "broadOrderRatio", "checkoutRatio",
                        "directOrderRatio", "directOrderAmountRatio", "directGmvRatio",
                        "directRoiRatio", "directCirRatio", "directCrRatio", "cpdcRatio",
                        "type", "shopeeFrom", "shopeeTo")
                .andExpression("{$literal: '" + from.toString() + "'}").as("from")
                .andExpression("{$literal: '" + to.toString() + "'}").as("to")
        );

        // Add explicit sorting for consistent results
        baseOps.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.ASC, "campaignId"))));

        // Lookup keywords
        baseOps.add(Aggregation.lookup(
                "ProductKey",
                "campaignId",
                "campaign_id",
                "keywords"
        ));

        // Facet for pagination
        FacetOperation facet = Aggregation.facet(
                        Aggregation.skip((long) pageable.getOffset()),
                        Aggregation.limit(pageable.getPageSize())
                ).as("pagedResults")
                .and(Aggregation.count().as("total")).as("countResult");

        List<AggregationOperation> fullPipeline = new ArrayList<>(baseOps);
        fullPipeline.add(facet);

        // Execute aggregation
        AggregationResults<Document> aggResults = mongoTemplate.aggregate(
                Aggregation.newAggregation(fullPipeline),
                "ProductAds",
                Document.class
        );

        Document root = aggResults.getMappedResults().stream().findFirst().orElse(null);
        if (root == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        @SuppressWarnings("unchecked")
        List<Document> docs = (List<Document>) root.get("pagedResults");
        int total = ((List<?>) root.get("countResult")).isEmpty()
                ? 0
                : ((Document) ((List<?>) root.get("countResult")).get(0)).getInteger("total");

        // Map to DTOs
        List<ProductAdsResponseDto> dtos = docs.stream()
                .map(doc -> mapToProductAdsDto(doc, kpi))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, total);
    }

    @Override
    public Page<ProductAdsResponseDto> findByRange(
            String shopId,
            String biddingStrategy,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        return Page.empty();
    }

    @Override
    public Page<ProductAdsResponseWrapperDto> findByRangeAggTotal(
            String shopId,
            String biddingStrategy,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable,
            String type
    ) {
        Merchant merchant = merchantRepository
                .findByShopeeMerchantId(shopId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
        KPI kpi = kpiRepository
                .findByMerchantId(merchant.getId())
                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));

        List<AggregationOperation> baseOps = new ArrayList<>();
        baseOps.add(Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("createdAt").gte(from).lte(to)
        ));

        baseOps.add(Aggregation.unwind("data.entry_list"));
        if (biddingStrategy != null) {
            baseOps.add(Aggregation.match(
                    Criteria.where("data.entry_list.manual_product_ads.bidding_strategy")
                            .is(biddingStrategy)
            ));
        }
        if (type != null) {
            baseOps.add(Aggregation.match(
                    Criteria.where("data.entry_list.type")
                            .is(type)
            ));
        }
        baseOps.add(Aggregation.project()
                .and("_id").as("id")
                .and("shop_id").as("shopId")
                .and("createdAt").as("createdAt")
                .and("data.entry_list.campaign.campaign_id").as("campaignId")
                .and("data.entry_list.title").as("title")
                .and("data.entry_list.image").as("image")
                .and("data.entry_list.state").as("state")
                .and("data.entry_list.campaign.daily_budget").as("dailyBudget")
                .and("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
                .and("data.entry_list.report.cpc").as("cpc")
                .and("data.entry_list.report.broad_cir").as("acos")
                .and("data.entry_list.report.click").as("click")
                .and("data.entry_list.report.ctr").as("ctr")
                .and("data.entry_list.report.impression").as("impression")
                .and("data.entry_list.report.broad_roi").as("broadRoi")
                .and("data.entry_list.report.broad_order").as("broadOrder")
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
                .and("data.entry_list.type").as("type")
                .and("from").as("shopeeFrom")
                .and("to").as("shopeeTo")
                // Tambahkan custom_roas untuk mapping
                .and("data.entry_list.custom_roas").as("customRoas")
                .andExpression("{$literal: '" + from.toString() + "'}").as("from")
                .andExpression("{$literal: '" + to.toString() + "'}").as("to")
        );
        baseOps.add(Aggregation.lookup(
                "ProductKey",
                "campaignId",
                "campaign_id",
                "keywords"
        ));

        FacetOperation facet = Aggregation.facet(
                        Aggregation.skip((long) pageable.getOffset()),
                        Aggregation.limit(pageable.getPageSize())
                ).as("pagedResults")
                .and(Aggregation.count().as("total")).as("countResult");

        List<AggregationOperation> fullPipeline = new ArrayList<>(baseOps);
        fullPipeline.add(facet);

        AggregationResults<Document> aggResults = mongoTemplate.aggregate(
                Aggregation.newAggregation(fullPipeline),
                "ProductAds",
                Document.class
        );

        Document root = aggResults.getMappedResults().stream().findFirst().orElse(null);
        if (root == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        @SuppressWarnings("unchecked")
        List<Document> docs = (List<Document>) root.get("pagedResults");

        // Ganti mapToProductAdsDto dengan mapToProductAdsDtoWithCustomRoas
        List<ProductAdsResponseDto> dtos = docs.stream()
                .map(doc -> mapToProductAdsDtoWithCustomRoas(doc, kpi))
                .toList();

        for (ProductAdsResponseDto dto : dtos) {
            System.out.println("Ratios: " + dto.getAcosRatio());
        }

        // Group by campaignId
        Map<Long, List<ProductAdsResponseDto>> grouped = dtos.stream()
                .filter(d -> d.getCampaignId() != null)
                .collect(Collectors.groupingBy(ProductAdsResponseDto::getCampaignId));

        // Build wrapper DTOs
        List<ProductAdsResponseWrapperDto> wrapperList = grouped.entrySet().stream()
                .map(e -> ProductAdsResponseWrapperDto.builder()
                        .campaignId(e.getKey())
                        .from(from)
                        .to(to)
                        .data(e.getValue())
                        .build()
                )
                .collect(Collectors.toList());

        // Apply paging
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), wrapperList.size());
        List<ProductAdsResponseWrapperDto> pageContent = start >= end
                ? Collections.emptyList()
                : wrapperList.subList(start, end);

        return new PageImpl<>(pageContent, pageable, wrapperList.size());
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
        dto.setShopeeFrom(getString(doc, "shopeeFrom"));
        dto.setShopeeTo(getString(doc, "shopeeTo"));
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

        @SuppressWarnings("unchecked")
        List<Document> kwDocs = (List<Document>) doc.get("keywords");
        List<ProductKeywordResponseDto> kws = kwDocs.stream()
                .flatMap(kd -> {
                    Object rawData = kd.get("data");
                    List<Document> dataDocs = new ArrayList<>();
                    if (rawData instanceof List) dataDocs.addAll((List<Document>) rawData);
                    else if (rawData instanceof Document) dataDocs.add((Document) rawData);

                    if (!dataDocs.isEmpty()) {
                        return dataDocs.stream().map(data -> buildKeywordDto(kd, data, kpi));
                    } else {
                        return Stream.of(buildKeywordDto(kd, null, kpi));
                    }
                })
                .collect(Collectors.toList());
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
        pk.setShopeeFrom(getString(kd, "shopeeFrom"));
        pk.setShopeeTo(getString(kd, "shopeeTo"));
        pk.setInsight(
                MathKt.renderInsight(
                        MathKt.formulateRecommendation(
                                pk.getCpc(), pk.getAcos(), pk.getClick(), kpi, null, null
                        )
                )
        );
        return pk;
    }

    // Utility extraction methods with type checking
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

    @Override
    public Page<ProductAdsResponseDto> findTodayData(
            String shopId,
            String biddingStrategy,
            Pageable pageable
    ) {
        // Get today's date range (start and end of today)
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        Merchant merchant = merchantRepository
                .findByShopeeMerchantId(shopId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
        KPI kpi = kpiRepository
                .findByMerchantId(merchant.getId())
                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));

        List<AggregationOperation> baseOps = new ArrayList<>();

        // Match by shop_id and TODAY's date range
        baseOps.add(Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("createdAt").gte(startOfDay).lte(endOfDay)
        ));

        // Unwind entry list
        baseOps.add(Aggregation.unwind("data.entry_list"));

        // Filter by bidding strategy if provided
        if (biddingStrategy != null) {
            baseOps.add(Aggregation.match(
                    Criteria.where("data.entry_list.manual_product_ads.bidding_strategy")
                            .is(biddingStrategy)
            ));
        }

        // Group by campaign_id with aggregations
        baseOps.add(Aggregation.group("data.entry_list.campaign.campaign_id")
                .first("_id").as("id")
                .first("shop_id").as("shopId")
                .first("createdAt").as("createdAt")
                .first("data.entry_list.title").as("title")
                .first("data.entry_list.image").as("image")
                .first("data.entry_list.state").as("state")
                .sum("data.entry_list.campaign.daily_budget").as("dailyBudget")
                .first("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
                .avg("data.entry_list.report.cpc").as("cpc")
                .avg("data.entry_list.report.broad_cir").as("acos")
                .avg("data.entry_list.report.click").as("click")
                .avg("data.entry_list.report.ctr").as("ctr")
                .avg("data.entry_list.report.impression").as("impression")
                .avg("data.entry_list.report.broad_roi").as("broadRoi")
                .avg("data.entry_list.report.broad_order").as("broadOrder")
                .avg("data.entry_list.report.broad_gmv").as("broadGmv")
                .avg("data.entry_list.report.direct_order").as("directOrder")
                .avg("data.entry_list.report.direct_order_amount").as("directOrderAmount")
                .avg("data.entry_list.report.direct_gmv").as("directGmv")
                .avg("data.entry_list.report.direct_roi").as("directRoi")
                .avg("data.entry_list.report.direct_cir").as("directCir")
                .avg("data.entry_list.report.direct_cr").as("directCr")
                .sum("data.entry_list.report.cost").as("cost")
                .avg("data.entry_list.report.cpdc").as("cpdc")
                .avg("data.entry_list.ratio.broad_cir").as("acosRatio")
                .avg("data.entry_list.ratio.cpc").as("cpcRatio")
                .avg("data.entry_list.ratio.click").as("clickRatio")
                .avg("data.entry_list.ratio.ctr").as("ctrRatio")
                .avg("data.entry_list.ratio.impression").as("impressionRatio")
                .avg("data.entry_list.ratio.cost").as("costRatio")
                .avg("data.entry_list.ratio.broad_gmv").as("broadGmvRatio")
                .avg("data.entry_list.ratio.broad_order").as("broadOrderRatio")
                .avg("data.entry_list.ratio.checkout").as("checkoutRatio")
                .avg("data.entry_list.ratio.direct_order").as("directOrderRatio")
                .avg("data.entry_list.ratio.direct_order_amount").as("directOrderAmountRatio")
                .avg("data.entry_list.ratio.direct_gmv").as("directGmvRatio")
                .avg("data.entry_list.ratio.direct_roi").as("directRoiRatio")
                .avg("data.entry_list.ratio.direct_cir").as("directCirRatio")
                .avg("data.entry_list.ratio.direct_cr").as("directCrRatio")
                .avg("data.entry_list.ratio.broad_roi").as("broadRoiRatio")
                .avg("data.entry_list.ratio.cpdc").as("cpdcRatio")
                .first("data.entry_list.type").as("type")
                .first("from").as("shopeeFrom")
                .first("to").as("shopeeTo")
                .first("data.entry_list.custom_roas").as("customRoas")
        );

        baseOps.add(Aggregation.project()
                .andExpression("_id").as("campaignId")
                .andInclude("id", "shopId", "createdAt", "title", "image", "state",
                        "dailyBudget", "biddingStrategy", "cpc", "acos", "click", "ctr", "impression",
                        "broadRoi", "broadOrder", "broadGmv", "directOrder", "directOrderAmount",
                        "directGmv", "directRoi", "directCir", "directCr", "cost", "cpdc",
                        "acosRatio", "cpcRatio", "clickRatio", "ctrRatio", "impressionRatio",
                        "costRatio", "broadGmvRatio", "broadOrderRatio", "checkoutRatio",
                        "directOrderRatio", "directOrderAmountRatio", "directGmvRatio",
                        "directRoiRatio", "directCirRatio", "directCrRatio", "cpdcRatio",
                        "type", "shopeeFrom", "shopeeTo", "customRoas")
                .andExpression("{$literal: '" + startOfDay.toString() + "'}").as("from")
                .andExpression("{$literal: '" + endOfDay.toString() + "'}").as("to")
        );

        // Add explicit sorting for consistent results
        baseOps.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.ASC, "campaignId"))));

        // Lookup keywords
        baseOps.add(Aggregation.lookup(
                "ProductKey",
                "campaignId",
                "campaign_id",
                "keywords"
        ));

        // Facet for pagination
        FacetOperation facet = Aggregation.facet(
                        Aggregation.skip((long) pageable.getOffset()),
                        Aggregation.limit(pageable.getPageSize())
                ).as("pagedResults")
                .and(Aggregation.count().as("total")).as("countResult");

        List<AggregationOperation> fullPipeline = new ArrayList<>(baseOps);
        fullPipeline.add(facet);

        // Execute aggregation
        AggregationResults<Document> aggResults = mongoTemplate.aggregate(
                Aggregation.newAggregation(fullPipeline),
                "ProductAds",
                Document.class
        );

        Document root = aggResults.getMappedResults().stream().findFirst().orElse(null);
        if (root == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        @SuppressWarnings("unchecked")
        List<Document> docs = (List<Document>) root.get("pagedResults");
        int total = ((List<?>) root.get("countResult")).isEmpty()
                ? 0
                : ((Document) ((List<?>) root.get("countResult")).get(0)).getInteger("total");

        List<ProductAdsResponseDto> dtos = docs.stream()
                .map(doc -> mapToProductAdsDtoWithCustomRoas(doc, kpi))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, total);
    }


    @Override
    public boolean insertCustomRoasForToday(String shopId, Long campaignId, Double customRoas) {
        try {
            // Get today's date range
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);

            // Create query to find today's data for specific shop and campaign
            Query query = new Query();
            query.addCriteria(
                    Criteria.where("shop_id").is(shopId)
                            .and("createdAt").gte(startOfDay).lte(endOfDay)
                            .and("data.entry_list.campaign.campaign_id").is(campaignId)
            );

            // Create update operation to set custom_roas in the matching entry_list element
            Update update = new Update();
            update.set("data.entry_list.$.custom_roas", customRoas);
            update.set("data.entry_list.$.custom_roas_updated_at", LocalDateTime.now());

            // Execute the update
            var result = mongoTemplate.updateMulti(query, update, "ProductAds");

            return result.getModifiedCount() > 0;

        } catch (Exception e) {
            System.err.println("Error inserting custom ROAS: " + e.getMessage());
            return false;
        }
    }

    // Method 3: Batch insert custom ROAS untuk multiple campaigns
    public Map<Long, Boolean> insertCustomRoasForTodayBatch(
            String shopId,
            Map<Long, Double> campaignRoasMap
    ) {
        Map<Long, Boolean> results = new HashMap<>();

        for (Map.Entry<Long, Double> entry : campaignRoasMap.entrySet()) {
            Long campaignId = entry.getKey();
            Double customRoas = entry.getValue();

            boolean success = insertCustomRoasForToday(shopId, campaignId, customRoas);
            results.put(campaignId, success);
        }

        return results;
    }

    private ProductAdsResponseDto mapToProductAdsDtoWithCustomRoas(Document doc, KPI kpi) {
        ProductAdsResponseDto dto = mapToProductAdsDto(doc, kpi); // Use existing mapping

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

        return dto;
    }

    // Method 4: Get summary of today's data with custom ROAS info
    public Map<String, Object> getTodayDataSummary(String shopId) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);

        List<AggregationOperation> ops = Arrays.asList(
                Aggregation.match(
                        Criteria.where("shop_id").is(shopId)
                                .and("createdAt").gte(startOfDay).lte(endOfDay)
                ),
                Aggregation.unwind("data.entry_list"),
                Aggregation.project()
                        .andInclude("shop_id")
                        .andInclude("data.entry_list.report.broad_roi")
                        .andInclude("data.entry_list.custom_roas")
                        .andInclude("data.entry_list.report.cost")
                        .andInclude("data.entry_list.report.broad_gmv")
                        .andExpression("{ $cond: { if: { $ne: ['$data.entry_list.custom_roas', null] }, then: 1, else: 0 } }")
                        .as("hasCustomRoas"),
                Aggregation.group("shop_id")
                        .count().as("totalCampaigns")
                        .sum("hasCustomRoas").as("campaignsWithCustomRoas")
                        .avg("data.entry_list.report.broad_roi").as("avgRoas")
                        .avg("data.entry_list.custom_roas").as("avgCustomRoas")
                        .sum("data.entry_list.report.cost").as("totalCost")
                        .sum("data.entry_list.report.broad_gmv").as("totalGmv")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops),
                "ProductAds",
                Document.class
        );

        Document summary = results.getMappedResults().stream().findFirst().orElse(new Document());

        Map<String, Object> summaryMap = new HashMap<>();
        summaryMap.put("date", LocalDateTime.now().toLocalDate().toString());
        summaryMap.put("totalCampaigns", summary.getInteger("totalCampaigns", 0));
        summaryMap.put("campaignsWithCustomRoas", summary.getInteger("campaignsWithCustomRoas", 0));
        summaryMap.put("avgRoas", getDouble(summary, "avgRoas"));
        summaryMap.put("avgCustomRoas", getDouble(summary, "avgCustomRoas"));
        summaryMap.put("totalCost", getDouble(summary, "totalCost"));
        summaryMap.put("totalGmv", getDouble(summary, "totalGmv"));

        return summaryMap;
    }
}
