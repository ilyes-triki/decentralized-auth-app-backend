package com.auth.backend.dto;

import java.time.Instant;

public class LoginHistoryItem {
    private final Long id;
    private final String address;
    private final boolean successful;
    private final String failureReason;
    private final Instant createdAt;
    private final String clientIp;

    public LoginHistoryItem(Long id, String address, boolean successful, String failureReason, Instant createdAt) {
        this(id, address, successful, failureReason, createdAt, null);
    }

    public LoginHistoryItem(
            Long id,
            String address,
            boolean successful,
            String failureReason,
            Instant createdAt,
            String clientIp
    ) {
        this.id = id;
        this.address = address;
        this.successful = successful;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.clientIp = clientIp;
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

    public String getClientIp() {
        return clientIp;
    }
}
