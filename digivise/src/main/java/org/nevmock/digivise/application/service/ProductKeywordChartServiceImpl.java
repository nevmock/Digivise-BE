package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordChartResponseDto;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordChartWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductKeywordChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductKeywordChartServiceImpl implements ProductKeywordChartService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<ProductKeywordChartWrapperDto> findMetricsByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            Long campaignId,
            String key            // nullable: jika null atau kosong, tampilkan semua keywords
    ) {
        long fromTs = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTs   = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        // 1. Match shop, campaign, date range
        MatchOperation matchBase = Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("campaign_id").is(campaignId)
                        .and("from").gte(fromTs).lte(toTs)
        );

        // 2. Unwind nested data array
        UnwindOperation unwind = Aggregation.unwind("data.data");

        // 3. (Optional) Filter by key, hanya jika key tidak null/empty
        List<AggregationOperation> ops = new ArrayList<>();
        ops.add(matchBase);
        ops.add(unwind);
        if (key != null && !key.isEmpty()) {
            ops.add(Aggregation.match(Criteria.where("data.data.key").is(key)));
        }

        // 4. Project fields
        ProjectionOperation project = Aggregation.project()
                .and("data.data.key").as("key")
                .and("data.data.metrics.impression").as("impression")
                .and("data.data.metrics.click").as("click")
                .and("data.data.metrics.ctr").as("ctr")
                .and("data.data.metrics.checkout").as("checkout")
                .and("data.data.metrics.broad_order_amount").as("broadOrderAmount")
                .and("data.data.metrics.broad_gmv").divide(100000.0).as("broadGmv")
                .and("data.data.metrics.cost").divide(100000.0).as("dailyBudget")
                .and("data.data.metrics.broad_roi").as("roas")
                .and("campaign_id").as("campaignId")
                .and("type").as("type")
                .and("from").as("shopeeFrom")
                .and("to").as("shopeeTo")
                .and("createdAt").as("createdAt");
        ops.add(project);

        // Execute aggregation
        Aggregation aggregation = Aggregation.newAggregation(ops);
        List<Document> results = mongoTemplate.aggregate(
                aggregation,
                "ProductKey",
                Document.class
        ).getMappedResults();

        // Map to DTOs
        List<ProductKeywordChartResponseDto> allDtos = results.stream()
                .map(this::toChartDto)
                .filter(dto -> dto.getCampaignId() != null)
                .collect(Collectors.toList());

        List<ProductKeywordChartWrapperDto> wrappers = new ArrayList<>();

        if (key != null && !key.isEmpty()) {
            // Single key: group by campaign only
            Map<Long, List<ProductKeywordChartResponseDto>> byCampaign = allDtos.stream()
                    .collect(Collectors.groupingBy(ProductKeywordChartResponseDto::getCampaignId, LinkedHashMap::new, Collectors.toList()));
            byCampaign.forEach((campId, list) -> wrappers.add(
                    new ProductKeywordChartWrapperDto(campId, from, to, list)
            ));
        } else {
            // No key filter: group by campaign and key
            Map<Long, Map<String, List<ProductKeywordChartResponseDto>>> nested = allDtos.stream()
                    .collect(Collectors.groupingBy(
                            ProductKeywordChartResponseDto::getCampaignId,
                            LinkedHashMap::new,
                            Collectors.groupingBy(ProductKeywordChartResponseDto::getKey, LinkedHashMap::new, Collectors.toList())
                    ));
            nested.forEach((campId, keyMap) -> {
                keyMap.forEach((k, list) -> wrappers.add(
                        new ProductKeywordChartWrapperDto(campId, from, to, k, list)
                ));
            });
        }

        return wrappers;
    }

    private ProductKeywordChartResponseDto toChartDto(Document doc) {
        ProductKeywordChartResponseDto dto = new ProductKeywordChartResponseDto();
        dto.setKey(getString(doc, "key"));
        dto.setCampaignId(getLong(doc, "campaignId"));
        dto.setType(getString(doc, "type"));
        dto.setImpression(getDouble(doc, "impression"));
        dto.setClick(getDouble(doc, "click"));
        dto.setCtr(getDouble(doc, "ctr"));
        dto.setCheckout(getDouble(doc, "checkout"));
        dto.setBroadOrderAmount(getDouble(doc, "broadOrderAmount"));
        dto.setBroadGmv(getDouble(doc, "broadGmv"));
        dto.setDailyBudget(getDouble(doc, "dailyBudget"));
        dto.setRoas(getDouble(doc, "roas"));
        dto.setShopeeFrom(getLong(doc, "shopeeFrom"));
        dto.setShopeeTo(getLong(doc, "shopeeTo"));
        dto.setCreatedAt(getDateTime(doc, "createdAt"));
        return dto;
    }

    private String getString(Document doc, String key) {
        Object v = doc.get(key);
        return v instanceof String ? (String) v : null;
    }

    private Long getLong(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        if (v instanceof String) {
            try { return Long.parseLong((String) v);} catch (NumberFormatException ex) { }
        }
        return null;
    }

    private Double getDouble(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof String) {
            try { return Double.parseDouble((String) v);} catch (NumberFormatException ex) { }
        }
        return null;
    }

    private LocalDateTime getDateTime(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Date) {
            return ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }
}

