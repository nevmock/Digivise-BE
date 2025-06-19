package org.nevmock.digivise.application.dto.merchant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//public class MerchantInfoResponseDto {
//    private int code;
//    private String status;
//    private String message;
//    private String debug_message;
//    private long shop_id;
//    private String shop_region;
//    private String name;
//    private int cb_option;
//    private boolean is_sip_primary;
//    private boolean official_shop;
//    private boolean is_3pf_shop;
//    private int shop_warehouse_flag;
//    private boolean is_sip_affiliated;
//    private boolean is_direct_shop;
//    private Boolean is_main_shop;
//}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MerchantInfoResponseDto {
    private int code;
    private String status;
    private String message;
    private OtpData data;
}