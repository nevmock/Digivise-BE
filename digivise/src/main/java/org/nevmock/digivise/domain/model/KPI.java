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
    public UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    public Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @Column(name = "max_cpc")
    public Double maxCpc;

    @Column(name = "max_acos")
    public Double maxAcos;

    @Column(name = "cpc_scale_factor")
    public Double cpcScaleFactor;

    @Column(name = "acos_scale_factor")
    public Double acosScaleFactor;

    @Column(name = "max_adjustment")
    public Double maxAdjustment;

    @Column(name = "min_adjustment")
    public Double minAdjustment;

    @Column(name = "max_klik")
    public Double maxKlik;

    @Column(name = "min_klik")
    public Double minKlik;

    @Column(name = "min_bid_search")
    public Double minBidSearch;

    @Column(name = "min_bid_reco")
    public Double minBidReco;

    @Column(name = "multiplier")
    public Double multiplier;
}