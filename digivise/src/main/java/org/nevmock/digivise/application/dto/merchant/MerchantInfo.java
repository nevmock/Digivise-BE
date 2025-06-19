package org.nevmock.digivise.application.dto.merchant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MerchantInfo {
    @JsonProperty("shop_id")
    private long shopId;
    @JsonProperty("shop_region")
    private String shopRegion;
    private String name;
    @JsonProperty("cb_option")
    private int cbOption;
    @JsonProperty("is_sip_primary")
    private boolean sipPrimary;
    @JsonProperty("official_shop")
    private boolean officialShop;
    @JsonProperty("is_3pf_shop")
    private boolean thirdPartyShop;
    @JsonProperty("shop_warehouse_flag")
    private int warehouseFlag;
    @JsonProperty("is_sip_affiliated")
    private boolean sipAffiliated;
    @JsonProperty("is_direct_shop")
    private boolean directShop;
    @JsonProperty("linked_main_shop")
    private Long linkedMainShop;
    @JsonProperty("is_main_shop")
    private boolean mainShop;
    @JsonProperty("linked_direct_shops")
    private java.util.List<Long> linkedDirectShops;
}