package org.nevmock.digivise.infrastructure.config.mongo;

import org.bson.Document;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseDto;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import java.time.ZoneId;

// 1) Create a converter from Document → ProductKeywordResponseDto
@ReadingConverter
public class DocumentToProductKeywordResponseDtoConverter
        implements Converter<Document, ProductKeywordResponseDto> {

    @Override
    public ProductKeywordResponseDto convert(Document src) {
        return ProductKeywordResponseDto.builder()
                .campaignId(src.getLong("campaignId"))
                .id(src.getObjectId("id").toString())
                .key(src.getString("key"))
                .shopeeMerchantId(src.getString("shopeeMerchantId"))
                .acos(getDoubleOrNull(src, "acos"))
                .cpc(getDoubleOrNull(src, "cpc"))
                .from(src.getString("from"))
                .to(src.getString("to"))
                .createdAt(src.containsKey("createdAt")
                        ? src.getDate("createdAt").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        : null)
                .cost(getDoubleOrNull(src, "cost"))
                .click(getDoubleOrNull(src, "click"))
                .ctr(getDoubleOrNull(src, "ctr"))
                .insight(src.getString("insight"))
                .impression(getDoubleOrNull(src, "impression"))
                .build();
    }

    // helper to avoid ClassCastException if field missing or not a Double
    private Double getDoubleOrNull(Document doc, String field) {
        Object val = doc.get(field);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return null;
    }
}
