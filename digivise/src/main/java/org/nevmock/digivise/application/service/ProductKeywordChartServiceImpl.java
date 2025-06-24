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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductKeywordChartServiceImpl implements ProductKeywordChartService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<ProductKeywordChartWrapperDto> findMetricsByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        long fromTs = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTs = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        MatchOperation matchStage = Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("from").gte(fromTs).lte(toTs)
        );

        UnwindOperation unwind = Aggregation.unwind("data.data");

        ProjectionOperation project = Aggregation.project()
                .and("data.data.key").as("key")
                .and("data.data.metrics.impression").as("impression")
                .and("data.data.metrics.click").as("click")
                .and("data.data.metrics.ctr").as("ctr")
                .and("data.data.metrics.checkout").as("checkout")
                .and("data.data.metrics.broad_order_amount").as("broadOrderAmount")
                .and("data.data.metrics.broad_gmv").as("broadGmv")
                .and("data.data.metrics.cost").as("dailyBudget")
                .and("data.data.metrics.broad_roi").as("roas")
                .and("campaign_id").as("campaignId")
                .and("type").as("type")
                .and("from").as("shopeeFrom")
                .and("to").as("shopeeTo")
                .and("createdAt").as("createdAt");

        Aggregation aggregation = Aggregation.newAggregation(
                matchStage,
                unwind,
                project
        );

        List<Document> results = mongoTemplate.aggregate(
                aggregation,
                "ProductKey",
                Document.class
        ).getMappedResults();

        List<ProductKeywordChartResponseDto> allDtos = results.stream()
                .map(this::toChartDto)
                .collect(Collectors.toList());

        Map<Long, List<ProductKeywordChartResponseDto>> grouped = allDtos.stream()
                .filter(dto -> dto.getCampaignId() != null)
                .collect(Collectors.groupingBy(
                        ProductKeywordChartResponseDto::getCampaignId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(e -> new ProductKeywordChartWrapperDto(
                        e.getKey(),
                        from,
                        to,
                        e.getValue()
                ))
                .collect(Collectors.toList());
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
        if (v instanceof Number) {
            return ((Number) v).longValue();
        } else if (v instanceof String) {
            try {
                return Long.parseLong((String) v);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private Double getDouble(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        } else if (v instanceof String) {
            try {
                return Double.parseDouble((String) v);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private LocalDateTime getDateTime(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Date) {
            return ((Date) v)
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
        }
        return null;
    }
}
