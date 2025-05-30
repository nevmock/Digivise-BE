package org.nevmock.digivise.application.service;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.kpi.KPIResponseDto;
import org.nevmock.digivise.application.dto.merchant.MerchantRequestDto;
import org.nevmock.digivise.application.dto.merchant.MerchantResponseDto;
import org.nevmock.digivise.domain.model.KPI;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.model.User;
import org.nevmock.digivise.domain.port.in.MerchantService;
import org.nevmock.digivise.domain.port.out.KPIRepository;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

import static org.nevmock.digivise.utils.UtilsKt.getCurrentUser;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final KPIRepository kpiRepository;

    @Override
    public MerchantResponseDto createMerchant(MerchantRequestDto merchant) {
        User user = Objects.requireNonNull(getCurrentUser(userRepository))
                .orElseThrow(() -> new RuntimeException("User not found!"));


        Merchant newMerchant = new Merchant();

        newMerchant.setId(UUID.randomUUID());
        newMerchant.setMerchantName(merchant.getMerchantName());
        newMerchant.setUser(user);
        newMerchant.setMerchantShopeeId(merchant.getMerchantShopeeId());
        newMerchant.setSessionPath(merchant.getSessionPath());
        newMerchant.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        newMerchant.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        KPI kpi = KPI
                .builder()
                .user(user)
                .merchant(newMerchant)
                .maxAcos(0.0)
                .maxCpc(0.0)
                .maxAdjustment(0.0)
                .multiplier(0.0)
                .minBidSearch(0.0)
                .maxKlik(0.0)
                .minKlik(0.0)
                .cpcScaleFactor(0.0)
                .acosScaleFactor(0.0)
                .id(UUID.randomUUID())
                .minAdjustment(0.0)
                .minBidReco(0.0)
                .build();

        merchantRepository.save(newMerchant);
        kpiRepository.save(kpi);

        return toDto(newMerchant, kpi);
    }

    @Override
    public MerchantResponseDto getMerchantById(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found with ID: " + merchantId));

        KPI kpi = kpiRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("KPI not found with Merchant ID: " + merchantId));

        return toDto(merchant, kpi);
    }

    @Override
    public List<MerchantResponseDto> getAllMerchants() {
        List<Merchant> merchants = merchantRepository.findAll();
        List<KPI> kpiList = kpiRepository.findAll();

        Map<UUID, KPI> merchantIdToKpi = kpiList.stream()
                .collect(Collectors.toMap(k -> k.getMerchant().getId(), k -> k));

        return merchants.stream()
                .map(m -> toDto(m, merchantIdToKpi.get(m.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public List<MerchantResponseDto> getMerchantsByUserId(UUID userId) {
        List<Merchant> merchants = merchantRepository.findByUserId(userId);
        List<KPI> kpiList = kpiRepository.findByUserId(userId);

        Map<UUID, KPI> merchantIdToKpi = kpiList.stream()
                .collect(Collectors.toMap(k -> k.getMerchant().getId(), k -> k));

        return merchants.stream()
                .map(m -> toDto(m, merchantIdToKpi.get(m.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public MerchantResponseDto updateMerchant(UUID merchantId, MerchantRequestDto updatedMerchant) {
        Optional<Merchant> existingMerchant = merchantRepository.findById(merchantId);

        KPI existingKPI = kpiRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("KPI not found with Merchant ID: " + merchantId));

        if (existingMerchant.isEmpty()) {
            throw new RuntimeException("Merchant not found with ID: " + merchantId);
        }

        Merchant merchantToUpdate = existingMerchant.get();

        merchantToUpdate.setMerchantName(updatedMerchant.getMerchantName());
        merchantToUpdate.setSessionPath(updatedMerchant.getSessionPath());
        merchantToUpdate.setUser(Objects.requireNonNull(getCurrentUser(userRepository))
                .orElseThrow(() -> new RuntimeException("User not found!")));
        merchantToUpdate.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        merchantRepository.save(merchantToUpdate);



        return toDto(merchantToUpdate, existingKPI);
    }

    @Override
    public void deleteMerchant(UUID merchantId) {
        merchantRepository.deleteById(merchantId);
    }

    @Override
    public void deleteMerchantByUserId(UUID userId) {
        List<Merchant> merchants = merchantRepository.findByUserId(userId);
        for (Merchant merchant : merchants) {
            merchantRepository.deleteById(merchant.getId());
        }
    }

    private MerchantResponseDto toDto(Merchant merchant, KPI kpi) {
        KPIResponseDto kpiResp = KPIResponseDto
                .builder()
                .id(kpi.getId())
                .merchantId(kpi.getMerchant().getId())
                .userId(kpi.getUser().getId())
                .maxCpc(kpi.getMaxCpc())
                .maxAcos(kpi.getMaxAcos())
                .cpcScaleFactor(kpi.getCpcScaleFactor())
                .acosScaleFactor(kpi.getAcosScaleFactor())
                .maxAdjustment(kpi.getMaxAdjustment())
                .minAdjustment(kpi.getMinAdjustment())
                .maxKlik(kpi.getMaxKlik())
                .minKlik(kpi.getMinKlik())
                .minBidSearch(kpi.getMinBidSearch())
                .minBidReco(kpi.getMinBidReco())
                .multiplier(kpi.getMultiplier())
                .build();

        return MerchantResponseDto.builder()
                .id(merchant.getId())
                .merchantName(merchant.getMerchantName())
                .sessionPath(merchant.getSessionPath())
                .merchantShopeeId(merchant.getMerchantShopeeId())
                .createdAt(merchant.getCreatedAt())
                .userId(merchant.getUser().getId())
                .kpi(
                    kpiResp
                ).build();

    }
}
