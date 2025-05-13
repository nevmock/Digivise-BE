package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsResponseDto;
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
        return productAdsRepository.findAll().stream()
                .flatMap(ad -> {
                    String id = ad.getId();
                    String shopeeMerchantId = ad.getShopId();
                    String toLoc = ad.getTo();
                    String fromLoc = ad.getFrom();
                    LocalDateTime createdAt = ad.getCreatedAt();
                    if (ad.getData() != null && ad.getData().getProfileInfo().getData() != null) {
                        return ad.getData().getProfileInfo().getData().getEntry_list().stream()
                                .map(e -> {
                                    ProductAdsResponseDto dto = new ProductAdsResponseDto();
                                    dto.setId(id);
                                    dto.setShopeeMerchantId(shopeeMerchantId);
                                    dto.setFrom(fromLoc);
                                    dto.setTo(toLoc);
                                    dto.setCreatedAt(createdAt);
                                    dto.setCampaignId(e.getCampaign().getCampaignId());
                                    dto.setAcos(e.getReport().getBroad_gmv());
                                    dto.setCpc(e.getReport().getCpc());
                                    dto.setState(e.getState());
                                    dto.setImpression(e.getReport().getImpression());
                                    return dto;
                                });
                    }
                    ProductAdsResponseDto baseDto = new ProductAdsResponseDto();
                    baseDto.setId(id);
                    baseDto.setShopeeMerchantId(shopeeMerchantId);
                    baseDto.setFrom(fromLoc);
                    baseDto.setTo(toLoc);
                    baseDto.setCreatedAt(createdAt);
                    return List.of(baseDto).stream();
                })
                .collect(Collectors.toList());
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
        baseOps.add(Aggregation.unwind("data.profile_info.data.entry_list"));
        if (biddingStrategy != null) {
            baseOps.add(Aggregation.match(
                    Criteria.where("data.profile_info.data.entry_list.manual_product_ads.bidding_strategy")
                            .is(biddingStrategy)
            ));
        }
        baseOps.add(Aggregation.group("data.profile_info.data.entry_list.campaign.campaign_id")
                .first("_id").as("id")
                .first("shop_id").as("shopId")
                .first(Aggregation.bind("from", "from").toString()).as("from")
                .first(Aggregation.bind("to",   "to").toString()).as("to")
                .last("createdAt").as("createdAt")
                .first("data.profile_info.data.entry_list.title").as("title")
                .first("data.profile_info.data.entry_list.image").as("image")
                .first("data.profile_info.data.entry_list.state").as("state")
                .first("data.profile_info.data.entry_list.campaign.daily_budget").as("dailyBudget")
                .first("data.profile_info.data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
                .avg("data.profile_info.data.entry_list.report.cpc").as("cpc")
                .avg("data.profile_info.data.entry_list.report.broad_gmv").as("acos")
                .avg("data.profile_info.data.entry_list.report.click").as("click")
                .avg("data.profile_info.data.entry_list.report.ctr").as("ctr")
                .sum("data.profile_info.data.entry_list.report.impression").as("impression")
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
            dto.setDailyBudget(db instanceof Number ? ((Number) db).doubleValue() : 0.0);
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
                                    Object rawAcos = metrics.get("broad_gmv");
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
                            return Stream.of(pk);
                        }
                    })
                    .collect(Collectors.toList());

            dto.setKeywords(kws);
            dto.setHasKeywords(!kws.isEmpty());

            if (dto.getCpc() > kpi.getMaxCpc()) {
                Recommendation rec = MathKt.formulateRecommendation(
                        dto.getCpc(), dto.getAcos(), dto.getClick(), kpi);
                dto.setInsight(MathKt.renderInsight(rec));
            }

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

        List<ProductAds> allAds;

        if (biddingStrategy != null) {
            allAds = productAdsRepository
                    .findByShopAndDateAndBiddingStrategy(shopId, biddingStrategy, from, to, Pageable.unpaged())
                    .getContent();
        } else {
            allAds = productAdsRepository
                    .findByShopIdAndCreatedAtBetween(shopId, from, to, Pageable.unpaged())
                    .getContent();
        }

        Merchant merchant = merchantRepository
                .findByShopeeMerchantId(shopId)
                .orElseThrow(() -> new RuntimeException("Merchant not found with ID: " + shopId));

        KPI kpi = kpiRepository
                .findByMerchantId(merchant.getId())
                .orElseThrow(() -> new RuntimeException("KPI not found for Merchant ID: " + merchant.getId()));

        List<ProductKeyword> productKeywords = productKeywordRepository
                .findByShopIdAndCreatedAtBetween(shopId, from, to);

        List<ProductKeywordResponseDto> allKeywordDtos = productKeywords.stream()
                .flatMap(pk -> {
                    Long campId = pk.getCampaignId();
                    return pk.getData().stream()
                            .map(dw -> {
                                ProductKeywordResponseDto kdto = new ProductKeywordResponseDto();
                                kdto.setCampaignId(campId);
                                kdto.setKey(dw.getKey());
                                kdto.setAcos(dw.getMetrics().getBroadGmv());
                                kdto.setCpc(dw.getMetrics().getCpc());
                                kdto.setId(pk.getId());
                                kdto.setShopeeMerchantId(pk.getShopId());
                                kdto.setFrom(pk.getFrom());
                                kdto.setTo(pk.getTo());
                                kdto.setCreatedAt(pk.getCreatedAt());
                                kdto.setCost(dw.getMetrics().getCost());
                                kdto.setClick(dw.getMetrics().getClick());
                                kdto.setImpression(dw.getMetrics().getImpression());
                                return kdto;
                            });
                })
                .toList();

        Map<Long, List<ProductKeywordResponseDto>> keywordsByCampaign = allKeywordDtos.stream()
                .collect(Collectors.groupingBy(ProductKeywordResponseDto::getCampaignId));

        List<ProductAdsResponseDto> allDtos = allAds.stream()
                .flatMap(ad -> {
                    String id = ad.getId();
                    String shopeeMerchantId = ad.getShopId();
                    String toLoc = ad.getTo();
                    String fromLoc = ad.getFrom();
                    LocalDateTime createdAt = ad.getCreatedAt();

                    if (ad.getData() != null && ad.getData().getProfileInfo().getData() != null) {

                        return ad.getData().getProfileInfo().getData().getEntry_list().stream()
                                .filter(e -> {
                                    if (biddingStrategy != null) {
                                        return e.getManualProductAds() != null &&
                                                biddingStrategy.equals(e.getManualProductAds().getBiddingStrategy());
                                    }
                                    return true;
                                })
                                .map(e -> {
                                    ProductAdsResponseDto dto = new ProductAdsResponseDto();
                                    dto.setId(id);
                                    dto.setShopeeMerchantId(shopeeMerchantId);
                                    dto.setFrom(fromLoc);
                                    dto.setTo(toLoc);
                                    dto.setCreatedAt(createdAt);
                                    dto.setCampaignId(e.getCampaign().getCampaignId());
                                    dto.setAcos(e.getReport().getBroad_gmv());
                                    dto.setCpc(e.getReport().getCpc());
                                    dto.setDailyBudget(e.getCampaign().getDailyBudget());
                                    dto.setImage(e.getImage());
                                    dto.setTitle(e.getTitle());
                                    dto.setClick(e.getReport().getClick());
                                    dto.setCtr(e.getReport().getCtr());
                                    dto.setState(e.getState());
                                    dto.setImpression(e.getReport().getImpression());
                                    if (e.getManualProductAds() != null) {
                                        dto.setBiddingStrategy(e.getManualProductAds().getBiddingStrategy());
                                    }
                                    return dto;
                                });
                    }

                    ProductAdsResponseDto baseDto = new ProductAdsResponseDto();
                    baseDto.setId(id);
                    baseDto.setShopeeMerchantId(shopeeMerchantId);
                    baseDto.setFrom(fromLoc);
                    baseDto.setTo(toLoc);
                    baseDto.setCreatedAt(createdAt);
                    return Stream.of(baseDto);
                })
                .collect(Collectors.toList());

        Map<Long, List<ProductAdsResponseDto>> campaignGroups = allDtos.stream()
                .filter(dto -> dto.getCampaignId() != null)
                .collect(Collectors.groupingBy(ProductAdsResponseDto::getCampaignId));

        List<ProductAdsResponseDto> uniqueCampaignDtos = campaignGroups.entrySet().stream()
                .map(entry -> {
                    Long campaignId = entry.getKey();
                    List<ProductAdsResponseDto> dtoList = entry.getValue();
                    ProductAdsResponseDto combinedDto = dtoList.get(0);

                    double avgCpc = dtoList.stream().mapToDouble(ProductAdsResponseDto::getCpc).filter(d -> !Double.isNaN(d)).average().orElse(0);
                    double avgAcos = dtoList.stream().mapToDouble(ProductAdsResponseDto::getAcos).filter(d -> !Double.isNaN(d)).average().orElse(0);
                    double avgClick = dtoList.stream().mapToDouble(ProductAdsResponseDto::getClick).filter(d -> !Double.isNaN(d)).average().orElse(0);
                    double avgCtr = dtoList.stream().mapToDouble(ProductAdsResponseDto::getCtr).filter(d -> !Double.isNaN(d)).average().orElse(0);
                    double totalImpression = dtoList.stream().mapToDouble(ProductAdsResponseDto::getImpression).filter(d -> !Double.isNaN(d)).sum();

                    combinedDto.setCpc(avgCpc);
                    combinedDto.setAcos(avgAcos);
                    combinedDto.setClick(avgClick);
                    combinedDto.setCtr(avgCtr);
                    combinedDto.setImpression(totalImpression);
                    combinedDto.setCreatedAt(dtoList.stream().map(ProductAdsResponseDto::getCreatedAt).max(LocalDateTime::compareTo).orElse(combinedDto.getCreatedAt()));

                    return combinedDto;
                })
                .collect(Collectors.toList());

        double overallAvgCpc = uniqueCampaignDtos.stream().mapToDouble(ProductAdsResponseDto::getCpc).average().orElse(0);
        if (overallAvgCpc > kpi.getMaxCpc()) {
            uniqueCampaignDtos.forEach(dto -> {
                Recommendation rec = MathKt.formulateRecommendation(dto.getCpc(), dto.getAcos(), dto.getClick(), kpi);
                dto.setInsight(MathKt.renderInsight(rec));
            });
        }

        uniqueCampaignDtos.forEach(adDto -> {
            List<ProductKeywordResponseDto> kws = keywordsByCampaign.getOrDefault(adDto.getCampaignId(), Collections.emptyList());
            adDto.setKeywords(kws);

            if (!kws.isEmpty()) {
                adDto.setHasKeywords(true);
            }
        });

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), uniqueCampaignDtos.size());
        List<ProductAdsResponseDto> pageContent = start < uniqueCampaignDtos.size() ?
                uniqueCampaignDtos.subList(start, end) : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, uniqueCampaignDtos.size());
    }

    @Override
    public Page<ProductAdsResponseDto> findByRangeAggTotal(
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
        baseOps.add(Aggregation.unwind("data.profile_info.data.entry_list"));
        if (biddingStrategy != null) {
            baseOps.add(Aggregation.match(
                    Criteria.where("data.profile_info.data.entry_list.manual_product_ads.bidding_strategy")
                            .is(biddingStrategy)
            ));
        }
        baseOps.add(Aggregation.project()
                .and("_id").as("id")
                .and("shop_id").as("shopId")
                .and("createdAt").as("createdAt")
                .and("data.profile_info.data.entry_list.campaign.campaign_id").as("campaignId")
                .and("data.profile_info.data.entry_list.title").as("title")
                .and("data.profile_info.data.entry_list.image").as("image")
                .and("data.profile_info.data.entry_list.state").as("state")
                .and("data.profile_info.data.entry_list.campaign.daily_budget").as("dailyBudget")
                .and("data.profile_info.data.entry_list.manual_product_ads.bidding_strategy").as("biddingStrategy")
                .and("data.profile_info.data.entry_list.report.cpc").as("cpc")
                .and("data.profile_info.data.entry_list.report.broad_gmv").as("acos")
                .and("data.profile_info.data.entry_list.report.click").as("click")
                .and("data.profile_info.data.entry_list.report.ctr").as("ctr")
                .and("data.profile_info.data.entry_list.report.impression").as("impression")
                .andExpression("{$literal: '" + from.toString() + "'}") .as("from")
                .andExpression("{$literal: '" + to.toString() + "'}") .as("to")
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
            dto.setDailyBudget(db instanceof Number ? ((Number) db).doubleValue() : 0.0);
            dto.setBiddingStrategy(doc.getString("biddingStrategy"));
            Object cpc = doc.get("cpc");
            dto.setCpc(cpc instanceof Number ? ((Number) cpc).doubleValue() : null);
            Object acos = doc.get("acos");
            dto.setAcos(acos instanceof Number ? ((Number) acos).doubleValue() : null);
            Object click = doc.get("click");
            dto.setClick(click instanceof Number ? ((Number) click).doubleValue() : null);
            Object ctr = doc.get("ctr");
            dto.setCtr(ctr instanceof Number ? ((Number) ctr).doubleValue() : null);
            Object imp = doc.get("impression");
            dto.setImpression(imp instanceof Number ? ((Number) imp).doubleValue() : null);

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
                                    Object rawAcos = metrics.get("broad_gmv");
                                    if (rawAcos instanceof Double) {
                                        pk.setAcos((Double) rawAcos);
                                    } else if (rawAcos instanceof Integer) {
                                        pk.setAcos(((Integer) rawAcos).doubleValue());
                                    } else if (rawAcos instanceof  Long) {
                                        pk.setAcos(((Long) rawAcos).doubleValue());
                                    }
                                    else {
                                        pk.setAcos(null);
                                    }

                                    Object rawCpc = metrics.get("cpc");
                                    if (rawCpc instanceof Double) {
                                        pk.setCpc((Double) rawCpc);
                                    } else if (rawCpc instanceof Integer) {
                                        pk.setCpc(((Integer) rawCpc).doubleValue());
                                    } else {
                                        pk.setCpc(null);
                                    }

                                    Object rawCost = metrics.get("cost");
                                    if (rawCost instanceof Double) {
                                        pk.setCost((Double) rawCost);
                                    } else if (rawCost instanceof Integer) {
                                        pk.setCost(((Integer) rawCost).doubleValue());
                                    } else {
                                        pk.setCost(null);
                                    }

                                    Object rawImpression = metrics.get("impression");
                                    if (rawImpression instanceof Double) {
                                        pk.setImpression((Double) rawImpression);
                                    } else if (rawImpression instanceof Integer) {
                                        pk.setImpression(((Integer) rawImpression).doubleValue());
                                    } else {
                                        pk.setImpression(null);
                                    }

                                    Object rawClick = metrics.get("click");
                                    if (rawClick instanceof Double) {
                                        pk.setClick((Double) rawClick);
                                    } else if (rawClick instanceof Integer) {
                                        pk.setClick(((Integer) rawClick).doubleValue());
                                    } else {
                                        pk.setClick(null);
                                    }

                                    Object rawCtr = metrics.get("ctr");
                                    if (rawCtr instanceof Double) {
                                        pk.setCtr((Double) rawCtr);
                                    } else if (rawCtr instanceof Integer) {
                                        pk.setCtr(((Integer) rawCtr).doubleValue());
                                    } else {
                                        pk.setCtr(null);
                                    }
                                }
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
                            return Stream.of(pk);
                        }
                    })
                    .collect(Collectors.toList());

            dto.setKeywords(kws);
            dto.setHasKeywords(!kws.isEmpty());

            if (dto.getCpc() != null && dto.getCpc() > kpi.getMaxCpc()) {
                Recommendation rec = MathKt.formulateRecommendation(
                        dto.getCpc(), dto.getAcos(), dto.getClick(), kpi);
                dto.setInsight(MathKt.renderInsight(rec));
            }

            dtos.add(dto);
        }

        return new PageImpl<>(dtos, pageable, total);
    }

}
