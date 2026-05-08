package com.auth.backend.service;

import com.auth.backend.model.AccountBlockAppeal;
import com.auth.backend.model.UserAccount;
import com.auth.backend.repository.AccountBlockAppealRepository;
import com.auth.backend.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AccountStatusService {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_BLOCKED = "BLOCKED";
    public static final String APPEAL_OPEN = "OPEN";
    public static final String APPEAL_APPROVED = "APPROVED";
    public static final String APPEAL_REJECTED = "REJECTED";

    private final UserAccountRepository userAccountRepository;
    private final AccountBlockAppealRepository appealRepository;

    public AccountStatusService(UserAccountRepository userAccountRepository, AccountBlockAppealRepository appealRepository) {
        this.userAccountRepository = userAccountRepository;
        this.appealRepository = appealRepository;
    }

    public Optional<UserAccount> findByWallet(String walletAddress) {
        if (walletAddress == null || walletAddress.isBlank()) {
            return Optional.empty();
        }
        return userAccountRepository.findById(walletAddress.trim().toLowerCase());
    }

    public boolean isBlocked(String walletAddress) {
        return findByWallet(walletAddress)
                .map(u -> STATUS_BLOCKED.equalsIgnoreCase(u.getAccountStatus()))
                .orElse(false);
    }

    @Transactional
    public void blockAccount(String walletAddress) {
        UserAccount user = findByWallet(walletAddress)
                .orElseThrow(() -> new IllegalArgumentException("Wallet account not found"));
        user.setAccountStatus(STATUS_BLOCKED);
        userAccountRepository.save(user);
    }

    @Transactional
    public void unblockAccount(String walletAddress, String adminPrincipal) {
        UserAccount user = findByWallet(walletAddress)
                .orElseThrow(() -> new IllegalArgumentException("Wallet account not found"));
        user.setAccountStatus(STATUS_ACTIVE);
        userAccountRepository.save(user);

        List<AccountBlockAppeal> openAppeals = appealRepository.findByWalletAddressAndStatus(user.getAddress(), APPEAL_OPEN);
        for (AccountBlockAppeal appeal : openAppeals) {
            appeal.setStatus(APPEAL_APPROVED);
            appeal.setAdminNote("Unblocked by admin");
            appeal.setResolvedAt(Instant.now());
            appeal.setResolvedBy(adminPrincipal);
        }
        appealRepository.saveAll(openAppeals);
    }
}
