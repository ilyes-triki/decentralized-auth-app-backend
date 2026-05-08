package com.auth.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmailVerifyResponse {
    private String emailTicket;
    private long expiresInSeconds;
}
