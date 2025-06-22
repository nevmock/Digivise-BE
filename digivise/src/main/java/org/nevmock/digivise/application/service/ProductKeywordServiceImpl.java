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

        long fromTs = from.atZone(ZoneId.systemDefault()).toEpochSecond();

        
        ops.add(Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("from").gte(fromTs)
        ));

        
        ops.add(Aggregation.unwind("data"));

        
        if (name != null && !name.trim().isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.key").regex(".*" + name.trim() + ".*", "i")
            ));
        }

        
        ops.add(Aggregation.project()
                .and("uuid").as("uuid")
                .and("shop_id").as("shopId")
                .and("createdAt").as("createdAt")
                .and("from").as("from")
                .and("to").as("to")
                .and("data.key").as("keyword")
                .and("data.ratio").as("ratio")
                .and("data.metrics").as("metrics")
        );

        
        FacetOperation facet = Aggregation.facet(
                        Aggregation.skip((long) pageable.getOffset()),
                        Aggregation.limit(pageable.getPageSize())
                ).as("pagedResults")
                .and(Aggregation.count().as("totalCount")).as("countResult");
        ops.add(facet);

        
        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops),
                "ProductKey",
                Document.class
        );

        Document root = results.getMappedResults().stream().findFirst().orElse(null);
        if (root == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        
        @SuppressWarnings("unchecked")
        List<Document> docs = (List<Document>) root.get("pagedResults");

        
        @SuppressWarnings("unchecked")
        List<Document> countDocs = (List<Document>) root.get("countResult");
        long total = countDocs.isEmpty() ? 0 : countDocs.get(0).getInteger("totalCount");

        
        List<ProductKeywordResponseDto> dtos = docs.stream()
                .map(doc -> {
                    Document ratioDoc = (Document) doc.get("ratio");
                    Document metricsDoc = (Document) doc.get("metrics");

                    return ProductKeywordResponseDto.builder()
                            .uuid(doc.getString("uuid"))
                            .shopId(doc.getString("shopId"))
                            .createdAt(convertDate(doc.getDate("createdAt")))
                            .from(getLong(doc, "from"))
                            .to(getLong(doc, "to"))
                            .campaignId(getLong(doc, "campaign_id"))
                            .type(doc.getString("type"))
                            .keyword(doc.getString("keyword"))

                            
                            .ratioBroadCir(getInteger(ratioDoc, "broad_cir"))
                            .ratioBroadGmv(getInteger(ratioDoc, "broad_gmv"))
                            .ratioBroadOrder(getInteger(ratioDoc, "broad_order"))
                            .ratioBroadOrderAmount(getInteger(ratioDoc, "broad_order_amount"))
                            .ratioBroadRoi(getInteger(ratioDoc, "broad_roi"))
                            .ratioCheckout(getInteger(ratioDoc, "checkout"))
                            .ratioCheckoutRate(getDouble(ratioDoc, "checkout_rate"))
                            .ratioClick(getInteger(ratioDoc, "click"))
                            .ratioCost(getInteger(ratioDoc, "cost"))
                            .ratioCpc(getInteger(ratioDoc, "cpc"))
                            .ratioCpdc(getInteger(ratioDoc, "cpdc"))
                            .ratioCr(getInteger(ratioDoc, "cr"))
                            .ratioCtr(getInteger(ratioDoc, "ctr"))
                            .ratioDirectCr(getInteger(ratioDoc, "direct_cr"))
                            .ratioDirectCir(getInteger(ratioDoc, "direct_cir"))
                            .ratioDirectGmv(getInteger(ratioDoc, "direct_gmv"))
                            .ratioDirectOrder(getInteger(ratioDoc, "direct_order"))
                            .ratioDirectOrderAmount(getInteger(ratioDoc, "direct_order_amount"))
                            .ratioDirectRoi(getInteger(ratioDoc, "direct_roi"))
                            .ratioImpression(getInteger(ratioDoc, "impression"))
                            .ratioProductClick(getInteger(ratioDoc, "product_click"))
                            .ratioProductImpression(getInteger(ratioDoc, "product_impression"))
                            .ratioProductCtr(getInteger(ratioDoc, "product_ctr"))
                            .ratioReach(getInteger(ratioDoc, "reach"))
                            .ratioPageViews(getInteger(ratioDoc, "page_views"))
                            .ratioUniqueVisitors(getInteger(ratioDoc, "unique_visitors"))
                            .ratioView(getInteger(ratioDoc, "view"))
                            .ratioCpm(getInteger(ratioDoc, "cpm"))
                            .ratioUniqueClickUser(getInteger(ratioDoc, "unique_click_user"))

                            
                            .metricsBroadCir(getInteger(metricsDoc, "broad_cir"))
                            .metricsBroadGmv(getInteger(metricsDoc, "broad_gmv"))
                            .metricsBroadOrder(getInteger(metricsDoc, "broad_order"))
                            .metricsBroadOrderAmount(getInteger(metricsDoc, "broad_order_amount"))
                            .metricsBroadRoi(getInteger(metricsDoc, "broad_roi"))
                            .metricsCheckout(getInteger(metricsDoc, "checkout"))
                            .metricsCheckoutRate(getDouble(metricsDoc, "checkout_rate"))
                            .metricsClick(getInteger(metricsDoc, "click"))
                            .metricsCost(getLong(metricsDoc, "cost"))
                            .metricsCpc(getInteger(metricsDoc, "cpc"))
                            .metricsCpdc(getInteger(metricsDoc, "cpdc"))
                            .metricsCr(getInteger(metricsDoc, "cr"))
                            .metricsCtr(getDouble(metricsDoc, "ctr"))
                            .metricsDirectCr(getInteger(metricsDoc, "direct_cr"))
                            .metricsDirectCir(getInteger(metricsDoc, "direct_cir"))
                            .metricsDirectGmv(getInteger(metricsDoc, "direct_gmv"))
                            .metricsDirectOrder(getInteger(metricsDoc, "direct_order"))
                            .metricsDirectOrderAmount(getInteger(metricsDoc, "direct_order_amount"))
                            .metricsDirectRoi(getInteger(metricsDoc, "direct_roi"))
                            .metricsImpression(getInteger(metricsDoc, "impression"))
                            .metricsAvgRank(getInteger(metricsDoc, "avg_rank"))
                            .metricsProductClick(getInteger(metricsDoc, "product_click"))
                            .metricsProductImpression(getInteger(metricsDoc, "product_impression"))
                            .metricsProductCtr(getInteger(metricsDoc, "product_ctr"))
                            .metricsLocationInAds(getInteger(metricsDoc, "location_in_ads"))
                            .metricsReach(getInteger(metricsDoc, "reach"))
                            .metricsPageViews(getInteger(metricsDoc, "page_views"))
                            .metricsUniqueVisitors(getInteger(metricsDoc, "unique_visitors"))
                            .metricsView(getInteger(metricsDoc, "view"))
                            .metricsCpm(getInteger(metricsDoc, "cpm"))
                            .metricsUniqueClickUser(getInteger(metricsDoc, "unique_click_user"))
                            .build();
                })
                .collect(Collectors.toList());

        
        Map<String, List<ProductKeywordResponseDto>> grouped = dtos.stream()
                .collect(Collectors.groupingBy(ProductKeywordResponseDto::getKeyword));

        List<ProductKeywordResponseWrapperDto> wrappers = grouped.entrySet().stream()
                .map(e -> ProductKeywordResponseWrapperDto.builder()
                        .shopId(shopId)
                        .from(from)
                        .to(to)
                        .data(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(wrappers, pageable, total);
    }

    private LocalDateTime convertDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Integer getInteger(Document doc, String key) {
        if (doc == null) return null;
        Object v = doc.get(key);
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return null;
    }

    private Double getDouble(Document doc, String key) {
        if (doc == null) return null;
        Object v = doc.get(key);
        if (v instanceof Number) {
            return ((Number) v).doubleValue();
        }
        return null;
    }

    private Long getLong(Document doc, String key) {
        if (doc == null) return null;
        Object v = doc.get(key);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return null;
    }

}
