package com.auth.backend.dto;

import lombok.Data;

@Data
public class LoginRequest {

    private String address;
    private String signature;
    private String message;

}