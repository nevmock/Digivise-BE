package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.performance.ProductPerformanceResponseDto;
import org.nevmock.digivise.application.dto.product.performance.ProductPerformanceWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductPerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductPerformanceServiceImpl implements ProductPerformanceService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ProductPerformanceWrapperDto> findByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        List<AggregationOperation> ops = new ArrayList<>();

        ops.add(Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("createdAt").gte(from).lte(to)
        ));

        ops.add(Aggregation.unwind("data"));

        ops.add(Aggregation.project()
                .and("data.id").as("productId")
                .and("_id").as("id")
                .and("uuid").as("uuid")
                .and("shop_id").as("shopId")
                .and("createdAt").as("createdAt")
                .and("data.name").as("name")
                .and("data.image").as("image")
                .and("data.status").as("status")
                .and("data.uv").as("uv")
                .and("data.pv").as("pv")
                .and("data.likes").as("likes")
                .and("data.bounce_visitors").as("bounceVisitors")
                .and("data.bounce_rate").as("bounceRate")
                .and("data.search_clicks").as("searchClicks")
                .and("data.add_to_cart_units").as("addToCartUnits")
                .and("data.add_to_cart_buyers").as("addToCartBuyers")
                .and("data.placed_sales").as("placedSales")
                .and("data.placed_units").as("placedUnits")
                .and("data.placed_buyers").as("placedBuyers")
                .and("data.paid_sales").as("paidSales")
                .and("data.paid_units").as("paidUnits")
                .and("data.paid_buyers").as("paidBuyers")
                .and("data.confirmed_sales").as("confirmedSales")
                .and("data.confirmed_units").as("confirmedUnits")
                .and("data.confirmed_buyers").as("confirmedBuyers")
        );

        FacetOperation facet = Aggregation.facet(
                        Aggregation.skip((long) pageable.getOffset()),
                        Aggregation.limit(pageable.getPageSize())
                ).as("pagedResults")
                .and(Aggregation.count().as("total")).as("countResult");
        ops.add(facet);

        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops),
                "ProductPerformance",
                Document.class
        );

        Document root = results.getMappedResults().stream().findFirst().orElse(null);
        if (root == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        @SuppressWarnings("unchecked")
        List<Document> docs = (List<Document>) root.get("pagedResults");

        List<ProductPerformanceResponseDto> dtos = docs.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        Map<Long, List<ProductPerformanceResponseDto>> grouped = dtos.stream()
                .filter(d -> d.getProductId() != null)
                .collect(Collectors.groupingBy(ProductPerformanceResponseDto::getProductId));

        List<ProductPerformanceWrapperDto> wrappers = grouped.entrySet().stream()
                .map(e -> ProductPerformanceWrapperDto.builder()
                        .productId(e.getKey())
                        .shopId(shopId)
                        .from(from)
                        .to(to)
                        .data(e.getValue())
                        .build())
                .collect(Collectors.toList());

        long total = wrappers.size();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), wrappers.size());
        List<ProductPerformanceWrapperDto> pageContent = start >= end
                ? Collections.emptyList()
                : wrappers.subList(start, end);

        return new PageImpl<>(pageContent, pageable, total);
    }

    private ProductPerformanceResponseDto mapToDto(Document doc) {
        return ProductPerformanceResponseDto.builder()
                .id(getString(doc, "id"))
                .uuid(getString(doc, "uuid"))
                .productId(getNumberLong(doc, "productId"))
                .shopId(getString(doc, "shopId"))
                .createdAt(getDateTime(doc, "createdAt"))
                .name(getString(doc, "name"))
                .image(getString(doc, "image"))
                .status(getNumberInteger(doc, "status"))
                .uv(getNumberLong(doc, "uv"))
                .pv(getNumberLong(doc, "pv"))
                .likes(getNumberLong(doc, "likes"))
                .bounceVisitors(getNumberLong(doc, "bounceVisitors"))
                .bounceRate(getNumberDouble(doc, "bounceRate"))
                .searchClicks(getNumberLong(doc, "searchClicks"))
                .addToCartUnits(getNumberLong(doc, "addToCartUnits"))
                .addToCartBuyers(getNumberLong(doc, "addToCartBuyers"))
                .placedSales(getNumberDouble(doc, "placedSales"))
                .placedUnits(getNumberLong(doc, "placedUnits"))
                .placedBuyers(getNumberLong(doc, "placedBuyers"))
                .paidSales(getNumberDouble(doc, "paidSales"))
                .paidUnits(getNumberLong(doc, "paidUnits"))
                .paidBuyers(getNumberLong(doc, "paidBuyers"))
                .confirmedSales(getNumberDouble(doc, "confirmedSales"))
                .confirmedUnits(getNumberLong(doc, "confirmedUnits"))
                .confirmedBuyers(getNumberLong(doc, "confirmedBuyers"))
                .build();
    }

    private String getString(Document d, String key) {
        Object v = d.get(key);
        return v instanceof String ? (String) v : null;
    }
    private Long getNumberLong(Document d, String key) {
        Object v = d.get(key);
        return v instanceof Number ? ((Number) v).longValue() : null;
    }
    private Integer getNumberInteger(Document d, String key) {
        Object v = d.get(key);
        return v instanceof Number ? ((Number) v).intValue() : null;
    }
    private Double getNumberDouble(Document d, String key) {
        Object v = d.get(key);
        return v instanceof Number ? ((Number) v).doubleValue() : null;
    }
    private LocalDateTime getDateTime(Document d, String key) {
        Object v = d.get(key);
        if (v instanceof Date) {
            return ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }
}
