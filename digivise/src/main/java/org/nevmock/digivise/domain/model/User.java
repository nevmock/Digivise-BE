package org.nevmock.digivise.domain.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Column(nullable = false, length = 255)
    public String password;
    @Id
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID id;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(nullable = false, unique = true, length = 255)
    private String username;
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "deleted_at")
    private Timestamp deletedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Merchant> merchants;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_merchant_id")
    private Merchant activeMerchant;
}