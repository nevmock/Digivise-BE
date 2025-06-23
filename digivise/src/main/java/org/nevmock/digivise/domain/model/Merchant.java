package org.nevmock.digivise.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "merchant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant {

    @Id
    @Column(name = "merchant_id", nullable = false, unique = true)
    private UUID id;

    @Column(name = "merchant_shopee_id", nullable = true, unique = false)
    private String merchantShopeeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = true, length = 255)
    private String name;

    @Column(name = "merchant_name", nullable = true, length = 255)
    private String merchantName;

    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<KPI> kpis;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "username", nullable = true, length = 255)
    private String username;

    @Column(name = "password", nullable = true, length = 255)
    private String password;

    @Column(name = "sector_industry", nullable = true, length = 255)
    private String sectorIndustry;

    @Column(name = "office_address", nullable = true, length = 255)
    private String officeAddress;

    @Column(name = "factory_address", nullable = true, length = 255)
    private String factoryAddress;
}