package com.auth.backend.service;

import com.auth.backend.model.AuthNonce;
import com.auth.backend.model.LoginHistory;
import com.auth.backend.model.UserAccount;
import com.auth.backend.repository.AuthNonceRepository;
import com.auth.backend.repository.LoginHistoryRepository;
import com.auth.backend.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.*;

import java.time.Instant;
import java.util.UUID;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

import java.math.BigInteger;

@Service
public class AuthService {
    private final AuthNonceRepository authNonceRepository;
    private final UserAccountRepository userAccountRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final Set<String> adminWallets;
    private final long nonceTtlSeconds;

    public AuthService(
            AuthNonceRepository authNonceRepository,
            UserAccountRepository userAccountRepository,
            LoginHistoryRepository loginHistoryRepository,
            @Value("${app.auth.admin-wallets:}") String adminWalletsConfig,
            @Value("${app.auth.nonce-ttl-seconds:300}") long nonceTtlSeconds
    ) {
        this.authNonceRepository = authNonceRepository;
        this.userAccountRepository = userAccountRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.adminWallets = Arrays.stream(adminWalletsConfig.split(","))
                .map(this::normalizeAddress)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        this.nonceTtlSeconds = nonceTtlSeconds;
    }

    private String normalizeAddress(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    public String generateNonce(String address) {
        String normalizedAddress = normalizeAddress(address);
        if (normalizedAddress.isBlank()) {
            throw new RuntimeException("Address is required");
        }

        String nonce = "Login to my app: " + UUID.randomUUID();

        AuthNonce authNonce = new AuthNonce();
        authNonce.setAddress(normalizedAddress);
        authNonce.setNonce(nonce);
        authNonce.setExpiresAt(Instant.now().plusSeconds(nonceTtlSeconds));
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

        org.web3j.crypto.Sign.SignatureData sigData =
                new org.web3j.crypto.Sign.SignatureData(v, r, s);

        BigInteger publicKey = org.web3j.crypto.Sign.signedPrefixedMessageToKey(
                message.getBytes(),
                sigData
        );

        return "0x" + org.web3j.crypto.Keys.getAddress(publicKey);
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

    public void saveOrUpdateUserRole(String address, String role) {
        String normalizedAddress = normalizeAddress(address);
        if (normalizedAddress.isBlank()) {
            return;
        }

        UserAccount userAccount = userAccountRepository.findById(normalizedAddress)
                .orElseGet(UserAccount::new);
        userAccount.setAddress(normalizedAddress);
        userAccount.setRole(role);
        userAccountRepository.save(userAccount);
    }

    public void recordLoginHistory(String address, boolean successful, String failureReason) {
        LoginHistory loginHistory = new LoginHistory();
        loginHistory.setAddress(normalizeAddress(address));
        loginHistory.setSuccessful(successful);
        loginHistory.setFailureReason(failureReason);
        loginHistoryRepository.save(loginHistory);
    }
}