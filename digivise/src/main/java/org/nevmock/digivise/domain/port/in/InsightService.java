package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.insight.InsightRequestDto;
import org.nevmock.digivise.application.dto.insight.InsightResponseDto;

import java.util.List;

public interface InsightService {
    List<InsightResponseDto> getInsights(InsightRequestDto insightRequestDto);

    InsightResponseDto getInsightById(String id);

    List<InsightResponseDto> getInsightsByMerchantId(String merchantId);

    List<InsightResponseDto> getInsightsByCampaignId(Long campaignId);

    List<InsightResponseDto> getInsightsByAdsId(String adsId);
}
