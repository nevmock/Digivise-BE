package org.nevmock.digivise.domain.port.in;

import org.nevmock.digivise.application.dto.merchant.MerchantInfoResponseDto;
import org.nevmock.digivise.application.dto.merchant.MerchantRequestDto;
import org.nevmock.digivise.application.dto.merchant.MerchantResponseDto;

import java.util.List;
import java.util.UUID;

public interface MerchantService {
    MerchantResponseDto createMerchant(MerchantRequestDto merchant);

    List<MerchantResponseDto> getAllMerchants();

    MerchantResponseDto getMerchantById(UUID merchantId);

    List<MerchantResponseDto> getMerchantsByUserId(UUID userId);

    MerchantResponseDto updateMerchant(UUID merchantId, MerchantRequestDto updatedMerchant);

    void deleteMerchant(UUID merchantId);

    void deleteMerchantByUserId(UUID userId);

    Boolean loginMerchant(String username, String password);

    MerchantInfoResponseDto otpLoginMerchant(String username, UUID merchantId, String otpCode);
}
