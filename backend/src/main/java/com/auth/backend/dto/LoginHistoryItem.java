package com.auth.backend.dto;

import java.time.Instant;

public class LoginHistoryItem {
    private final Long id;
    private final String address;
    private final boolean successful;
    private final String failureReason;
    private final Instant createdAt;

    public LoginHistoryItem(Long id, String address, boolean successful, String failureReason, Instant createdAt) {
        this.id = id;
        this.address = address;
        this.successful = successful;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
