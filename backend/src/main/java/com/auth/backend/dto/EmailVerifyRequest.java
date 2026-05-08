package com.auth.backend.dto;

import lombok.Data;

@Data
public class EmailVerifyRequest {
    private String email;
    private String otp;
}
