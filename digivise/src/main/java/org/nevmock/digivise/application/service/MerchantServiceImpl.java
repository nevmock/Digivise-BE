package org.nevmock.digivise.application.service;

import lombok.RequiredArgsConstructor;
import org.nevmock.digivise.application.dto.merchant.MerchantRequestDto;
import org.nevmock.digivise.application.dto.merchant.MerchantResponseDto;
import org.nevmock.digivise.domain.model.Merchant;
import org.nevmock.digivise.domain.model.User;
import org.nevmock.digivise.domain.port.in.MerchantService;
import org.nevmock.digivise.domain.port.out.MerchantRepository;
import org.nevmock.digivise.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;

    @Override
    public MerchantResponseDto createMerchant(MerchantRequestDto merchant) {
        User user = userRepository.findById(merchant.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + merchant.getUserId()));

        Merchant newMerchant = new Merchant();

        newMerchant.setId(UUID.randomUUID());
        newMerchant.setMerchantName(merchant.getMerchantName());
        newMerchant.setUser(user);
        newMerchant.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        newMerchant.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        merchantRepository.save(newMerchant);

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
        merchantToUpdate.setUser(userRepository.findById(updatedMerchant.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + updatedMerchant.getUserId())));
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
                .createdAt(merchant.getCreatedAt().toString())
                .userId(merchant.getUser().getId())
                .build();
    }
}
