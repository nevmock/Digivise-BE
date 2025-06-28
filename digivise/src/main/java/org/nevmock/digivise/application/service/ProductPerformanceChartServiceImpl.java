package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.performance.ProductPerformanceChartResponseDto;
import org.nevmock.digivise.application.dto.product.performance.ProductPerformanceChartWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductPerformanceChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
public class ProductPerformanceChartServiceImpl implements ProductPerformanceChartService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ProductPerformanceChartWrapperDto> findMetricsByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        long fromTs = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTs = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        
        MatchOperation matchStage = Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("from").gte(fromTs).lte(toTs)
        );

        
        UnwindOperation unwind = Aggregation.unwind("data");

        
        ProjectionOperation project = Aggregation.project()
                .and("data.id").as("productId")
                .and("data.pv").as("pv")
                .and("data.add_to_cart_units").as("addToCartUnits")
                .and("data.uv_to_add_to_cart_rate").as("uvToAddToCartRate")
                .and("data.placed_units").as("placedUnits")
                .and("data.placed_buyers").as("placedBuyers")
                .and("data.confirmed_buyers").as("confirmedBuyers")
                .and("data.uv_to_confirmed_buyers_rate").as("uvToConfirmedBuyersRate")
                .and("data.uv_to_placed_buyers_rate").as("uvToPlacedBuyersRate")
                .and("data.confirmed_sales").as("confirmedSales")
                .and("data.placed_sales").as("placedSales")
                .and("from").as("shopeeFrom")
                .and("to").as("shopeeTo")
                .and("createdAt").as("createdAt");

        
        Aggregation aggAll = Aggregation.newAggregation(
                matchStage,
                unwind,
                project
        );

        
        List<Document> results = mongoTemplate.aggregate(aggAll, "ProductPerformance", Document.class)
                .getMappedResults();

        
        List<ProductPerformanceChartResponseDto> allDtos = results.stream()
                .map(this::toChartDto)
                .collect(Collectors.toList());

        
        Map<Long, List<ProductPerformanceChartResponseDto>> grouped = allDtos.stream()
                .filter(dto -> dto.getProductId() != null)
                .collect(Collectors.groupingBy(
                        ProductPerformanceChartResponseDto::getProductId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        
        List<ProductPerformanceChartWrapperDto> wrappers = grouped.entrySet().stream()
                .map(e -> new ProductPerformanceChartWrapperDto(
                        e.getKey(),
                        from,
                        to,
                        e.getValue()
                ))
                .collect(Collectors.toList());

        
//        int total = wrappers.size();
//        int start = (int) pageable.getOffset();
//        int end = Math.min(start + pageable.getPageSize(), total);
//        List<ProductPerformanceChartWrapperDto> pageContent =
//                start >= total ? List.of() : wrappers.subList(start, end);
//
//        return new PageImpl<>(pageContent, pageable, total);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), wrappers.size());

        if (start > wrappers.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, wrappers.size());
        }

        return new PageImpl<>(wrappers.subList(start, end), pageable, wrappers.size());
    }

    private ProductPerformanceChartResponseDto toChartDto(Document doc) {
        ProductPerformanceChartResponseDto dto = new ProductPerformanceChartResponseDto();

        dto.setProductId(getLong(doc, "productId"));
        dto.setPv(getDouble(doc, "pv"));
        dto.setAddToCartUnits(getDouble(doc, "addToCartUnits"));
        dto.setUvToAddToCartRate(getDouble(doc, "uvToAddToCartRate"));
        dto.setPlacedUnits(getDouble(doc, "placedUnits"));

        
        Long placedBuyers = getLong(doc, "placedBuyers");
        Long confirmedBuyers = getLong(doc, "confirmedBuyers");
        double rate = 0.0;
        if (placedBuyers != null && placedBuyers != 0) {
            rate = (confirmedBuyers != null ? confirmedBuyers.doubleValue() : 0.0) * 100.0 / placedBuyers;
        }
        dto.setPlacedBuyersToConfirmedBuyersRate(rate);

        dto.setUvToConfirmedBuyersRate(getDouble(doc, "uvToConfirmedBuyersRate"));
        dto.setUvToPlacedBuyersRate(getDouble(doc, "uvToPlacedBuyersRate"));
        dto.setConfirmedSales(getDouble(doc, "confirmedSales"));
        dto.setPlacedSales(getDouble(doc, "placedSales"));
        dto.setShopeeFrom(getLong(doc, "shopeeFrom"));
        dto.setShopeeTo(getLong(doc, "shopeeTo"));
        dto.setCreatedAt(getDateTime(doc, "createdAt"));

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
        } else if (v instanceof String) {
            try {
                return Long.parseLong((String) v);
            } catch (NumberFormatException e) {
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
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}