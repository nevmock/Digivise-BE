package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseDto;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseWrapperDto;
import org.nevmock.digivise.domain.model.KPI;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.port.in.ProductKeywordService;
import org.nevmock.digivise.domain.port.out.KPIRepository;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.utils.MathKt;
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

    @Autowired
    private KPIRepository kpiRepository;

    @Autowired
    private MerchantRepository merchantRepository;

    @Override
    public Page<ProductKeywordResponseWrapperDto> findByRange(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            String name,
            Pageable pageable
    ) {
        Merchant merchant = merchantRepository
                .findByShopeeMerchantId(shopId)
                .orElseThrow(() -> new RuntimeException("Merchant not found: " + shopId));
        KPI kpi = kpiRepository
                .findByMerchantId(merchant.getId())
                .orElseThrow(() -> new RuntimeException("KPI not found for merchant " + merchant.getId()));

        List<AggregationOperation> ops = new ArrayList<>();

        long fromTs = from.atZone(ZoneId.systemDefault()).toEpochSecond();

        ops.add(Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("from").gte(fromTs)
        ));

        ops.add(Aggregation.unwind("data.data"));

        if (name != null && !name.trim().isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.data.key").regex(".*" + name.trim() + ".*", "i")
            ));
        }

        ops.add(Aggregation.project()
                .and("uuid").as("uuid")
                .and("shop_id").as("shopId")
                .and("campaign_id").as("campaignId")
                .and("type").as("type")
                .and("createdAt").as("createdAt")
                .and("from").as("from")
                .and("to").as("to")
                .and("data.data.key").as("keyword")
                .and("data.data.ratio").as("ratio")
                .and("data.data.metrics").as("metrics")
        );

        FacetOperation facet = Aggregation.facet(
                        Aggregation.skip((long) pageable.getOffset()),
                        Aggregation.limit(pageable.getPageSize())
                ).as("pagedResults")
                .and(Aggregation.count().as("totalCount")).as("countResult");
        ops.add(facet);

        // execute aggregation
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
                            .campaignId(getLong(doc, "campaignId"))
                            .type(doc.getString("type"))
                            .createdAt(convertDate(doc.getDate("createdAt")))
                            .from(getLong(doc, "from"))
                            .to(getLong(doc, "to"))

                            .ratioBroadCir(getDouble(ratioDoc, "broad_cir"))
                            .ratioBroadGmv(getDouble(ratioDoc, "broad_gmv"))
                            .ratioBroadOrder(getDouble(ratioDoc, "broad_order"))
                            .ratioBroadOrderAmount(getDouble(ratioDoc, "broad_order_amount"))
                            .ratioBroadRoi(getDouble(ratioDoc, "broad_roi"))
                            .ratioCheckout(getDouble(ratioDoc, "checkout"))
                            .ratioCheckoutRate(getDouble(ratioDoc, "checkout_rate"))
                            .ratioClick(getDouble(ratioDoc, "click"))
                            .ratioCost(getDouble(ratioDoc, "cost"))
                            .ratioCpc(getDouble(ratioDoc, "cpc"))
                            .ratioCpdc(getDouble(ratioDoc, "cpdc"))
                            .ratioCr(getDouble(ratioDoc, "cr"))
                            .ratioCtr(getDouble(ratioDoc, "ctr"))
                            .ratioDirectCr(getDouble(ratioDoc, "direct_cr"))
                            .ratioDirectCir(getDouble(ratioDoc, "direct_cir"))
                            .ratioDirectGmv(getDouble(ratioDoc, "direct_gmv"))
                            .ratioDirectOrder(getDouble(ratioDoc, "direct_order"))
                            .ratioDirectOrderAmount(getDouble(ratioDoc, "direct_order_amount"))
                            .ratioDirectRoi(getDouble(ratioDoc, "direct_roi"))
                            .ratioImpression(getDouble(ratioDoc, "impression"))
                            .ratioView(getDouble(ratioDoc, "view"))

                            .metricsBroadCir(getDouble(metricsDoc, "broad_cir"))
                            .metricsBroadGmv(getLong(metricsDoc, "broad_gmv"))
                            .metricsBroadOrder(getInteger(metricsDoc, "broad_order"))
                            .metricsBroadOrderAmount(getInteger(metricsDoc, "broad_order_amount"))
                            .metricsBroadRoi(getDouble(metricsDoc, "broad_roi"))
                            .metricsCheckout(getInteger(metricsDoc, "checkout"))
                            .metricsCheckoutRate(getDouble(metricsDoc, "checkout_rate"))
                            .metricsClick(getDouble(metricsDoc, "click"))
                            .metricsCost(getLong(metricsDoc, "cost"))
                            .metricsCr(getDouble(metricsDoc, "cr"))
                            .metricsCtr(getDouble(metricsDoc, "ctr"))
                            .metricsDirectGmv(getLong(metricsDoc, "direct_gmv"))
                            .metricsDirectOrder(getInteger(metricsDoc, "direct_order"))
                            .metricsDirectRoi(getDouble(metricsDoc, "direct_roi"))
                            .metricsImpression(getInteger(metricsDoc, "impression"))
                            .metricsAvgRank(getInteger(metricsDoc, "avg_rank"))
                            .metricsView(getLong(metricsDoc, "view"))
                            .keyword(doc.getString("keyword"))
                            .insight(MathKt.renderInsight(
                                    MathKt.formulateRecommendation(
                                            getDouble(metricsDoc, "cpc"), getDouble(metricsDoc, "broad_cir"), getDouble(metricsDoc, "click"), kpi, null, null
                                    )

                            ))
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
        if (v instanceof Number) return ((Number) v).intValue();
        return null;
    }

    private Long getLong(Document doc, String key) {
        if (doc == null) return null;
        Object v = doc.get(key);
        if (v instanceof Number) return ((Number) v).longValue();
        return null;
    }

    private Double getDouble(Document doc, String key) {
        if (doc == null) return null;
        Object v = doc.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return null;
    }
}
