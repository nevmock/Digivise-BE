package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.stock.ModelStockDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockChartResponseDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockChartWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductStockChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductStockChartServiceImpl implements ProductStockChartService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<ProductStockChartWrapperDto> findStockChartByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            Long productId  
    ) {
        List<AggregationOperation> ops = new ArrayList<>();

        Instant fromInstant = from != null ? from.atZone(ZoneId.systemDefault()).toInstant() : null;
        Instant toInstant = to != null ? to.atZone(ZoneId.systemDefault()).toInstant() : null;

        Criteria criteria = Criteria.where("shop_id").is(shopId);

        if (fromInstant != null || toInstant != null) {
            Criteria dateRange = Criteria.where("createdAt");
            if (fromInstant != null) {
                dateRange.gte(fromInstant);
            }
            if (toInstant != null) {
                dateRange.lte(toInstant);
            }
            criteria.andOperator(dateRange);
        }

        MatchOperation matchBase = Aggregation.match(criteria);
        ops.add(matchBase);

        // 2. Unwind nested data array
        UnwindOperation unwind = Aggregation.unwind("data");
        ops.add(unwind);

        // 3. (Optional) Filter by productId, hanya jika productId tidak null
        if (productId != null) {
            ops.add(Aggregation.match(Criteria.where("data.id").is(productId)));
        }

        // 4. Project fields
        ProjectionOperation project = Aggregation.project()
                .and("data.id").as("productId")
                .and("data.name").as("name")
                .and("data.stock_detail.total_available_stock").as("totalAvailableStock")
                .and("createdAt").as("createdAt")
                .and("data.model_list").as("modelList");
        ops.add(project);

        // Execute aggregation
        Aggregation aggregation = Aggregation.newAggregation(ops);
        List<Document> results = mongoTemplate.aggregate(
                aggregation,
                "ProductStock",
                Document.class
        ).getMappedResults();

        // Map to DTOs
        List<ProductStockChartResponseDto> allDtos = results.stream()
                .map(this::toChartDto)
                .filter(dto -> dto.getProductId() != null)
                .collect(Collectors.toList());

        List<ProductStockChartWrapperDto> wrappers = new ArrayList<>();

        if (productId != null) {
            // Single product: group by product only
            Map<Long, List<ProductStockChartResponseDto>> byProduct = allDtos.stream()
                    .collect(Collectors.groupingBy(ProductStockChartResponseDto::getProductId, LinkedHashMap::new, Collectors.toList()));
            byProduct.forEach((prodId, list) -> {
                String productName = list.isEmpty() ? null : list.get(0).getName();
                wrappers.add(new ProductStockChartWrapperDto(prodId, from, to, productName, list));
            });
        } else {
            // No product filter: group by product
            Map<Long, List<ProductStockChartResponseDto>> byProduct = allDtos.stream()
                    .collect(Collectors.groupingBy(ProductStockChartResponseDto::getProductId, LinkedHashMap::new, Collectors.toList()));
            byProduct.forEach((prodId, list) -> {
                String productName = list.isEmpty() ? null : list.get(0).getName();
                wrappers.add(new ProductStockChartWrapperDto(prodId, from, to, productName, list));
            });
        }

        return wrappers;
    }


    private ProductStockChartResponseDto toChartDto(Document doc) {
        ProductStockChartResponseDto dto = new ProductStockChartResponseDto();
        dto.setProductId(getLong(doc, "productId"));
        dto.setName(getString(doc, "name"));
        dto.setTotalAvailableStock(getInteger(doc, "totalAvailableStock"));
        dto.setCreatedAt(getDateTime(doc, "createdAt"));

        List<Document> modelList = doc.get("modelList", List.class);
        if (modelList != null) {
            List<ModelStockDto> modelStocks = modelList.stream()
                    .map(modelDoc -> mapModelStockDto(modelDoc, dto.getCreatedAt()))
                    .collect(Collectors.toList());
            dto.setModelStock(modelStocks);
        } else {
            dto.setModelStock(Collections.emptyList());
        }

        return dto;
    }

    private ModelStockDto mapModelStockDto(Document modelDoc, LocalDateTime createdAt) {
        Document stockDetail = modelDoc.get("stock_detail", Document.class);
        Document advancedStock = stockDetail != null ? stockDetail.get("advanced_stock", Document.class) : null;
        Document statistics = modelDoc.get("statistics", Document.class);

        return ModelStockDto.builder()
                .id(getLong(modelDoc, "id"))
                .name(getString(modelDoc, "name"))
                .sku(getString(modelDoc, "sku"))
                .isDefault(getBoolean(modelDoc, "is_default"))
                .image(getString(modelDoc, "image"))
                .totalAvailableStock(stockDetail != null ? getInteger(stockDetail, "total_available_stock") : null)
                .totalSellerStock(stockDetail != null ? getInteger(stockDetail, "total_seller_stock") : null)
                .totalShopeeStock(stockDetail != null ? getInteger(stockDetail, "total_shopee_stock") : null)
                .sellableStock(advancedStock != null ? getInteger(advancedStock, "sellable_stock") : null)
                .inTransitStock(advancedStock != null ? getInteger(advancedStock, "in_transit_stock") : null)
                .soldCount(statistics != null ? getInteger(statistics, "sold_count") : null)
                .createdAt(createdAt) // Inherit parent's createdAt
                .build();
    }

    // Add helper for Boolean extraction
    private Boolean getBoolean(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        return null;
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

    private Integer getInteger(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v);} catch (NumberFormatException ex) { }
        }
        return null;
    }

    private LocalDateTime getDateTime(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Date) {
            return ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (v instanceof Instant) {
            return ((Instant) v).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }
}