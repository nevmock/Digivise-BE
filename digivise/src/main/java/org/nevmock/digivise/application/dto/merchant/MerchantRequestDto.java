package org.nevmock.digivise.application.dto.merchant;

import lombok.Getter;
import lombok.Setter;

public class MerchantRequestDto {
    @Getter
    @Setter
    private String merchantName;

    @Getter
    @Setter
    private String merchantShopeeId;

    @Getter
    @Setter
    private String sessionPath;
}