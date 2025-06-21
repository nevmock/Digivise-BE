package org.nevmock.digivise.application.dto.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.nevmock.digivise.application.dto.merchant.MerchantResponseDto;
import org.nevmock.digivise.domain.model.Merchant;

import java.util.List;
import java.util.UUID;

@Builder
@Getter
@Setter
public class LoginResponseDto {
    private String accessToken;
    private String refreshToken;

    private UUID userId;
    private String username;
    private List<MerchantResponseDto> merchants;
    private MerchantResponseDto activeMerchant;
}
