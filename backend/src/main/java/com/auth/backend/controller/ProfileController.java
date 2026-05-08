package com.auth.backend.controller;

import com.auth.backend.model.UserAccount;
import com.auth.backend.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserAccountRepository userAccountRepository;

    public ProfileController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        String address = authentication.getName().toLowerCase();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("address", address);
        userAccountRepository.findById(address).ifPresent(u -> {
            if (u.getEmail() != null) {
                body.put("email", u.getEmail());
            }
            body.put("emailVerified", u.isEmailVerified());
            if (u.getLastLoginIp() != null) {
                body.put("lastLoginIp", u.getLastLoginIp());
            }
        });
        return body;
    }
}
