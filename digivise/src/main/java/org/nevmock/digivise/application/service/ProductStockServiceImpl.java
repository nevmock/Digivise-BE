package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.stock.ModelStockDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductStockService;
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
public class ProductStockServiceImpl implements ProductStockService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ProductStockResponseWrapperDto> findByRange(
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
                .and("data.cover_image").as("coverImage")
                .and("data.status").as("status")
                .and("data.parent_sku").as("parentSku")
                .and("data.stock_detail.total_available_stock").as("totalAvailableStock")
                .and("data.stock_detail.total_seller_stock").as("totalSellerStock")
                .and("data.stock_detail.total_shopee_stock").as("totalShopeeStock")
                .and("data.stock_detail.low_stock_status").as("lowStockStatus")
                .and("data.stock_detail.enable_stock_reminder").as("enableStockReminder")
                .and("data.stock_detail.model_seller_stock_sold_out").as("modelSellerStockSoldOut")
                .and("data.stock_detail.model_shopee_stock_sold_out").as("modelShopeeStockSoldOut")
                .and("data.stock_detail.advanced_stock.sellable_stock").as("sellableStock")
                .and("data.stock_detail.advanced_stock.in_transit_stock").as("inTransitStock")
                .and("data.stock_detail.enable_stock_reminder_status").as("enableStockReminderStatus")
                .and("data.model_list").as("modelList")
                .and("data.statistics.sold_count").as("soldCount")
        );

        FacetOperation facet = Aggregation.facet(
                        Aggregation.skip((long) pageable.getOffset()),
                        Aggregation.limit(pageable.getPageSize())
                ).as("pagedResults")
                .and(Aggregation.count().as("total")).as("countResult");
        ops.add(facet);

        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops),
                "ProductStock", // Assuming collection name is ProductStock
                Document.class
        );

        Document root = results.getMappedResults().stream().findFirst().orElse(null);
        if (root == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        @SuppressWarnings("unchecked")
        List<Document> docs = (List<Document>) root.get("pagedResults");

        List<ProductStockResponseDto> dtos = docs.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        Map<Long, List<ProductStockResponseDto>> grouped = dtos.stream()
                .filter(d -> d.getProductId() != null)
                .collect(Collectors.groupingBy(ProductStockResponseDto::getProductId));

        List<ProductStockResponseWrapperDto> wrappers = grouped.entrySet().stream()
                .map(e -> ProductStockResponseWrapperDto.builder()
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
        List<ProductStockResponseWrapperDto> pageContent = start >= end
                ? Collections.emptyList()
                : wrappers.subList(start, end);

        return new PageImpl<>(pageContent, pageable, total);
    }

    private ProductStockResponseDto mapToDto(Document doc) {
        List<ModelStockDto> modelStocks = mapModelList(doc.get("modelList"));

        return ProductStockResponseDto.builder()
                .id(getString(doc, "id"))
                .uuid(getString(doc, "uuid"))
                .productId(getNumberLong(doc, "productId"))
                .shopId(getString(doc, "shopId"))
                .createdAt(getDateTime(doc, "createdAt"))
                .name(getString(doc, "name"))
                .coverImage(getString(doc, "coverImage"))
                .status(getNumberInteger(doc, "status"))
                .parentSku(getString(doc, "parentSku"))
                .totalAvailableStock(getNumberInteger(doc, "totalAvailableStock"))
                .totalSellerStock(getNumberInteger(doc, "totalSellerStock"))
                .totalShopeeStock(getNumberInteger(doc, "totalShopeeStock"))
                .lowStockStatus(getNumberInteger(doc, "lowStockStatus"))
                .enableStockReminder(getBoolean(doc, "enableStockReminder"))
                .modelSellerStockSoldOut(getBoolean(doc, "modelSellerStockSoldOut"))
                .modelShopeeStockSoldOut(getBoolean(doc, "modelShopeeStockSoldOut"))
                .sellableStock(getNumberInteger(doc, "sellableStock"))
                .inTransitStock(getNumberInteger(doc, "inTransitStock"))
                .enableStockReminderStatus(getNumberInteger(doc, "enableStockReminderStatus"))
                .soldCount(getNumberInteger(doc, "soldCount"))
                .modelStocks(modelStocks)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<ModelStockDto> mapModelList(Object modelListObj) {
        if (!(modelListObj instanceof List)) {
            return Collections.emptyList();
        }

        List<Document> modelList = (List<Document>) modelListObj;
        return modelList.stream()
                .map(this::mapModelStockDto)
                .collect(Collectors.toList());
    }

    private ModelStockDto mapModelStockDto(Document modelDoc) {
        Document stockDetail = (Document) modelDoc.get("stock_detail");
        Document advancedStock = stockDetail != null ?
                (Document) stockDetail.get("advanced_stock") : null;
        Document statistics = (Document) modelDoc.get("statistics");

        return ModelStockDto.builder()
                .id(getNumberLong(modelDoc, "id"))
                .name(getString(modelDoc, "name"))
                .sku(getString(modelDoc, "sku"))
                .isDefault(getBoolean(modelDoc, "is_default"))
                .image(getString(modelDoc, "image"))
                .totalAvailableStock(stockDetail != null ?
                        getNumberInteger(stockDetail, "total_available_stock") : null)
                .totalSellerStock(stockDetail != null ?
                        getNumberInteger(stockDetail, "total_seller_stock") : null)
                .totalShopeeStock(stockDetail != null ?
                        getNumberInteger(stockDetail, "total_shopee_stock") : null)
                .sellableStock(advancedStock != null ?
                        getNumberInteger(advancedStock, "sellable_stock") : null)
                .inTransitStock(advancedStock != null ?
                        getNumberInteger(advancedStock, "in_transit_stock") : null)
                .soldCount(statistics != null ?
                        getNumberInteger(statistics, "sold_count") : null)
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