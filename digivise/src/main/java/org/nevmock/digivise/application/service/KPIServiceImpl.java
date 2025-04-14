package org.nevmock.digivise.application.service;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.kpi.KPIRequestDto;
import org.nevmock.digivise.application.dto.kpi.KPIResponseDto;
import org.nevmock.digivise.domain.model.KPI;
import org.nevmock.digivise.domain.port.in.KPIService;
import org.nevmock.digivise.domain.port.out.KPIRepository;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.infrastructure.config.exceptions.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KPIServiceImpl implements KPIService {

    private final KPIRepository kpiRepository;

    @Override
    public KPIResponseDto getKPIById(UUID kpiId) {
        Optional<KPIResponseDto> kpiData = kpiRepository.findKPIById(kpiId)
                .map(kpi -> new KPIResponseDto(kpi.getId(), kpi.getCpc(), kpi.getAcos(), kpi.getCtr(), kpi.getMerchant()));

        if (kpiData.isEmpty()) {
            throw new NotFoundException("KPI not found with ID: " + kpiId);
        }

        return kpiData.get();
    }

    @Override
    public KPIResponseDto getKPIByMerchantId(UUID merchantId) {
        Optional<KPIResponseDto> kpiData = kpiRepository.findByMerchantId(merchantId)
                .map(kpi -> new KPIResponseDto(kpi.getId(), kpi.getCpc(), kpi.getAcos(), kpi.getCtr(), kpi.getMerchant()));

        if (kpiData.isEmpty()) {
            throw new NotFoundException("KPI not found with Merchant ID: " + merchantId);
        }

        return kpiData.get();
    }

    @Override
    public KPIResponseDto createKPI(KPIRequestDto kpi) {
        KPI newKPI = new KPI();
        newKPI.setCpc(kpi.getCpc());
        newKPI.setAcos(kpi.getAcos());
        newKPI.setCtr(kpi.getCtr());
        newKPI.setMerchant(kpi.getMerchant());
        newKPI.setId(UUID.randomUUID());

        kpiRepository.save(newKPI);

        return new KPIResponseDto(newKPI.getId(), newKPI.getCpc(), newKPI.getAcos(), newKPI.getCtr(), newKPI.getMerchant());
    }

    @Override
    public KPIResponseDto updateKPI(KPIRequestDto kpi) {
        Optional<KPI> existingKPI = kpiRepository.findKPIById(kpi.getId());

        if (existingKPI.isEmpty()) {
            throw new NotFoundException("KPI not found with ID: " + kpi.getId());
        }

        KPI updatedKPI = existingKPI.get();
        updatedKPI.setCpc(kpi.getCpc());
        updatedKPI.setAcos(kpi.getAcos());
        updatedKPI.setCtr(kpi.getCtr());
        updatedKPI.setMerchant(kpi.getMerchant());

        kpiRepository.save(updatedKPI);

        return new KPIResponseDto(updatedKPI.getId(), updatedKPI.getCpc(), updatedKPI.getAcos(), updatedKPI.getCtr(), updatedKPI.getMerchant());
    }

    @Override
    public void deleteKPIById(UUID kpiId) {
        Optional<KPI> existingKPI = kpiRepository.findKPIById(kpiId);

        if (existingKPI.isEmpty()) {
            throw new NotFoundException("KPI not found with ID: " + kpiId);
        }

        kpiRepository.deleteByID(kpiId);
    }

    @Override
    public List<KPIResponseDto> getAllKPIs() {
        return kpiRepository.findAll().stream()
                .map(kpi -> new KPIResponseDto(kpi.getId(), kpi.getCpc(), kpi.getAcos(), kpi.getCtr(), kpi.getMerchant()))
                .toList();
    }

    @Override
    public List<KPIResponseDto> getKPIsByUserId(UUID userId) {
        return kpiRepository.findByUserId(userId).stream()
                .map(kpi -> new KPIResponseDto(kpi.getId(), kpi.getCpc(), kpi.getAcos(), kpi.getCtr(), kpi.getMerchant()))
                .toList();
    }
}
