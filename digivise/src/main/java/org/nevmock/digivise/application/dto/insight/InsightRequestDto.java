package org.nevmock.digivise.application.dto.insight;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InsightRequestDto {
    private String merchantId;
    private String adsId;
    private Long campaignId;
    private String insight;
}
