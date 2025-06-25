package org.nevmock.digivise.application.service;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.nevmock.digivise.application.dto.product.stock.ModelStockDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseWrapperDto;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductStockServiceImpl implements ProductStockService {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<ProductStockResponseWrapperDto> findByShopId(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            String name,
            Pageable pageable
    ) {
        // Single range method - delegate to findByRange2
        return findByRange2(shopId, from, to, name, pageable);
    }

    /**
     * New method for comparing two time ranges
     */
    @Override
    public Page<ProductStockResponseWrapperDto> findByRange(
            String shopId,
            LocalDateTime from1,
            LocalDateTime to1,
            LocalDateTime from2,
            LocalDateTime to2,
            String name,
            String state,
            Pageable pageable
    ) {
        // Get aggregated data for period 1
        List<ProductStockResponseDto> period1DataList = getAggregatedDataByProductForRange(
                shopId, name, from1, to1, state
        );

        // Get aggregated data for period 2 and create map for easy lookup
        Map<Long, ProductStockResponseDto> period2DataMap = getAggregatedDataByProductForRange(
                shopId, name, from2, to2, state
        ).stream()
                .collect(Collectors.toMap(ProductStockResponseDto::getProductId, Function.identity()));

        if (period1DataList.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // Create wrapper DTOs with comparison data
        List<ProductStockResponseWrapperDto> resultList = period1DataList.stream().map(period1Data -> {
            ProductStockResponseDto period2Data = period2DataMap.get(period1Data.getProductId());

            // Calculate comparison fields
            populateComparisonFields(period1Data, period2Data);

            return ProductStockResponseWrapperDto.builder()
                    .productId(period1Data.getProductId())
                    .shopId(shopId)
                    .from1(from1)
                    .to1(to1)
                    .from2(from2)
                    .to2(to2)
                    .data(Collections.singletonList(period1Data))
                    .build();
        }).collect(Collectors.toList());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), resultList.size());

        if (start > resultList.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, resultList.size());
        }

        return new PageImpl<>(resultList.subList(start, end), pageable, resultList.size());
    }

    /**
     * Single range method (existing functionality)
     */
    public Page<ProductStockResponseWrapperDto> findByRange2(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            String name,
            Pageable pageable
    ) {
        Criteria criteria = Criteria.where("shop_id").is(shopId);
        if (from != null || to != null) {
            Criteria dateRange = Criteria.where("createdAt");
            if (from != null) dateRange.gte(from);
            if (to != null) dateRange.lte(to);
            criteria.andOperator(dateRange);
        }

        List<AggregationOperation> ops = new ArrayList<>();
        ops.add(Aggregation.match(criteria));
        ops.add(Aggregation.unwind("data"));

        if (name != null && !name.trim().isEmpty()) {
            Criteria nameFilter = Criteria.where("data.name")
                    .regex(".*" + name.trim() + ".*", "i");
            ops.add(Aggregation.match(nameFilter));
        }

        ProjectionOperation projectOperation = Aggregation.project()
                .and("$_id").as("id")
                .and("$uuid").as("uuid")
                .and("$createdAt").as("createdAt")
                .and("$shop_id").as("shopId")
                .and("data.id").as("productId")
                .and("data.name").as("name")
                .and("data.status").as("status")
                .and("data.cover_image").as("coverImage")
                .and("data.parent_sku").as("parentSku")
                .and("data.price_detail.price_min").as("priceMin")
                .and("data.price_detail.price_max").as("priceMax")
                .and("data.price_detail.has_discount").as("hasDiscount")
                .and("data.price_detail.max_discount_percentage").as("maxDiscountPercentage")
                .and("data.price_detail.max_discount").as("maxDiscount")
                .and("data.price_detail.selling_price_min").as("sellingPriceMin")
                .and("data.price_detail.selling_price_max").as("sellingPriceMax")
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
                .and("data.promotion.wholesale").as("wholesale")
                .and("data.promotion.has_bundle_deal").as("hasBundleDeal")
                .and("data.statistics.view_count").as("viewCount")
                .and("data.statistics.liked_count").as("likedCount")
                .and("data.statistics.sold_count").as("soldCount")
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
                .and("data.boost_info.boost_entry_status").as("boostEntryStatus")
                .and("data.boost_info.show_boost_history").as("showBoostHistory")
                .and("data.boost_info.campaign_id").as("boostCampaignId")
                .and("data.modify_time").as("modifyTime")
                .and("data.create_time").as("createTime")
                .and("data.scheduled_publish_time").as("scheduledPublishTime")
                .and("data.mtsku_item_id").as("mtskuItemId")
                .and("data.appeal_info.ipr_appeal_info.appeal_opt").as("appealOpt")
                .and("data.appeal_info.ipr_appeal_info.can_not_appeal_transify_key").as("canNotAppealTransifyKey")
                .and("data.appeal_info.ipr_appeal_info.reference_id").as("referenceId")
                .and("data.appeal_info.ipr_appeal_info.appeal_status").as("appealStatus")
                .and("data.model_list").as("modelList"); // Add model_list to projection
        ops.add(projectOperation);

        // Apply pagination at MongoDB level
        ops.add(Aggregation.skip(pageable.getOffset()));
        ops.add(Aggregation.limit(pageable.getPageSize()));

        Aggregation aggregation = Aggregation.newAggregation(ops);
        AggregationResults<Document> results = mongoTemplate.aggregate(
                aggregation, "ProductStock", Document.class
        );

        List<ProductStockResponseDto> dtos = results.getMappedResults().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        Map<Long, List<ProductStockResponseDto>> grouped = dtos.stream()
                .filter(dto -> dto.getProductId() != null)
                .collect(Collectors.groupingBy(ProductStockResponseDto::getProductId));

        List<ProductStockResponseWrapperDto> wrappers = grouped.entrySet().stream()
                .map(entry -> ProductStockResponseWrapperDto.builder()
                        .productId(entry.getKey())
                        .shopId(shopId)
                        .from1(from)
                        .to1(to)
                        .from2(null)
                        .to2(null)
                        .data(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(wrappers, pageable, wrappers.size());
    }

    /**
     * Get aggregated data by product for a specific time range
     */
    private List<ProductStockResponseDto> getAggregatedDataByProductForRange(
            String shopId, String name, LocalDateTime from, LocalDateTime to, String state) {

        List<AggregationOperation> ops = new ArrayList<>();

        // Match by shop_id and date range
        Criteria criteria = Criteria.where("shop_id").is(shopId);
        if (from != null || to != null) {
            Criteria dateRange = Criteria.where("createdAt");
            if (from != null) dateRange.gte(from);
            if (to != null) dateRange.lte(to);
            criteria.andOperator(dateRange);
        }
        ops.add(Aggregation.match(criteria));

        ops.add(Aggregation.unwind("data"));

        // Filter by name if provided
        if (name != null && !name.trim().isEmpty()) {
            ops.add(Aggregation.match(Criteria.where("data.name").regex(name, "i")));
        }


        if (state != null && !state.trim().isEmpty()) {
            Criteria stateFilter = Criteria.where("data.status").is(state);
            ops.add(Aggregation.match(stateFilter));
        }

        // Group by product ID and calculate averages
        ops.add(Aggregation.group("data.id")
                .avg("data.stock_detail.total_available_stock").as("avgTotalAvailableStock")
                .avg("data.stock_detail.total_seller_stock").as("avgTotalSellerStock")
                .avg("data.stock_detail.total_shopee_stock").as("avgTotalShopeeStock")
                .avg("data.stock_detail.advanced_stock.sellable_stock").as("avgAdvancedSellableStock")
                .avg("data.stock_detail.advanced_stock.in_transit_stock").as("avgAdvancedInTransitStock")
                .avg("data.statistics.view_count").as("avgViewCount")
                .avg("data.statistics.liked_count").as("avgLikedCount")
                .avg("data.statistics.sold_count").as("avgSoldCount")
                .avg("data.price_detail.price_min").as("avgPriceMin")
                .avg("data.price_detail.price_max").as("avgPriceMax")
                .avg("data.price_detail.selling_price_min").as("avgSellingPriceMin")
                .avg("data.price_detail.selling_price_max").as("avgSellingPriceMax")
                .avg("data.price_detail.max_discount_percentage").as("avgMaxDiscountPercentage")
                .avg("data.price_detail.max_discount").as("avgMaxDiscount")
                .first("data.name").as("name")
                .first("data.cover_image").as("coverImage")
                .first("data.status").as("status")
                .first("createdAt").as("createdAt")
                .first("data.model_list").as("modelList") // Add model_list to group
        );

        // Project to match DTO structure
        ops.add(Aggregation.project()
                .and("_id").as("productId")
                .and("avgTotalAvailableStock").as("totalAvailableStock")
                .and("avgTotalSellerStock").as("totalSellerStock")
                .and("avgTotalShopeeStock").as("totalShopeeStock")
                .and("avgAdvancedSellableStock").as("advancedSellableStock")
                .and("avgAdvancedInTransitStock").as("advancedInTransitStock")
                .and("avgViewCount").as("viewCount")
                .and("avgLikedCount").as("likedCount")
                .and("avgSoldCount").as("soldCount")
                .and("avgPriceMin").as("priceMin")
                .and("avgPriceMax").as("priceMax")
                .and("avgSellingPriceMin").as("sellingPriceMin")
                .and("avgSellingPriceMax").as("sellingPriceMax")
                .and("avgMaxDiscountPercentage").as("maxDiscountPercentage")
                .and("avgMaxDiscount").as("maxDiscount")
                .andInclude("name", "coverImage", "status", "modelList", "createdAt")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops), "ProductStock", Document.class
        );

        return results.getMappedResults().stream()
                .map(this::mapDocumentToAggregatedDto)
                .collect(Collectors.toList());
    }

    /**
     * Populate comparison fields by comparing current data with previous data
     */
    private void populateComparisonFields(ProductStockResponseDto currentData, ProductStockResponseDto previousData) {
        currentData.setTotalAvailableStockComparison(calculateComparison(
                currentData.getTotalAvailableStock() != null ? currentData.getTotalAvailableStock().doubleValue() : null,
                previousData != null && previousData.getTotalAvailableStock() != null ? previousData.getTotalAvailableStock().doubleValue() : null
        ));
        currentData.setTotalSellerStockComparison(calculateComparison(
                currentData.getTotalSellerStock() != null ? currentData.getTotalSellerStock().doubleValue() : null,
                previousData != null && previousData.getTotalSellerStock() != null ? previousData.getTotalSellerStock().doubleValue() : null
        ));
        currentData.setTotalShopeeStockComparison(calculateComparison(
                currentData.getTotalShopeeStock() != null ? currentData.getTotalShopeeStock().doubleValue() : null,
                previousData != null && previousData.getTotalShopeeStock() != null ? previousData.getTotalShopeeStock().doubleValue() : null
        ));
        currentData.setAdvancedSellableStockComparison(calculateComparison(
                currentData.getAdvancedSellableStock() != null ? currentData.getAdvancedSellableStock().doubleValue() : null,
                previousData != null && previousData.getAdvancedSellableStock() != null ? previousData.getAdvancedSellableStock().doubleValue() : null
        ));
        currentData.setAdvancedInTransitStockComparison(calculateComparison(
                currentData.getAdvancedInTransitStock() != null ? currentData.getAdvancedInTransitStock().doubleValue() : null,
                previousData != null && previousData.getAdvancedInTransitStock() != null ? previousData.getAdvancedInTransitStock().doubleValue() : null
        ));
        currentData.setViewCountComparison(calculateComparison(
                currentData.getViewCount() != null ? currentData.getViewCount().doubleValue() : null,
                previousData != null && previousData.getViewCount() != null ? previousData.getViewCount().doubleValue() : null
        ));
        currentData.setLikedCountComparison(calculateComparison(
                currentData.getLikedCount() != null ? currentData.getLikedCount().doubleValue() : null,
                previousData != null && previousData.getLikedCount() != null ? previousData.getLikedCount().doubleValue() : null
        ));
        currentData.setSoldCountComparison(calculateComparison(
                currentData.getSoldCount() != null ? currentData.getSoldCount().doubleValue() : null,
                previousData != null && previousData.getSoldCount() != null ? previousData.getSoldCount().doubleValue() : null
        ));
        currentData.setPriceMinComparison(calculateComparison(
                currentData.getPriceMin(),
                previousData != null ? previousData.getPriceMin() : null
        ));
        currentData.setPriceMaxComparison(calculateComparison(
                currentData.getPriceMax(),
                previousData != null ? previousData.getPriceMax() : null
        ));
        currentData.setSellingPriceMinComparison(calculateComparison(
                currentData.getSellingPriceMin(),
                previousData != null ? previousData.getSellingPriceMin() : null
        ));
        currentData.setSellingPriceMaxComparison(calculateComparison(
                currentData.getSellingPriceMax(),
                previousData != null ? previousData.getSellingPriceMax() : null
        ));
        currentData.setMaxDiscountPercentageComparison(calculateComparison(
                currentData.getMaxDiscountPercentage() != null ? currentData.getMaxDiscountPercentage().doubleValue() : null,
                previousData != null && previousData.getMaxDiscountPercentage() != null ? previousData.getMaxDiscountPercentage().doubleValue() : null
        ));
        currentData.setMaxDiscountComparison(calculateComparison(
                currentData.getMaxDiscount() != null ? currentData.getMaxDiscount().doubleValue() : null,
                previousData != null && previousData.getMaxDiscount() != null ? previousData.getMaxDiscount().doubleValue() : null
        ));
    }

    /**
     * Calculate percentage comparison between current and previous values
     */
    private Double calculateComparison(Double currentValue, Double previousValue) {
        if (currentValue == null || previousValue == null) {
            return null;
        }
        if (previousValue == 0) {
            return (currentValue > 0) ? 1.0 : 0.0;
        }
        return (currentValue - previousValue) / previousValue;
    }

    /**
     * Map aggregated document to DTO
     */
    private ProductStockResponseDto mapDocumentToAggregatedDto(Document doc) {
        ProductStockResponseDto dto = ProductStockResponseDto.builder()
                .productId(getNumberLong(doc, "productId"))
                .name(getString(doc, "name"))
                .coverImage(getString(doc, "coverImage"))
                .status(convertDoubleToInteger(getDouble(doc, "status")))
                .totalAvailableStock(convertDoubleToInteger(getDouble(doc, "totalAvailableStock")))
                .totalSellerStock(convertDoubleToInteger(getDouble(doc, "totalSellerStock")))
                .totalShopeeStock(convertDoubleToInteger(getDouble(doc, "totalShopeeStock")))
                .advancedSellableStock(convertDoubleToInteger(getDouble(doc, "advancedSellableStock")))
                .advancedInTransitStock(convertDoubleToInteger(getDouble(doc, "advancedInTransitStock")))
                .viewCount(convertDoubleToInteger(getDouble(doc, "viewCount")))
                .likedCount(convertDoubleToInteger(getDouble(doc, "likedCount")))
                .soldCount(convertDoubleToInteger(getDouble(doc, "soldCount")))
                .priceMin(getDouble(doc, "priceMin"))
                .priceMax(getDouble(doc, "priceMax"))
                .sellingPriceMin(getDouble(doc, "sellingPriceMin"))
                .sellingPriceMax(getDouble(doc, "sellingPriceMax"))
                .maxDiscountPercentage(convertDoubleToInteger(getDouble(doc, "maxDiscountPercentage")))
                .maxDiscount(convertDoubleToInteger(getDouble(doc, "maxDiscount")))
                .createdAt(getLocalDateTime(doc, "createdAt"))
                .build();

        // Map model stocks from modelList
        List<ModelStockDto> modelStocks = mapModelStocks(doc, "modelList");
        dto.setModelStocks(modelStocks);

        return dto;
    }

    /**
     * Convert Double to Integer for aggregated values
     */
    private Integer convertDoubleToInteger(Double value) {
        return value != null ? (int) Math.round(value) : null;
    }

    /**
     * Map document to DTO (original method)
     */
    private ProductStockResponseDto mapToDto(Document doc) {
        ProductStockResponseDto dto = ProductStockResponseDto.builder()
                .id(getString(doc, "id"))
                .uuid(getString(doc, "uuid"))
                .createdAt(getLocalDateTime(doc, "createdAt"))
                .shopId(getString(doc, "shopId"))
                .productId(getLong(doc, "productId"))
                .name(getString(doc, "name"))
                .status(getInteger(doc, "status"))
                .coverImage(getString(doc, "coverImage"))
                .parentSku(getString(doc, "parentSku"))
                .priceMin(getDouble(doc, "priceMin"))
                .priceMax(getDouble(doc, "priceMax"))
                .hasDiscount(getBoolean(doc, "hasDiscount"))
                .maxDiscountPercentage(getInteger(doc, "maxDiscountPercentage"))
                .maxDiscount(getInteger(doc, "maxDiscount"))
                .sellingPriceMin(getDouble(doc, "sellingPriceMin"))
                .sellingPriceMax(getDouble(doc, "sellingPriceMax"))
                .totalAvailableStock(getInteger(doc, "totalAvailableStock"))
                .totalSellerStock(getInteger(doc, "totalSellerStock"))
                .totalShopeeStock(getInteger(doc, "totalShopeeStock"))
                .lowStockStatus(getInteger(doc, "lowStockStatus"))
                .enableStockReminder(getBoolean(doc, "enableStockReminder"))
                .modelSellerStockSoldOut(getBoolean(doc, "modelSellerStockSoldOut"))
                .modelShopeeStockSoldOut(getBoolean(doc, "modelShopeeStockSoldOut"))
                .advancedSellableStock(getInteger(doc, "advancedSellableStock"))
                .advancedInTransitStock(getInteger(doc, "advancedInTransitStock"))
                .enableStockReminderStatus(getInteger(doc, "enableStockReminderStatus"))
                .wholesale(getBoolean(doc, "wholesale"))
                .hasBundleDeal(getBoolean(doc, "hasBundleDeal"))
                .viewCount(getInteger(doc, "viewCount"))
                .likedCount(getInteger(doc, "likedCount"))
                .soldCount(getInteger(doc, "soldCount"))
                .isVirtualSku(getBoolean(doc, "isVirtualSku"))
                .unlist(getBoolean(doc, "unlist"))
                .hasDiscountTag(getBoolean(doc, "hasDiscountTag"))
                .wholesaleTag(getBoolean(doc, "wholesaleTag"))
                .hasBundleDealTag(getBoolean(doc, "hasBundleDealTag"))
                .hasAddOnDeal(getBoolean(doc, "hasAddOnDeal"))
                .liveSku(getBoolean(doc, "liveSku"))
                .ssp(getBoolean(doc, "ssp"))
                .hasAmsCommission(getBoolean(doc, "hasAmsCommission"))
                .memberExclusive(getBoolean(doc, "memberExclusive"))
                .isIprAppealing(getBoolean(doc, "isIprAppealing"))
                .boostEntryStatus(getInteger(doc, "boostEntryStatus"))
                .showBoostHistory(getBoolean(doc, "showBoostHistory"))
                .boostCampaignId(getLong(doc, "boostCampaignId"))
                .modifyTime(getLong(doc, "modifyTime"))
                .createTime(getLong(doc, "createTime"))
                .scheduledPublishTime(getLong(doc, "scheduledPublishTime"))
                .mtskuItemId(getLong(doc, "mtskuItemId"))
                .appealOpt(getInteger(doc, "appealOpt"))
                .canNotAppealTransifyKey(getString(doc, "canNotAppealTransifyKey"))
                .referenceId(getLong(doc, "referenceId"))
                .appealStatus(getInteger(doc, "appealStatus"))
                .build();

        // Map model stocks from modelList
        List<ModelStockDto> modelStocks = mapModelStocks(doc, "modelList");
        dto.setModelStocks(modelStocks);

        return dto;
    }

    /**
     * Map modelList from document to List<ModelStockDto>
     */
    private List<ModelStockDto> mapModelStocks(Document doc, String fieldName) {
        List<ModelStockDto> modelStocks = new ArrayList<>();

        Object modelListObj = doc.get(fieldName);
        if (modelListObj instanceof List) {
            List<?> modelList = (List<?>) modelListObj;

            for (Object modelObj : modelList) {
                if (modelObj instanceof Document) {
                    Document modelDoc = (Document) modelObj;

                    // Extract stock detail information
                    Document stockDetail = modelDoc.get("stock_detail", Document.class);
                    Document advancedStock = null;
                    if (stockDetail != null) {
                        advancedStock = stockDetail.get("advanced_stock", Document.class);
                    }

                    // Extract statistics information
                    Document statistics = modelDoc.get("statistics", Document.class);

                    ModelStockDto modelStock = ModelStockDto.builder()
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
                            .build();

                    modelStocks.add(modelStock);
                }
            }
        }

        return modelStocks;
    }

    private String getString(Document d, String key) {
        Object v = d.get(key);
        return v instanceof String ? (String) v : null;
    }

    private Long getLong(Document d, String key) {
        Object v = d.get(key);
        return v instanceof Number ? ((Number) v).longValue() : null;
    }

    private Integer getInteger(Document d, String key) {
        Object v = d.get(key);
        return v instanceof Number ? ((Number) v).intValue() : null;
    }

    private Double getDouble(Document d, String key) {
        Object v = d.get(key);
        return v instanceof Number ? ((Number) v).doubleValue() : null;
    }

    private Boolean getBoolean(Document d, String key) {
        Object v = d.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        return null;
    }

    private LocalDateTime getLocalDateTime(Document d, String key) {
        Object v = d.get(key);
        if (v instanceof Date) {
            return ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }

    private Long getNumberLong(Document doc, String key) {
        Object value = doc.get(key);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }
}