package org.nevmock.digivise.application.dto.kpi;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KPIResponseDto {
    private UUID id;
    private UUID merchantId;
    private UUID userId;

    private Double maxCpc;
    private Double maxAcos;
    private Double cpcScaleFactor;
    private Double acosScaleFactor;
    private Double maxKlik;
    private Double minKlik;
    private Double minBidSearch;
    private Double minBidReco;
    private Double multiplier;
}