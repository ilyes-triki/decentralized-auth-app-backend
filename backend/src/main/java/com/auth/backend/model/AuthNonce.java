package com.auth.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auth_nonces")
public class AuthNonce {

    @Id
    @Column(name = "address", nullable = false, length = 128)
    private String address;

    @Column(name = "nonce", nullable = false, length = 512)
    private String nonce;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
