package org.nevmock.digivise.application.service;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.nevmock.digivise.application.dto.product.stock.ModelStockDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductStockService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductStockServiceImpl implements ProductStockService {

    private final MongoTemplate mongoTemplate;

    public Page<ProductStockResponseWrapperDto> findByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            String name,
            String state,
            Pageable pageable
    ) {
        List<AggregationOperation> ops = new ArrayList<>();

        // Convert LocalDateTime to Instant for UTC
        Instant fromInstant = from != null ? from.atZone(ZoneId.systemDefault()).toInstant() : null;
        Instant toInstant = to != null ? to.atZone(ZoneId.systemDefault()).toInstant() : null;

        // Match by shop_id and date range
        Criteria criteria = Criteria.where("shop_id").is(shopId);
        Criteria dateRange = Criteria.where("createdAt");
        if (fromInstant != null) {
            dateRange.gte(fromInstant);
        }
        if (toInstant != null) {
            dateRange.lte(toInstant);
        }
        if (fromInstant != null || toInstant != null) {
            criteria.andOperator(dateRange);
        }
        ops.add(Aggregation.match(criteria));

        // Unwind the data array
        ops.add(Aggregation.unwind("data"));

        // Filter by name if provided
        if (name != null && !name.trim().isEmpty()) {
            Criteria nameFilter = Criteria.where("data.name")
                    .regex(".*" + name.trim() + ".*", "i");
            ops.add(Aggregation.match(nameFilter));
        }

        // Sort by createdAt descending to get the latest records first
        ops.add(Aggregation.sort(Sort.Direction.DESC, "createdAt"));

        // Group by product ID and take the first (latest) record for each product
        ops.add(Aggregation.group("data.id")
                .first("$$ROOT").as("latestRecord")
        );

        // Project the latest record data with correct field paths
        ProjectionOperation projectOperation = Aggregation.project()
                .and("latestRecord._id").as("id")
                .and("latestRecord.uuid").as("uuid")
                .and("latestRecord.createdAt").as("createdAt")
                .and("latestRecord.shop_id").as("shopId")
                .and("_id").as("productId") // Grouped field (product ID)
                .and("latestRecord.data.name").as("name")
                .and("latestRecord.data.status").as("status")
                .and("latestRecord.data.cover_image").as("coverImage")
                .and("latestRecord.data.parent_sku").as("parentSku")
                .and("latestRecord.data.price_detail.price_min").as("priceMin")
                .and("latestRecord.data.price_detail.price_max").as("priceMax")
                .and("latestRecord.data.price_detail.has_discount").as("hasDiscount")
                .and("latestRecord.data.price_detail.max_discount_percentage").as("maxDiscountPercentage")
                .and("latestRecord.data.price_detail.max_discount").as("maxDiscount")
                .and("latestRecord.data.price_detail.selling_price_min").as("sellingPriceMin")
                .and("latestRecord.data.price_detail.selling_price_max").as("sellingPriceMax")
                .and("latestRecord.data.stock_detail.total_available_stock").as("totalAvailableStock")
                .and("latestRecord.data.stock_detail.total_seller_stock").as("totalSellerStock")
                .and("latestRecord.data.stock_detail.total_shopee_stock").as("totalShopeeStock")
                .and("latestRecord.data.stock_detail.low_stock_status").as("lowStockStatus")
                .and("latestRecord.data.stock_detail.enable_stock_reminder").as("enableStockReminder")
                .and("latestRecord.data.stock_detail.model_seller_stock_sold_out").as("modelSellerStockSoldOut")
                .and("latestRecord.data.stock_detail.model_shopee_stock_sold_out").as("modelShopeeStockSoldOut")
                .and("latestRecord.data.stock_detail.advanced_stock.sellable_stock").as("advancedSellableStock")
                .and("latestRecord.data.stock_detail.advanced_stock.in_transit_stock").as("advancedInTransitStock")
                .and("latestRecord.data.stock_detail.enable_stock_reminder_status").as("enableStockReminderStatus")
                .and("latestRecord.data.promotion.wholesale").as("wholesale")
                .and("latestRecord.data.promotion.has_bundle_deal").as("hasBundleDeal")
                .and("latestRecord.data.statistics.view_count").as("viewCount")
                .and("latestRecord.data.statistics.liked_count").as("likedCount")
                .and("latestRecord.data.statistics.sold_count").as("soldCount")
                .and("latestRecord.data.tag.is_virtual_sku").as("isVirtualSku")
                .and("latestRecord.data.tag.unlist").as("unlist")
                .and("latestRecord.data.tag.has_discount").as("hasDiscountTag")
                .and("latestRecord.data.tag.wholesale").as("wholesaleTag")
                .and("latestRecord.data.tag.has_bundle_deal").as("hasBundleDealTag")
                .and("latestRecord.data.tag.has_add_on_deal").as("hasAddOnDeal")
                .and("latestRecord.data.tag.live_sku").as("liveSku")
                .and("latestRecord.data.tag.ssp").as("ssp")
                .and("latestRecord.data.tag.has_ams_commission").as("hasAmsCommission")
                .and("latestRecord.data.tag.member_exclusive").as("memberExclusive")
                .and("latestRecord.data.tag.is_ipr_appealing").as("isIprAppealing")
                .and("latestRecord.data.boost_info.boost_entry_status").as("boostEntryStatus")
                .and("latestRecord.data.boost_info.show_boost_history").as("showBoostHistory")
                .and("latestRecord.data.boost_info.campaign_id").as("boostCampaignId")
                .and("latestRecord.data.modify_time").as("modifyTime")
                .and("latestRecord.data.create_time").as("createTime")
                .and("latestRecord.data.scheduled_publish_time").as("scheduledPublishTime")
                .and("latestRecord.data.mtsku_item_id").as("mtskuItemId")
                .and("latestRecord.data.appeal_info.ipr_appeal_info.appeal_opt").as("appealOpt")
                .and("latestRecord.data.appeal_info.ipr_appeal_info.can_not_appeal_transify_key").as("canNotAppealTransifyKey")
                .and("latestRecord.data.appeal_info.ipr_appeal_info.reference_id").as("referenceId")
                .and("latestRecord.data.appeal_info.ipr_appeal_info.appeal_status").as("appealStatus")
                .and("latestRecord.data.model_list").as("modelList"); // Correct field name

        ops.add(projectOperation);

        // Count total elements without pagination
        Aggregation countAggregation = Aggregation.newAggregation(ops);
        long total = mongoTemplate.aggregate(countAggregation, "ProductStock", Document.class)
                .getMappedResults().size();

        // Add pagination
        ops.add(Aggregation.skip(pageable.getOffset()));
        ops.add(Aggregation.limit(pageable.getPageSize()));

        Aggregation aggregation = Aggregation.newAggregation(ops);
        System.out.println("Aggregation Pipeline: " + aggregation); // Debugging

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "ProductStock", Document.class);
        List<Document> mappedResults = results.getMappedResults();

        List<ProductStockResponseDto> dtos = mappedResults.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        // Create wrapper DTOs
        List<ProductStockResponseWrapperDto> wrappers = dtos.stream()
                .map(dto -> ProductStockResponseWrapperDto.builder()
                        .productId(dto.getProductId())
                        .shopId(shopId)
                        .from1(from)
                        .to1(to)
                        .from2(null)
                        .to2(null)
                        .data(Collections.singletonList(dto))
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(wrappers, pageable, total);
    }

    private ProductStockResponseDto mapToDto(Document doc) {
        Long totalStock = (getLong(doc, "totalAvailableStock") + getLong(doc, "soldCount"));
        Long currentStockPercentage = totalStock > 0
                ? (getLong(doc, "totalAvailableStock") * 100) / totalStock
                : 0L;

        String salesAvailability = currentStockPercentage <= 70 ? "Stock mencapai 70% kebawah" : "Stock diatas 70%";

        ProductStockResponseDto dto = ProductStockResponseDto.builder()
                .id(getObjectIdAsString(doc, "id"))
                .uuid(getString(doc, "uuid"))
                .createdAt(convertToLocalDateTime(doc.get("createdAt")))
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
                .salesAvailability(
                        salesAvailability
                )
                .build();

        // Map model stocks
        List<Document> modelList = doc.get("modelList", List.class);
        if (modelList != null) {
            LocalDateTime parentCreated = dto.getCreatedAt();

            List<ModelStockDto> modelStocks = modelList.stream()
                    .map(modelStock -> mapModelStockDto(modelStock, parentCreated))
                    .collect(Collectors.toList());
            dto.setModelStocks(modelStocks);
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
                .createdAt(createdAt)
                .build();
    }

    // Helper methods for safe type conversion
    private String getString(Document doc, String key) {
        if (doc == null) return null;
        Object value = doc.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getInteger(Document doc, String key) {
        if (doc == null) return null;
        Object value = doc.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Long getLong(Document doc, String key) {
        if (doc == null) return null;
        Object value = doc.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Double getDouble(Document doc, String key) {
        if (doc == null) return null;
        Object value = doc.get(key);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Boolean getBoolean(Document doc, String key) {
        if (doc == null) return null;
        Object value = doc.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    private String getObjectIdAsString(Document doc, String key) {
        if (doc == null) return null;
        ObjectId objectId = doc.getObjectId(key);
        return objectId != null ? objectId.toString() : null;
    }

    private LocalDateTime convertToLocalDateTime(Object dateObj) {
        if (dateObj instanceof Date) {
            return ((Date) dateObj).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } else if (dateObj instanceof Instant) {
            return ((Instant) dateObj).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }
}