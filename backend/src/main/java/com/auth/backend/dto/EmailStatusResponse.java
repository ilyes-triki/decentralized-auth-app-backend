package com.auth.backend.dto;

public class EmailStatusResponse {
    private final boolean verified;
    private final boolean linked;
    private final boolean accountBlocked;
    private final String email;

    public EmailStatusResponse(boolean verified, boolean linked, boolean accountBlocked, String email) {
        this.verified = verified;
        this.linked = linked;
        this.accountBlocked = accountBlocked;
        this.email = email;
    }

    public boolean isVerified() {
        return verified;
    }

    public boolean isLinked() {
        return linked;
    }

    public boolean isAccountBlocked() {
        return accountBlocked;
    }

    public String getEmail() {
        return email;
    }
}
