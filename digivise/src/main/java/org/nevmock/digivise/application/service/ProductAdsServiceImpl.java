package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseClassificationDto;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
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
            String type
    ) {
        Merchant merchant = merchantRepository
                .findByShopeeMerchantId(shopId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
        KPI kpi = kpiRepository
                .findByMerchantId(merchant.getId())
                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));

        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        List<AggregationOperation> baseOps = new ArrayList<>();
        baseOps.add(Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("from").gte(fromTimestamp).lte(toTimestamp)
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
                .andExpression("{$literal: '" + to.toString() + "'}").as("to")
        );


        baseOps.add(Aggregation.lookup(
                "ProductKey",
                "campaignId",
                "campaign_id",
                "keywords"
        ));

        baseOps.add(
                Aggregation.lookup()
                        .from("ProductStock")
                        .let(
                                VariableOperators.Let.ExpressionVariable
                                        .newVariable("campaign_id")
                                        .forField("campaignId")
                        )
                        .pipeline(
                                Aggregation.unwind("data"),

                                Aggregation.match(
                                        Criteria.expr(
                                                ComparisonOperators.Eq.valueOf("$data.boost_info.campaign_id")
                                                        .equalTo("$$campaign_id")
                                        )
                                ),

                                Aggregation.project()
                                        .and("_id").as("id")
                                        .and("data.boost_info.campaign_id").as("campaignId")
                                        .and("data.statistics.sold_count").as("soldCount")
                                        .and("data.price_detail.selling_price_max").as("sellingPriceMax")
                                        .and("createdAt").as("createdAt")
                        )
                        .as("productStock")
        );


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

        List<ProductAdsResponseDto> dtos = docs.stream()
                .map(doc -> mapToProductAdsDtoWithCustomRoas(doc, kpi))
                .toList();

        Map<Long, List<ProductAdsResponseDto>> grouped = dtos.stream()
                .filter(d -> d.getCampaignId() != null)
                .collect(Collectors.groupingBy(ProductAdsResponseDto::getCampaignId, HashMap::new, Collectors.toList()));

        List<ProductAdsResponseWrapperDto> wrapperList = grouped.entrySet().stream()
                .map(e -> ProductAdsResponseWrapperDto.builder()
                        .campaignId(e.getKey())
                        .from(from)
                        .to(to)
                        .data(e.getValue())
                        .build()
                )
                .collect(Collectors.toList());


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
        List<ProductKeywordResponseDto> kws = kwDocs.stream()
                .flatMap(kd -> {
                    Object rawData = kd.get("data");
                    List<Document> dataDocs = new ArrayList<>();
                    if (rawData instanceof List) dataDocs.addAll((List<Document>) rawData);
                    else if (rawData instanceof Document) dataDocs.add((Document) rawData);

                    if (!dataDocs.isEmpty()) {
                        return dataDocs.stream()
                                .map(data -> buildKeywordDto(kd, data, kpi))
                                .filter(pk -> Objects.equals(pk.getCampaignId(), dto.getCampaignId())
                                        && Objects.equals(pk.getShopeeFrom(), dto.getShopeeFrom())
                                        && Objects.equals(pk.getShopeeTo(), dto.getShopeeTo()));
                    } else {
                        ProductKeywordResponseDto pk = buildKeywordDto(kd, null, kpi);
                        return Stream.of(pk)
                                .filter(p -> Objects.equals(p.getCampaignId(), dto.getCampaignId())
                                        && Objects.equals(p.getShopeeFrom(), dto.getShopeeFrom())
                                        && Objects.equals(p.getShopeeTo(), dto.getShopeeTo()));
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

    @Override
    public boolean insertCustomRoasForToday(String shopId, Long campaignId, Double customRoas) {
        try {
            // Get current day as Unix timestamp range
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

    private ProductAdsResponseDto mapToProductAdsDtoWithCustomRoas(Document doc, KPI kpi) {
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
        List<Document> productStockDocs = (List<Document>) doc.get("productStock");
        List<ProductStockResponseClassificationDto> productStocks = mapToProductStockDtos(productStockDocs);

        dto.setHasProductStock(!productStocks.isEmpty());
        dto.setProductStocks(productStocks);

        return dto;
    }

    private List<ProductStockResponseClassificationDto> mapToProductStockDtos(List<Document> productStockDocs) {
        if (productStockDocs == null || productStockDocs.isEmpty()) {
            return Collections.emptyList();
        }

        return productStockDocs.stream()
                .map(this::mapToProductStockDto)
                .collect(Collectors.toList());
    }

    private ProductStockResponseClassificationDto mapToProductStockDto(Document doc) {
        ProductStockResponseClassificationDto dto = ProductStockResponseClassificationDto.builder().build();

        dto.setId(getString(doc, "id"));
        dto.setCampaignId(getLong(doc, "campaignId"));
        dto.setSoldCount(getInteger(doc, "soldCount"));
        dto.setSellingPriceMax(getString(doc, "sellingPriceMax"));
        dto.setCreatedAt(getDateTime(doc, "createdAt"));

        Integer soldCount = dto.getSoldCount();
        String sellingPriceMaxStr = dto.getSellingPriceMax();

        if (soldCount != null && sellingPriceMaxStr != null) {
            try {
                Double sellingPriceMax = Double.parseDouble(sellingPriceMaxStr);
                dto.setRevenue((int) (soldCount * sellingPriceMax));
            } catch (NumberFormatException e) {
                dto.setRevenue(0);
            }
        } else {
            dto.setRevenue(0);
        }

        return dto;
    }

    private Integer getInteger(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return null;
    }
}