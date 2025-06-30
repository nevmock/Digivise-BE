package org.nevmock.digivise.application.service;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.kpi.KPIRequestDto;
import org.nevmock.digivise.application.dto.kpi.KPIResponseDto;
import org.nevmock.digivise.domain.model.KPI;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.model.User;
import org.nevmock.digivise.domain.port.in.KPIService;
import org.nevmock.digivise.domain.port.out.KPIRepository;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.domain.port.out.UserRepository;
import org.nevmock.digivise.infrastructure.config.exceptions.ForbiddenException;
import org.nevmock.digivise.infrastructure.config.exceptions.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.nevmock.digivise.utils.UtilsKt.getCurrentUser;

@Service
@RequiredArgsConstructor
public class KPIServiceImpl implements KPIService {

    private final KPIRepository kpiRepository;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;

    @Override
    public KPIResponseDto getKPIById(UUID kpiId) {
        Optional<KPIResponseDto> kpiData = kpiRepository.findKPIById(kpiId)
                .map(kpi -> new KPIResponseDto(kpi.getId(), kpi.getMerchant().getId(), kpi.getUser().getId(), kpi.getMaxCpc(), kpi.getMaxAcos(), kpi.getCpcScaleFactor(), kpi.getAcosScaleFactor(), kpi.getMaxAdjustment(), kpi.getMinAdjustment(), kpi.getMaxKlik(), kpi.getMinKlik(), kpi.getMinBidSearch(), kpi.getMinBidReco(), kpi.getMultiplier(), kpi.getRoasKpi()));

        if (kpiData.isEmpty()) {
            throw new NotFoundException("KPI not found with ID: " + kpiId);
        }

        return kpiData.get();
    }

    @Override
    public KPIResponseDto getKPIByMerchantId(UUID merchantId) {
        Optional<KPIResponseDto> kpiData = kpiRepository.findByMerchantId(merchantId)
                .map(kpi -> new KPIResponseDto(kpi.getId(), kpi.getMerchant().getId(), kpi.getUser().getId(), kpi.getMaxCpc(), kpi.getMaxAcos(), kpi.getCpcScaleFactor(), kpi.getAcosScaleFactor(), kpi.getMaxAdjustment(), kpi.getMinAdjustment(), kpi.getMaxKlik(), kpi.getMinKlik(), kpi.getMinBidSearch(), kpi.getMinBidReco(), kpi.getMultiplier(), kpi.getRoasKpi()));

        if (kpiData.isEmpty()) {
            throw new NotFoundException("KPI not found with Merchant ID: " + merchantId);
        }

        return kpiData.get();
    }

    @Override
    public KPIResponseDto createKPI(KPIRequestDto kpi) {
        Merchant merchant = merchantRepository.findById(kpi.getMerchantId())
                .orElseThrow(() -> new NotFoundException("Merchant not found with ID: " + kpi.getMerchantId()));

        User user = userRepository.findById(kpi.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + kpi.getUserId()));

        KPI newKPI = new KPI();
        newKPI.setId(UUID.randomUUID());
        newKPI.setMerchant(merchant);
        newKPI.setUser(user);
        newKPI.setMaxCpc(kpi.getMaxCpc());
        newKPI.setMaxAcos(kpi.getMaxAcos());
        newKPI.setCpcScaleFactor(kpi.getCpcScaleFactor());
        newKPI.setAcosScaleFactor(kpi.getAcosScaleFactor());
        newKPI.setMaxAdjustment(kpi.getMaxAdjustment());
        newKPI.setMinAdjustment(kpi.getMinAdjustment());
        newKPI.setMaxKlik(kpi.getMaxKlik());
        newKPI.setMinKlik(kpi.getMinKlik());
        newKPI.setMinBidSearch(kpi.getMinBidSearch());
        newKPI.setMinBidReco(kpi.getMinBidReco());
        newKPI.setMultiplier(kpi.getMultiplier());
        newKPI.setRoasKpi(kpi.getRoasKpi());

        kpiRepository.save(newKPI);

        return new KPIResponseDto(
                newKPI.getId(),
                newKPI.getMerchant().getId(),
                newKPI.getUser().getId(),
                newKPI.getMaxCpc(),
                newKPI.getMaxAcos(),
                newKPI.getCpcScaleFactor(),
                newKPI.getAcosScaleFactor(),
                newKPI.getMaxAdjustment(),
                newKPI.getMinAdjustment(),
                newKPI.getMaxKlik(),
                newKPI.getMinKlik(),
                newKPI.getMinBidSearch(),
                newKPI.getMinBidReco(),
                newKPI.getMultiplier(),
                newKPI.getRoasKpi()
        );
    }

    @Override
    public KPIResponseDto updateKPI(KPIRequestDto kpi) {
        Optional<KPI> existingKPI = kpiRepository.findByMerchantId(kpi.getMerchantId());

        User user = Objects.requireNonNull(getCurrentUser(userRepository))
                .orElseThrow(() -> new NotFoundException("User not found!"));

        if (existingKPI.isEmpty()) {
            throw new NotFoundException("KPI not found with Merchant ID: " + kpi.getMerchantId());
        }

        if (!existingKPI.get().getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Unauthorized access to KPI with Merchant ID: " + kpi.getMerchantId());
        }

        KPI updatedKPI = existingKPI.get();
        updatedKPI.setMaxCpc(kpi.getMaxCpc());
        updatedKPI.setMaxAcos(kpi.getMaxAcos());
        updatedKPI.setCpcScaleFactor(kpi.getCpcScaleFactor());
        updatedKPI.setAcosScaleFactor(kpi.getAcosScaleFactor());
        updatedKPI.setMaxAdjustment(kpi.getMaxAdjustment());
        updatedKPI.setMinAdjustment(kpi.getMinAdjustment());
        updatedKPI.setMaxKlik(kpi.getMaxKlik());
        updatedKPI.setMinKlik(kpi.getMinKlik());
        updatedKPI.setMinBidSearch(kpi.getMinBidSearch());
        updatedKPI.setMinBidReco(kpi.getMinBidReco());
        updatedKPI.setMultiplier(kpi.getMultiplier());
        updatedKPI.setRoasKpi(kpi.getRoasKpi());
        updatedKPI.setMerchant(merchantRepository.findById(kpi.getMerchantId())
                .orElseThrow(() -> new NotFoundException("Merchant not found with ID: " + kpi.getMerchantId())));

        kpiRepository.save(updatedKPI);

        return new KPIResponseDto(updatedKPI.getId(),
                updatedKPI.getMerchant().getId(),
                updatedKPI.getUser().getId(),
                updatedKPI.getMaxCpc(),
                updatedKPI.getMaxAcos(),
                updatedKPI.getCpcScaleFactor(),
                updatedKPI.getAcosScaleFactor(),
                updatedKPI.getMaxAdjustment(),
                updatedKPI.getMinAdjustment(),
                updatedKPI.getMaxKlik(),
                updatedKPI.getMinKlik(),
                updatedKPI.getMinBidSearch(),
                updatedKPI.getMinBidReco(),
                updatedKPI.getMultiplier(),
                updatedKPI.getRoasKpi()
        );
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
                .map(kpi -> new KPIResponseDto(kpi.getId(), kpi.getMerchant().getId(), kpi.getUser().getId(), kpi.getMaxCpc(), kpi.getMaxAcos(), kpi.getCpcScaleFactor(), kpi.getAcosScaleFactor(), kpi.getMaxAdjustment(), kpi.getMinAdjustment(), kpi.getMaxKlik(), kpi.getMinKlik(), kpi.getMinBidSearch(), kpi.getMinBidReco(), kpi.getMultiplier(), kpi.getRoasKpi()))
                .toList();
    }

    @Override
    public List<KPIResponseDto> getKPIsByUserId(UUID userId) {
        return kpiRepository.findByUserId(userId).stream()
                .map(kpi -> new KPIResponseDto(kpi.getId(), kpi.getMerchant().getId(), kpi.getUser().getId(), kpi.getMaxCpc(), kpi.getMaxAcos(), kpi.getCpcScaleFactor(), kpi.getAcosScaleFactor(), kpi.getMaxAdjustment(), kpi.getMinAdjustment(), kpi.getMaxKlik(), kpi.getMinKlik(), kpi.getMinBidSearch(), kpi.getMinBidReco(), kpi.getMultiplier(), kpi.getRoasKpi()))
                .toList();
    }
}
