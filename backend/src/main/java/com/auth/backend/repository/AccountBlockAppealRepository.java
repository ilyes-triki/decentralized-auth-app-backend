package com.auth.backend.repository;

import com.auth.backend.model.AccountBlockAppeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountBlockAppealRepository extends JpaRepository<AccountBlockAppeal, Long> {
    boolean existsByWalletAddressAndStatus(String walletAddress, String status);

    List<AccountBlockAppeal> findTop200ByOrderByCreatedAtDesc();

    List<AccountBlockAppeal> findTop200ByStatusOrderByCreatedAtDesc(String status);

    List<AccountBlockAppeal> findByWalletAddressAndStatus(String walletAddress, String status);
}
