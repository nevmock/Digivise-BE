package org.nevmock.digivise.application.dto.merchant;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
public class MerchantResponseDto {
    @Getter
    private UUID id;

    @Getter
    private String merchantName;

    @Getter
    private String sessionPath;

    @Getter
    private String createdAt;

    @Getter
    private String updatedAt;

    @Getter
    private UUID userId;

    public MerchantResponseDto(UUID id, String merchantName, String sessionPath, String createdAt, String updatedAt, UUID userId) {
        this.id = id;
        this.merchantName = merchantName;
        this.sessionPath = sessionPath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.userId = userId;
    }
}