package com.auth.backend.dto;

import java.time.Instant;

public record AccountBlockAppealItem(
        Long id,
        String walletAddress,
        String email,
        String justification,
        String evidenceUrl,
        String status,
        String adminNote,
        Instant createdAt,
        Instant resolvedAt,
        String resolvedBy
) {}
