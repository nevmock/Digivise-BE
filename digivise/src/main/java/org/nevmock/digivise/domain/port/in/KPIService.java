package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.kpi.KPIRequestDto;
import org.nevmock.digivise.application.dto.kpi.KPIResponseDto;

import java.util.List;
import java.util.UUID;

public interface KPIService {
    KPIResponseDto getKPIById(UUID kpiId);
    KPIResponseDto getKPIByMerchantId(UUID merchantId);
    KPIResponseDto createKPI(KPIRequestDto kpi);
    KPIResponseDto updateKPI(KPIRequestDto kpi);
    void deleteKPIById(UUID kpiId);
    List<KPIResponseDto> getAllKPIs();
    List<KPIResponseDto> getKPIsByUserId(UUID userId);
}
