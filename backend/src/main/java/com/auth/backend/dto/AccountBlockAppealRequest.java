package com.auth.backend.dto;

import lombok.Data;

@Data
public class AccountBlockAppealRequest {
    private String walletAddress;
    private String email;
    private String justification;
    private String evidenceUrl;
}
