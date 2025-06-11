//package org.nevmock.digivise.application.service;
//
//import org.bson.Document;
//import org.nevmock.digivise.application.dto.product.stock.ModelStockDto;
//import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseDto;
//import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseWrapperDto;
//import org.nevmock.digivise.domain.port.in.ProductStockService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.data.mongodb.core.aggregation.Aggregation;
//import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
//import org.springframework.data.mongodb.core.aggregation.AggregationResults;
//import org.springframework.data.mongodb.core.aggregation.FacetOperation;
//import org.springframework.data.mongodb.core.query.Criteria;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//public class ProductStockServiceImpl implements ProductStockService {
//
//    @Autowired
//    private MongoTemplate mongoTemplate;
//
//    @Override
//    public Page<ProductStockResponseWrapperDto> findByRange(
//            String shopId,
//            LocalDateTime from,
//            LocalDateTime to,
//            Pageable pageable
//    ) {
//        List<AggregationOperation> ops = new ArrayList<>();
//
//        ops.add(Aggregation.match(
//                Criteria.where("shop_id").is(shopId)
//                        .and("createdAt").gte(from).lte(to)
//        ));
//
//        ops.add(Aggregation.unwind("data"));
//
//        ops.add(Aggregation.project()
//                .and("data.id").as("productId")
//                .and("_id").as("id")
//                .and("uuid").as("uuid")
//                .and("shop_id").as("shopId")
//                .and("createdAt").as("createdAt")
//                .and("data.name").as("name")
//                .and("data.cover_image").as("coverImage")
//                .and("data.status").as("status")
//                .and("data.parent_sku").as("parentSku")
//                .and("data.stock_detail.total_available_stock").as("totalAvailableStock")
//                .and("data.stock_detail.total_seller_stock").as("totalSellerStock")
//                .and("data.stock_detail.total_shopee_stock").as("totalShopeeStock")
//                .and("data.stock_detail.low_stock_status").as("lowStockStatus")
//                .and("data.stock_detail.enable_stock_reminder").as("enableStockReminder")
//                .and("data.stock_detail.model_seller_stock_sold_out").as("modelSellerStockSoldOut")
//                .and("data.stock_detail.model_shopee_stock_sold_out").as("modelShopeeStockSoldOut")
//                .and("data.stock_detail.advanced_stock.sellable_stock").as("sellableStock")
//                .and("data.stock_detail.advanced_stock.in_transit_stock").as("inTransitStock")
//                .and("data.stock_detail.enable_stock_reminder_status").as("enableStockReminderStatus")
//                .and("data.model_list").as("modelList")
//                .and("data.statistics.sold_count").as("soldCount")
//        );
//
//        FacetOperation facet = Aggregation.facet(
//                        Aggregation.skip((long) pageable.getOffset()),
//                        Aggregation.limit(pageable.getPageSize())
//                ).as("pagedResults")
//                .and(Aggregation.count().as("total")).as("countResult");
//        ops.add(facet);
//
//        AggregationResults<Document> results = mongoTemplate.aggregate(
//                Aggregation.newAggregation(ops),
//                "ProductStock", // Assuming collection name is ProductStock
//                Document.class
//        );
//
//        Document root = results.getMappedResults().stream().findFirst().orElse(null);
//        if (root == null) {
//            return new PageImpl<>(Collections.emptyList(), pageable, 0);
//        }
//
//        @SuppressWarnings("unchecked")
//        List<Document> docs = (List<Document>) root.get("pagedResults");
//
//        List<ProductStockResponseDto> dtos = docs.stream()
//                .map(this::mapToDto)
//                .collect(Collectors.toList());
//
//        Map<Long, List<ProductStockResponseDto>> grouped = dtos.stream()
//                .filter(d -> d.getProductId() != null)
//                .collect(Collectors.groupingBy(ProductStockResponseDto::getProductId));
//
//        List<ProductStockResponseWrapperDto> wrappers = grouped.entrySet().stream()
//                .map(e -> ProductStockResponseWrapperDto.builder()
//                        .productId(e.getKey())
//                        .shopId(shopId)
//                        .from(from)
//                        .to(to)
//                        .data(e.getValue())
//                        .build())
//                .collect(Collectors.toList());
//
//        long total = wrappers.size();
//
//        int start = (int) pageable.getOffset();
//        int end = Math.min(start + pageable.getPageSize(), wrappers.size());
//        List<ProductStockResponseWrapperDto> pageContent = start >= end
//                ? Collections.emptyList()
//                : wrappers.subList(start, end);
//
//        return new PageImpl<>(pageContent, pageable, total);
//    }
//
//    @Override
//    public Page<ProductStockResponseWrapperDto> findByShop(
//            String shopId,
//            Pageable pageable
//    ) {
//        List<AggregationOperation> ops = List.of(
//                Aggregation.match(Criteria.where("shop_id").is(shopId)),
//                Aggregation.unwind("data"),
//                Aggregation.project()
//                        .and("data.id").as("productId")
//                        .and("_id").as("id")
//                        .and("uuid").as("uuid")
//                        .and("shop_id").as("shopId")
//                        .and("createdAt").as("createdAt")
//                        .and("data.name").as("name")
//                        .and("data.cover_image").as("coverImage")
//                        .and("data.status").as("status")
//                        .and("data.parent_sku").as("parentSku")
//                        .and("data.stock_detail.total_available_stock").as("totalAvailableStock")
//                        .and("data.stock_detail.total_seller_stock").as("totalSellerStock")
//                        .and("data.stock_detail.total_shopee_stock").as("totalShopeeStock")
//                        .and("data.stock_detail.low_stock_status").as("lowStockStatus")
//                        .and("data.stock_detail.enable_stock_reminder").as("enableStockReminder")
//                        .and("data.stock_detail.model_seller_stock_sold_out").as("modelSellerStockSoldOut")
//                        .and("data.stock_detail.model_shopee_stock_sold_out").as("modelShopeeStockSoldOut")
//                        .and("data.stock_detail.advanced_stock.sellable_stock").as("sellableStock")
//                        .and("data.stock_detail.advanced_stock.in_transit_stock").as("inTransitStock")
//                        .and("data.stock_detail.enable_stock_reminder_status").as("enableStockReminderStatus")
//                        .and("data.model_list").as("modelList")
//                        .and("data.statistics.sold_count").as("soldCount")
//        );
//
//        // Define facet for pagination and count
//        FacetOperation facet = Aggregation.facet(
//                        Aggregation.skip((long) pageable.getOffset()),
//                        Aggregation.limit(pageable.getPageSize())
//                )
//                .as("pagedResults")
//                .and(Aggregation.count().as("total")).as("countResult");
//
//        // Add facet to pipeline
//        ops = new java.util.ArrayList<>(ops);
//        ops.add(facet);
//
//        // Execute aggregation
//        AggregationResults<Document> results = mongoTemplate.aggregate(
//                Aggregation.newAggregation(ops),
//                "ProductStock",
//                Document.class
//        );
//
//        Document root = results.getMappedResults().stream().findFirst().orElse(null);
//        if (root == null) {
//            return new PageImpl<>(Collections.emptyList(), pageable, 0);
//        }
//
//        @SuppressWarnings("unchecked")
//        List<Document> docs = (List<Document>) root.get("pagedResults");
//
//        // Map documents to DTOs
//        List<ProductStockResponseDto> dtos = docs.stream()
//                .map(this::mapToDto)
//                .collect(Collectors.toList());
//
//        // Group by productId
//        Map<Long, List<ProductStockResponseDto>> grouped = dtos.stream()
//                .filter(d -> d.getProductId() != null)
//                .collect(Collectors.groupingBy(ProductStockResponseDto::getProductId));
//
//        // Build wrapper DTOs
//        List<ProductStockResponseWrapperDto> wrappers = grouped.entrySet().stream()
//                .map(e -> ProductStockResponseWrapperDto.builder()
//                        .productId(e.getKey())
//                        .shopId(shopId)
//                        .data(e.getValue())
//                        .build())
//                .collect(Collectors.toList());
//
//        long total = wrappers.size();
//        int start = (int) pageable.getOffset();
//        int end = Math.min(start + pageable.getPageSize(), wrappers.size());
//        List<ProductStockResponseWrapperDto> pageContent = start >= end
//                ? Collections.emptyList()
//                : wrappers.subList(start, end);
//
//        return new PageImpl<>(pageContent, pageable, total);
//    }
//
//    private ProductStockResponseDto mapToDto(Document doc) {
//        List<ModelStockDto> modelStocks = mapModelList(doc.get("modelList"));
//
//        return ProductStockResponseDto.builder()
//                .id(getString(doc, "id"))
//                .uuid(getString(doc, "uuid"))
//                .productId(getNumberLong(doc, "productId"))
//                .shopId(getString(doc, "shopId"))
//                .createdAt(getDateTime(doc, "createdAt"))
//                .name(getString(doc, "name"))
//                .coverImage(getString(doc, "coverImage"))
//                .status(getNumberInteger(doc, "status"))
//                .parentSku(getString(doc, "parentSku"))
//                .totalAvailableStock(getNumberInteger(doc, "totalAvailableStock"))
//                .totalSellerStock(getNumberInteger(doc, "totalSellerStock"))
//                .totalShopeeStock(getNumberInteger(doc, "totalShopeeStock"))
//                .lowStockStatus(getNumberInteger(doc, "lowStockStatus"))
//                .enableStockReminder(getBoolean(doc, "enableStockReminder"))
//                .modelSellerStockSoldOut(getBoolean(doc, "modelSellerStockSoldOut"))
//                .modelShopeeStockSoldOut(getBoolean(doc, "modelShopeeStockSoldOut"))
//                .sellableStock(getNumberInteger(doc, "sellableStock"))
//                .inTransitStock(getNumberInteger(doc, "inTransitStock"))
//                .enableStockReminderStatus(getNumberInteger(doc, "enableStockReminderStatus"))
//                .soldCount(getNumberInteger(doc, "soldCount"))
//                .modelStocks(modelStocks)
//                .build();
//    }
//
//    @SuppressWarnings("unchecked")
//    private List<ModelStockDto> mapModelList(Object modelListObj) {
//        if (!(modelListObj instanceof List)) {
//            return Collections.emptyList();
//        }
//
//        List<Document> modelList = (List<Document>) modelListObj;
//        return modelList.stream()
//                .map(this::mapModelStockDto)
//                .collect(Collectors.toList());
//    }
//
//    private ModelStockDto mapModelStockDto(Document modelDoc) {
//        Document stockDetail = (Document) modelDoc.get("stock_detail");
//        Document advancedStock = stockDetail != null ?
//                (Document) stockDetail.get("advanced_stock") : null;
//        Document statistics = (Document) modelDoc.get("statistics");
//
//        return ModelStockDto.builder()
//                .id(getNumberLong(modelDoc, "id"))
//                .name(getString(modelDoc, "name"))
//                .sku(getString(modelDoc, "sku"))
//                .isDefault(getBoolean(modelDoc, "is_default"))
//                .image(getString(modelDoc, "image"))
//                .totalAvailableStock(stockDetail != null ?
//                        getNumberInteger(stockDetail, "total_available_stock") : null)
//                .totalSellerStock(stockDetail != null ?
//                        getNumberInteger(stockDetail, "total_seller_stock") : null)
//                .totalShopeeStock(stockDetail != null ?
//                        getNumberInteger(stockDetail, "total_shopee_stock") : null)
//                .sellableStock(advancedStock != null ?
//                        getNumberInteger(advancedStock, "sellable_stock") : null)
//                .inTransitStock(advancedStock != null ?
//                        getNumberInteger(advancedStock, "in_transit_stock") : null)
//                .soldCount(statistics != null ?
//                        getNumberInteger(statistics, "sold_count") : null)
//                .build();
//    }
//
//    private String getString(Document d, String key) {
//        Object v = d.get(key);
//        return v instanceof String ? (String) v : null;
//    }
//
//    private Long getNumberLong(Document d, String key) {
//        Object v = d.get(key);
//        return v instanceof Number ? ((Number) v).longValue() : null;
//    }
//
//    private Integer getNumberInteger(Document d, String key) {
//        Object v = d.get(key);
//        return v instanceof Number ? ((Number) v).intValue() : null;
//    }
//
//    private Boolean getBoolean(Document d, String key) {
//        Object v = d.get(key);
//        return v instanceof Boolean ? (Boolean) v : null;
//    }
//
//    private LocalDateTime getDateTime(Document d, String key) {
//        Object v = d.get(key);
//        if (v instanceof Date) {
//            return ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
//        }
//        return null;
//    }
//}

package org.nevmock.digivise.application.service;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseDto;
import org.nevmock.digivise.domain.port.in.ProductStockService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductStockServiceImpl implements ProductStockService {

    private final MongoTemplate mongoTemplate;

    public Page<ProductStockResponseDto> findByShopId(String shopId, Pageable pageable) {
        // Convert LocalDateTime to epoch seconds
        long now = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();

        List<AggregationOperation> operations = List.of(
                Aggregation.match(Criteria.where("shop_id").is(shopId)),
                Aggregation.unwind("data"),
                Aggregation.project()
                        .and("$_id").as("id")
                        .and("$uuid").as("uuid")
                        .and("$createdAt").as("createdAt")
                        .and("$shop_id").as("shopId")

                        // Product data fields
                        .and("data.id").as("productId")
                        .and("data.name").as("name")
                        .and("data.status").as("status")
                        .and("data.cover_image").as("coverImage")
                        .and("data.parent_sku").as("parentSku")

                        // Price detail
                        .and("data.price_detail.price_min").as("priceMin")
                        .and("data.price_detail.price_max").as("priceMax")
                        .and("data.price_detail.has_discount").as("hasDiscount")
                        .and("data.price_detail.max_discount_percentage").as("maxDiscountPercentage")
                        .and("data.price_detail.max_discount").as("maxDiscount")
                        .and("data.price_detail.selling_price_min").as("sellingPriceMin")
                        .and("data.price_detail.selling_price_max").as("sellingPriceMax")

                        // Stock detail
                        .and("data.stock_detail.total_available_stock").as("totalAvailableStock")
                        .and("data.stock_detail.total_seller_stock").as("totalSellerStock")
                        .and("data.stock_detail.total_shopee_stock").as("totalShopeeStock")
                        .and("data.stock_detail.low_stock_status").as("lowStockStatus")
                        .and("data.stock_detail.enable_stock_reminder").as("enableStockReminder")
                        .and("data.stock_detail.model_seller_stock_sold_out").as("modelSellerStockSoldOut")
                        .and("data.stock_detail.model_shopee_stock_sold_out").as("modelShopeeStockSoldOut")
                        .and("data.stock_detail.advanced_stock.sellable_stock").as("advancedSellableStock")
                        .and("data.stock_detail.advanced_stock.in_transit_stock").as("advancedInTransitStock")
                        .and("data.stock_detail.enable_stock_reminder_status").as("enableStockReminderStatus")

                        // Promotion
                        .and("data.promotion.wholesale").as("wholesale")
                        .and("data.promotion.has_bundle_deal").as("hasBundleDeal")

                        // Statistics
                        .and("data.statistics.view_count").as("viewCount")
                        .and("data.statistics.liked_count").as("likedCount")
                        .and("data.statistics.sold_count").as("soldCount")

                        // Tag
                        .and("data.tag.is_virtual_sku").as("isVirtualSku")
                        .and("data.tag.unlist").as("unlist")
                        .and("data.tag.has_discount").as("hasDiscountTag")
                        .and("data.tag.wholesale").as("wholesaleTag")
                        .and("data.tag.has_bundle_deal").as("hasBundleDealTag")
                        .and("data.tag.has_add_on_deal").as("hasAddOnDeal")
                        .and("data.tag.live_sku").as("liveSku")
                        .and("data.tag.ssp").as("ssp")
                        .and("data.tag.has_ams_commission").as("hasAmsCommission")
                        .and("data.tag.member_exclusive").as("memberExclusive")
                        .and("data.tag.is_ipr_appealing").as("isIprAppealing")

                        // Boost info
                        .and("data.boost_info.boost_entry_status").as("boostEntryStatus")
                        .and("data.boost_info.show_boost_history").as("showBoostHistory")
                        .and("data.boost_info.campaign_id").as("boostCampaignId")

                        // Timestamps
                        .and("data.modify_time").as("modifyTime")
                        .and("data.create_time").as("createTime")

                        // Other fields
                        .and("data.scheduled_publish_time").as("scheduledPublishTime")
                        .and("data.mtsku_item_id").as("mtskuItemId")

                        // Appeal info
                        .and("data.appeal_info.ipr_appeal_info.appeal_opt").as("appealOpt")
                        .and("data.appeal_info.ipr_appeal_info.can_not_appeal_transify_key").as("canNotAppealTransifyKey")
                        .and("data.appeal_info.ipr_appeal_info.reference_id").as("referenceId")
                        .and("data.appeal_info.ipr_appeal_info.appeal_status").as("appealStatus")
        );

        // Pagination operations
        Aggregation aggregation = Aggregation.newAggregation(
                operations.get(0),
                operations.get(1),
                operations.get(2),
                Aggregation.skip((long) pageable.getPageNumber() * pageable.getPageSize()),
                Aggregation.limit(pageable.getPageSize())
        );

        // Count aggregation
        Aggregation countAggregation = Aggregation.newAggregation(
                operations.get(0),
                operations.get(1),
                Aggregation.count().as("total")
        );

        List<ProductStockResponseDto> results = mongoTemplate
                .aggregate(aggregation, "ProductStock", ProductStockResponseDto.class)
                .getMappedResults();

        long totalCount = mongoTemplate
                .aggregate(countAggregation, "ProductStock", Document.class)
                .getMappedResults()
                .stream()
                .findFirst()
                .map(doc -> getNumberLong(doc, "total"))
                .orElse(0L);

        return new PageImpl<>(results, pageable, totalCount);
    }

    private Long getNumberLong(Document doc, String key) {
        Object value = doc.get(key);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }
}