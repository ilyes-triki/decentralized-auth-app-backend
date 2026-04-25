package com.auth.backend.controller;

import com.auth.backend.dto.LoginRequest;
import com.auth.backend.dto.LoginResponse;
import org.springframework.web.bind.annotation.*;
import com.auth.backend.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/nonce")
    public String getNonce(@RequestParam String address) {
        return authService.generateNonce(address);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) throws Exception {

        String storedNonce = authService.getNonce(request.getAddress());

        if (storedNonce == null || !storedNonce.equals(request.getMessage())) {
            throw new RuntimeException("Invalid or expired nonce");
        }

        String recovered = authService.recoverAddress(
                request.getMessage(),
                request.getSignature()
        );

        if (!recovered.equalsIgnoreCase(request.getAddress())) {
            throw new RuntimeException("Invalid signature");
        }

        authService.removeNonce(request.getAddress());

        return new LoginResponse(request.getAddress(), "user");
    }
}