package com.auth.backend.controller;

import com.auth.backend.dto.LoginRequest;
import com.auth.backend.dto.LoginResponse;
import com.auth.backend.security.JwtService;
import org.springframework.web.bind.annotation.*;
import com.auth.backend.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {


    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @GetMapping("/nonce")
    public String getNonce(@RequestParam String address) {
        return authService.generateNonce(address);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) throws Exception {
        try {
            logger.info("Login attempt for address={}", request.getAddress());

            String storedNonce = authService.getNonce(request.getAddress());
            if (storedNonce == null) {
                authService.recordLoginHistory(request.getAddress(), false, "Nonce not found");
                throw new RuntimeException("Nonce not found (expired or already used)");
            }

            if (!storedNonce.equals(request.getMessage())) {
                authService.recordLoginHistory(request.getAddress(), false, "Nonce mismatch");
                throw new RuntimeException("Nonce mismatch");
            }

            String recovered = authService.recoverAddress(
                    request.getMessage(),
                    request.getSignature()
            );

            if (!recovered.equalsIgnoreCase(request.getAddress())) {
                authService.recordLoginHistory(request.getAddress(), false, "Invalid signature");
                throw new RuntimeException("Invalid signature");
            }

            authService.removeNonce(request.getAddress());
            String role = authService.getRole(request.getAddress());
            authService.saveOrUpdateUserRole(request.getAddress(), role);
            authService.recordLoginHistory(request.getAddress(), true, null);
            String token = jwtService.generateToken(request.getAddress(), role);
            logger.info("Login success for address={} role={}", request.getAddress(), role);
            return new LoginResponse(request.getAddress(), role, token);
        } catch (RuntimeException ex) {
            logger.warn("Login failed for address={} reason={}", request.getAddress(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            authService.recordLoginHistory(request.getAddress(), false, "Unexpected error");
            logger.error("Unexpected login error for address={}", request.getAddress(), ex);
            throw ex;
        }
    }
}