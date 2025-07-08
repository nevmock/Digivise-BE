package org.nevmock.digivise.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.nevmock.digivise.application.dto.product.stock.ModelStockDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseDto;
import org.nevmock.digivise.application.dto.product.stock.ProductStockResponseWrapperDto;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.port.in.ProductStockService;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.domain.port.out.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductStockServiceImpl implements ProductStockService {

    private final MongoTemplate mongoTemplate;

    @Autowired
    private final MerchantRepository merchantRepository;

    public Page<ProductStockResponseWrapperDto> findByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            String name,
            String state,
            Pageable pageable
    ) {
        List<AggregationOperation> ops = new ArrayList<>();

        
        Instant fromInstant = from != null ? from.atZone(ZoneId.systemDefault()).toInstant() : null;
        Instant toInstant = to != null ? to.atZone(ZoneId.systemDefault()).toInstant() : null;

        
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

        
        ops.add(Aggregation.unwind("data"));

        
        if (name != null && !name.trim().isEmpty()) {
            Criteria nameFilter = Criteria.where("data.name")
                    .regex(".*" + name.trim() + ".*", "i");
            ops.add(Aggregation.match(nameFilter));
        }

        
        ops.add(Aggregation.sort(Sort.Direction.DESC, "createdAt"));

        
        ops.add(Aggregation.group("data.id")
                .first("$$ROOT").as("latestRecord")
        );

        
        ProjectionOperation projectOperation = Aggregation.project()
                .and("latestRecord._id").as("id")
                .and("latestRecord.uuid").as("uuid")
                .and("latestRecord.createdAt").as("createdAt")
                .and("latestRecord.shop_id").as("shopId")
                .and("_id").as("productId") 
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
                .and("latestRecord.data.model_list").as("modelList"); 

        ops.add(projectOperation);

        
        Aggregation countAggregation = Aggregation.newAggregation(ops);
        long total = mongoTemplate.aggregate(countAggregation, "ProductStock", Document.class)
                .getMappedResults().size();

        
        ops.add(Aggregation.skip(pageable.getOffset()));
        ops.add(Aggregation.limit(pageable.getPageSize()));

        Aggregation aggregation = Aggregation.newAggregation(ops);
        System.out.println("Aggregation Pipeline: " + aggregation); 

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "ProductStock", Document.class);
        List<Document> mappedResults = results.getMappedResults();

        List<ProductStockResponseDto> dtos = mappedResults.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        
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

                .build();

        
        List<Document> modelList = doc.get("modelList", List.class);
        if (modelList != null) {
            LocalDateTime parentCreated = dto.getCreatedAt();

            List<ModelStockDto> modelStocks = modelList.stream()
                    .map(modelStock -> mapModelStockDto(modelStock, parentCreated))
                    .collect(Collectors.toList());

            dto.setSalesAvailability(getSalesAvailability(modelStocks));

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

    public ProductStockResponseWrapperDto fetchStockLive(
            String username,
            String type,
            boolean isAsc,
            int pageSize,
            int targetPage,
            String searchKeyword
    ) {
        final String URL = "http://103.150.116.30:1337/api/v1/shopee-seller/stock-live";

        // Ambil merchant
        Merchant merchant = merchantRepository.findByShopeeMerchantId(username)
                .orElseThrow(() -> new RuntimeException("Merchant not found for username: " + username));

        if (merchant.getUsername() == null || merchant.getUsername().isEmpty()) {
            throw new RuntimeException("Merchant username is not set for: " + username);
        }

        // Siapkan body JSON
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", merchant.getUsername());
        requestBody.put("type", type);
        requestBody.put("isAsc", isAsc);
        requestBody.put("pageSize", pageSize);
        requestBody.put("targetPage", targetPage);
        requestBody.put("searchKeyword", searchKeyword);

        ObjectMapper objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing request body", e);
        }

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .method("GET", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error sending HTTP request", e);
        }

        String respBody = response.body();
        int status = response.statusCode();

        if (status == 200) {
            try {
                JsonNode root = objectMapper.readTree(respBody);
                JsonNode products = root.path("data").path("products");

                List<ProductStockResponseDto> productDtos = new ArrayList<>();
                for (JsonNode node : products) {
                    productDtos.add(mapProductNode(node));
                }

                return ProductStockResponseWrapperDto.builder()
                        .data(productDtos)
                        // ... set field lain jika perlu
                        .build();

            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error parsing JSON response: " + respBody, e);
            }
        } else {
            // Log full response body biar jelas kenapa fail
            throw new RuntimeException("API request failed (status=" + status + "): " + respBody);
        }
    }


//    private ProductStockResponseDto mapProductNode(JsonNode product) {
//        return ProductStockResponseDto.builder()
//                .id(product.path("id").asText())
//                .productId(product.path("id").asLong())
//                .name(product.path("name").asText())
//                .status(product.path("status").asInt())
//                .coverImage(product.path("cover_image").asText())
//                .parentSku(product.path("parent_sku").asText())
//                .priceMin(product.path("price_detail").path("price_min").asDouble())
//                .priceMax(product.path("price_detail").path("price_max").asDouble())
//                .totalAvailableStock(product.path("stock_detail").path("total_available_stock").asInt())
//                .totalSellerStock(product.path("stock_detail").path("total_seller_stock").asInt())
//                .modelStocks(mapModelStocks(product.path("model_list")))
//                // Add other fields based on your DTO structure
//                .build();
//    }
    private String getSalesAvailability(List<ModelStockDto> modelStocks) {
        int availableCount = 0;

        if (modelStocks == null || modelStocks.isEmpty()) {
            return "0%";
        }

        for (ModelStockDto model : modelStocks) {
            if (model.getTotalAvailableStock() != null && model.getTotalAvailableStock() > 0) {
                availableCount++;
            }
        }

        if (availableCount == 0) {
            return "0%";
        }

        return (availableCount / modelStocks.size()) * 100 + "%";
    }

private ProductStockResponseDto mapProductNode(JsonNode product) {
    JsonNode priceDetail = product.path("price_detail");
    JsonNode stockDetail = product.path("stock_detail");
    JsonNode advancedStock = stockDetail.path("advanced_stock");
    JsonNode promotion = product.path("promotion");
    JsonNode statistics = product.path("statistics");
    JsonNode tag = product.path("tag");
    JsonNode boostInfo = product.path("boost_info");
    JsonNode appealInfo = product.path("appeal_info").path("ipr_appeal_info");

    List<ModelStockDto> modelStocks = mapModelStocks(product.path("model_list"));

    String salesAvailability = getSalesAvailability(modelStocks);

    return ProductStockResponseDto.builder()
            .id(product.path("id").asText())
            .uuid(UUID.randomUUID().toString()) // Generate if not from API
            .createdAt(LocalDateTime.now()) // Set current time or parse from API if available
            .shopId("") // Set from context if available
            .productId(product.path("id").asLong())
            .name(product.path("name").asText())
            .status(product.path("status").asInt())
            .coverImage(product.path("cover_image").asText())
            .parentSku(product.path("parent_sku").asText())

            // Price Details
            .priceMin(priceDetail.path("price_min").asDouble())
            .priceMax(priceDetail.path("price_max").asDouble())
            .hasDiscount(priceDetail.path("has_discount").asBoolean())
            .maxDiscountPercentage(priceDetail.path("max_discount_percentage").asInt())
            .maxDiscount(priceDetail.path("max_discount").asInt())
            .sellingPriceMin(priceDetail.path("selling_price_min").asDouble())
            .sellingPriceMax(priceDetail.path("selling_price_max").asDouble())

            // Stock Details
            .totalAvailableStock(stockDetail.path("total_available_stock").asInt())
            .totalSellerStock(stockDetail.path("total_seller_stock").asInt())
            .totalShopeeStock(stockDetail.path("total_shopee_stock").asInt())
            .lowStockStatus(stockDetail.path("low_stock_status").asInt())
            .enableStockReminder(stockDetail.path("enable_stock_reminder").asBoolean())
            .modelSellerStockSoldOut(stockDetail.path("model_seller_stock_sold_out").asBoolean())
            .modelShopeeStockSoldOut(stockDetail.path("model_shopee_stock_sold_out").asBoolean())
            .advancedSellableStock(advancedStock.path("sellable_stock").asInt())
            .advancedInTransitStock(advancedStock.path("in_transit_stock").asInt())
            .enableStockReminderStatus(stockDetail.path("enable_stock_reminder_status").asInt())

            // Promotion
            .wholesale(promotion.path("wholesale").asBoolean())
            .hasBundleDeal(promotion.path("has_bundle_deal").asBoolean())

            // Statistics
            .viewCount(statistics.path("view_count").asInt())
            .likedCount(statistics.path("liked_count").asInt())
            .soldCount(statistics.path("sold_count").asInt())

            // Tags
            .isVirtualSku(tag.path("is_virtual_sku").asBoolean())
            .unlist(tag.path("unlist").asBoolean())
            .hasDiscountTag(tag.path("has_discount").asBoolean())
            .wholesaleTag(tag.path("wholesale").asBoolean())
            .hasBundleDealTag(tag.path("has_bundle_deal").asBoolean())
            .hasAddOnDeal(tag.path("has_add_on_deal").asBoolean())
            .liveSku(tag.path("live_sku").asBoolean())
            .ssp(tag.path("ssp").asBoolean())
            .hasAmsCommission(tag.path("has_ams_commission").asBoolean())
            .memberExclusive(tag.path("member_exclusive").asBoolean())
            .isIprAppealing(tag.path("is_ipr_appealing").asBoolean())

            // Boost Info
            .boostEntryStatus(boostInfo.path("boost_entry_status").asInt())
            .showBoostHistory(boostInfo.path("show_boost_history").asBoolean())
            .boostCampaignId(boostInfo.path("campaign_id").asLong())

            // Timestamps
            .modifyTime(product.path("modify_time").asLong())
            .createTime(product.path("create_time").asLong())
            .scheduledPublishTime(product.path("scheduled_publish_time").asLong())
            .mtskuItemId(product.path("mtsku_item_id").asLong())

            // Appeal Info
            .appealOpt(appealInfo.path("appeal_opt").asInt())
            .canNotAppealTransifyKey(appealInfo.path("can_not_appeal_transify_key").asText())
            .referenceId(appealInfo.path("reference_id").asLong())
            .appealStatus(appealInfo.path("appeal_status").asInt())

            // Model Stocks
            .modelStocks(modelStocks)

            // Comparison fields (set to 0 if not available)
            .totalAvailableStockComparison(0.0)
            .totalSellerStockComparison(0.0)
            .totalShopeeStockComparison(0.0)
            .advancedSellableStockComparison(0.0)
            .advancedInTransitStockComparison(0.0)
            .viewCountComparison(0.0)
            .likedCountComparison(0.0)
            .soldCountComparison(0.0)
            .priceMinComparison(0.0)
            .priceMaxComparison(0.0)
            .sellingPriceMinComparison(0.0)
            .sellingPriceMaxComparison(0.0)
            .maxDiscountPercentageComparison(0.0)
            .maxDiscountComparison(0.0)
            .salesAvailability(
                    salesAvailability
            )
            .build();
}



    private List<ModelStockDto> mapModelStocks(JsonNode modelList) {
        List<ModelStockDto> models = new ArrayList<>();
        for (JsonNode model : modelList) {
            models.add(ModelStockDto.builder()
                    .id(model.path("id").asLong())
                    .name(model.path("name").asText())
                    .sku(model.path("sku").asText())
                    .isDefault(model.path("is_default").asBoolean())
                    .image(model.path("image").asText())
                    .totalAvailableStock(model.path("stock_detail").path("total_available_stock").asInt())
                    .totalSellerStock(model.path("stock_detail").path("total_seller_stock").asInt())
                    .soldCount(model.path("statistics").path("sold_count").asInt())
                    .build());
        }
        return models;
    }
    
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