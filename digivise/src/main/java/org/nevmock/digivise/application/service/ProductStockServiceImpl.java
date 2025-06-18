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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductStockServiceImpl implements ProductStockService {

    private final MongoTemplate mongoTemplate;

    public Page<ProductStockResponseDto> findByShopId(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        Criteria criteria = Criteria.where("shop_id").is(shopId);
        if (from != null || to != null) {
            Criteria dateRange = Criteria.where("createdAt");
            if (from != null) dateRange.gte(from);
            if (to   != null) dateRange.lte(to);
            criteria.andOperator(dateRange);
        }

        List<AggregationOperation> baseOperations = new ArrayList<>();
        baseOperations.add(Aggregation.match(criteria));
        baseOperations.add(Aggregation.unwind("data"));

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

        List<AggregationOperation> mainOperations = new ArrayList<>(baseOperations);
        mainOperations.add(projectOperation);
        mainOperations.add(Aggregation.skip((long) pageable.getPageNumber() * pageable.getPageSize()));
        mainOperations.add(Aggregation.limit(pageable.getPageSize()));

        Aggregation aggregation = Aggregation.newAggregation(mainOperations);

        List<AggregationOperation> countOperations = new ArrayList<>(baseOperations);
        countOperations.add(Aggregation.count().as("total"));

        List<ProductStockResponseDto> results = mongoTemplate
                .aggregate(aggregation, "ProductStock", ProductStockResponseDto.class)
                .getMappedResults();

        long totalCount = mongoTemplate
                .aggregate(Aggregation.newAggregation(countOperations), "ProductStock", Document.class)
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