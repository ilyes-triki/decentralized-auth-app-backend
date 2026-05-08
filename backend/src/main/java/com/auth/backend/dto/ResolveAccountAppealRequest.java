package com.auth.backend.dto;

import lombok.Data;

@Data
public class ResolveAccountAppealRequest {
    private boolean approve;
    private String adminNote;
}
