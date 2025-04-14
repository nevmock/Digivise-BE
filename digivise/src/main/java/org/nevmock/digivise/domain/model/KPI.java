package org.nevmock.digivise.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "kpi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KPI {
    @Id
    @Column(name = "kpi_id", nullable = false, unique = true)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "cpc")
    private Double cpc;

    @Column(name = "acos")
    private Double acos;

    @Column(name = "ctr")
    private Double ctr;
}