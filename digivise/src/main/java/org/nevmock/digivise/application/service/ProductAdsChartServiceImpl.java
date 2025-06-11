package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsChartResponseDto;
import org.nevmock.digivise.application.dto.product.ads.ProductAdsChartWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductAdsChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
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
public class ProductAdsChartServiceImpl implements ProductAdsChartService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ProductAdsChartWrapperDto> findMetricsByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        long fromTs = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTs   = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        MatchOperation matchStage = Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("from").gte(fromTs).lte(toTs)
        );
        AggregationOperation unwind = Aggregation.unwind("data.entry_list");
        ProjectionOperation project = Aggregation.project()
                .and("data.entry_list.campaign.campaign_id").as("campaignId")
                .andExpression("data.entry_list.report.impression").as("impression")
                .andExpression("data.entry_list.report.click").as("click")
                .andExpression("data.entry_list.report.ctr").as("ctr")
                .andExpression("data.entry_list.report.broad_order_amount").as("broadOrderAmount")
                .andExpression("data.entry_list.report.broad_gmv").as("broadGmv")
                .andExpression("data.entry_list.campaign.daily_budget").as("dailyBudget")
                .andExpression("data.entry_list.report.broad_roi").as("roas")
                .andExpression("from").as("shopeeFrom")
                .andExpression("to").as("shopeeTo")
                .andExpression("createdAt").as("createdAt");

        Aggregation aggAll = Aggregation.newAggregation(
                matchStage,
                unwind,
                project
        );
        AggregationResults<Document> allResults = mongoTemplate.aggregate(
                aggAll, "ProductAds", Document.class
        );

        
        List<ProductAdsChartResponseDto> allDtos = allResults.getMappedResults().stream()
                .map(this::toChartDto)
                .collect(Collectors.toList());

        
        Map<Long, List<ProductAdsChartResponseDto>> grouped = allDtos.stream()
                .filter(dto -> dto.getCampaignId() != null)
                .collect(Collectors.groupingBy(
                        ProductAdsChartResponseDto::getCampaignId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        
        List<ProductAdsChartWrapperDto> wrappers = grouped.entrySet().stream()
                .map(e -> new ProductAdsChartWrapperDto(
                        e.getKey(),
                        from,
                        to,
                        e.getValue()
                ))
                .collect(Collectors.toList());

        
        int total = wrappers.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<ProductAdsChartWrapperDto> pageContent =
                start >= total ? List.of() : wrappers.subList(start, end);

        return new PageImpl<>(pageContent, pageable, total);
    }

    private ProductAdsChartResponseDto toChartDto(Document doc) {
        ProductAdsChartResponseDto dto = new ProductAdsChartResponseDto();
        dto.setCampaignId(getLong(doc, "campaignId"));
        dto.setImpression(getDouble(doc, "impression"));
        dto.setClick(getDouble(doc, "click"));
        dto.setCtr(getDouble(doc, "ctr"));
        dto.setBroadOrderAmount(getDouble(doc, "broadOrderAmount"));
        dto.setBroadGmv(getDouble(doc, "broadGmv"));
        dto.setDailyBudget(getDouble(doc, "dailyBudget"));
        dto.setRoas(getDouble(doc, "roas"));
        dto.setCreatedAt(getDateTime(doc, "createdAt"));
        dto.setShopeeFrom(getLong(doc, "shopeeFrom"));
        dto.setShopeeTo(getLong(doc, "shopeeTo"));
        return dto;
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
}
