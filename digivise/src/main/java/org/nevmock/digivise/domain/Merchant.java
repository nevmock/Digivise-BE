package org.nevmock.digivise.domain;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "merchant")
public class Merchant {

    @Id
    @GeneratedValue
    @Column(name = "merchant_id", nullable = false, unique = true)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "merchant_name", nullable = false, length = 255)
    private String merchantName;

    @Column(name = "session_path", nullable = false, length = 255)
    private String sessionPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @Column(name = "deleted_at")
    private Timestamp deletedAt;

    public Merchant() {
    }

    public Merchant(UUID id, User user, String merchantName, String sessionPath, Timestamp createdAt, Timestamp updatedAt, Timestamp deletedAt) {
        this.id = id;
        this.user = user;
        this.merchantName = merchantName;
        this.sessionPath = sessionPath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getSessionPath() {
        return sessionPath;
    }

    public void setSessionPath(String sessionPath) {
        this.sessionPath = sessionPath;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Timestamp getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Timestamp deletedAt) {
        this.deletedAt = deletedAt;
    }
}