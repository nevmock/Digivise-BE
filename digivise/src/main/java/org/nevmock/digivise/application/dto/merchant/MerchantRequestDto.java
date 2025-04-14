package org.nevmock.digivise.application.dto.merchant;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

public class MerchantRequestDto {
    @Getter
    @Setter
    private UUID userId;

    @Getter
    @Setter
    private String merchantName;

    @Getter
    @Setter
    private String sessionPath;
}