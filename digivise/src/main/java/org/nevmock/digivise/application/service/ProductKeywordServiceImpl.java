package org.nevmock.digivise.application.service;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.nevmock.digivise.application.dto.product.keyword.ProductKeywordResponseDto;
import org.nevmock.digivise.domain.model.mongo.keyword.ProductKeyword;
import org.nevmock.digivise.domain.port.in.ProductKeywordService;
import org.nevmock.digivise.domain.port.out.ProductKeywordRepository;

import java.util.List;

@AllArgsConstructor
public class ProductKeywordServiceImpl implements ProductKeywordService {
    private final ProductKeywordRepository productKeywordRepository;

    @Override
    public List<ProductKeywordResponseDto> findAll() {
//        return productKeywordRepository.findAll().stream()
//                .flatMap( productKeyword -> {
//                            String id = productKeyword.getId();
//
//                            if (productKeyword.getData() != null) {
//                                ProductKeywordResponseDto productKeywordResponseDto = new ProductKeywordResponseDto();
//
//                                productKeywordResponseDto.setId(id);
//                                productKeywordResponseDto.setKey(productKeyword.getData().getKey());
//                                productKeywordResponseDto.setShopeeMerchantId(productKeyword.getShopeeMerchantId());
//                            }
//                        }
//
//                )
//                .map(productKeyword -> {
//                    ProductKeywordResponseDto dto = new ProductKeywordResponseDto();
//                    dto.setId(productKeyword.getId());
//                    dto.setKey(productKeyword.getData().getKey());
//                    dto.setShopeeMerchantId(productKeyword.getShopeeMerchantId());
//                    return dto;
//                })
        return null;
    }

    @Override
    public List<ProductKeywordResponseDto> findByCampaignId(Long campaignId) {
        // Implementation here
        return null;
    }

    @Override
    public List<ProductKeywordResponseDto> findByShopId(String shopId) {
        // Implementation here
        return null;
    }

    @Override
    public List<ProductKeywordResponseDto> findByCreatedAtBetween(String from, String to) {
        // Implementation here
        return null;
    }

    @Override
    public List<ProductKeywordResponseDto> findByCreatedAtGreaterThanEqual(String from) {
        // Implementation here
        return null;
    }

    @Override
    public List<ProductKeywordResponseDto> findByCreatedAtLessThanEqual(String to) {
        // Implementation here
        return null;
    }

    @Override
    public List<ProductKeywordResponseDto> findByFromAndTo(String from, String to) {
        // Implementation here
        return null;
    }
}
