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

        // Match by shop_id and from >=
        ops.add(Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("from").gte(fromTs)
        ));

        // Unwind the data array
        ops.add(Aggregation.unwind("data"));

        // Optional: filter by keyword in data.key
        if (name != null && !name.trim().isEmpty()) {
            ops.add(Aggregation.match(
                    Criteria.where("data.key").regex(".*" + name.trim() + ".*", "i")
            ));
        }

        // Project only the fields you need
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

        // Facet for pagination and counting
        FacetOperation facet = Aggregation.facet(
                        Aggregation.skip((long) pageable.getOffset()),
                        Aggregation.limit(pageable.getPageSize())
                ).as("pagedResults")
                .and(Aggregation.count().as("totalCount")).as("countResult");
        ops.add(facet);

        // Execute aggregation
        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops),
                "ProductKey",
                Document.class
        );

        Document root = results.getMappedResults().stream().findFirst().orElse(null);
        if (root == null) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // Extract paged documents
        @SuppressWarnings("unchecked")
        List<Document> docs = (List<Document>) root.get("pagedResults");

        // Total count
        @SuppressWarnings("unchecked")
        List<Document> countDocs = (List<Document>) root.get("countResult");
        long total = countDocs.isEmpty() ? 0 : countDocs.get(0).getInteger("totalCount");

        // Map to DTOs
        List<ProductKeywordResponseDto> dtos = docs.stream()
                .map(doc -> ProductKeywordResponseDto.builder()
                        .uuid(doc.getString("uuid"))
                        .shopId(doc.getString("shopId"))
                        .createdAt(convertDate(doc.getDate("createdAt")))
                        .from(getLong(doc, "from"))
                        .to(getLong(doc, "to"))
                        .keyword(doc.getString("keyword"))
                        //.ratio((Document) doc.get("ratio"))
//                        .metrics((Document) doc.get("metrics"))
                        .build()
                )
                .collect(Collectors.toList());

        // Group by keyword (or another key) if needed
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

    private Long getLong(Document doc, String key) {
        Object v = doc.get(key);
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return null;
    }

}
