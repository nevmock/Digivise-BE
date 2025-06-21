package org.nevmock.digivise.application.dto.merchant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MerchantOtpLoginDto {
    private String username;
    private String merchantId;
    private String otp;
}
