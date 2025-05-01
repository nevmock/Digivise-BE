package org.nevmock.digivise.application.service;

import lombok.RequiredArgsConstructor;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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

        return toDto(newMerchant);
    }

    @Override
    public MerchantResponseDto getMerchantById(UUID merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found with ID: " + merchantId));

        return toDto(merchant);
    }

    @Override
    public List<MerchantResponseDto> getAllMerchants() {
        return merchantRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<MerchantResponseDto> getMerchantsByUserId(UUID userId) {
        List<Merchant> merchant = merchantRepository.findByUserId(userId);

        return merchant.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public MerchantResponseDto updateMerchant(UUID merchantId, MerchantRequestDto updatedMerchant) {
        Optional<Merchant> existingMerchant = merchantRepository.findById(merchantId);

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

        return toDto(merchantToUpdate);
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

    private MerchantResponseDto toDto(Merchant merchant) {
        return MerchantResponseDto.builder()
                .id(merchant.getId())
                .merchantName(merchant.getMerchantName())
                .sessionPath(merchant.getSessionPath())
                .merchantShopeeId(merchant.getMerchantShopeeId())
                .createdAt(merchant.getCreatedAt().toString())
                .userId(merchant.getUser().getId())
                .build();
    }
}
