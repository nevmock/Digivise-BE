package org.nevmock.digivise.application.dto.merchant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MerchantRequestDto {

    private String name;
    private String sectorIndustry;
    private String officeAddress;
    private String factoryAddress;

//    private String merchantName;
//    private String merchantShopeeId;
//    private String username;
//    private String email;
//    private String password;

//    @Getter
//    @Setter
//    private String merchantShopeeId;
//
//    @Getter
//    @Setter
//    private String sessionPath;

}