package com.auth.backend.dto;

import lombok.Data;

@Data
public class LoginRequest {

    private String address;
    private String signature;
    private String message;
    /** Issued after successful email OTP verification; optional for wallets already linked to a verified email. */
    private String emailTicket;
}