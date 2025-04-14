package org.nevmock.digivise.application.dto.kpi;

import lombok.*;
import org.nevmock.digivise.domain.model.Merchant;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KPIRequestDto {
    private UUID id;
    private Double cpc;
    private Double acos;
    private Double ctr;
    private Merchant merchant;
}
