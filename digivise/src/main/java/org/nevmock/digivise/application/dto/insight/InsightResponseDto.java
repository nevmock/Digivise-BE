package org.nevmock.digivise.application.dto.insight;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class InsightResponseDto {
    private String adsId;
    private Long campaignId;
    private String insight;
}
