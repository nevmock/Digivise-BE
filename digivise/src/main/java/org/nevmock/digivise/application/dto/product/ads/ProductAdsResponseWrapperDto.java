//package org.nevmock.digivise.application.dto.product.ads;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class ProductAdsResponseWrapperDto {
//    private Long campaignId;
//    private LocalDateTime from;
//    private LocalDateTime to;
//    private String shopeeFrom;
//    private String shopeeTo;
//    private List<ProductAdsResponseDto> data;
//}

package org.nevmock.digivise.application.dto.product.ads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAdsResponseWrapperDto {
    private Long campaignId;
    // The two date ranges for comparison
    private LocalDateTime from1;
    private LocalDateTime to1;
    private LocalDateTime from2;
    private LocalDateTime to2;

    // The data list will contain a single aggregated and compared result
    private List<ProductAdsResponseDto> data;
}