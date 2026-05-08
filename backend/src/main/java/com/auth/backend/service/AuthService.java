package com.auth.backend.service;

import com.auth.backend.model.AuthNonce;
import com.auth.backend.model.EmailTicket;
import com.auth.backend.model.LoginHistory;
import com.auth.backend.model.UserAccount;
import com.auth.backend.repository.AuthNonceRepository;
import com.auth.backend.repository.LoginHistoryRepository;
import com.auth.backend.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final AuthNonceRepository authNonceRepository;
    private final UserAccountRepository userAccountRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final EmailAuthService emailAuthService;
    private final Set<String> adminWallets;
    private final long nonceTtlSeconds;

    public AuthService(
            AuthNonceRepository authNonceRepository,
            UserAccountRepository userAccountRepository,
            LoginHistoryRepository loginHistoryRepository,
            EmailAuthService emailAuthService,
            @Value("${app.auth.admin-wallets:}") String adminWalletsConfig,
            @Value("${app.auth.nonce-ttl-seconds:300}") long nonceTtlSeconds
    ) {
        this.authNonceRepository = authNonceRepository;
        this.userAccountRepository = userAccountRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.emailAuthService = emailAuthService;
        this.adminWallets = Arrays.stream(adminWalletsConfig.split(","))
                .map(this::normalizeAddress)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        this.nonceTtlSeconds = nonceTtlSeconds;
    }

    private String normalizeAddress(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    @Transactional
    public String generateNonce(String address, String emailTicket) {
        String normalizedAddress = normalizeAddress(address);
        if (normalizedAddress.isBlank()) {
            throw new RuntimeException("Address is required");
        }
        String nonceEmailTicket = null;
        if (emailTicket != null && !emailTicket.isBlank()) {
            EmailTicket ticket = emailAuthService.findValidTicket(emailTicket)
                    .orElseThrow(() -> new RuntimeException("Invalid or expired email verification"));
            nonceEmailTicket = ticket.getToken();
        } else if (!hasVerifiedEmailForWallet(normalizedAddress)) {
            throw new RuntimeException("Email verification is required before wallet login");
        }

        String nonce = "Login to my app: " + UUID.randomUUID();

        AuthNonce authNonce = new AuthNonce();
        authNonce.setAddress(normalizedAddress);
        authNonce.setNonce(nonce);
        authNonce.setExpiresAt(Instant.now().plusSeconds(nonceTtlSeconds));
        authNonce.setEmailTicket(nonceEmailTicket);
        authNonceRepository.save(authNonce);

        return nonce;
    }

    public String getNonce(String address) {
        String normalizedAddress = normalizeAddress(address);
        if (normalizedAddress.isBlank()) {
            return null;
        }

        return authNonceRepository.findById(normalizedAddress)
                .map(authNonce -> {
                    if (authNonce.getExpiresAt().isBefore(Instant.now())) {
                        authNonceRepository.deleteById(normalizedAddress);
                        return null;
                    }
                    return authNonce.getNonce();
                })
                .orElse(null);
    }

    /** Nonce row must have been created with the same email ticket (or both empty for trusted returning wallet login). */
    public boolean nonceMatchesEmailTicket(String address, String emailTicket) {
        String normalizedAddress = normalizeAddress(address);
        if (normalizedAddress.isBlank()) {
            return false;
        }
        String normalizedTicket = (emailTicket == null || emailTicket.isBlank()) ? null : emailTicket;
        return authNonceRepository.findById(normalizedAddress)
                .map(n -> {
                    String nonceTicket = n.getEmailTicket();
                    if (nonceTicket == null || nonceTicket.isBlank()) {
                        return normalizedTicket == null;
                    }
                    return nonceTicket.equals(normalizedTicket);
                })
                .orElse(false);
    }

    public boolean hasVerifiedEmailForWallet(String address) {
        String normalizedAddress = normalizeAddress(address);
        if (normalizedAddress.isBlank()) {
            return false;
        }
        return userAccountRepository.findById(normalizedAddress)
                .map(u -> u.isEmailVerified() && u.getEmail() != null && !u.getEmail().isBlank())
                .orElse(false);
    }

    public Optional<UserAccount> findUserAccount(String address) {
        String normalizedAddress = normalizeAddress(address);
        if (normalizedAddress.isBlank()) {
            return Optional.empty();
        }
        return userAccountRepository.findById(normalizedAddress);
    }

    public void removeNonce(String address) {
        String normalizedAddress = normalizeAddress(address);
        if (!normalizedAddress.isBlank()) {
            authNonceRepository.deleteById(normalizedAddress);
        }
    }

    public String recoverAddress(String message, String signature) throws Exception {

        byte[] signatureBytes = org.web3j.utils.Numeric.hexStringToByteArray(signature);

        if (signatureBytes.length != 65) {
            throw new RuntimeException("Invalid signature length");
        }

        byte v = signatureBytes[64];
        if (v < 27) {
            v += 27;
        }

        byte[] r = new byte[32];
        byte[] s = new byte[32];

        System.arraycopy(signatureBytes, 0, r, 0, 32);
        System.arraycopy(signatureBytes, 32, s, 0, 32);

        Sign.SignatureData sigData = new Sign.SignatureData(v, r, s);

        BigInteger publicKey = Sign.signedPrefixedMessageToKey(
                message.getBytes(),
                sigData
        );

        return "0x" + Keys.getAddress(publicKey);
    }

    public String getRole(String address) {
        String normalizedAddress = normalizeAddress(address);
        if (normalizedAddress.isBlank()) {
            return "user";
        }

        if (adminWallets.contains(normalizedAddress)) {
            return "admin";
        }

        return userAccountRepository.findById(normalizedAddress)
                .map(UserAccount::getRole)
                .orElse("user");
    }

    /**
     * After signature verification: bind verified email to wallet, enforce uniqueness,
     * persist role + profile fields.
     */
    @Transactional
    public String bindEmailAndPersistUser(String address, String emailTicket, String clientIp) {
        String normalizedAddress = normalizeAddress(address);
        EmailTicket ticket = emailAuthService.findValidTicket(emailTicket)
                .orElseThrow(() -> new RuntimeException("Invalid or expired email verification"));
        String ticketEmail = ticket.getEmail();

        Optional<UserAccount> otherWallet = userAccountRepository.findByEmail(ticketEmail);
        if (otherWallet.isPresent() && !otherWallet.get().getAddress().equalsIgnoreCase(normalizedAddress)) {
            throw new RuntimeException("This email is already linked to another wallet address");
        }

        UserAccount userAccount = userAccountRepository.findById(normalizedAddress)
                .orElseGet(UserAccount::new);
        userAccount.setAddress(normalizedAddress);

        if (userAccount.getEmail() != null
                && !userAccount.getEmail().equalsIgnoreCase(ticketEmail)) {
            throw new RuntimeException("This wallet is registered to a different email address");
        }

        String role = getRole(normalizedAddress);
        userAccount.setRole(role);
        userAccount.setEmail(ticketEmail);
        userAccount.setEmailVerified(true);
        if (clientIp != null && !clientIp.isBlank()) {
            userAccount.setLastLoginIp(clientIp);
        }
        userAccountRepository.save(userAccount);
        return role;
    }

    @Transactional
    public void consumeEmailTicket(String emailTicket) {
        if (emailTicket == null || emailTicket.isBlank()) {
            return;
        }
        emailAuthService.consumeTicket(emailTicket);
    }

    @Transactional
    public String persistTrustedWalletLogin(String address, String clientIp) {
        String normalizedAddress = normalizeAddress(address);
        UserAccount userAccount = userAccountRepository.findById(normalizedAddress)
                .orElseThrow(() -> new RuntimeException("Email verification is required before wallet login"));
        if (!userAccount.isEmailVerified() || userAccount.getEmail() == null || userAccount.getEmail().isBlank()) {
            throw new RuntimeException("Email verification is required before wallet login");
        }
        String role = getRole(normalizedAddress);
        userAccount.setRole(role);
        if (clientIp != null && !clientIp.isBlank()) {
            userAccount.setLastLoginIp(clientIp);
        }
        userAccountRepository.save(userAccount);
        return role;
    }

    public void recordLoginHistory(String address, boolean successful, String failureReason) {
        recordLoginHistory(address, successful, failureReason, null);
    }

    public void recordLoginHistory(String address, boolean successful, String failureReason, String clientIp) {
        LoginHistory loginHistory = new LoginHistory();
        loginHistory.setAddress(normalizeAddress(address));
        loginHistory.setSuccessful(successful);
        loginHistory.setFailureReason(failureReason);
        loginHistory.setClientIp(clientIp);
        loginHistoryRepository.save(loginHistory);
    }
}
