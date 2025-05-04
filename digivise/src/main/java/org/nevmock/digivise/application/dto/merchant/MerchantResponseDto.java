package org.nevmock.digivise.application.dto.merchant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalTime;
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

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private UUID userId;
}