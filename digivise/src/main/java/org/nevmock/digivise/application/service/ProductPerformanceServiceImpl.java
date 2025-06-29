package org.nevmock.digivise.application.service;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.performance.ProductPerformanceResponseDto;
import org.nevmock.digivise.application.dto.product.performance.ProductPerformanceWrapperDto;
import org.nevmock.digivise.domain.port.in.ProductPerformanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductPerformanceServiceImpl implements ProductPerformanceService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Page<ProductPerformanceWrapperDto> findByRange(
            String shopId,
            LocalDateTime from1,
            LocalDateTime to1,
            LocalDateTime from2,
            LocalDateTime to2,
            String name,
            Integer status,
            String salesClassification,
            Pageable pageable
    ) {

        List<ProductPerformanceResponseDto> period1DataList = getAggregatedDataByProductForRange(
                shopId, name, status, salesClassification, from1, to1
        );


        Map<Long, ProductPerformanceResponseDto> period2DataMap = getAggregatedDataByProductForRange(
                shopId, name, status, salesClassification, from2, to2
        ).stream()
                .collect(Collectors.toMap(ProductPerformanceResponseDto::getProductId, Function.identity()));

        if (period1DataList.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }


        List<ProductPerformanceWrapperDto> resultList = period1DataList.stream().map(period1Data -> {
            ProductPerformanceResponseDto period2Data = period2DataMap.get(period1Data.getProductId());


            populateComparisonFields(period1Data, period2Data);

            return ProductPerformanceWrapperDto.builder()
                    .productId(period1Data.getProductId())
                    .shopId(shopId)
                    .from1(from1)
                    .to1(to1)
                    .from2(from2)
                    .to2(to2)
                    .data(Collections.singletonList(period1Data))
                    .build();
        }).collect(Collectors.toList());


        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), resultList.size());

        if (start > resultList.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, resultList.size());
        }

        return new PageImpl<>(resultList.subList(start, end), pageable, resultList.size());
    }

    public Page<ProductPerformanceWrapperDto> findByRange2(
            String shopId,
            LocalDateTime from,
            LocalDateTime to,
            String search,
            Integer status,
            String salesClassification,
            Pageable pageable
    ) {

        List<AggregationOperation> ops = new ArrayList<>();

        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        ops.add(Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("from").gte(fromTimestamp).lte(toTimestamp)
        ));

        ops.add(Aggregation.unwind("data"));

        
        if (search != null && !search.trim().isEmpty()) {
            Criteria searchFilter = Criteria.where("data.name")
                    .regex(".*" + search.trim() + ".*", "i");
            ops.add(Aggregation.match(searchFilter));
        }

        
        if (status != null) {
            ops.add(Aggregation.match(Criteria.where("data.status").is(status)));
        }

        
        if (salesClassification != null && !salesClassification.trim().isEmpty()) {
            ops.add(Aggregation.match(getSalesClassificationCriteria(salesClassification)));
        }

        ops.add(Aggregation.project()
                .and("data.id").as("productId")
                .and("_id").as("id")
                .and("uuid").as("uuid")
                .and("shop_id").as("shopId")
                .and("createdAt").as("createdAt")
                .and("data.name").as("name")
                .and("data.image").as("image")
                .and("data.status").as("status")
                .and("data.uv").as("uv")
                .and("data.pv").as("pv")
                .and("data.likes").as("likes")
                .and("data.bounce_visitors").as("bounceVisitors")
                .and("data.bounce_rate").as("bounceRate")
                .and("data.search_clicks").as("searchClicks")
                .and("data.add_to_cart_units").as("addToCartUnits")
                .and("data.add_to_cart_buyers").as("addToCartBuyers")
                .and("data.placed_sales").as("placedSales")
                .and("data.placed_units").as("placedUnits")
                .and("data.placed_buyers").as("placedBuyers")
                .and("data.paid_sales").as("paidSales")
                .and("data.paid_units").as("paidUnits")
                .and("data.paid_buyers").as("paidBuyers")
                .and("data.confirmed_sales").as("confirmedSales")
                .and("data.confirmed_units").as("confirmedUnits")
                .and("data.confirmed_buyers").as("confirmedBuyers")
                .and("data.uv_to_add_to_cart_rate").as("uvToAddToCartRate")
                .and("data.uv_to_placed_buyers_rate").as("uvToPlacedBuyersRate")
                .and("data.uv_to_confirmed_buyers_rate").as("uvToConfirmedBuyersRate")
                .and("data.placed_buyers_to_confirmed_buyers_rate").as("placedBuyersToConfirmedBuyersRate")
                .and("data.placed_to_paid_buyers_rate").as("confirmedSellRatio")

        );


        ops.add(Aggregation.skip(pageable.getOffset()));
        ops.add(Aggregation.limit(pageable.getPageSize()));

        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops),
                "ProductPerformance",
                Document.class
        );

        List<ProductPerformanceResponseDto> dtos = results.getMappedResults().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        Map<Long, List<ProductPerformanceResponseDto>> grouped = dtos.stream()
                .filter(d -> d.getProductId() != null)
                .collect(Collectors.groupingBy(ProductPerformanceResponseDto::getProductId));

        List<ProductPerformanceWrapperDto> wrappers = grouped.entrySet().stream()
                .map(e -> ProductPerformanceWrapperDto.builder()
                        .productId(e.getKey())
                        .shopId(shopId)
                        .from1(from)
                        .to1(to)
                        .from2(null)
                        .to2(null)
                        .data(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(wrappers, pageable, wrappers.size());
    }

    private void populateComparisonFields(ProductPerformanceResponseDto currentData, ProductPerformanceResponseDto previousData) {
        currentData.setUvComparison(
                roundDouble(calculateComparison(
                        currentData.getUv() != null ? currentData.getUv().doubleValue() : null,
                        previousData != null && previousData.getUv() != null ? previousData.getUv().doubleValue() : null
                ))
        );
        currentData.setPvComparison(
                roundDouble(calculateComparison(
                        currentData.getPv() != null ? currentData.getPv().doubleValue() : null,
                        previousData != null && previousData.getPv() != null ? previousData.getPv().doubleValue() : null
                ))
        );
        currentData.setLikesComparison(
                roundDouble(calculateComparison(
                        currentData.getLikes() != null ? currentData.getLikes().doubleValue() : null,
                        previousData != null && previousData.getLikes() != null ? previousData.getLikes().doubleValue() : null
                ))
        );
        currentData.setBounceVisitorsComparison(
                roundDouble(calculateComparison(
                        currentData.getBounceVisitors() != null ? currentData.getBounceVisitors().doubleValue() : null,
                        previousData != null && previousData.getBounceVisitors() != null ? previousData.getBounceVisitors().doubleValue() : null
                ))
        );
        currentData.setBounceRateComparison(
                roundDouble(calculateComparison(
                        currentData.getBounceRate(),
                        previousData != null ? previousData.getBounceRate() : null
                ))
        );
        currentData.setSearchClicksComparison(
                roundDouble(calculateComparison(
                        currentData.getSearchClicks() != null ? currentData.getSearchClicks().doubleValue() : null,
                        previousData != null && previousData.getSearchClicks() != null ? previousData.getSearchClicks().doubleValue() : null
                ))
        );
        currentData.setAddToCartUnitsComparison(
                roundDouble(calculateComparison(
                        currentData.getAddToCartUnits() != null ? currentData.getAddToCartUnits().doubleValue() : null,
                        previousData != null && previousData.getAddToCartUnits() != null ? previousData.getAddToCartUnits().doubleValue() : null
                ))
        );
        currentData.setAddToCartBuyersComparison(
                roundDouble(calculateComparison(
                        currentData.getAddToCartBuyers() != null ? currentData.getAddToCartBuyers().doubleValue() : null,
                        previousData != null && previousData.getAddToCartBuyers() != null ? previousData.getAddToCartBuyers().doubleValue() : null
                ))
        );
        currentData.setPlacedSalesComparison(
                roundDouble(calculateComparison(
                        currentData.getPlacedSales(),
                        previousData != null ? previousData.getPlacedSales() : null
                ))
        );
        currentData.setPlacedUnitsComparison(
                roundDouble(calculateComparison(
                        currentData.getPlacedUnits() != null ? currentData.getPlacedUnits().doubleValue() : null,
                        previousData != null && previousData.getPlacedUnits() != null ? previousData.getPlacedUnits().doubleValue() : null
                ))
        );
        currentData.setPlacedBuyersComparison(
                roundDouble(calculateComparison(
                        currentData.getPlacedBuyers() != null ? currentData.getPlacedBuyers().doubleValue() : null,
                        previousData != null && previousData.getPlacedBuyers() != null ? previousData.getPlacedBuyers().doubleValue() : null
                ))
        );
        currentData.setPaidSalesComparison(
                roundDouble(calculateComparison(
                        currentData.getPaidSales(),
                        previousData != null ? previousData.getPaidSales() : null
                ))
        );
        currentData.setPaidUnitsComparison(
                roundDouble(calculateComparison(
                        currentData.getPaidUnits() != null ? currentData.getPaidUnits().doubleValue() : null,
                        previousData != null && previousData.getPaidUnits() != null ? previousData.getPaidUnits().doubleValue() : null
                ))
        );
        currentData.setPaidBuyersComparison(
                roundDouble(calculateComparison(
                        currentData.getPaidBuyers() != null ? currentData.getPaidBuyers().doubleValue() : null,
                        previousData != null && previousData.getPaidBuyers() != null ? previousData.getPaidBuyers().doubleValue() : null
                ))
        );
        currentData.setConfirmedSalesComparison(
                roundDouble(calculateComparison(
                        currentData.getConfirmedSales(),
                        previousData != null ? previousData.getConfirmedSales() : null
                ))
        );
        currentData.setConfirmedUnitsComparison(
                roundDouble(calculateComparison(
                        currentData.getConfirmedUnits() != null ? currentData.getConfirmedUnits().doubleValue() : null,
                        previousData != null && previousData.getConfirmedUnits() != null ? previousData.getConfirmedUnits().doubleValue() : null
                ))
        );
        currentData.setConfirmedBuyersComparison(
                roundDouble(calculateComparison(
                        currentData.getConfirmedBuyers() != null ? currentData.getConfirmedBuyers().doubleValue() : null,
                        previousData != null && previousData.getConfirmedBuyers() != null ? previousData.getConfirmedBuyers().doubleValue() : null
                ))
        );

        currentData.setUvToAddToCartRateComparison(
                roundDouble(calculateComparison(
                        currentData.getUvToAddToCartRate(),
                        previousData != null ? previousData.getUvToAddToCartRate() : null
                ))
        );
        currentData.setUvToPlacedBuyersRateComparison(
                roundDouble(calculateComparison(
                        currentData.getUvToPlacedBuyersRate(),
                        previousData != null ? previousData.getUvToPlacedBuyersRate() : null
                ))
        );
        currentData.setUvToConfirmedBuyersRateComparison(
                roundDouble(calculateComparison(
                        currentData.getUvToConfirmedBuyersRate(),
                        previousData != null ? previousData.getUvToConfirmedBuyersRate() : null
                ))
        );
        currentData.setPlacedBuyersToConfirmedBuyersRateComparison(
                roundDouble(calculateComparison(
                        currentData.getPlacedBuyersToConfirmedBuyersRate(),
                        previousData != null ? previousData.getPlacedBuyersToConfirmedBuyersRate() : null
                ))
        );
        currentData.setConfirmedSellRatioComparison(
                roundDouble(calculateComparison(
                        currentData.getConfirmedSellRatio(),
                        previousData != null ? previousData.getConfirmedSellRatio() : null
                ))
        );
        currentData.setUvToPaidBuyersRateComparison(
                roundDouble(calculateComparison(
                        currentData.getUvToPaidBuyersRate(),
                        previousData != null ? previousData.getUvToPaidBuyersRate() : null
                ))
        );
    }

    private Double calculateComparison(Double currentValue, Double previousValue) {
        if (currentValue == null || previousValue == null) {
            return null;
        }
        if (previousValue == 0) {
            return (currentValue > 0) ? 1.0 : 0.0;
        }
        return (currentValue - previousValue) / previousValue;
    }

    private List<ProductPerformanceResponseDto> getAggregatedDataByProductForRange(
            String shopId, String search, Integer status, String salesClassification, LocalDateTime from, LocalDateTime to) {

        List<AggregationOperation> ops = new ArrayList<>();

        long fromTimestamp = from.atZone(ZoneId.systemDefault()).toEpochSecond();
        long toTimestamp = to.atZone(ZoneId.systemDefault()).toEpochSecond();

        ops.add(Aggregation.match(
                Criteria.where("shop_id").is(shopId)
                        .and("from").gte(fromTimestamp).lte(toTimestamp)
        ));
        ops.add(Aggregation.unwind("data"));

        
        if (search != null && !search.trim().isEmpty()) {
            ops.add(Aggregation.match(Criteria.where("data.name").regex(".*" + search.trim() + ".*", "i")));
        }

        
        if (status != null) {
            ops.add(Aggregation.match(Criteria.where("data.status").is(status)));
        }

        
        if (salesClassification != null && !salesClassification.trim().isEmpty()) {
            ops.add(Aggregation.match(getSalesClassificationCriteria(salesClassification)));
        }


        ops.add(Aggregation.group("data.id")
                .avg("data.uv").as("avgUv")
                .avg("data.pv").as("avgPv")
                .avg("data.likes").as("avgLikes")
                .avg("data.bounce_visitors").as("avgBounceVisitors")
                .avg("data.bounce_rate").as("avgBounceRate")
                .avg("data.search_clicks").as("avgSearchClicks")
                .avg("data.add_to_cart_units").as("avgAddToCartUnits")
                .avg("data.add_to_cart_buyers").as("avgAddToCartBuyers")
                .avg("data.placed_sales").as("avgPlacedSales")
                .avg("data.placed_units").as("avgPlacedUnits")
                .avg("data.placed_buyers").as("avgPlacedBuyers")
                .avg("data.paid_sales").as("avgPaidSales")
                .avg("data.paid_units").as("avgPaidUnits")
                .avg("data.paid_buyers").as("avgPaidBuyers")
                .avg("data.confirmed_sales").as("avgConfirmedSales")
                .avg("data.confirmed_units").as("avgConfirmedUnits")
                .avg("data.confirmed_buyers").as("avgConfirmedBuyers")
                .avg("data.uv_to_add_to_cart_rate").as("avgUvToAddToCartRate")
                .avg("data.uv_to_placed_buyers_rate").as("avgUvToPlacedBuyersRate")
                .avg("data.uv_to_confirmed_buyers_rate").as("avgUvToConfirmedBuyersRate")
                .avg("data.placed_buyers_to_confirmed_buyers_rate").as("avgPlacedBuyersToConfirmedBuyersRate")
                .avg("data.placed_to_paid_buyers_rate").as("avgConfirmedSellRatio")
                .avg("data.uv_to_paid_buyers_rate").as("avgUvToPaidBuyersRate")
                .first("data.name").as("name")
                .first("data.image").as("image")
                .first("data.status").as("status")
        );


        ops.add(Aggregation.project()
                .and("_id").as("productId")
                .and("avgUv").as("uv")
                .and("avgPv").as("pv")
                .and("avgLikes").as("likes")
                .and("avgBounceVisitors").as("bounceVisitors")
                .and("avgBounceRate").as("bounceRate")
                .and("avgSearchClicks").as("searchClicks")
                .and("avgAddToCartUnits").as("addToCartUnits")
                .and("avgAddToCartBuyers").as("addToCartBuyers")
                .and("avgPlacedSales").as("placedSales")
                .and("avgPlacedUnits").as("placedUnits")
                .and("avgPlacedBuyers").as("placedBuyers")
                .and("avgPaidSales").as("paidSales")
                .and("avgPaidUnits").as("paidUnits")
                .and("avgPaidBuyers").as("paidBuyers")
                .and("avgConfirmedSales").as("confirmedSales")
                .and("avgConfirmedUnits").as("confirmedUnits")
                .and("avgConfirmedBuyers").as("confirmedBuyers")
                .and("avgUvToAddToCartRate").as("uvToAddToCartRate")
                .and("avgUvToPlacedBuyersRate").as("uvToPlacedBuyersRate")
                .and("avgUvToConfirmedBuyersRate").as("uvToConfirmedBuyersRate")
                .and("avgPlacedBuyersToConfirmedBuyersRate").as("placedBuyersToConfirmedBuyersRate")
                .and("avgUvToPaidBuyersRate").as("uvToPaidBuyersRate")
                .and("avgConfirmedSellRatio").as("confirmedSellRatio")
                .and("from").as("shopeeFrom")
                .and("to").as("shopeeTo")
                .andInclude("name", "image", "status")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                Aggregation.newAggregation(ops), "ProductPerformance", Document.class
        );

        return results.getMappedResults().stream()
                .map(this::mapDocumentToAggregatedDto)
                .collect(Collectors.toList());
    }

    private Criteria getSalesClassificationCriteria(String salesClassification) {
        switch (salesClassification.toLowerCase()) {
            case "high":
                return Criteria.where("data.confirmed_sales").gte(1000000);
            case "medium":
                return Criteria.where("data.confirmed_sales").gte(100000).lt(1000000);
            case "low":
                return Criteria.where("data.confirmed_sales").lt(100000);
            case "no_sales":
                return Criteria.where("data.confirmed_sales").is(0);
            default:
                return new Criteria();
        }
    }

    private ProductPerformanceResponseDto mapDocumentToAggregatedDto(Document doc) {
        return ProductPerformanceResponseDto.builder()
                .productId(getNumberLong(doc, "productId"))
                .name(getString(doc, "name"))
                .image(getString(doc, "image"))
                .status(getNumberInteger(doc, "status"))
                .uv(convertDoubleToLong(getNumberDouble(doc, "uv")))
                .pv(convertDoubleToLong(getNumberDouble(doc, "pv")))
                .likes(convertDoubleToLong(getNumberDouble(doc, "likes")))
                .bounceVisitors(convertDoubleToLong(getNumberDouble(doc, "bounceVisitors")))
                .bounceRate(roundDouble(getNumberDouble(doc, "bounceRate")))
                .searchClicks(convertDoubleToLong(getNumberDouble(doc, "searchClicks")))
                .addToCartUnits(convertDoubleToLong(getNumberDouble(doc, "addToCartUnits")))
                .addToCartBuyers(convertDoubleToLong(getNumberDouble(doc, "addToCartBuyers")))
                .placedSales(getNumberDouble(doc, "placedSales"))
                .placedUnits(convertDoubleToLong(getNumberDouble(doc, "placedUnits")))
                .placedBuyers(convertDoubleToLong(getNumberDouble(doc, "placedBuyers")))
                .paidSales(getNumberDouble(doc, "paidSales"))
                .paidUnits(convertDoubleToLong(getNumberDouble(doc, "paidUnits")))
                .paidBuyers(convertDoubleToLong(getNumberDouble(doc, "paidBuyers")))
                .confirmedSales(getNumberDouble(doc, "confirmedSales"))
                .confirmedUnits(convertDoubleToLong(getNumberDouble(doc, "confirmedUnits")))
                .confirmedBuyers(convertDoubleToLong(getNumberDouble(doc, "confirmedBuyers")))
                .uvToAddToCartRate(getNumberDouble(doc, "uvToAddToCartRate"))
                .uvToPlacedBuyersRate(getNumberDouble(doc, "uvToPlacedBuyersRate"))
                .uvToConfirmedBuyersRate(getNumberDouble(doc, "uvToConfirmedBuyersRate"))
                .placedBuyersToConfirmedBuyersRate(getNumberDouble(doc, "placedBuyersToConfirmedBuyersRate"))
                .uvToPaidBuyersRate(getNumberDouble(doc, "uvToPaidBuyersRate"))
                .confirmedSellRatio(getNumberDouble(doc, "confirmedSellRatio"))
                .shopeeFrom(getNumberLong(doc, "shopeeFrom"))
                .shopeeTo(getNumberLong(doc, "shopeeTo"))
                .build();
    }

    private Long convertDoubleToLong(Double value) {
        return value != null ? Math.round(value) : null;
    }

    private ProductPerformanceResponseDto mapToDto(Document doc) {
        return ProductPerformanceResponseDto.builder()
                .id(getString(doc, "id"))
                .uuid(getString(doc, "uuid"))
                .productId(getNumberLong(doc, "productId"))
                .shopId(getString(doc, "shopId"))
                .createdAt(getDateTime(doc, "createdAt"))
                .name(getString(doc, "name"))
                .image(getString(doc, "image"))
                .status(getNumberInteger(doc, "status"))
                .uv(getNumberLong(doc, "uv"))
                .pv(getNumberLong(doc, "pv"))
                .likes(getNumberLong(doc, "likes"))
                .bounceVisitors(getNumberLong(doc, "bounceVisitors"))
                .bounceRate(getNumberDouble(doc, "bounceRate"))
                .searchClicks(getNumberLong(doc, "searchClicks"))
                .addToCartUnits(getNumberLong(doc, "addToCartUnits"))
                .addToCartBuyers(getNumberLong(doc, "addToCartBuyers"))
                .placedSales(getNumberDouble(doc, "placedSales"))
                .placedUnits(getNumberLong(doc, "placedUnits"))
                .placedBuyers(getNumberLong(doc, "placedBuyers"))
                .paidSales(getNumberDouble(doc, "paidSales"))
                .paidUnits(getNumberLong(doc, "paidUnits"))
                .paidBuyers(getNumberLong(doc, "paidBuyers"))
                .confirmedSales(getNumberDouble(doc, "confirmedSales"))
                .confirmedUnits(getNumberLong(doc, "confirmedUnits"))
                .confirmedBuyers(getNumberLong(doc, "confirmedBuyers"))
                .uvToAddToCartRate(getNumberDouble(doc, "uvToAddToCartRate"))
                .uvToPlacedBuyersRate(getNumberDouble(doc, "uvToPlacedBuyersRate"))
                .uvToConfirmedBuyersRate(getNumberDouble(doc, "uvToConfirmedBuyersRate"))
                .placedBuyersToConfirmedBuyersRate(getNumberDouble(doc, "placedBuyersToConfirmedBuyersRate"))
                .uvToPaidBuyersRate(getNumberDouble(doc, "uvToPaidBuyersRate"))
                .confirmedSellRatio(getNumberDouble(doc, "confirmedSellRatio"))
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

    private Double getNumberDouble(Document d, String key) {
        Object v = d.get(key);
        return v instanceof Number ? ((Number) v).doubleValue() : null;
    }

    private LocalDateTime getDateTime(Document d, String key) {
        Object v = d.get(key);
        if (v instanceof Date) {
            return ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return null;
    }


    public void debugProductPerformanceData(String shopId) {
        System.out.println("=== SIMPLE DEBUG ===");


        long total = mongoTemplate.count(
                org.springframework.data.mongodb.core.query.Query.query(
                        Criteria.where("shop_id").is(shopId)
                ),
                "ProductPerformance"
        );
        System.out.println("Total documents with shop_id " + shopId + ": " + total);


        Document sample = mongoTemplate.findOne(
                org.springframework.data.mongodb.core.query.Query.query(
                        Criteria.where("shop_id").is(shopId)
                ).limit(1),
                Document.class,
                "ProductPerformance"
        );

        if (sample != null) {
            System.out.println("Sample document structure:");
            System.out.println(sample.toJson());
        } else {
            System.out.println("No documents found for shop_id: " + shopId);
        }


        if (sample != null) {
            System.out.println("Available fields: " + sample.keySet());


            if (sample.containsKey("data")) {
                Object dataField = sample.get("data");
                System.out.println("Data field type: " + dataField.getClass().getSimpleName());

                if (dataField instanceof List) {
                    List<?> dataList = (List<?>) dataField;
                    System.out.println("Data array size: " + dataList.size());
                    if (!dataList.isEmpty()) {
                        System.out.println("First data item: " + dataList.get(0));
                    }
                }
            }


            if (sample.containsKey("createdAt")) {
                Object createdAt = sample.get("createdAt");
                System.out.println("CreatedAt field type: " + createdAt.getClass().getSimpleName());
                System.out.println("CreatedAt value: " + createdAt);
            }
        }
    }

    private Double roundDouble(Double value) {
        if (value == null) return null;
        return Math.round(value * 100.0) / 100.0;
    }
}