

































































































































package org.nevmock.digivise.application.service;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
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
        Criteria criteria = Criteria.where("shop_id").is(shopId);
        if (from != null || to != null) {
            Criteria dateRange = Criteria.where("createdAt");
            if (from != null) dateRange.gte(from);
            if (to   != null) dateRange.lte(to);
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
                .and("data.appeal_info.ipr_appeal_info.appeal_status").as("appealStatus");
        ops.add(projectOperation);

        FacetOperation facet = Aggregation.facet(
                        Aggregation.skip((long) pageable.getOffset()),
                        Aggregation.limit(pageable.getPageSize())
                ).as("pagedResults")
                .and(Aggregation.count().as("total")).as("countResult");
        ops.add(facet);

        Aggregation aggregation = Aggregation.newAggregation(ops);
        AggregationResults<Document> results = mongoTemplate.aggregate(
                aggregation, "ProductStock", Document.class
        );

        Document root = results.getMappedResults().isEmpty() ? null : results.getMappedResults().get(0);
        if (root == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Document> pagedResults = root.getList("pagedResults", Document.class);
        List<ProductStockResponseDto> dtos = pagedResults.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        Map<Long, List<ProductStockResponseDto>> grouped = dtos.stream()
                .filter(dto -> dto.getProductId() != null)
                .collect(Collectors.groupingBy(ProductStockResponseDto::getProductId));

        List<ProductStockResponseWrapperDto> wrappers = grouped.entrySet().stream()
                .map(entry -> ProductStockResponseWrapperDto.builder()
                        .productId(entry.getKey())
                        .shopId(shopId)
                        .from(from)
                        .to(to)
                        .data(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        long totalCount = 0;
        List<Document> countResult = root.getList("countResult", Document.class);
        if (!countResult.isEmpty()) {
            totalCount = getNumberLong(countResult.get(0), "total");
        }

        return new PageImpl<>(wrappers, pageable, totalCount);
    }

    private ProductStockResponseDto mapToDto(Document doc) {
        return ProductStockResponseDto.builder()
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