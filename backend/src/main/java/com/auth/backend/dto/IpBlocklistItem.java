package com.auth.backend.dto;

import java.time.Instant;

public class IpBlocklistItem {
    private final String ip;
    private final String source;
    private final String reason;
    private final Instant blockedUntil;
    private final Instant createdAt;
    private final String createdBy;

    public IpBlocklistItem(
            String ip,
            String source,
            String reason,
            Instant blockedUntil,
            Instant createdAt,
            String createdBy
    ) {
        this.ip = ip;
        this.source = source;
        this.reason = reason;
        this.blockedUntil = blockedUntil;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public String getIp() {
        return ip;
    }

    public String getSource() {
        return source;
    }

    public String getReason() {
        return reason;
    }

    public Instant getBlockedUntil() {
        return blockedUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
