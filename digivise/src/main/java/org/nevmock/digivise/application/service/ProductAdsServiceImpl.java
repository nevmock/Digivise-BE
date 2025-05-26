package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseWrapperDto;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseDto;
import org.nevmock.digivise.domain.model.KPI;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.model.mongo.ads.ProductAds;
import org.nevmock.digivise.domain.model.mongo.keyword.ProductKeyword;
import org.nevmock.digivise.domain.port.in.ProductAdsService;
import org.nevmock.digivise.domain.port.out.KPIRepository;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.domain.port.out.ProductAdsRepository;
import org.nevmock.digivise.domain.port.out.ProductKeywordRepository;
import org.nevmock.digivise.utils.Recommendation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
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
        baseOps.add(Aggregation.group("data.entry_list.campaign.campaign_id")
                .first("_id").as("id")
                .first("shop_id").as("shopId")
                .first(Aggregation.bind("from", "from").toString()).as("from")
                .first(Aggregation.bind("to",   "to").toString()).as("to")
                .last("createdAt").as("createdAt")
                .first("data.entry_list.title").as("title")
                .first("data.entry_list.image").as("image")
                .first("data.entry_list.state").as("state")
                .sum("data.entry_list.campaign.daily_budget").as("dailyBudget")
                .first("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
                .avg("data.entry_list.report.cpc").as("cpc")
                .avg("data.entry_list.report.broad_gmv").as("acos")
                .avg("data.entry_list.report.click").as("click")
                .avg("data.entry_list.report.ctr").as("ctr")
                .sum("data.entry_list.report.impression").as("impression")
        );
        baseOps.add(Aggregation.project()
                .andExpression("_id").as("campaignId")
                .andInclude("id","shopId","from","to","createdAt","title","image","state",
                        "dailyBudget","biddingStrategy","cpc","acos","click","ctr","impression")
        );
        baseOps.add(Aggregation.lookup(
                "ProductKey",                "campaignId",                "campaign_id",                "keywords"        ));

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

        List<Document> mapped = aggResults.getMappedResults();
        if (mapped.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        Document root = mapped.get(0);
        @SuppressWarnings("unchecked")
        List<Document> docs = (List<Document>) root.get("pagedResults");
        int total = ((List<?>)root.get("countResult")).isEmpty()
                ? 0
                : ((Document)((List<?>)root.get("countResult")).get(0)).getInteger("total");

        List<ProductAdsResponseDto> dtos = new ArrayList<>();
        for (Document doc : docs) {
            ProductAdsResponseDto dto = new ProductAdsResponseDto();
            dto.setId(doc.getObjectId("id").toString());
            dto.setShopeeMerchantId(doc.getString("shopId"));
            dto.setFrom(doc.getString("from"));
            dto.setTo(doc.getString("to"));
            dto.setCreatedAt(doc.getDate("createdAt")
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            Object camp = doc.get("campaignId");
            dto.setCampaignId(camp instanceof Number ? ((Number) camp).longValue() : null);
            dto.setTitle(doc.getString("title"));
            dto.setImage(doc.getString("image"));
            dto.setState(doc.getString("state"));
            Object db = doc.get("dailyBudget");
            dto.setDailyBudget((db instanceof Number ? ((Number) db).doubleValue() : 0.0) / 100000);
            dto.setBiddingStrategy(doc.getString("biddingStrategy"));
            dto.setCpc(doc.getDouble("cpc"));
            dto.setAcos(doc.getDouble("acos"));
            dto.setClick(doc.getDouble("click"));
            dto.setCtr(doc.getDouble("ctr"));
            Object imp = doc.get("impression");
            dto.setImpression(imp instanceof Number ? ((Number) imp).doubleValue() : 0.0);

            @SuppressWarnings("unchecked")
            List<Document> kwDocs = (List<Document>) doc.get("keywords");
            List<ProductKeywordResponseDto> kws = kwDocs.stream()
                    .flatMap(kd -> {
                        Object rawData = kd.get("data");
                        List<Document> dataDocs = new ArrayList<>();
                        if (rawData instanceof List) {
                            dataDocs.addAll((List<Document>) rawData);
                        } else if (rawData instanceof Document) {
                            dataDocs.add((Document) rawData);
                        }

                        if (!dataDocs.isEmpty()) {
                            return dataDocs.stream().map(data -> {
                                ProductKeywordResponseDto pk = new ProductKeywordResponseDto();
                                pk.setId(kd.getObjectId("_id").toString());
                                pk.setShopeeMerchantId(kd.getString("shop_id"));
                                Object cid = kd.get("campaign_id");
                                pk.setCampaignId(cid instanceof Number ? ((Number) cid).longValue() : null);
                                pk.setFrom(kd.getString("from"));
                                pk.setTo(kd.getString("to"));
                                if (kd.getDate("createdAt") != null) {
                                    pk.setCreatedAt(kd.getDate("createdAt")
                                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                                }
                                pk.setKey(data.getString("key"));
                                Document metrics = data.get("metrics", Document.class);
                                if (metrics != null) {
                                    if (pk.getKey().equals("baju koko pria lengan pendek")) {
                                        System.out.println("Campaign ID: " + pk.getCampaignId());
                                    }
                                    Object rawAcos = metrics.get("acos");
                                    if (rawAcos instanceof Double) {
                                        pk.setAcos((Double) rawAcos);
                                    } else if (rawAcos instanceof Integer) {
                                        pk.setAcos(((Integer) rawAcos).doubleValue());
                                    } else if (rawAcos instanceof  Long) {
                                        pk.setAcos(((Long) rawAcos).doubleValue());
                                    }
                                    else {
                                        pk.setAcos(0d);
                                    }

                                    Object rawCpc = metrics.get("cpc");
                                    if (rawCpc instanceof Double) {
                                        pk.setCpc((Double) rawCpc);
                                    } else if (rawCpc instanceof Integer) {
                                        pk.setCpc(((Integer) rawCpc).doubleValue());
                                    } else {
                                        pk.setCpc(0d);
                                    }

                                    Object rawCost = metrics.get("cost");
                                    if (rawCost instanceof Double) {
                                        pk.setCost((Double) rawCost);
                                    } else if (rawCost instanceof Integer) {
                                        pk.setCost(((Integer) rawCost).doubleValue());
                                    } else {
                                        pk.setCost(0d);
                                    }

                                    Object rawImpression = metrics.get("impression");
                                    if (rawImpression instanceof Double) {
                                        pk.setImpression((Double) rawImpression);
                                    } else if (rawImpression instanceof Integer) {
                                        pk.setImpression(((Integer) rawImpression).doubleValue());
                                    } else {
                                        pk.setImpression(0d);
                                    }

                                    Object rawClick = metrics.get("click");
                                    if (rawClick instanceof Double) {
                                        pk.setClick((Double) rawClick);
                                    } else if (rawClick instanceof Integer) {
                                        pk.setClick(((Integer) rawClick).doubleValue());
                                    } else {
                                        pk.setClick(0d);
                                    }

                                    Object rawCtr = metrics.get("ctr");
                                    if (rawCtr instanceof Double) {
                                        pk.setCtr((Double) rawCtr);
                                    } else if (rawCtr instanceof Integer) {
                                        pk.setCtr(((Integer) rawCtr).doubleValue());
                                    } else {
                                        pk.setCtr(0d);
                                    }
                                }

                                Recommendation rec = MathKt.formulateRecommendation(
                                        dto.getCpc(), dto.getAcos(), dto.getClick(), kpi, null, null);
                                dto.setInsight(MathKt.renderInsight(rec));

                                Recommendation rec2 = MathKt.formulateRecommendation(
                                        pk.getCpc(), pk.getAcos(), pk.getClick(), kpi, null, null);

                                pk.setInsight(MathKt.renderInsight(rec2));


                                return pk;
                            });
                        } else {
                            ProductKeywordResponseDto pk = new ProductKeywordResponseDto();
                            pk.setId(kd.getObjectId("_id").toString());
                            pk.setShopeeMerchantId(kd.getString("shop_id"));
                            Object cid = kd.get("campaign_id");
                            pk.setCampaignId(cid instanceof Number ? ((Number) cid).longValue() : null);
                            pk.setFrom(kd.getString("from"));
                            pk.setTo(kd.getString("to"));
                            if (kd.getDate("createdAt") != null) {
                                pk.setCreatedAt(kd.getDate("createdAt")
                                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
                            }
                            pk.setKey(kd.getString("key"));
                            pk.setAcos(kd.getDouble("acos"));
                            pk.setCpc(kd.getDouble("cpc"));
                            pk.setCost(kd.getDouble("cost"));
                            pk.setImpression(kd.getDouble("impression"));
                            pk.setClick(kd.getDouble("click"));
                            pk.setCtr(kd.getDouble("ctr"));

                            Recommendation rec = MathKt.formulateRecommendation(
                                    dto.getCpc(), dto.getAcos(), dto.getClick(), kpi, null, null);
                            dto.setInsight(MathKt.renderInsight(rec));

                            Recommendation rec2 = MathKt.formulateRecommendation(
                                    pk.getCpc(), pk.getAcos(), pk.getClick(), kpi, null, null);

                            pk.setInsight(MathKt.renderInsight(rec2));


                            return Stream.of(pk);
                        }
                    })
                    .collect(Collectors.toList());

            dto.setKeywords(kws);
            dto.setHasKeywords(!kws.isEmpty());


            Recommendation rec = MathKt.formulateRecommendation(
                    dto.getCpc(), dto.getAcos(), dto.getClick(), kpi, null, null);
            dto.setInsight(MathKt.renderInsight(rec));

            dtos.add(dto);
        }

        return new PageImpl<>(dtos, pageable, total);
    }

    private List<ProductKeywordResponseDto> getKeywordsForCampaign(Long campaignId, String shopId, LocalDateTime from, LocalDateTime to) {
        List<AggregationOperation> keywordOperations = new ArrayList<>();

        keywordOperations.add(Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("createdAt").gte(from).lte(to)
                        .and("campaign_id").is(campaignId)
        ));

        keywordOperations.add(Aggregation.unwind("data"));

        keywordOperations.add(Aggregation.project()
                .and("_id").as("id")
                .and("shop_id").as("shopeeMerchantId")
                .and("campaign_id").as("campaignId")
                .and("from").as("from")
                .and("to").as("to")
                .and("createdAt").as("createdAt")
                .and("data.key").as("key")
                .and("data.metrics.broadGmv").as("acos")
                .and("data.metrics.cpc").as("cpc")
                .and("data.metrics.cost").as("cost")
                .and("data.metrics.click").as("click")
                .and("data.metrics.ctr").as("ctr")
                .and("data.metrics.impression").as("impression")
        );

        AggregationResults<ProductKeywordResponseDto> keywordResults = mongoTemplate.aggregate(
                Aggregation.newAggregation(keywordOperations),
                "ProductKey",
                ProductKeywordResponseDto.class
        );

        return keywordResults.getMappedResults();
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

//    @Override
//    public Page<ProductAdsResponseDto> findByRangeAggTotal(
//            String shopId,
//            String biddingStrategy,
//            LocalDateTime from,
//            LocalDateTime to,
//            Pageable pageable
//    ) {
//        Merchant merchant = merchantRepository
//                .findByShopeeMerchantId(shopId)
//                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
//        KPI kpi = kpiRepository
//                .findByMerchantId(merchant.getId())
//                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));
//
//        List<AggregationOperation> baseOps = new ArrayList<>();
//
//        baseOps.add(Aggregation.match(
//                Criteria.where("shop_id").is(shopId)
//                        .and("createdAt").gte(from).lte(to)
//        ));
//        baseOps.add(Aggregation.unwind("data.entry_list"));
//        if (biddingStrategy != null) {
//            baseOps.add(Aggregation.match(
//                    Criteria.where("data.entry_list.manual_product_ads.bidding_strategy")
//                            .is(biddingStrategy)
//            ));
//        }
//        baseOps.add(Aggregation.project()
//                .and("_id").as("id")
//                .and("shop_id").as("shopId")
//                .and("createdAt").as("createdAt")
//                .and("data.entry_list.campaign.campaign_id").as("campaignId")
//                .and("data.entry_list.title").as("title")
//                .and("data.entry_list.image").as("image")
//                .and("data.entry_list.state").as("state")
//                .and("data.entry_list.campaign.daily_budget").as("dailyBudget")
//                .and("data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
//                .and("data.entry_list.report.cpc").as("cpc")
//                .and("data.entry_list.report.broad_gmv").as("acos")
//                .and("data.entry_list.report.click").as("click")
//                .and("data.entry_list.report.ctr").as("ctr")
//                .and("data.entry_list.report.impression").as("impression")
//                .and("data.entry_list.report.broad_roi").as("roas")
//                .andExpression("{$literal: '" + from.toString() + "'}") .as("from")
//                .andExpression("{$literal: '" + to.toString() + "'}") .as("to")
//        );
//
//        baseOps.add(Aggregation.lookup(
//                "ProductKey",                "campaignId",                "campaign_id",                "keywords"        ));
//
//        FacetOperation facet = Aggregation.facet(
//                        Aggregation.skip((long) pageable.getOffset()),
//                        Aggregation.limit(pageable.getPageSize())
//                ).as("pagedResults")
//                .and(Aggregation.count().as("total")).as("countResult");
//
//        List<AggregationOperation> fullPipeline = new ArrayList<>(baseOps);
//        fullPipeline.add(facet);
//
//        AggregationResults<Document> aggResults = mongoTemplate.aggregate(
//                Aggregation.newAggregation(fullPipeline),
//                "ProductAds",
//                Document.class
//        );
//
//        List<Document> mapped = aggResults.getMappedResults();
//        if (mapped.isEmpty()) {
//            return new PageImpl<>(Collections.emptyList(), pageable, 0);
//        }
//
//        Document root = mapped.get(0);
//        @SuppressWarnings("unchecked")
//        List<Document> docs = (List<Document>) root.get("pagedResults");
//        int total = ((List<?>)root.get("countResult")).isEmpty()
//                ? 0
//                : ((Document)((List<?>)root.get("countResult")).get(0)).getInteger("total");
//
//        List<ProductAdsResponseDto> dtos = new ArrayList<>();
//        for (Document doc : docs) {
//            ProductAdsResponseDto dto = new ProductAdsResponseDto();
//            dto.setId(doc.getObjectId("id").toString());
//            dto.setShopeeMerchantId(doc.getString("shopId"));
//            dto.setFrom(doc.getString("from"));
//            dto.setTo(doc.getString("to"));
//            dto.setCreatedAt(doc.getDate("createdAt")
//                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
//            Object camp = doc.get("campaignId");
//            dto.setCampaignId(camp instanceof Number ? ((Number) camp).longValue() : null);
//            dto.setTitle(doc.getString("title"));
//            dto.setImage(doc.getString("image"));
//            dto.setState(doc.getString("state"));
//            Object db = doc.get("dailyBudget");
//            dto.setDailyBudget((db instanceof Number ? ((Number) db).doubleValue() : 0.0) / 100000);
//            dto.setBiddingStrategy(doc.getString("biddingStrategy"));
//            Object cpc = doc.get("cpc");
//            dto.setCpc(cpc instanceof Number ? ((Number) cpc).doubleValue() : null);
//            Object acos = doc.get("acos");
//            dto.setAcos(acos instanceof Number ? ((Number) acos).doubleValue() : null);
//            Object click = doc.get("click");
//            dto.setClick(click instanceof Number ? ((Number) click).doubleValue() : null);
//            Object ctr = doc.get("ctr");
//            dto.setCtr(ctr instanceof Number ? ((Number) ctr).doubleValue() : null);
//            Object imp = doc.get("impression");
//            dto.setImpression(imp instanceof Number ? ((Number) imp).doubleValue() : null);
//            Object roas = doc.get("roas");
//            dto.setRoas(roas instanceof Number ? ((Number) roas).doubleValue() : null);
//
//            dto.setInsightBudget(
//                    MathKt.renderInsight(
//                            MathKt.formulateRecommendation(
//                                    dto.getCpc(), dto.getAcos(), dto.getClick(), kpi, dto.getRoas(), dto.getDailyBudget()
//                            )
//                    )
//            );
//
//            @SuppressWarnings("unchecked")
//            List<Document> kwDocs = (List<Document>) doc.get("keywords");
//            List<ProductKeywordResponseDto> kws = kwDocs.stream()
//                    .flatMap(kd -> {
//                        Object rawData = kd.get("data");
//                        List<Document> dataDocs = new ArrayList<>();
//                        if (rawData instanceof List) {
//                            dataDocs.addAll((List<Document>) rawData);
//                        } else if (rawData instanceof Document) {
//                            dataDocs.add((Document) rawData);
//                        }
//
//                        if (!dataDocs.isEmpty()) {
//                            return dataDocs.stream().map(data -> {
//                                ProductKeywordResponseDto pk = new ProductKeywordResponseDto();
//                                pk.setId(kd.getObjectId("_id").toString());
//                                pk.setShopeeMerchantId(kd.getString("shop_id"));
//                                Object cid = kd.get("campaign_id");
//                                pk.setCampaignId(cid instanceof Number ? ((Number) cid).longValue() : null);
//                                pk.setFrom(kd.getString("from"));
//                                pk.setTo(kd.getString("to"));
//                                if (kd.getDate("createdAt") != null) {
//                                    pk.setCreatedAt(kd.getDate("createdAt")
//                                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
//                                }
//                                pk.setKey(data.getString("key"));
//                                Document metrics = data.get("metrics", Document.class);
//                                if (metrics != null) {
//                                    Object rawAcos = metrics.get("broad_gmv");
//                                    if (rawAcos instanceof Double) {
//                                        pk.setAcos((Double) rawAcos);
//                                    } else if (rawAcos instanceof Integer) {
//                                        pk.setAcos(((Integer) rawAcos).doubleValue());
//                                    } else if (rawAcos instanceof  Long) {
//                                        pk.setAcos(((Long) rawAcos).doubleValue());
//                                    }
//                                    else {
//                                        pk.setAcos(null);
//                                    }
//
//                                    Object rawCpc = metrics.get("cpc");
//                                    if (rawCpc instanceof Double) {
//                                        pk.setCpc((Double) rawCpc);
//                                    } else if (rawCpc instanceof Integer) {
//                                        pk.setCpc(((Integer) rawCpc).doubleValue());
//                                    } else {
//                                        pk.setCpc(null);
//                                    }
//
//                                    Object rawCost = metrics.get("cost");
//                                    if (rawCost instanceof Double) {
//                                        pk.setCost((Double) rawCost);
//                                    } else if (rawCost instanceof Integer) {
//                                        pk.setCost(((Integer) rawCost).doubleValue());
//                                    } else {
//                                        pk.setCost(null);
//                                    }
//
//                                    Object rawImpression = metrics.get("impression");
//                                    if (rawImpression instanceof Double) {
//                                        pk.setImpression((Double) rawImpression);
//                                    } else if (rawImpression instanceof Integer) {
//                                        pk.setImpression(((Integer) rawImpression).doubleValue());
//                                    } else {
//                                        pk.setImpression(null);
//                                    }
//
//                                    Object rawClick = metrics.get("click");
//                                    if (rawClick instanceof Double) {
//                                        pk.setClick((Double) rawClick);
//                                    } else if (rawClick instanceof Integer) {
//                                        pk.setClick(((Integer) rawClick).doubleValue());
//                                    } else {
//                                        pk.setClick(null);
//                                    }
//
//                                    Object rawCtr = metrics.get("ctr");
//                                    if (rawCtr instanceof Double) {
//                                        pk.setCtr((Double) rawCtr);
//                                    } else if (rawCtr instanceof Integer) {
//                                        pk.setCtr(((Integer) rawCtr).doubleValue());
//                                    } else {
//                                        pk.setCtr(null);
//                                    }
//                                }
//
//                                Recommendation rec = MathKt.formulateRecommendation(
//                                        dto.getCpc(), dto.getAcos(), dto.getClick(), kpi, null, null);
//                                dto.setInsight(MathKt.renderInsight(rec));
//
//                                Recommendation rec2 = MathKt.formulateRecommendation(
//                                        pk.getCpc(), pk.getAcos(), pk.getClick(), kpi, null, null);
//
//                                pk.setInsight(MathKt.renderInsight(rec2));
//
//                                return pk;
//                            });
//                        } else {
//                            ProductKeywordResponseDto pk = new ProductKeywordResponseDto();
//                            pk.setId(kd.getObjectId("_id").toString());
//                            pk.setShopeeMerchantId(kd.getString("shop_id"));
//                            Object cid = kd.get("campaign_id");
//                            pk.setCampaignId(cid instanceof Number ? ((Number) cid).longValue() : null);
//                            pk.setFrom(kd.getString("from"));
//                            pk.setTo(kd.getString("to"));
//                            if (kd.getDate("createdAt") != null) {
//                                pk.setCreatedAt(kd.getDate("createdAt")
//                                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
//                            }
//                            pk.setKey(kd.getString("key"));
//                            pk.setAcos(kd.getDouble("acos"));
//                            pk.setCpc(kd.getDouble("cpc"));
//                            pk.setCost(kd.getDouble("cost"));
//                            pk.setImpression(kd.getDouble("impression"));
//                            pk.setClick(kd.getDouble("click"));
//                            pk.setCtr(kd.getDouble("ctr"));
//
//                            Recommendation rec = MathKt.formulateRecommendation(
//                                    dto.getCpc(), dto.getAcos(), dto.getClick(), kpi, null, null);
//                            dto.setInsight(MathKt.renderInsight(rec));
//
//                            Recommendation rec2 = MathKt.formulateRecommendation(
//                                    pk.getCpc(), pk.getAcos(), pk.getClick(), kpi, null, null);
//
//                            pk.setInsight(MathKt.renderInsight(rec2));
//
//
//                            return Stream.of(pk);
//                        }
//                    })
//                    .collect(Collectors.toList());
//
//            dto.setKeywords(kws);
//            dto.setHasKeywords(!kws.isEmpty());
//
//            Recommendation rec = MathKt.formulateRecommendation(
//                    dto.getCpc(), dto.getAcos(), dto.getClick(), kpi, null, null);
//            dto.setInsight(MathKt.renderInsight(rec));
//
//
//            dtos.add(dto);
//        }
//
//        return new PageImpl<>(dtos, pageable, total);
//    }

    @Override
    public Page<ProductAdsResponseWrapperDto> findByRangeAggTotal(
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
                .and("data.entry_list.report.broad_gmv").as("acos")
                .and("data.entry_list.report.click").as("click")
                .and("data.entry_list.report.ctr").as("ctr")
                .and("data.entry_list.report.impression").as("impression")
                .and("data.entry_list.report.broad_roi").as("roas")
                .and("data.from").as("shopeeFrom")
                .and("data.to").as("shopeeTo")
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
        // Map to DTOs
        List<ProductAdsResponseDto> dtos = docs.stream()
                .map(doc -> mapToProductAdsDto(doc, kpi))
                .toList();

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
        dto.setCpc(getDouble(doc, "cpc"));
        dto.setAcos(getDouble(doc, "acos"));
        dto.setClick(getDouble(doc, "click"));
        dto.setCtr(getDouble(doc, "ctr"));
        dto.setImpression(getDouble(doc, "impression"));
        dto.setRoas(getDouble(doc, "roas"));
        dto.setShopeeFrom(getString(doc, "shopeeFrom"));
        dto.setShopeeTo(getString(doc, "shopeeTo"));
        dto.setInsightBudget(
                MathKt.renderInsight(
                        MathKt.formulateRecommendation(
                                dto.getCpc(), dto.getAcos(), dto.getClick(), kpi, dto.getRoas(), dto.getDailyBudget()
                        )
                )
        );

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
        ProductKeywordResponseDto pk = ProductKeywordResponseDto.builder().build();
        pk.setId(getString(kd, "_id"));
        pk.setShopeeMerchantId(getString(kd, "shop_id"));
        pk.setCampaignId(getLong(kd, "campaign_id"));
        pk.setFrom(getString(kd, "from"));
        pk.setTo(getString(kd, "to"));
        pk.setCreatedAt(getDateTime(kd, "createdAt"));
        pk.setKey(data != null ? getString(data, "key") : getString(kd, "key"));
        Document metrics = data != null ? data.get("metrics", Document.class) : kd;
        pk.setAcos(getDouble(metrics, "broad_gmv"));
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
}
