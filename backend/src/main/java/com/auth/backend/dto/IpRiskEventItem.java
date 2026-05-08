package com.auth.backend.dto;

import java.time.Instant;

public class IpRiskEventItem {
    private final Long id;
    private final String ip;
    private final String riskLevel;
    private final String reason;
    private final String path;
    private final String httpMethod;
    private final String requestId;
    private final Instant createdAt;
    private final String details;

    public IpRiskEventItem(
            Long id,
            String ip,
            String riskLevel,
            String reason,
            String path,
            String httpMethod,
            String requestId,
            Instant createdAt,
            String details
    ) {
        this.id = id;
        this.ip = ip;
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.path = path;
        this.httpMethod = httpMethod;
        this.requestId = requestId;
        this.createdAt = createdAt;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getReason() {
        return reason;
    }

    public String getPath() {
        return path;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getRequestId() {
        return requestId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getDetails() {
        return details;
    }
}
