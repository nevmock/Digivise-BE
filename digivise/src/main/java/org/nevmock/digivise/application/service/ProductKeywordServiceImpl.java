package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseDto;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductKeywordService;
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
public class ProductKeywordServiceImpl implements ProductKeywordService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ProductKeywordResponseWrapperDto> findByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            String name,
            Pageable pageable
    ) {
        List<AggregationOperation> ops = new ArrayList<>();

        // Match by shop_id and date range
        ops.add(Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("createdAt").gte(from).lte(to)
        ));

        // Unwind the data array to process each product individually
        ops.add(Aggregation.unwind("data"));

        // Filter by product name if provided
        if (name != null && !name.trim().isEmpty()) {
            Criteria nameFilter = Criteria.where("data.name")
                    .regex(".*" + name.trim() + ".*", "i"); // Case-insensitive partial match
            ops.add(Aggregation.match(nameFilter));
        }

        // Project the fields we need
        ops.add(Aggregation.project()
                .and("data.id").as("productId")
                .and("_id").as("id")
                .and("uuid").as("uuid")
                .and("shop_id").as("shopId")
                .and("createdAt").as("createdAt")
                .and("data.name").as("name")
                .and("data.cover_image").as("coverImage")
                .and("data.parent_sku").as("parentSku")
                .and("data.status").as("status")
                .and("data.price_detail.price_min").as("priceMin")
                .and("data.price_detail.price_max").as("priceMax")
                .and("data.price_detail.selling_price_min").as("sellingPriceMin")
                .and("data.price_detail.selling_price_max").as("sellingPriceMax")
                .and("data.price_detail.has_discount").as("hasDiscount")
                .and("data.price_detail.max_discount_percentage").as("maxDiscountPercentage")
                .and("data.stock_detail.total_available_stock").as("totalAvailableStock")
                .and("data.stock_detail.total_seller_stock").as("totalSellerStock")
                .and("data.statistics.view_count").as("viewCount")
                .and("data.statistics.liked_count").as("likedCount")
                .and("data.statistics.sold_count").as("soldCount")
                .and("data.promotion.wholesale").as("wholesale")
                .and("data.promotion.has_bundle_deal").as("hasBundleDeal")
                .and("data.modify_time").as("modifyTime")
                .and("data.create_time").as("createTime")
        );

        // Use facet for pagination and counting
        FacetOperation facet = Aggregation.facet(
                        Aggregation.skip((long) pageable.getOffset()),
                        Aggregation.limit(pageable.getPageSize())
                ).as("pagedResults")
                .and(Aggregation.count().as("total")).as("countResult");
        ops.add(facet);

        // Execute aggregation
        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops),
                "ProductKeyword", // Collection name - adjust as needed
                Document.class
        );

        Document root = results.getMappedResults().stream().findFirst().orElse(null);
        if (root == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        @SuppressWarnings("unchecked")
        List<Document> docs = (List<Document>) root.get("pagedResults");

        // Get total count
        @SuppressWarnings("unchecked")
        List<Document> countResults = (List<Document>) root.get("countResult");
        long total = countResults.isEmpty() ? 0 : countResults.get(0).getInteger("total", 0);

        // Map documents to DTOs
        List<ProductKeywordResponseDto> dtos = docs.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        // Group by product ID
        Map<Long, List<ProductKeywordResponseDto>> grouped = dtos.stream()
                .filter(d -> d.getProductId() != null)
                .collect(Collectors.groupingBy(ProductKeywordResponseDto::getProductId));

        // Create wrapper DTOs
        List<ProductKeywordResponseWrapperDto> wrappers = grouped.entrySet().stream()
                .map(e -> ProductKeywordResponseWrapperDto.builder()
                        .productId(e.getKey())
                        .shopId(shopId)
                        .from(from)
                        .to(to)
                        .data(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(wrappers, pageable, total);
    }

    private ProductKeywordResponseDto mapToDto(Document doc) {
        return ProductKeywordResponseDto.builder()
                .id(getString(doc, "id"))
                .uuid(getString(doc, "uuid"))
                .productId(getNumberLong(doc, "productId"))
                .shopId(getString(doc, "shopId"))
                .createdAt(getDateTime(doc, "createdAt"))
                .name(getString(doc, "name"))
                .coverImage(getString(doc, "coverImage"))
                .parentSku(getString(doc, "parentSku"))
                .status(getNumberInteger(doc, "status"))
                .priceMin(getBigDecimal(doc, "priceMin"))
                .priceMax(getBigDecimal(doc, "priceMax"))
                .sellingPriceMin(getBigDecimal(doc, "sellingPriceMin"))
                .sellingPriceMax(getBigDecimal(doc, "sellingPriceMax"))
                .hasDiscount(getBoolean(doc, "hasDiscount"))
                .maxDiscountPercentage(getNumberInteger(doc, "maxDiscountPercentage"))
                .totalAvailableStock(getNumberInteger(doc, "totalAvailableStock"))
                .totalSellerStock(getNumberInteger(doc, "totalSellerStock"))
                .viewCount(getNumberInteger(doc, "viewCount"))
                .likedCount(getNumberInteger(doc, "likedCount"))
                .soldCount(getNumberInteger(doc, "soldCount"))
                .wholesale(getBoolean(doc, "wholesale"))
                .hasBundleDeal(getBoolean(doc, "hasBundleDeal"))
                .modifyTime(getNumberLong(doc, "modifyTime"))
                .createTime(getNumberLong(doc, "createTime"))
                .build();
    }

    // Helper methods for type conversion
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

    private java.math.BigDecimal getBigDecimal(Document d, String key) {
        Object v = d.get(key);
        if (v instanceof String) {
            try {
                return new java.math.BigDecimal((String) v);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (v instanceof Number) {
            return java.math.BigDecimal.valueOf(((Number) v).doubleValue());
        }
        return null;
    }

    private Boolean getBoolean(Document d, String key) {
        Object v = d.get(key);
        return v instanceof Boolean ? (Boolean) v : null;
    }

    private LocalDateTime getDateTime(Document d, String key) {
        Object v = d.get(key);
        if (v instanceof Date) {
            return ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }
}