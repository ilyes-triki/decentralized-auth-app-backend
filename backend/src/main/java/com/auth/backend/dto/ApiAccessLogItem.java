package com.auth.backend.dto;

import java.time.Instant;

public record ApiAccessLogItem(
        long id,
        Instant createdAt,
        String requestId,
        String httpMethod,
        String path,
        int statusCode,
        long durationMs,
        String principal
) {
}
