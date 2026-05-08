package com.auth.backend.dto;

import lombok.Data;

@Data
public class BlockIpRequest {
    private String ip;
    private String reason;
    /** When true, block has no automatic expiry. */
    private boolean permanent;
}
