package org.nevmock.digivise.application.service;

import lombok.AllArgsConstructor;
import org.nevmock.digivise.application.dto.insight.InsightRequestDto;
import org.nevmock.digivise.application.dto.insight.InsightResponseDto;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.model.mongo.ads.ProductAds;
import org.nevmock.digivise.domain.port.in.InsightService;
import org.nevmock.digivise.domain.port.out.KPIRepository;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.domain.port.out.ProductAdsRepository;

import java.util.List;

@AllArgsConstructor
public class InsightServiceImpl implements InsightService {
    private final KPIRepository kpiRepository;
    private final MerchantRepository merchantRepository;
    private final ProductAdsRepository productAdsRepository;

    @Override
    public List<InsightResponseDto> getInsights(InsightRequestDto insightRequestDto) {
        return List.of();
    }

    @Override
    public InsightResponseDto getInsightById(String id) {
        return null;
    }

    @Override
    public List<InsightResponseDto> getInsightsByMerchantId(String merchantId) {
        Merchant merchant = merchantRepository.findByShopeeMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found with ID: " + merchantId));

        List<ProductAds> productAdsList = productAdsRepository.findByShopId(merchant.getMerchantShopeeId());

        return List.of();
    }

    @Override
    public List<InsightResponseDto> getInsightsByCampaignId(Long campaignId) {
        return List.of();
    }

    @Override
    public List<InsightResponseDto> getInsightsByAdsId(String adsId) {
        return List.of();
    }
}
