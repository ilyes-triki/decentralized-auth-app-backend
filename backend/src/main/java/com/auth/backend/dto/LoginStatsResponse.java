package com.auth.backend.dto;

public class LoginStatsResponse {
    private final long totalAttempts;
    private final long successfulAttempts;
    private final long failedAttempts;

    public LoginStatsResponse(long totalAttempts, long successfulAttempts, long failedAttempts) {
        this.totalAttempts = totalAttempts;
        this.successfulAttempts = successfulAttempts;
        this.failedAttempts = failedAttempts;
    }

    public long getTotalAttempts() {
        return totalAttempts;
    }

    public long getSuccessfulAttempts() {
        return successfulAttempts;
    }

    public long getFailedAttempts() {
        return failedAttempts;
    }
}
