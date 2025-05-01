package org.nevmock.digivise.application.dto.merchant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantResponseDto {
    private UUID id;

    private String merchantName;

    private String sessionPath;

    private String merchantShopeeId;

    private String createdAt;

    private String updatedAt;

    private UUID userId;
}