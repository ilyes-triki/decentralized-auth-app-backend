package com.auth.backend.service;

import com.auth.backend.model.AccountBlockAppeal;
import com.auth.backend.model.UserAccount;
import com.auth.backend.repository.AccountBlockAppealRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AccountAppealService {
    private final AccountStatusService accountStatusService;
    private final AccountBlockAppealRepository appealRepository;

    public AccountAppealService(AccountStatusService accountStatusService, AccountBlockAppealRepository appealRepository) {
        this.accountStatusService = accountStatusService;
        this.appealRepository = appealRepository;
    }

    @Transactional
    public AccountBlockAppeal submitAppeal(String walletAddress, String email, String justification, String evidenceUrl) {
        if (walletAddress == null || walletAddress.isBlank()) {
            throw new IllegalArgumentException("Wallet address is required");
        }
        if (justification == null || justification.trim().length() < 10) {
            throw new IllegalArgumentException("Justification must be at least 10 characters");
        }
        UserAccount user = accountStatusService.findByWallet(walletAddress)
                .orElseThrow(() -> new IllegalArgumentException("Wallet account not found"));
        if (!AccountStatusService.STATUS_BLOCKED.equalsIgnoreCase(user.getAccountStatus())) {
            throw new IllegalArgumentException("Appeal can only be submitted for blocked accounts");
        }
        if (appealRepository.existsByWalletAddressAndStatus(user.getAddress(), AccountStatusService.APPEAL_OPEN)) {
            throw new IllegalArgumentException("An open appeal already exists for this wallet");
        }
        AccountBlockAppeal appeal = new AccountBlockAppeal();
        appeal.setWalletAddress(user.getAddress());
        appeal.setEmail(email != null && !email.isBlank() ? email.trim().toLowerCase() : user.getEmail());
        appeal.setJustification(justification.trim());
        appeal.setEvidenceUrl(evidenceUrl != null && !evidenceUrl.isBlank() ? evidenceUrl.trim() : null);
        appeal.setStatus(AccountStatusService.APPEAL_OPEN);
        return appealRepository.save(appeal);
    }

    public List<AccountBlockAppeal> listAppeals(String status) {
        if (status == null || status.isBlank()) {
            return appealRepository.findTop200ByOrderByCreatedAtDesc();
        }
        return appealRepository.findTop200ByStatusOrderByCreatedAtDesc(status.trim().toUpperCase());
    }

    @Transactional
    public AccountBlockAppeal resolveAppeal(long id, boolean approve, String adminNote, String adminPrincipal) {
        AccountBlockAppeal appeal = appealRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appeal not found"));
        if (!AccountStatusService.APPEAL_OPEN.equalsIgnoreCase(appeal.getStatus())) {
            throw new IllegalArgumentException("Appeal is already resolved");
        }
        appeal.setStatus(approve ? AccountStatusService.APPEAL_APPROVED : AccountStatusService.APPEAL_REJECTED);
        appeal.setAdminNote(adminNote != null && !adminNote.isBlank() ? adminNote.trim() : null);
        appeal.setResolvedAt(Instant.now());
        appeal.setResolvedBy(adminPrincipal);
        appealRepository.save(appeal);

        if (approve) {
            accountStatusService.unblockAccount(appeal.getWalletAddress(), adminPrincipal);
        }
        return appeal;
    }
}
