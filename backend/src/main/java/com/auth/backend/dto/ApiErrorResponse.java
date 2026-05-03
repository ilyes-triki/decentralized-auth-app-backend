package com.auth.backend.dto;

import com.auth.backend.security.RequestIdFilter;
import org.slf4j.MDC;

import java.time.Instant;

/**
 * Structured API error (Step 6): machine-readable code + correlation id for support and logs.
 */
public class ApiErrorResponse {
    private final String message;
    private final int status;
    private final Instant timestamp;
    private final String errorCode;
    private final String requestId;

    public ApiErrorResponse(String message, int status, String errorCode, String requestId) {
        this.message = message;
        this.status = status;
        this.timestamp = Instant.now();
        this.errorCode = errorCode != null ? errorCode : "ERROR";
        this.requestId = requestId != null ? requestId : "";
    }

    /** Error with code; {@code requestId} taken from MDC when present. */
    public ApiErrorResponse(String message, int status, String errorCode) {
        this(message, status, errorCode, currentRequestId());
    }

    public ApiErrorResponse(String message, int status) {
        this(message, status, "ERROR");
    }

    private static String currentRequestId() {
        String v = MDC.get(RequestIdFilter.REQUEST_ID);
        return v != null ? v : "";
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getRequestId() {
        return requestId;
    }
}
