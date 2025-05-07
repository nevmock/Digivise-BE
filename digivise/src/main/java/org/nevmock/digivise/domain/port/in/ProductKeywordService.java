package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseDto;
import org.nevmock.digivise.domain.model.mongo.keyword.ProductKeyword;

import java.util.List;

public interface ProductKeywordService {
    List<ProductKeywordResponseDto> findAll();

    List<ProductKeywordResponseDto> findByCampaignId(Long campaignId);

    List<ProductKeywordResponseDto> findByShopId(String shopId);

    List<ProductKeywordResponseDto> findByCreatedAtBetween(String from, String to);

    List<ProductKeywordResponseDto> findByCreatedAtGreaterThanEqual(String from);

    List<ProductKeywordResponseDto> findByCreatedAtLessThanEqual(String to);

    List<ProductKeywordResponseDto> findByFromAndTo(String from, String to);
}
