package com.auth.backend.controller;

import com.auth.backend.dto.EmailStartRequest;
import com.auth.backend.dto.EmailStatusResponse;
import com.auth.backend.dto.EmailVerifyRequest;
import com.auth.backend.dto.EmailVerifyResponse;
import com.auth.backend.dto.AccountBlockAppealRequest;
import com.auth.backend.dto.LoginRequest;
import com.auth.backend.dto.LoginResponse;
import com.auth.backend.security.ClientIpResolver;
import com.auth.backend.security.AccountBlockedException;
import com.auth.backend.security.JwtService;
import com.auth.backend.model.UserAccount;
import com.auth.backend.service.AuthService;
import com.auth.backend.service.AccountAppealService;
import com.auth.backend.service.AccountStatusService;
import com.auth.backend.service.EmailAuthService;
import com.auth.backend.service.IpAuthFailureTracker;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final JwtService jwtService;
    private final EmailAuthService emailAuthService;
    private final IpAuthFailureTracker ipAuthFailureTracker;
    private final AccountStatusService accountStatusService;
    private final AccountAppealService accountAppealService;
    private final boolean trustForwardedFor;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            EmailAuthService emailAuthService,
            IpAuthFailureTracker ipAuthFailureTracker,
            AccountStatusService accountStatusService,
            AccountAppealService accountAppealService,
            @Value("${app.security.ip.trust-x-forwarded-for:false}") boolean trustForwardedFor
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.emailAuthService = emailAuthService;
        this.ipAuthFailureTracker = ipAuthFailureTracker;
        this.accountStatusService = accountStatusService;
        this.accountAppealService = accountAppealService;
        this.trustForwardedFor = trustForwardedFor;
    }

    @PostMapping("/email/start")
    public Map<String, String> startEmail(@RequestBody EmailStartRequest body) {
        emailAuthService.startEmailOtp(body.getEmail());
        return Map.of(
                "message",
                "If the address is valid, a verification code was issued. In development, check server logs (logger EMAIL_OTP_DEV)."
        );
    }

    @PostMapping("/email/verify")
    public EmailVerifyResponse verifyEmail(@RequestBody EmailVerifyRequest body) {
        String ticket = emailAuthService.verifyEmailOtpAndIssueTicket(body.getEmail(), body.getOtp());
        return new EmailVerifyResponse(ticket, emailAuthService.getTicketTtlSeconds());
    }

    @GetMapping("/email-status")
    public EmailStatusResponse getEmailStatus(@RequestParam String address) {
        UserAccount account = authService.findUserAccount(address).orElse(null);
        if (account == null) {
            return new EmailStatusResponse(false, false, false, null);
        }
        boolean linked = account.getEmail() != null && !account.getEmail().isBlank();
        boolean verified = linked && account.isEmailVerified();
        boolean accountBlocked = "BLOCKED".equalsIgnoreCase(account.getAccountStatus());
        return new EmailStatusResponse(verified, linked, accountBlocked, account.getEmail());
    }

    @GetMapping("/nonce")
    public String getNonce(@RequestParam String address, @RequestParam(value = "emailTicket", required = false) String emailTicket) {
        return authService.generateNonce(address, emailTicket);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) throws Exception {
        String clientIp = ClientIpResolver.resolve(httpRequest, trustForwardedFor);
        try {
            logger.info("Login attempt for address={}", request.getAddress());

            String emailTicket = request.getEmailTicket();
            boolean hasEmailTicket = emailTicket != null && !emailTicket.isBlank();
            if (!hasEmailTicket && !authService.hasVerifiedEmailForWallet(request.getAddress())) {
                authService.recordLoginHistory(request.getAddress(), false, "Missing email ticket", clientIp);
                throw new RuntimeException("Email verification is required before wallet login");
            }

            String storedNonce = authService.getNonce(request.getAddress());
            if (storedNonce == null) {
                authService.recordLoginHistory(request.getAddress(), false, "Nonce not found", clientIp);
                throw new RuntimeException("Nonce not found (expired or already used)");
            }

            if (!storedNonce.equals(request.getMessage())) {
                authService.recordLoginHistory(request.getAddress(), false, "Nonce mismatch", clientIp);
                throw new RuntimeException("Nonce mismatch");
            }

            if (!authService.nonceMatchesEmailTicket(request.getAddress(), emailTicket)) {
                authService.recordLoginHistory(request.getAddress(), false, "Email ticket mismatch", clientIp);
                throw new RuntimeException("Email verification ticket does not match this sign-in session");
            }

            String recovered = authService.recoverAddress(
                    request.getMessage(),
                    request.getSignature()
            );

            if (!recovered.equalsIgnoreCase(request.getAddress())) {
                authService.recordLoginHistory(request.getAddress(), false, "Invalid signature", clientIp);
                throw new RuntimeException("Invalid signature");
            }

            if (accountStatusService.isBlocked(request.getAddress())) {
                authService.recordLoginHistory(request.getAddress(), false, "Account blocked", clientIp);
                throw new AccountBlockedException("Your account is blocked. Submit an appeal for review.");
            }

            authService.removeNonce(request.getAddress());
            String role = hasEmailTicket
                    ? authService.bindEmailAndPersistUser(request.getAddress(), emailTicket, clientIp)
                    : authService.persistTrustedWalletLogin(request.getAddress(), clientIp);
            authService.recordLoginHistory(request.getAddress(), true, null, clientIp);
            authService.consumeEmailTicket(emailTicket);
            String token = jwtService.generateToken(request.getAddress(), role);
            logger.info("Login success for address={} role={}", request.getAddress(), role);
            return new LoginResponse(request.getAddress(), role, token);
        } catch (RuntimeException ex) {
            ipAuthFailureTracker.recordFailure(clientIp);
            logger.warn("Login failed for address={} reason={}", request.getAddress(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            ipAuthFailureTracker.recordFailure(clientIp);
            authService.recordLoginHistory(request.getAddress(), false, "Unexpected error", clientIp);
            logger.error("Unexpected login error for address={}", request.getAddress(), ex);
            throw ex;
        }
    }

    @PostMapping("/block-appeals")
    public Map<String, Object> submitBlockAppeal(@RequestBody AccountBlockAppealRequest body) {
        var appeal = accountAppealService.submitAppeal(
                body.getWalletAddress(),
                body.getEmail(),
                body.getJustification(),
                body.getEvidenceUrl()
        );
        return Map.of(
                "status", "submitted",
                "appealId", appeal.getId(),
                "walletAddress", appeal.getWalletAddress()
        );
    }
}
